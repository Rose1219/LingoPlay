package com.lingolearn.service;

import com.lingolearn.common.BusinessException;
import com.lingolearn.dto.AuthResponse;
import com.lingolearn.dto.LoginRequest;
import com.lingolearn.dto.RegisterRequest;
import com.lingolearn.dto.UserVO;
import com.lingolearn.entity.User;
import com.lingolearn.repository.UserRepository;
import com.lingolearn.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 认证服务：注册 / 登录 */
@Service
public class AuthService {

    private static final String[] AVATARS = {"🦊", "🐼", "🐨", "🦁", "🐯", "🐰", "🦉", "🐳"};

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new BusinessException("用户名已被注册");
        }
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new BusinessException("邮箱已被注册");
        }
        User user = new User();
        user.setUsername(req.getUsername());
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setNickname(req.getNickname() == null || req.getNickname().trim().isEmpty()
                ? req.getUsername() : req.getNickname().trim());
        user.setAvatar(AVATARS[Math.abs(req.getUsername().hashCode()) % AVATARS.length]);
        userRepository.save(user);
        return new AuthResponse(jwtUtil.generate(user.getId()), UserVO.of(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByUsername(req.getAccount())
                .orElseGet(() -> userRepository.findByEmail(req.getAccount())
                        .orElseThrow(() -> new BusinessException("账号或密码错误")));
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BusinessException("账号或密码错误");
        }
        return new AuthResponse(jwtUtil.generate(user.getId()), UserVO.of(user));
    }
}