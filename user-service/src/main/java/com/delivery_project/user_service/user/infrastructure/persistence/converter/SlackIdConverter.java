package com.delivery_project.user_service.user.infrastructure.persistence.converter;

import com.delivery_project.user_service.global.crypto.AesGcmCipher;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;

/**
 * User.slackId를 DB에 저장/조회할 때 투명하게 암호화/복호화한다. slackId는 부분 유니크
 * 인덱스(UserTableSchemaInitializer)와 existsBySlackId 중복확인이 걸려있어서, 같은
 * 평문이면 항상 같은 암호문이 나오는 결정적 IV 방식을 쓴다 — 그래야 이 값이 컬럼 값 그대로
 * 비교되는 기존 쿼리/인덱스가 코드 변경 없이 그대로 동작한다.
 *
 * "두 계정이 같은 slackId를 쓰는지" 자체는 암호문만으로도 드러나지만, 이미 UNIQUE 제약이
 * 있는 필드라 그 사실은 애플리케이션 레벨(중복확인 API 응답)에서도 어차피 노출돼 있다.
 */
@Converter
@RequiredArgsConstructor
public class SlackIdConverter implements AttributeConverter<String, String> {

	private final AesGcmCipher cipher;

	@Override
	public String convertToDatabaseColumn(String attribute) {
		return attribute == null ? null : cipher.encryptDeterministic(attribute);
	}

	@Override
	public String convertToEntityAttribute(String dbData) {
		return dbData == null ? null : cipher.decrypt(dbData);
	}
}
