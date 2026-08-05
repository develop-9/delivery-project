package com.delivery_project.hub_service.global.common;

/**
 * 사용자 권한. user-service 가 Access Token 의 {@code role} 클레임에 이 이름 그대로 담는다.
 *
 * <p>hub-service 는 토큰을 발급하지 않고 읽기만 하므로 <b>user-service 의 {@code Role} 과
 * 상수 이름이 어긋나면 안 된다.</b> 값을 바꿀 일이 생기면 두 서비스를 함께 고친다.
 */
public enum Role {
	MASTER,
	HUB_MANAGER,
	DELIVERY_MANAGER,
	COMPANY_MANAGER
}
