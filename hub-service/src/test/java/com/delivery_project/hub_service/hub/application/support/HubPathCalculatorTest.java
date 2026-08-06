package com.delivery_project.hub_service.hub.application.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.delivery_project.hub_service.hub.domain.entity.Hub;

/**
 * 경로 산출 알고리즘 단위 테스트 (D2).
 *
 * <p>00_common.md 의 케이스 표를 그대로 옮겼다. 구간 수가 1~3 이고
 * 서로 다른 MAIN 관할의 SUB → SUB 가 최대라는 것이 이 알고리즘의 계약이다.
 */
class HubPathCalculatorTest {

	private final HubPathCalculator hubPathCalculator = new HubPathCalculator();

	private Hub gyeonggiSouth;
	private Hub daegu;
	private Hub seoul;
	private Hub incheon;
	private Hub busan;

	@BeforeEach
	void setUp() {
		// given: 요구사항 토폴로지의 일부 — 중앙 허브 2개와 그 관할 허브들
		gyeonggiSouth = main("경기 남부 센터");
		daegu = main("대구광역시 센터");

		seoul = sub("서울특별시 센터", gyeonggiSouth);
		incheon = sub("인천광역시 센터", gyeonggiSouth);
		busan = sub("부산광역시 센터", daegu);
	}

	@Test
	@DisplayName("MAIN → MAIN 은 구간이 1개다")
	void mainToMainHasSingleSegment() {
		// when
		List<HubSegmentPair> segments = hubPathCalculator.calculate(gyeonggiSouth, daegu);

		// then
		assertThat(segments).containsExactly(
				new HubSegmentPair(gyeonggiSouth.getId(), daegu.getId()));
	}

	@Test
	@DisplayName("SUB → 자기 관할 MAIN 은 구간이 1개다")
	void subToOwnMainHasSingleSegment() {
		// when
		List<HubSegmentPair> segments = hubPathCalculator.calculate(seoul, gyeonggiSouth);

		// then
		assertThat(segments).containsExactly(
				new HubSegmentPair(seoul.getId(), gyeonggiSouth.getId()));
	}

	@Test
	@DisplayName("MAIN → 자기 관할 SUB 은 구간이 1개다")
	void mainToOwnSubHasSingleSegment() {
		// when
		List<HubSegmentPair> segments = hubPathCalculator.calculate(daegu, busan);

		// then
		assertThat(segments).containsExactly(
				new HubSegmentPair(daegu.getId(), busan.getId()));
	}

	@Test
	@DisplayName("같은 MAIN 관할의 SUB → SUB 은 중앙 허브를 거쳐 구간이 2개다")
	void subToSiblingSubHasTwoSegments() {
		// when
		List<HubSegmentPair> segments = hubPathCalculator.calculate(seoul, incheon);

		// then
		assertThat(segments).containsExactly(
				new HubSegmentPair(seoul.getId(), gyeonggiSouth.getId()),
				new HubSegmentPair(gyeonggiSouth.getId(), incheon.getId()));
	}

	@Test
	@DisplayName("SUB → 타 MAIN 은 구간이 2개다")
	void subToOtherMainHasTwoSegments() {
		// when
		List<HubSegmentPair> segments = hubPathCalculator.calculate(seoul, daegu);

		// then
		assertThat(segments).containsExactly(
				new HubSegmentPair(seoul.getId(), gyeonggiSouth.getId()),
				new HubSegmentPair(gyeonggiSouth.getId(), daegu.getId()));
	}

	@Test
	@DisplayName("MAIN → 타 MAIN 관할 SUB 은 구간이 2개다")
	void mainToOtherMainsSubHasTwoSegments() {
		// when
		List<HubSegmentPair> segments = hubPathCalculator.calculate(gyeonggiSouth, busan);

		// then
		assertThat(segments).containsExactly(
				new HubSegmentPair(gyeonggiSouth.getId(), daegu.getId()),
				new HubSegmentPair(daegu.getId(), busan.getId()));
	}

	@Test
	@DisplayName("타 MAIN 관할의 SUB → SUB 은 최대인 구간 3개가 된다")
	void subToOtherMainsSubHasThreeSegments() {
		// when
		List<HubSegmentPair> segments = hubPathCalculator.calculate(seoul, busan);

		// then
		assertThat(segments).containsExactly(
				new HubSegmentPair(seoul.getId(), gyeonggiSouth.getId()),
				new HubSegmentPair(gyeonggiSouth.getId(), daegu.getId()),
				new HubSegmentPair(daegu.getId(), busan.getId()));
	}

	private Hub main(String name) {
		return Hub.createMain(name, name + " 주소", BigDecimal.valueOf(37.5), BigDecimal.valueOf(127.0));
	}

	private Hub sub(String name, Hub parent) {
		return Hub.createSub(name, name + " 주소", BigDecimal.valueOf(37.5), BigDecimal.valueOf(127.0),
				parent.getId());
	}
}
