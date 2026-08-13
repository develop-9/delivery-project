package com.delivery_project.hub_service.hub.application.command;

import java.math.BigDecimal;
import java.util.UUID;

import com.delivery_project.hub_service.hub.domain.entity.HubType;

/**
 * 허브 생성 (01_hubs.md 1번).
 *
 * <p>{@code parentHubId} 는 {@code SUB} 일 때만 값이 온다. {@code MAIN} 인데 값이 있으면
 * {@code 400 PARENT_HUB_NOT_ALLOWED} — 중앙 허브의 상위는 서버가 자기 자신으로 채운다 (D1).
 */
public record HubCreateCommand(
		String name,
		String address,
		BigDecimal latitude,
		BigDecimal longitude,
		HubType hubType,
		UUID parentHubId
) {
}
