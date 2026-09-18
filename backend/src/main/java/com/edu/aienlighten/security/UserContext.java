package com.edu.aienlighten.security;

/** 当前请求登录用户（ThreadLocal） */
public class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static Long userId() {
        LoginUser u = HOLDER.get();
        return u == null ? null : u.getId();
    }

    public static Integer role() {
        LoginUser u = HOLDER.get();
        return u == null ? null : u.getRole();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
