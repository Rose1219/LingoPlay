package com.lingolearn.dto;

import com.lingolearn.entity.User;
import lombok.Data;

/** 用户信息视图 */
@Data
public class UserVO {

    private Long id;
    private String username;
    private String email;
    private String nickname;
    private String avatar;
    /** 偏好语言代码，逗号分隔 */
    private String preferredLanguages;

    public static UserVO of(User u) {
        UserVO vo = new UserVO();
        vo.id = u.getId();
        vo.username = u.getUsername();
        vo.email = u.getEmail();
        vo.nickname = u.getNickname();
        vo.avatar = u.getAvatar();
        vo.preferredLanguages = u.getPreferredLanguages();
        return vo;
    }
}