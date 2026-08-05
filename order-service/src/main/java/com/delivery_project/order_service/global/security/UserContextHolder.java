package com.delivery_project.order_service.global.security;

import com.delivery_project.order_service.global.exception.BusinessException;
import com.delivery_project.order_service.global.exception.ErrorCode;

/**
 * 요청 스레드 하나에 사용자 정보를 담아둔다.
 * Service 계층 어디서든 파라미터로 넘기지 않고 꺼내 쓸 수 있다.
 */
public final class UserContextHolder {

    private static final ThreadLocal<UserContext> HOLDER = new ThreadLocal<>();

    private UserContextHolder() {
    }

    public static void set(UserContext context) {
        HOLDER.set(context);
    }

    public static UserContext get() {
        return HOLDER.get();
    }

    /** 사용자 정보가 반드시 있어야 하는 지점에서 사용한다 */
    public static UserContext getRequired() {
        UserContext context = HOLDER.get();
        if (context == null) {
            throw new BusinessException(ErrorCode.AUTH_UNAUTHORIZED);
        }
        return context;
    }

    /** 톰캣은 스레드를 재사용한다. 안 지우면 다음 요청에 남의 정보가 섞인다. */
    public static void clear() {
        HOLDER.remove();
    }
}
