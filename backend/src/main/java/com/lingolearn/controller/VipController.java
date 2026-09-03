package com.lingolearn.controller;

import com.lingolearn.common.ApiResponse;
import com.lingolearn.entity.SubscriptionOrder;
import com.lingolearn.payment.AlipayClient;
import com.lingolearn.payment.PaypalClient;
import com.lingolearn.payment.WechatPayClient;
import com.lingolearn.service.VipService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * VIP 会员接口。
 *
 * 认证策略：
 * - /status、/orders、下单接口需要登录（JwtInterceptor 默认拦截）；
 * - /notify/** 与 /paypal/return 是支付渠道回调，无法携带 JWT，需放行并强验签。
 */
@RestController
@RequestMapping("/api/vip")
public class VipController {

    private static final Logger log = LoggerFactory.getLogger(VipController.class);

    private final VipService vipService;
    private final WechatPayClient wechatPayClient;
    private final AlipayClient alipayClient;
    private final PaypalClient paypalClient;

    public VipController(VipService vipService, WechatPayClient wechatPayClient,
                         AlipayClient alipayClient, PaypalClient paypalClient) {
        this.vipService = vipService;
        this.wechatPayClient = wechatPayClient;
        this.alipayClient = alipayClient;
        this.paypalClient = paypalClient;
    }

    /** 会员中心：状态 + 套餐 + 可用渠道 */
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> status() {
        return ApiResponse.ok(vipService.status(com.lingolearn.security.AuthContext.requireUserId()));
    }

    /** 创建订单并发起支付 */
    @PostMapping("/orders")
    public ApiResponse<Map<String, Object>> createOrder(@RequestBody Map<String, String> body,
                                                        HttpServletRequest request) {
        Long userId = com.lingolearn.security.AuthContext.requireUserId();
        String channel = body == null ? null : body.get("channel");
        String openid = body == null ? null : body.get("openid");
        Map<String, Object> pay = vipService.createOrder(userId, channel, openid,
                baseUrl(request), clientIp(request));
        return ApiResponse.ok(pay);
    }

    /** 我的订单 */
    @GetMapping("/orders")
    public ApiResponse<List<SubscriptionOrder>> myOrders() {
        return ApiResponse.ok(vipService.myOrders(com.lingolearn.security.AuthContext.requireUserId()));
    }

    /** 订单状态轮询（扫码支付场景） */
    @GetMapping("/orders/{orderNo}")
    public ApiResponse<Map<String, Object>> orderStatus(@PathVariable String orderNo) {
        Long userId = com.lingolearn.security.AuthContext.requireUserId();
        SubscriptionOrder order = vipService.myOrders(userId).stream()
                .filter(o -> orderNo.equals(o.getOrderNo()))
                .findFirst()
                .orElse(null);
        if (order == null) {
            return ApiResponse.error(404, "订单不存在");
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("orderNo", order.getOrderNo());
        m.put("status", order.getStatus().name());
        m.put("amount", order.getAmount());
        m.put("currency", order.getCurrency());
        m.put("channel", order.getChannel().name());
        return ApiResponse.ok(m);
    }

    /** 演示支付（仅 pay.mock-enabled=true 时可用） */
    @PostMapping("/mock-pay/{orderNo}")
    public ApiResponse<Map<String, Object>> mockPay(@PathVariable String orderNo) {
        vipService.mockPay(com.lingolearn.security.AuthContext.requireUserId(), orderNo);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("paid", true);
        return ApiResponse.ok(m);
    }

    // ------------------------------------------------------------------ 回调（免登录，强验签）

    /** 微信支付异步通知（XML） */
    @PostMapping(value = "/notify/wechat", produces = "application/xml")
    public ResponseEntity<String> wechatNotify(@RequestBody String xmlBody) {
        WechatPayClient.NotifyResult n = wechatPayClient.verifyNotify(xmlBody);
        if (n == null) {
            log.warn("wechat notify rejected");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("<xml><return_code><![CDATA[FAIL]]></return_code>"
                            + "<return_msg><![CDATA[VERIFY_FAILED]]></return_msg></xml>");
        }
        vipService.onWechatNotify(n);
        return ResponseEntity.ok("<xml><return_code><![CDATA[SUCCESS]]></return_code>"
                + "<return_msg><![CDATA[OK]]></return_msg></xml>");
    }

    /** 支付宝异步通知（form 表单），验签通过必须返回纯文本 success */
    @PostMapping(value = "/notify/alipay", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> alipayNotify(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Enumeration<String> names = request.getParameterNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            params.put(name, request.getParameter(name));
        }
        AlipayClient.NotifyResult n = alipayClient.verifyNotify(params);
        if (n == null) {
            log.warn("alipay notify rejected");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("fail");
        }
        vipService.onAlipayNotify(n);
        return ResponseEntity.ok("success");
    }

    /** PayPal 支付完成回跳：服务端 Capture 后 302 回前端会员页 */
    @GetMapping("/paypal/return")
    public ResponseEntity<Void> paypalReturn(@RequestParam(required = false) String token,
                                             HttpServletRequest request) {
        String frontBase = frontendBase(request);
        if (token == null || token.isEmpty() || !paypalClient.configured()) {
            HttpHeaders h = new HttpHeaders();
            h.setLocation(java.net.URI.create(frontBase + "/vip?pay=cancel"));
            return new ResponseEntity<>(h, HttpStatus.FOUND);
        }
        // 通过 PayPal 反查该 token 对应我方订单号，防伪造回跳
        String orderNo = paypalClient.getOrderByPaypalToken(token);
        PaypalClient.CaptureResult capture = paypalClient.captureOrder(token);
        boolean ok = orderNo != null && capture != null;
        if (ok) {
            vipService.onPaypalCaptured(orderNo, capture.captureId, capture.amount);
        }
        String pay = ok ? "success" : "fail";
        HttpHeaders h = new HttpHeaders();
        h.setLocation(java.net.URI.create(frontBase + "/vip?pay=" + pay));
        return new ResponseEntity<>(h, HttpStatus.FOUND);
    }

    // ------------------------------------------------------------------ 工具

    /** 站点对外地址：优先信任反向代理头（PocketBay/Nginx 场景） */
    private static String baseUrl(HttpServletRequest request) {
        String proto = headerOr(request, "X-Forwarded-Proto", request.getScheme());
        String host = headerOr(request, "X-Forwarded-Host",
                headerOr(request, "Host", request.getServerName() + ":" + request.getServerPort()));
        return proto + "://" + host;
    }

    /** 前端地址：部署形态是前后端同域（SPA 由后端托管），直接用站点地址 */
    private static String frontendBase(HttpServletRequest request) {
        return baseUrl(request);
    }

    private static String headerOr(HttpServletRequest request, String name, String fallback) {
        String v = request.getHeader(name);
        return v == null || v.trim().isEmpty() ? fallback : v.trim();
    }

    private static String clientIp(HttpServletRequest request) {
        String[] candidates = {"X-Real-IP", "X-Forwarded-For"};
        for (String h : candidates) {
            String v = request.getHeader(h);
            if (v != null && !v.trim().isEmpty()) {
                int comma = v.indexOf(',');
                return (comma > 0 ? v.substring(0, comma) : v).trim();
            }
        }
        return request.getRemoteAddr();
    }

    @SuppressWarnings("unused")
    private static String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
