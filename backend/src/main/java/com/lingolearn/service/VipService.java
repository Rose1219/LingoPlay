package com.lingolearn.service;

import com.lingolearn.common.BusinessException;
import com.lingolearn.entity.Language;
import com.lingolearn.entity.SubscriptionOrder;
import com.lingolearn.entity.User;
import com.lingolearn.payment.AlipayClient;
import com.lingolearn.payment.PaypalClient;
import com.lingolearn.payment.WechatPayClient;
import com.lingolearn.repository.SubscriptionOrderRepository;
import com.lingolearn.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * VIP 会员与订阅支付服务。
 *
 * 安全设计（支付链路四道闸）：
 * 1. 金额服务端定死：套餐价格只存在于服务端，客户端仅传渠道与套餐码；
 * 2. 回调强验签：微信 MD5 验签、支付宝 RSA2 验签、PayPal 服务端 capture 核验；
 * 3. 回调金额核对：通知金额与本地订单金额逐一比对，不一致即拒绝；
 * 4. 幂等发放：订单状态机 PENDING → PAID 单向流转，重复通知不会重复加时长。
 */
@Service
public class VipService {

    private static final Logger log = LoggerFactory.getLogger(VipService.class);

    /** 套餐：¥5 / 月；PayPal 走美元定价（汇率波动不追差，后续可调） */
    public static final BigDecimal PRICE_CNY = new BigDecimal("5.00");
    public static final String PRICE_USD = "0.99";
    public static final String PLAN_CODE = "vip_monthly";
    public static final int PLAN_MONTHS = 1;

    /** 订单 2 小时未支付自动作废 */
    private static final int ORDER_EXPIRE_MINUTES = 120;

    private final UserRepository userRepository;
    private final SubscriptionOrderRepository orderRepository;
    private final WechatPayClient wechatPayClient;
    private final AlipayClient alipayClient;
    private final PaypalClient paypalClient;

    /** 模拟支付开关：仅当支付凭证未配置时用于联调演示，生产环境必须关闭 */
    @Value("${pay.mock-enabled:false}")
    private boolean mockEnabled;

    private final SecureRandom random = new SecureRandom();

