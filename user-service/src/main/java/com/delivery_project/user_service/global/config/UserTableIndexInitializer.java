package com.delivery_project.user_service.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * username/slack_id의 유일성을 Hibernate가 관리하는 전체 테이블 UNIQUE 제약이 아니라,
 * 삭제되지 않은 행(deleted_at IS NULL)에 대해서만 적용하는 부분 유니크 인덱스로 강제한다.
 * 전체 테이블 제약이면 Soft Delete된 행까지 유일성 검사에 포함되어, 탈퇴한 사용자의
 * username/slack_id를 영구히 재사용할 수 없다(User.java 참고).
 *
 * ddl-auto가 테이블을 만들 때 쓰는 스키마 이름(hibernate.default_schema)을 그대로
 * 재사용해서, 운영 스키마(user_schema)와 테스트 스키마(public) 어느 쪽에서 실행되든
 * 동일하게 동작한다. ApplicationRunner로 등록해 Hibernate가 테이블 생성을 마친 뒤
 * (컨텍스트 초기화 완료 후)에만 실행되도록 순서를 보장한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserTableIndexInitializer implements ApplicationRunner {

	private final JdbcTemplate jdbcTemplate;

	@Value("${spring.jpa.properties.hibernate.default_schema:public}")
	private String schema;

	@Override
	public void run(ApplicationArguments args) {
		String table = schema + ".p_users";

		// User.java에서 @Column(unique = true)를 제거해도 Hibernate ddl-auto=update는
		// 이미 만들어둔 제약을 스스로 지우지 않는다. 이 서비스를 예전 코드로 한 번이라도
		// 띄워본 스키마에는 아래 두 전체 테이블 UNIQUE 제약이 남아있어서, 부분 인덱스를
		// 추가해도 여전히 막는다 — 명시적으로 제거한다. 처음 만든 스키마에는 애초에 이
		// 제약이 없어 두 구문 모두 아무 일도 하지 않는다.
		jdbcTemplate.execute("ALTER TABLE " + table + " DROP CONSTRAINT IF EXISTS uknxv056jdi7he17wqmeaxoc3t5");
		jdbcTemplate.execute("ALTER TABLE " + table + " DROP CONSTRAINT IF EXISTS ukphouvugf4gcoqs44fr765w4lm");

		jdbcTemplate.execute(
				"CREATE UNIQUE INDEX IF NOT EXISTS ux_users_username_active ON " + table
						+ " (username) WHERE deleted_at IS NULL");
		jdbcTemplate.execute(
				"CREATE UNIQUE INDEX IF NOT EXISTS ux_users_slack_id_active ON " + table
						+ " (slack_id) WHERE deleted_at IS NULL");

		log.info("[Schema] username/slack_id 부분 유니크 인덱스 확인 완료 schema={}", schema);
	}
}
