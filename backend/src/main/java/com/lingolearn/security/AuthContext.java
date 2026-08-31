package com.lingolearn.security;

/** 当前登录用户上下文（ThreadLocal） */
public class AuthContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    public static void set(Long userId) {
        USER_ID.set(userId);
    }

    public static Long getUserId() {
        return USER_ID.get();
    }

    /** 获取当前用户，未登录抛业务异常 */
    public static Long requireUserId() {
        Long id = USER_ID.get();
        if (id == null) {
            throw new com.lingolearn.common.BusinessException(401, "请先登录");
        }
        return id;
    }

    public static void clear() {
        USER_ID.remove();
    }
}