    public VipService(UserRepository userRepository,
                      SubscriptionOrderRepository orderRepository,
                      WechatPayClient wechatPayClient,
                      AlipayClient alipayClient,
                      PaypalClient paypalClient) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.wechatPayClient = wechatPayClient;
        this.alipayClient = alipayClient;
        this.paypalClient = paypalClient;
    }

    // ------------------------------------------------------------------ 查询

    /** 会员中心聚合数据：我的 VIP 状态 + 套餐 + 当前可用支付渠道 */
    @Transactional(readOnly = true)
    public Map<String, Object> status(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(401, "用户不存在"));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("vip", isVip(user));
        data.put("vipExpireAt", user.getVipExpireAt());
        data.put("vipMonths", user.getVipMonths() == null ? 0 : user.getVipMonths());
        // 前端据此区分「未开通」与「已过期」：有过购买记录但现在过期
        data.put("expired", user.getVipExpireAt() != null
                && user.getVipExpireAt().isBefore(LocalDateTime.now()));

        List<Map<String, Object>> plans = new ArrayList<>();
        Map<String, Object> plan = new LinkedHashMap<>();
        plan.put("code", PLAN_CODE);
        plan.put("name", "VIP 月卡");
        plan.put("months", PLAN_MONTHS);
        plan.put("priceCny", PRICE_CNY);
        plan.put("priceUsd", PRICE_USD);
        plan.put("perks", java.util.Arrays.asList(
                "方言课程（广东话/四川话/北京话/上海话）",
                "更多语种持续上新",
                "专属 VIP 标识"));
        plans.add(plan);
        data.put("plans", plans);

        // 渠道可用性由服务端判定（凭证是否配置），前端只渲染可用项
        List<Map<String, Object>> channels = new ArrayList<>();
        channels.add(channel("WECHAT", "微信支付", "💬", wechatPayClient.configured()));
        channels.add(channel("ALIPAY", "支付宝", "🅰️", alipayClient.configured()));
        channels.add(channel("PAYPAL", "PayPal / 信用卡", "🅿️", paypalClient.configured()));
        channels.add(channel("MOCK", "演示支付（测试）", "🧪", mockEnabled));
        data.put("channels", channels);
        return data;
    }

    private static Map<String, Object> channel(String code, String name, String icon, boolean enabled) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", code);
        m.put("name", name);
        m.put("icon", icon);
        m.put("enabled", enabled);
        return m;
    }

    @Transactional(readOnly = true)
    public List<SubscriptionOrder> myOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    // ------------------------------------------------------------------ 下单

    /**
     * 创建支付订单并拉起对应渠道。
     *
     * @param channel  WECHAT / ALIPAY / PAYPAL / MOCK
     * @param openid   微信 JSAPI 支付所需 openid（小程序场景；可空，服务端会自动补全）
     * @param baseUrl  当前站点对外地址（用于拼回调/回跳 URL）
     * @param clientIp 下单 IP
     * @return 支付发起信息（按渠道不同：codeUrl / jsapi / redirect / mock）
     */
    @Transactional
    public Map<String, Object> createOrder(Long userId, String channel, String openid,
                                           String baseUrl, String clientIp) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(401, "用户不存在"));

        String ch = channel == null ? "" : channel.trim().toUpperCase();
        SubscriptionOrder order = new SubscriptionOrder();
        order.setOrderNo("LP" + System.currentTimeMillis() + String.format("%06d", random.nextInt(1000000)));
        order.setUserId(userId);
        order.setPlanCode(PLAN_CODE);
        order.setMonths(PLAN_MONTHS);
        order.setExpireAt(LocalDateTime.now().plusMinutes(ORDER_EXPIRE_MINUTES));

        Map<String, Object> pay = new LinkedHashMap<>();
        pay.put("orderNo", order.getOrderNo());

        if ("MOCK".equals(ch)) {
            if (!mockEnabled) {
                throw new BusinessException(403, "演示支付未开启");
            }
            order.setChannel(SubscriptionOrder.Channel.MOCK);
            order.setCurrency("CNY");
            order.setAmount(PRICE_CNY);
            order.setMock(true);
            orderRepository.save(order);
            pay.put("mode", "mock");
            return pay;
        }

        order.setMock(false);

        if ("WECHAT".equals(ch)) {
            if (!wechatPayClient.configured()) {
                throw new BusinessException(503, "微信支付尚未配置商户凭证，请管理员设置 WX_PAY_* 环境变量");
            }
            order.setChannel(SubscriptionOrder.Channel.WECHAT);
            order.setCurrency("CNY");
            order.setAmount(PRICE_CNY);
            orderRepository.save(order);

            // 小程序用户优先使用账号绑定的 openid（JSAPI）
            String payOpenid = (openid == null || openid.trim().isEmpty()) ? user.getOpenid() : openid.trim();
            String notifyUrl = baseUrl + "/api/vip/notify/wechat";
            int fen = PRICE_CNY.movePointRight(2).intValue();
            if (payOpenid != null && !payOpenid.isEmpty()) {
                String prepayId = wechatPayClient.unifiedOrder(order.getOrderNo(),
                        "LingoPlay VIP月卡", fen, "JSAPI", payOpenid, notifyUrl, clientIp);
                if (prepayId == null) {
                    throw new BusinessException(502, "微信支付下单失败，请稍后再试");
                }
                pay.put("mode", "jsapi");
                pay.put("jsapi", wechatPayClient.buildJsapiParams(prepayId));
            } else {
                String codeUrl = wechatPayClient.unifiedOrder(order.getOrderNo(),
                        "LingoPlay VIP月卡", fen, "NATIVE", null, notifyUrl, clientIp);
                if (codeUrl == null) {
                    throw new BusinessException(502, "微信支付下单失败，请稍后再试");
                }
                pay.put("mode", "qrcode");
                pay.put("codeUrl", codeUrl);
            }
            return pay;
        }

        if ("ALIPAY".equals(ch)) {
            if (!alipayClient.configured()) {
                throw new BusinessException(503, "支付宝尚未配置商户凭证，请管理员设置 ALIPAY_* 环境变量");
            }
            order.setChannel(SubscriptionOrder.Channel.ALIPAY);
            order.setCurrency("CNY");
            order.setAmount(PRICE_CNY);
            orderRepository.save(order);
            String url = alipayClient.buildPayUrl(order.getOrderNo(), "LingoPlay VIP月卡",
                    PRICE_CNY.toPlainString());
            if (url == null) {
                throw new BusinessException(502, "支付宝下单失败，请稍后再试");
            }
            pay.put("mode", "redirect");
            pay.put("redirectUrl", url);
            return pay;
        }

        if ("PAYPAL".equals(ch)) {
            if (!paypalClient.configured()) {
                throw new BusinessException(503, "PayPal 尚未配置凭证，请管理员设置 PAYPAL_* 环境变量");
            }
            order.setChannel(SubscriptionOrder.Channel.PAYPAL);
            order.setCurrency("USD");
            order.setAmount(new BigDecimal(PRICE_USD));
            orderRepository.save(order);
            String approve = paypalClient.createOrder(order.getOrderNo(), PRICE_USD,
                    baseUrl + "/api/vip/paypal/return", baseUrl + "/vip?pay=cancel");
            if (approve == null) {
                throw new BusinessException(502, "PayPal 下单失败，请稍后再试");
            }
            pay.put("mode", "redirect");
            pay.put("redirectUrl", approve);
            return pay;
        }

        throw new BusinessException(400, "不支持的支付渠道：" + channel);
    }

    // ------------------------------------------------------------------ 支付确认

    /** 演示支付：仅 mock 渠道订单可用，走与真实回调完全相同的幂等入账逻辑 */
    @Transactional
    public void mockPay(Long userId, String orderNo) {
        if (!mockEnabled) {
            throw new BusinessException(403, "演示支付未开启");
        }
        SubscriptionOrder order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException(404, "订单不存在"));
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作该订单");
        }
        if (order.getChannel() != SubscriptionOrder.Channel.MOCK) {
            throw new BusinessException(403, "该订单不是演示订单");
        }
        order.setTradeNo("MOCK" + System.currentTimeMillis());
        grantIfFirstPay(order);
    }

    /** 微信异步通知入口（已由 VipController 完成验签） */
    @Transactional
    public void onWechatNotify(WechatPayClient.NotifyResult n) {
        SubscriptionOrder order = orderRepository.findByOrderNo(n.orderNo).orElse(null);
        if (order == null) {
            log.warn("wechat notify: unknown order {}", n.orderNo);
            return;
        }
        if (order.getChannel() != SubscriptionOrder.Channel.WECHAT) {
            log.warn("wechat notify: channel mismatch {}", n.orderNo);
            return;
        }
        // 金额核对（分）
        int expectFen = order.getAmount().movePointRight(2).intValue();
        if (n.totalFen != expectFen) {
            log.warn("wechat notify: amount mismatch order={} expect={} actual={}",
                    n.orderNo, expectFen, n.totalFen);
            return;
        }
        order.setTradeNo(n.tradeNo);
        grantIfFirstPay(order);
    }

    /** 支付宝异步通知入口（已验签） */
    @Transactional
    public void onAlipayNotify(AlipayClient.NotifyResult n) {
        SubscriptionOrder order = orderRepository.findByOrderNo(n.orderNo).orElse(null);
        if (order == null) {
            log.warn("alipay notify: unknown order {}", n.orderNo);
            return;
        }
        if (order.getChannel() != SubscriptionOrder.Channel.ALIPAY) {
            log.warn("alipay notify: channel mismatch {}", n.orderNo);
            return;
        }
        try {
            if (new BigDecimal(n.amountYuan).compareTo(order.getAmount()) != 0) {
                log.warn("alipay notify: amount mismatch order={}", n.orderNo);
                return;
            }
        } catch (Exception e) {
            return;
        }
        order.setTradeNo(n.tradeNo);
        grantIfFirstPay(order);
    }

    /** PayPal capture 完成后入账 */
    @Transactional
    public void onPaypalCaptured(String orderNo, String captureId, String amountUsd) {
        SubscriptionOrder order = orderRepository.findByOrderNo(orderNo).orElse(null);
        if (order == null) {
            log.warn("paypal capture: unknown order {}", orderNo);
            return;
        }
        if (order.getChannel() != SubscriptionOrder.Channel.PAYPAL) {
            return;
        }
        if (amountUsd != null && order.getAmount() != null) {
            try {
                if (new BigDecimal(amountUsd).compareTo(order.getAmount()) != 0) {
                    log.warn("paypal capture: amount mismatch order={}", orderNo);
                    return;
                }
            } catch (Exception ignored) {
            }
        }
        order.setTradeNo(captureId);
        grantIfFirstPay(order);
    }

    /**
     * 幂等发放权益：只有 PENDING 订单会入账。
     * VIP 时长 = max(当前时间, 现有到期时间) + 订单月数（续费叠加，未过期不清零）。
     */
    private void grantIfFirstPay(SubscriptionOrder order) {
        if (order.getStatus() != SubscriptionOrder.Status.PENDING) {
            log.info("order {} already settled ({}), skip", order.getOrderNo(), order.getStatus());
            return;
        }
        order.setStatus(SubscriptionOrder.Status.PAID);
        order.setPaidAt(LocalDateTime.now());
        orderRepository.save(order);

        User user = userRepository.findById(order.getUserId()).orElse(null);
        if (user == null) {
            return;
        }
        LocalDateTime base = user.getVipExpireAt();
        LocalDateTime now = LocalDateTime.now();
        if (base == null || base.isBefore(now)) {
            base = now;
        }
        user.setVipExpireAt(base.plusMonths(order.getMonths()));
        user.setVipMonths((user.getVipMonths() == null ? 0 : user.getVipMonths()) + order.getMonths());
        userRepository.save(user);
        log.info("VIP granted: user={} order={} months={} newExpire={}",
                user.getId(), order.getOrderNo(), order.getMonths(), user.getVipExpireAt());
    }

    // ------------------------------------------------------------------ 校验

    public boolean isVip(User user) {
        return user.getVipExpireAt() != null && user.getVipExpireAt().isAfter(LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public boolean isVip(Long userId) {
        // 游客（未登录）一律视为非 VIP
        if (userId == null) {
            return false;
        }
        return userRepository.findById(userId).map(this::isVip).orElse(false);
    }

    /**
     * VIP 专属语种访问校验：非 VIP 访问 vipOnly 语种直接 403。
     * 课程详情 / 课时详情 / 闯关词库等入口统一走这里。
     */
    @Transactional(readOnly = true)
    public void assertLanguageAccess(Long userId, Language language) {
        if (language == null || !Boolean.TRUE.equals(language.getVipOnly())) {
            return;
        }
        // 游客仅浏览课程结构（不含课时内容），进入课时学习时需先登录，届时再校验
        if (userId == null) {
            return;
        }
        if (!isVip(userId)) {
            throw new BusinessException(403, "「" + language.getNameCn() + "」为 VIP 专属语种，开通会员后即可学习");
        }
    }
}
