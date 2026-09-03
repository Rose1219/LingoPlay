package com.lingolearn.config;

import com.lingolearn.security.JwtInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Web 配置：CORS + 认证拦截器 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    public WebConfig(JwtInterceptor jwtInterceptor) {
        this.jwtInterceptor = jwtInterceptor;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                // 登录注册接口放行；App 版本检查放行（未登录也可检查更新）
                // 发音接口放行：音频组件（InnerAudioContext / <audio>）无法携带自定义请求头，
                // 服务端靠限长 + 限频 + 配额控制滥用风险
                // 支付回调放行：第三方渠道通知无法携带 JWT，安全性由验签保证
                .excludePathPatterns("/api/auth/**", "/api/app/**", "/api/tts",
                        "/api/vip/notify/**", "/api/vip/paypal/return");
    }
}