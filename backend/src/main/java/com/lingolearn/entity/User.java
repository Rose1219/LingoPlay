package com.lingolearn.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/** 用户 */
@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "username"),
        @UniqueConstraint(columnNames = "email")
})
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String email;

    /** BCrypt 加密后的密码 */
    @Column(nullable = false)
    private String password;

    @Column(length = 50)
    private String nickname;

    /** 头像 emoji */
    @Column(length = 20)
    private String avatar;

    /** 偏好语言，逗号分隔，如 en,ja */
    @Column(name = "preferred_languages", length = 50)
    private String preferredLanguages;

    /** 微信小程序 openid（授权登录用，非小程序用户为空） */
    @Column(name = "openid", length = 64)
    private String openid;

    @Column(name = "registered_at", nullable = false, updatable = false)
    private LocalDateTime registeredAt = LocalDateTime.now();
}