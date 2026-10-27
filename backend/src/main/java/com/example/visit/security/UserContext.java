package com.example.visit.security;

import com.example.visit.common.BizException;

public final class UserContext {

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(CurrentUser user) {
        HOLDER.set(user);
    }

    public static CurrentUser get() {
        CurrentUser user = HOLDER.get();
        if (user == null) {
            throw BizException.unauthorized("登录已失效，请重新登录");
        }
        return user;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
