package com.delivery_project.user_service.user.application.port;

/**
 * 활성 MASTER가 0명인지 확인하고 최초 MASTER를 자동 승인하는 구간을 직렬화하기 위한 락.
 *
 * countActiveMastersForUpdate()(비관적 락)는 이미 있는 MASTER 행에만 걸 수 있어서,
 * "아직 MASTER가 한 명도 없는" 최초 부트스트랩 상황(잠글 행 자체가 없음)은 못 막는다 —
 * 그래서 특정 행이 아니라 임의의 키에 거는 세션 독립적 락(advisory lock)이 필요하다.
 */
public interface MasterBootstrapLockPort {

	void lock();
}
