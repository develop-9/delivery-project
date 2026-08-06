package com.delivery_project.hub_service.hub.application.support;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.delivery_project.hub_service.hub.domain.entity.Hub;

/**
 * 두 허브 사이의 경로를 구간으로 쪼갠다 (D2).
 *
 * <p><b>그래프 탐색이 아니다.</b> Hub-and-Spoke 토폴로지에서 경로는 항상
 * {@code 출발 → 출발의 MAIN → 도착의 MAIN → 도착} 형태라, {@code parentHubId} 계층만 보면
 * 최단 경로가 유일하게 정해진다. {@code p_hub_routes} 는 그렇게 정해진 구간의 거리·시간을
 * 읽는 데만 쓴다.
 *
 * <p>{@code mainOf(h) = h.getParentHubId()} 로 {@code hubType} 분기가 필요 없다 —
 * {@code MAIN} 은 자기 자신이 상위이기 때문이다 (D1).
 *
 * <p>구간 수는 1~3 이다. 세 조건이 모두 참인 최대 케이스가 서로 다른 MAIN 관할의 SUB → SUB 다.
 */
@Component
public class HubPathCalculator {

	public List<HubSegmentPair> calculate(Hub departure, Hub arrival) {
		UUID departureMainId = departure.getParentHubId();
		UUID arrivalMainId = arrival.getParentHubId();

		List<HubSegmentPair> segments = new ArrayList<>();

		if (!departure.getId().equals(departureMainId)) {
			segments.add(new HubSegmentPair(departure.getId(), departureMainId));
		}
		if (!departureMainId.equals(arrivalMainId)) {
			segments.add(new HubSegmentPair(departureMainId, arrivalMainId));
		}
		if (!arrivalMainId.equals(arrival.getId())) {
			segments.add(new HubSegmentPair(arrivalMainId, arrival.getId()));
		}

		return segments;
	}
}
