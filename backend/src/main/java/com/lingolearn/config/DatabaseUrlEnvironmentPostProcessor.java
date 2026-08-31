package com.lingolearn.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * 解析部署平台注入的 DATABASE_URL 环境变量，转换为 Spring 数据源配置。
 * 支持 postgres://user:pass@host:port/db 与 jdbc:postgresql://host:port/db 两种形式；
 * 未注入时保持 application.yml 中的本地 MySQL 默认配置。
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String SOURCE_NAME = "pocketbayDatabaseUrl";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String url = environment.getProperty("DATABASE_URL");
        if (url == null || url.trim().isEmpty()) {
            return;
        }
        url = url.trim();
        Map<String, Object> props = new HashMap<>();
        String username = environment.getProperty("DATABASE_USERNAME");
        String password = environment.getProperty("DATABASE_PASSWORD");

        if (url.startsWith("jdbc:")) {
            // 已经是 JDBC 连接串，直接使用
            props.put("spring.datasource.url", url);
        } else if (url.startsWith("postgres://") || url.startsWith("postgresql://")) {
            // 形如 postgres://user:password@host:port/dbname?sslmode=require
            String rest = url.substring(url.indexOf("//") + 2);
            String hostPart = rest;
            String creds = null;
            int at = rest.lastIndexOf('@');
            if (at >= 0) {
                creds = rest.substring(0, at);
                hostPart = rest.substring(at + 1);
            }
            props.put("spring.datasource.url", "jdbc:postgresql://" + hostPart);
            if (creds != null && username == null) {
                int colon = creds.indexOf(':');
                if (colon >= 0) {
                    username = creds.substring(0, colon);
                    if (password == null) {
                        password = creds.substring(colon + 1);
                    }
                } else {
                    username = creds;
                }
            }
        } else {
            // 无法识别的格式，回退到默认配置
            return;
        }

        if (username != null) {
            props.put("spring.datasource.username", username);
        }
        if (password != null) {
            props.put("spring.datasource.password", password);
        }
        environment.getPropertySources().addFirst(new MapPropertySource(SOURCE_NAME, props));
    }
}