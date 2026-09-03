package com.lingolearn.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PayPal Orders v2 客户端（国际支付 + 信用卡）。
 *
 * - 信用卡（Visa/Mastercard 等）由 PayPal 收银台代收（guest checkout / Debit or Credit Card），
 *   我方不接触任何卡号信息，天然规避 PCI-DSS 合规负担。
 * - 流程：服务端 OAuth 拿 token → 创建 Order → 前端跳 approve 链接 →
 *   用户支付后回到 return_url → 服务端 Capture 并核验 COMPLETED → 发放权益。
 * - 金额在服务端写死（订单表），捕获结果与订单金额核对后才入账。
 */
@Component
public class PaypalClient {

    private static final Logger log = LoggerFactory.getLogger(PaypalClient.class);

    @Value("${pay.paypal.mode:sandbox}")
    private String mode;

    @Value("${pay.paypal.client-id:}")
    private String clientId;

    @Value("${pay.paypal.secret:}")
    private String secret;

    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** access token 缓存（PayPal token 有效期一般 9 小时） */
    private volatile String token;
    private volatile long tokenExpireAt = 0;

    public boolean configured() {
        return notEmpty(clientId) && notEmpty(secret);
    }

    private String apiBase() {
        return "live".equalsIgnoreCase(mode) ? "https://api-m.paypal.com" : "https://api-m.sandbox.paypal.com";
    }

    // ------------------------------------------------------------------ API

    /**
     * 创建订单。
     * @return approve 链接（用户浏览器跳转）；失败返回 null
     */
    public String createOrder(String orderNo, String amountUsd, String returnUrl, String cancelUrl) {
        try {
            String t = accessToken();
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("intent", "CAPTURE");
            Map<String, Object> amount = new LinkedHashMap<>();
            amount.put("currency_code", "USD");
            amount.put("value", amountUsd);
            Map<String, Object> unit = new LinkedHashMap<>();
            unit.put("reference_id", orderNo);
            unit.put("amount", amount);
            body.put("purchase_units", java.util.Collections.singletonList(unit));
            Map<String, Object> ctx = new LinkedHashMap<>();
            ctx.put("return_url", returnUrl);
            ctx.put("cancel_url", cancelUrl);
            ctx.put("user_action", "PAY_NOW");
            ctx.put("shipping_preference", "NO_SHIPPING");
            body.put("application_context", ctx);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(t);
            String resp = rest.exchange(apiBase() + "/v2/checkout/orders", HttpMethod.POST,
                    new HttpEntity<>(objectMapper.writeValueAsString(body), headers), String.class).getBody();
            JsonNode root = objectMapper.readTree(resp);
            if (!"CREATED".equalsIgnoreCase(root.path("status").asText())) {
                log.warn("paypal create order status={}", root.path("status").asText());
                return null;
            }
            return findLink(root, "approve");
        } catch (Exception e) {
            log.warn("paypal createOrder failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 捕获订单（用户支付完成后服务端执行）。
     * @return 捕获结果（交易号 + 实付金额），失败返回 null
     */
    public CaptureResult captureOrder(String paypalOrderId) {
        try {
            String t = accessToken();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(t);
            String resp = rest.exchange(apiBase() + "/v2/checkout/orders/" + paypalOrderId + "/capture",
                    HttpMethod.POST, new HttpEntity<>("{}", headers), String.class).getBody();
            JsonNode root = objectMapper.readTree(resp);
            if (!"COMPLETED".equalsIgnoreCase(root.path("status").asText())) {
                log.warn("paypal capture status={}", root.path("status").asText());
                return null;
            }
            JsonNode capture = root.path("purchase_units").path(0)
                    .path("payments").path("captures").path(0);
            String captureId = capture.path("id").asText(null);
            if (captureId == null || captureId.isEmpty()) {
                return null;
            }
            CaptureResult r = new CaptureResult();
            r.captureId = captureId;
            r.currency = capture.path("amount").path("currency_code").asText("USD");
            r.amount = capture.path("amount").path("value").asText("0");
            return r;
        } catch (Exception e) {
            log.warn("paypal capture failed: {}", e.getMessage());
            return null;
        }
    }

    public static class CaptureResult {
        public String captureId;
        public String currency;
        public String amount;
    }

    /** 用户取消支付后 PayPal 会带 token 重定向回来，可反查我方订单号 */
    public String getOrderByPaypalToken(String paypalToken) {
        try {
            String t = accessToken();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(t);
            String resp = rest.exchange(apiBase() + "/v2/checkout/orders/" + paypalToken,
                    HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), String.class).getBody();
            JsonNode root = objectMapper.readTree(resp);
            JsonNode units = root.path("purchase_units");
            if (units.isArray() && units.size() > 0) {
                return units.get(0).path("reference_id").asText(null);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    // ------------------------------------------------------------------ OAuth

    private synchronized String accessToken() throws Exception {
        if (token != null && System.currentTimeMillis() < tokenExpireAt) {
            return token;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String auth = clientId + ":" + secret;
        headers.set(HttpHeaders.AUTHORIZATION, "Basic "
                + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8)));
        String resp = rest.postForObject(apiBase() + "/v1/oauth2/token",
                new HttpEntity<>("grant_type=client_credentials", headers), String.class);
        JsonNode root = objectMapper.readTree(resp);
        token = root.path("access_token").asText(null);
        int expiresIn = root.path("expires_in").asInt(0);
        if (token == null || token.isEmpty()) {
            throw new IllegalStateException("paypal oauth failed");
        }
        // 提前 5 分钟过期，避免临界点 401
        tokenExpireAt = System.currentTimeMillis() + Math.max(60, expiresIn - 300) * 1000L;
        return token;
    }

    private static String findLink(JsonNode root, String rel) {
        Iterator<JsonNode> it = root.path("links").elements();
        while (it.hasNext()) {
            JsonNode link = it.next();
            if (rel.equalsIgnoreCase(link.path("rel").asText())) {
                return link.path("href").asText(null);
            }
        }
        return null;
    }

    private static boolean notEmpty(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
