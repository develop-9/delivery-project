package com.delivery_project.hub_service.hub.application.command;

import java.util.UUID;

/**
 * 이동정보 삭제 (02_hub-routes.md 10번).
 *
 * <p><b>{@code callerId} 는 담지 않는다.</b> JWT 필터가 파생시킨 인증 컨텍스트이지 클라이언트가 제출한
 * 데이터가 아니라서, Command 에 넣으면 신원을 위조할 수 있는 형태가 된다. 서비스 파라미터로 따로 받는다.
 */
public record HubRouteDeleteCommand(
		UUID hubRouteId
) {
}
