package com.daodejing.config;

import com.daodejing.common.BizException;

/**
 * 当前请求的登录身份（由 JwtInterceptor 填充）
 */
public class AuthContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> ADMIN_ID = new ThreadLocal<>();

    public static void setUserId(Long id) { USER_ID.set(id); }

    public static void setAdminId(Long id) { ADMIN_ID.set(id); }

    /** 可能为 null（游客） */
    public static Long userIdOrNull() { return USER_ID.get(); }

    /** 未登录时抛 401 */
    public static long userId() {
        Long id = USER_ID.get();
        if (id == null) throw new BizException(401, "请先登录");
        return id;
    }

    public static long adminId() {
        Long id = ADMIN_ID.get();
        if (id == null) throw new BizException(401, "请先登录");
        return id;
    }

    public static void clear() {
        USER_ID.remove();
        ADMIN_ID.remove();
    }
}
