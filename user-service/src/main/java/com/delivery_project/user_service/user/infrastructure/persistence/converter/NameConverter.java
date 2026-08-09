package com.delivery_project.user_service.user.infrastructure.persistence.converter;

import com.delivery_project.user_service.global.crypto.AesGcmCipher;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;

/**
 * User.name을 DB에 저장/조회할 때 투명하게 암호화/복호화한다. name은 동등성 검사(중복확인,
 * 인덱스)가 필요 없는 필드라 매번 다른 암호문을 만드는 랜덤 IV 방식을 쓴다 — SlackIdConverter
 * 참고.
 */
@Converter
@RequiredArgsConstructor
public class NameConverter implements AttributeConverter<String, String> {

	private final AesGcmCipher cipher;

	@Override
	public String convertToDatabaseColumn(String attribute) {
		return attribute == null ? null : cipher.encrypt(attribute);
	}

	@Override
	public String convertToEntityAttribute(String dbData) {
		return dbData == null ? null : cipher.decrypt(dbData);
	}
}
