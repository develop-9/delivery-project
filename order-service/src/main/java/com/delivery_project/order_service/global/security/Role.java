package com.delivery_project.order_service.global.security;

/** 사용자 역할. user-service 가 토큰에 담는 {@code role} 클레임과 이름이 같아야 한다. */
public enum Role {
	MASTER,
	HUB_MANAGER,
	DELIVERY_MANAGER,
	COMPANY_MANAGER
}
