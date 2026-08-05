package com.delivery_project.order_service.global.security;

import java.util.UUID;

/**
 * Gateway 가 JWT 를 검증한 뒤 X-User-Id / X-User-Role 헤더로 넘겨주는 사용자 정보.
 */
public record UserContext(
        UUID userId,
        Role role
) {
    public boolean isMaster() {
        return role == Role.MASTER;
    }

    public boolean hasAnyRole(Role... roles) {
        for (Role r : roles) {
            if (this.role == r) {
                return true;
            }
        }
        return false;
    }
}
