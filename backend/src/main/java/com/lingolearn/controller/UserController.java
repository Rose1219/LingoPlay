package com.lingolearn.controller;

import com.lingolearn.common.ApiResponse;
import com.lingolearn.common.BusinessException;
import com.lingolearn.dto.UserUpdateRequest;
import com.lingolearn.dto.UserVO;
import com.lingolearn.entity.User;
import com.lingolearn.repository.UserRepository;
import com.lingolearn.security.AuthContext;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/** 用户接口 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ApiResponse<UserVO> me() {
        Long userId = AuthContext.requireUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(401, "用户不存在"));
        return ApiResponse.ok(UserVO.of(user));
    }

    @PutMapping("/me")
    @Transactional
    public ApiResponse<UserVO> updateMe(@Valid @RequestBody UserUpdateRequest req) {
        Long userId = AuthContext.requireUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(401, "用户不存在"));
        if (req.getNickname() != null) {
            user.setNickname(req.getNickname().trim());
        }
        if (req.getPreferredLanguages() != null) {
            user.setPreferredLanguages(req.getPreferredLanguages());
        }
        userRepository.save(user);
        return ApiResponse.ok(UserVO.of(user));
    }
}