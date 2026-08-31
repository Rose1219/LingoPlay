package com.lingolearn.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingolearn.common.BusinessException;
import com.lingolearn.dto.AuthResponse;
import com.lingolearn.dto.UserVO;
import com.lingolearn.entity.User;
import com.lingolearn.repository.UserRepository;
import com.lingolearn.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

/** 微信小程序授权登录：wx.login 的 code 换 openid 并关联/创建账号 */
@Service
public class WxAuthService {

    private static final String[] WX_AVATARS = {"🐨", "🦁", "🐯", "🐰", "🦉", "🐳", "🐢", "🦜"};

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${wx.appid:}")
    private String appid;

    @Value("${wx.secret:}")
    private String secret;

    public WxAuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /** 用小程序端 wx.login 得到的 code 完成登录 */
    @Transactional
    public AuthResponse loginByCode(String code) {
        if (appid == null || appid.trim().isEmpty() || secret == null || secret.trim().isEmpty()) {
            throw new BusinessException(503, "服务器尚未配置微信小程序 AppID/Secret，请联系管理员在环境变量 WX_APPID / WX_SECRET 中设置");
        }
        String openid = code2Openid(code);
        User user = userRepository.findByOpenid(openid).orElseGet(() -> createUser(openid));
        return new AuthResponse(jwtUtil.generate(user.getId()), UserVO.of(user));
    }

    /** 调用微信 jscode2session 换取 openid */
    private String code2Openid(String code) {
        try {
            RestTemplate rest = new RestTemplate();
            String url = "https://api.weixin.qq.com/sns/jscode2session"
                    + "?appid=" + appid.trim()
                    + "&secret=" + secret.trim()
                    + "&js_code=" + code
                    + "&grant_type=authorization_code";
            String body = rest.getForObject(url, String.class);
            JsonNode node = objectMapper.readTree(body);
            if (node.hasNonNull("errcode") && node.get("errcode").asInt() != 0) {
                throw new BusinessException(401, "微信登录失败：" + node.path("errmsg").asText("无效的登录凭证"));
            }
            String openid = node.path("openid").asText(null);
            if (openid == null || openid.isEmpty()) {
                throw new BusinessException(401, "微信登录失败：未获取到 openid");
            }
            return openid;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(502, "请求微信接口失败，请稍后再试");
        }
    }

    /** 首次微信登录：自动建号（随机密码，仅微信入口可用） */
    private User createUser(String openid) {
        User user = new User();
        user.setOpenid(openid);
        user.setUsername("wx_" + openid.substring(0, Math.min(12, openid.length())));
        user.setEmail(openid + "@wx.lingoplay.local");
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setNickname("微信用户");
        user.setAvatar(WX_AVATARS[Math.abs(openid.hashCode()) % WX_AVATARS.length]);
        user.setPreferredLanguages("");
        return userRepository.save(user);
    }
}