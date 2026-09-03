package com.lingolearn.entity;

import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会员订阅订单。
 *
 * 设计要点：
 * - orderNo 由服务端生成并唯一，全程作为业务主键；第三方流水号另存一列，
 *   避免不同渠道的流水号格式互相污染。
 * - status 只允许单向流转 PENDING → PAID/FAILED，重复回调不会重复加时长
 *   （见 PaymentService 的幂等处理）。
 * - 金额用 BigDecimal 且以元为单位存储，禁止用浮点数算钱。
 */
@Entity
@Table(name = "subscription_orders", uniqueConstraints = {
        @UniqueConstraint(columnNames = "order_no")
})
@Data
public class SubscriptionOrder {

    public enum Status {
        /** 已创建，等待支付 */
        PENDING,
        /** 支付成功，已发放权益 */
        PAID,
        /** 支付失败或超时关闭 */
        FAILED,
        /** 已退款（退款后权益应回收，本版暂不自动回收） */
        REFUNDED
    }

    public enum Channel {
        WECHAT, ALIPAY, PAYPAL, STRIPE,
        /** 凭证未配置时的模拟支付，仅用于联调，上线前必须被真实渠道取代 */
        MOCK
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 业务订单号：LP + yyyyMMddHHmmss + 随机 6 位 */
    @Column(name = "order_no", nullable = false, unique = true, length = 40)
    private String orderNo;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 套餐编码，如 vip_monthly */
    @Column(nullable = false, length = 40)
    private String planCode;

    /** 购买月数 */
    @Column(nullable = false)
    private Integer months;

    /** 应付金额（元） */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /** 币种：CNY / USD */
    @Column(nullable = false, length = 8)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Channel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status = Status.PENDING;

    /** 第三方交易流水号，用于对账 */
    @Column(name = "trade_no", length = 80)
    private String tradeNo;

    /** 渠道返回的支付链接/二维码内容，前端据此拉起收银台 */
    @Column(name = "pay_url", length = 1000)
    private String payUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /** 订单过期时间，超时未支付自动关闭 */
    @Column(name = "expire_at")
    private LocalDateTime expireAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    /** 是否走了模拟支付（凭证未配置）。上线前监控此项告警 */
    @Column(name = "is_mock")
    private Boolean mock = false;
}
