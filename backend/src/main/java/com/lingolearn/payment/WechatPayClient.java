package com.lingolearn.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * 微信支付 V2 客户端（Native 扫码 + JSAPI 小程序支付）。
 *
 * 设计说明：
 * - 选择 V2 而非 V3：V2 仅需商户号 + API 密钥即可联调，不依赖平台证书下载与验签序列化，
 *   对本项目的体量与部署形态（单容器）更友好；V3 凭证齐备后可平滑替换本类。
 * - 所有金额以「分」为单位在服务端计算，禁止由前端传入。
 * - 回调验签：重算 MD5 签名并与通知中的 sign 比对，任何不一致直接拒绝。
 */
@Component
public class WechatPayClient {

    private static final Logger log = LoggerFactory.getLogger(WechatPayClient.class);

    private static final String UNIFIED_ORDER_URL = "https://api.mch.weixin.qq.com/pay/unifiedorder";

    @Value("${pay.wechat.appid:}")
    private String appid;

    @Value("${pay.wechat.mch-id:}")
    private String mchId;

    @Value("${pay.wechat.mch-key:}")
    private String mchKey;

    /** 是否已配置完整凭证 */
    public boolean configured() {
        return notEmpty(appid) && notEmpty(mchId) && notEmpty(mchKey);
    }

    public String getAppid() {
        return appid;
    }

    // ------------------------------------------------------------------ 下单

    /**
     * 统一下单
     *
     * @param tradeType NATIVE（返回二维码内容 code_url）或 JSAPI（返回 prepay_id）
     * @param openid    JSAPI 必填，NATIVE 传 null
     * @return NATIVE → code_url；JSAPI → prepay_id
     */
    public String unifiedOrder(String orderNo, String subject, int totalFen, String tradeType,
                               String openid, String notifyUrl, String clientIp) {
        try {
            TreeMap<String, String> p = new TreeMap<>();
            p.put("appid", appid);
            p.put("mch_id", mchId);
            p.put("nonce_str", nonce());
            p.put("body", subject);
            p.put("out_trade_no", orderNo);
            p.put("total_fee", String.valueOf(totalFen));
            p.put("spbill_create_ip", clientIp == null || clientIp.trim().isEmpty() ? "127.0.0.1" : clientIp.trim());
            p.put("notify_url", notifyUrl);
            p.put("trade_type", tradeType);
            if ("JSAPI".equals(tradeType)) {
                p.put("openid", openid);
            }
            p.put("sign", sign(p));

            String xml = toXml(p);
            String resp = httpPostXml(UNIFIED_ORDER_URL, xml);
            Map<String, String> result = xmlToMap(resp);
            if (result == null
                    || !"SUCCESS".equals(result.get("return_code"))
                    || !"SUCCESS".equals(result.get("result_code"))) {
                log.warn("wechat unifiedorder failed: {}", resp);
                return null;
            }
            // 响应验签，防伪造
            if (!verifySign(result)) {
                log.warn("wechat unifiedorder sign mismatch");
                return null;
            }
            return "NATIVE".equals(tradeType) ? result.get("code_url") : result.get("prepay_id");
        } catch (Exception e) {
            log.warn("wechat unifiedorder error: {}", e.getMessage());
            return null;
        }
    }

    /** 组装小程序 wx.requestPayment 所需参数（JSON 字符串由控制器转） */
    public Map<String, String> buildJsapiParams(String prepayId) {
        TreeMap<String, String> p = new TreeMap<>();
        p.put("appId", appid);
        p.put("timeStamp", String.valueOf(System.currentTimeMillis() / 1000));
        p.put("nonceStr", nonce());
        p.put("package", "prepay_id=" + prepayId);
        p.put("signType", "MD5");
        p.put("paySign", sign(p));
        return p;
    }

    // ------------------------------------------------------------------ 回调验签

    /** 解析并验证支付结果通知。验证通过返回订单号与微信流水号，否则返回 null */
    public NotifyResult verifyNotify(String xmlBody) {
        try {
            Map<String, String> map = xmlToMap(xmlBody);
            if (map == null
                    || !"SUCCESS".equals(map.get("return_code"))
                    || !"SUCCESS".equals(map.get("result_code"))) {
                return null;
            }
            if (!verifySign(map)) {
                log.warn("wechat notify sign mismatch, out_trade_no={}", map.get("out_trade_no"));
                return null;
            }
            NotifyResult r = new NotifyResult();
            r.orderNo = map.get("out_trade_no");
            r.tradeNo = map.get("transaction_id");
            r.totalFen = Integer.parseInt(map.getOrDefault("total_fee", "0"));
            return r;
        } catch (Exception e) {
            log.warn("wechat notify parse error: {}", e.getMessage());
            return null;
        }
    }

    public static class NotifyResult {
        public String orderNo;
        public String tradeNo;
        public int totalFen;
    }

    // ------------------------------------------------------------------ 签名

    /** V2 签名：参数按 ASCII 升序 k=v& 拼接，末尾拼 &key=商户密钥，MD5 后转大写 */
    private String sign(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : new TreeMap<>(params).entrySet()) {
            String k = e.getKey();
            String v = e.getValue();
            if (v == null || v.isEmpty() || "sign".equals(k)) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(k).append('=').append(v);
        }
        sb.append("&key=").append(mchKey);
        return md5(sb.toString()).toUpperCase();
    }

    /** 校验回调/响应签名 */
    private boolean verifySign(Map<String, String> map) {
        String expect = map.get("sign");
        if (expect == null || expect.isEmpty()) {
            return false;
        }
        return sign(map).equalsIgnoreCase(expect);
    }

    private static String md5(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("MD5 unavailable", e);
        }
    }

    private static String nonce() {
        return Long.toHexString(System.nanoTime()) + Long.toHexString((long) (Math.random() * 0xFFFFFFF));
    }

    // ------------------------------------------------------------------ XML

    private static String toXml(TreeMap<String, String> params) {
        StringBuilder sb = new StringBuilder("<xml>");
        for (Map.Entry<String, String> e : params.entrySet()) {
            sb.append('<').append(e.getKey()).append("><![CDATA[")
              .append(e.getValue()).append("]]></").append(e.getKey()).append('>');
        }
        sb.append("</xml>");
        return sb.toString();
    }

    /** 简易 XML → Map（微信 V2 协议为单层 XML，禁用外部实体防 XXE） */
    private static Map<String, String> xmlToMap(String xml) {
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            f.setFeature("http://xml.org/sax/features/external-general-entities", false);
            f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            f.setXIncludeAware(false);
            f.setExpandEntityReferences(false);
            DocumentBuilder b = f.newDocumentBuilder();
            Document doc = b.parse(new java.io.ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            Element root = doc.getDocumentElement();
            Map<String, String> map = new java.util.LinkedHashMap<>();
            org.w3c.dom.NodeList children = root.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                org.w3c.dom.Node n = children.item(i);
                if (n.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    map.put(n.getNodeName(), n.getTextContent());
                }
            }
            return map;
        } catch (Exception e) {
            return null;
        }
    }

    private static String httpPostXml(String url, String body) throws Exception {
        javax.net.ssl.HttpsURLConnection conn = (javax.net.ssl.HttpsURLConnection) new java.net.URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(15000);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "text/xml;charset=UTF-8");
        conn.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
        int code = conn.getResponseCode();
        if (code != 200) {
            throw new IllegalStateException("wechat pay http " + code);
        }
        try (InputStream in = conn.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static boolean notEmpty(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
