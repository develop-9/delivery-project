package com.delivery_project.hub_service.hub.application.command;

import java.util.UUID;

/**
 * 허브 삭제 (01_hubs.md 5번).
 *
 * <p>{@code callerId} 는 여기 담지 않는다 — 클라이언트가 제출한 요청 데이터가 아니라 JWT 필터가
 * 파생시킨 인증 컨텍스트라, Command 에 넣으면 호출자가 신원을 위조할 수 있는 형태가 된다.
 */
public record HubDeleteCommand(
		UUID hubId
) {
}
