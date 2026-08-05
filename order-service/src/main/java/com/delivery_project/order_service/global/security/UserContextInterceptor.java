package com.delivery_project.order_service.global.security;

import com.delivery_project.order_service.global.exception.BusinessException;
import com.delivery_project.order_service.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * Gateway 가 주입한 X-User-Id / X-User-Role 헤더를 UserContext 로 옮긴다.
 *
 * order.auth.require-gateway-headers = true 이면 헤더가 없을 때 401 로 막는다.
 * Gateway 가 아직 붙지 않은 초기 개발 단계에서는 false 로 두고
 * order.auth.dev-user-id / dev-user-role 을 대체 사용자로 쓴다.
 */
@Slf4j
@Component
public class UserContextInterceptor implements HandlerInterceptor {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USER_ROLE = "X-User-Role";

    private final boolean requireGatewayHeaders;
    private final UUID devUserId;
    private final Role devUserRole;

    public UserContextInterceptor(
            @Value("${order.auth.require-gateway-headers:false}") boolean requireGatewayHeaders,
            @Value("${order.auth.dev-user-id:00000000-0000-0000-0000-000000000000}") String devUserId,
            @Value("${order.auth.dev-user-role:MASTER}") String devUserRole
    ) {
        this.requireGatewayHeaders = requireGatewayHeaders;
        this.devUserId = UUID.fromString(devUserId);
        this.devUserRole = Role.valueOf(devUserRole);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

        String userId = request.getHeader(HEADER_USER_ID);
        String role = request.getHeader(HEADER_USER_ROLE);

        if (!StringUtils.hasText(userId) || !StringUtils.hasText(role)) {
            if (requireGatewayHeaders) {
                throw new BusinessException(ErrorCode.AUTH_UNAUTHORIZED,
                        "Gateway 를 통해 호출해야 합니다. (X-User-Id, X-User-Role 누락)");
            }

            log.debug("[UserContext] 인증 헤더가 없어 개발용 사용자로 대체합니다. userId={}", devUserId);
            UserContextHolder.set(new UserContext(devUserId, devUserRole));
            return true;
        }

        try {
            UserContextHolder.set(new UserContext(UUID.fromString(userId), Role.valueOf(role)));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.AUTH_UNAUTHORIZED, "인증 헤더 형식이 올바르지 않습니다.");
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContextHolder.clear();
    }
}
