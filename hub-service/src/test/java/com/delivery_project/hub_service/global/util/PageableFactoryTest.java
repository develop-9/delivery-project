package com.delivery_project.hub_service.global.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * 공통 페이징 규약 단위 테스트 (00_common.md).
 *
 * <p>핵심 계약은 <b>잘못된 값이 에러가 아니라 기본값으로 보정된다</b>는 것이다.
 * 목록 조회는 결과 0건도 정상 응답인 API 라, 파라미터 하나 때문에 조회가 실패하면 안 된다.
 */
class PageableFactoryTest {

	@Test
	@DisplayName("허용된 size 는 그대로 쓴다")
	void keepsAllowedSize() {
		// given & when
		Pageable pageable = PageableFactory.of(0, 30, null, null);

		// then
		assertThat(pageable.getPageSize()).isEqualTo(30);
	}

	@Test
	@DisplayName("허용되지 않은 size 는 에러가 아니라 10 으로 보정된다")
	void correctsDisallowedSizeToDefault() {
		// given: 10·30·50 이 아닌 값
		// when
		Pageable pageable = PageableFactory.of(0, 17, null, null);

		// then
		assertThat(pageable.getPageSize()).isEqualTo(10);
	}

	@Test
	@DisplayName("음수 page 는 0 으로 보정된다")
	void correctsNegativePageToZero() {
		// given & when
		Pageable pageable = PageableFactory.of(-5, null, null, null);

		// then
		assertThat(pageable.getPageNumber()).isZero();
	}

	@Test
	@DisplayName("파라미터를 하나도 주지 않으면 createdAt desc 10건이다")
	void appliesDefaultsWhenNothingGiven() {
		// given & when
		Pageable pageable = PageableFactory.of(null, null, null, null);

		// then
		assertThat(pageable.getPageNumber()).isZero();
		assertThat(pageable.getPageSize()).isEqualTo(10);
		assertThat(pageable.getSort().getOrderFor("createdAt")).isNotNull();
		assertThat(pageable.getSort().getOrderFor("createdAt").getDirection())
				.isEqualTo(Sort.Direction.DESC);
	}

	@Test
	@DisplayName("허용된 sort 와 direction 은 그대로 쓴다")
	void keepsAllowedSortAndDirection() {
		// given & when
		Pageable pageable = PageableFactory.of(0, 10, "updatedAt", "asc");

		// then
		assertThat(pageable.getSort().getOrderFor("updatedAt")).isNotNull();
		assertThat(pageable.getSort().getOrderFor("updatedAt").getDirection())
				.isEqualTo(Sort.Direction.ASC);
	}

	@Test
	@DisplayName("허용되지 않은 sort 는 createdAt 으로 되돌린다")
	void correctsDisallowedSortToDefault() {
		// given: 정렬 허용값은 createdAt·updatedAt 뿐이다
		// when
		Pageable pageable = PageableFactory.of(0, 10, "name", "asc");

		// then
		assertThat(pageable.getSort().getOrderFor("name")).isNull();
		assertThat(pageable.getSort().getOrderFor("createdAt")).isNotNull();
	}

	@Test
	@DisplayName("허용되지 않은 direction 은 desc 로 되돌린다")
	void correctsDisallowedDirectionToDefault() {
		// given & when
		Pageable pageable = PageableFactory.of(0, 10, "createdAt", "sideways");

		// then
		assertThat(pageable.getSort().getOrderFor("createdAt").getDirection())
				.isEqualTo(Sort.Direction.DESC);
	}
}
