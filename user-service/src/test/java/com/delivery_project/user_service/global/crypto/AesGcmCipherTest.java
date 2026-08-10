package com.delivery_project.user_service.global.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AesGcmCipherTest {

	private final AesGcmCipher cipher = new AesGcmCipher("test-encryption-key-for-unit-tests");

	@Test
	void encrypt로_암호화한_값을_decrypt하면_원래_평문이_나온다() {
		// given
		String plaintext = "김철수";

		// when
		String encrypted = cipher.encrypt(plaintext);
		String decrypted = cipher.decrypt(encrypted);

		// then
		assertThat(decrypted).isEqualTo(plaintext);
	}

	@Test
	void encrypt는_같은_평문이어도_호출할_때마다_다른_암호문을_만든다() {
		// given
		String plaintext = "U0123456789";

		// when
		String first = cipher.encrypt(plaintext);
		String second = cipher.encrypt(plaintext);

		// then
		assertThat(first).isNotEqualTo(second);
	}

	@Test
	void encryptDeterministic로_암호화한_값을_decrypt하면_원래_평문이_나온다() {
		// given
		String plaintext = "U0123456789";

		// when
		String encrypted = cipher.encryptDeterministic(plaintext);
		String decrypted = cipher.decrypt(encrypted);

		// then
		assertThat(decrypted).isEqualTo(plaintext);
	}

	@Test
	void encryptDeterministic는_같은_평문이면_항상_같은_암호문을_만든다() {
		// given
		String plaintext = "U0123456789";

		// when
		String first = cipher.encryptDeterministic(plaintext);
		String second = cipher.encryptDeterministic(plaintext);

		// then
		assertThat(first).isEqualTo(second);
	}

	@Test
	void encryptDeterministic는_다른_평문이면_다른_암호문을_만든다() {
		// given & when
		String first = cipher.encryptDeterministic("U0123456789");
		String second = cipher.encryptDeterministic("U9999999999");

		// then
		assertThat(first).isNotEqualTo(second);
	}

	/**
	 * DB 값이 손상돼 Base64 디코딩 자체가 실패하는 경우(수동 편집, 마이그레이션 오류 등)를
	 * 재현한다. IllegalArgumentException이 그대로 새면 GlobalExceptionHandler가 "잘못된
	 * 요청"(400)으로 오분류하므로, GeneralSecurityException과 동일하게 IllegalStateException으로
	 * 변환돼야 한다.
	 */
	@Test
	void decrypt는_Base64가_아닌_값에_대해_IllegalStateException을_던진다() {
		assertThatThrownBy(() -> cipher.decrypt("not-valid-base64!!!"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("복호화에 실패했습니다.");
	}

	/**
	 * Base64로는 유효해도 IV(12바이트)조차 담을 수 없을 만큼 짧은 값을 재현한다.
	 */
	@Test
	void decrypt는_IV_길이보다_짧은_값에_대해_IllegalStateException을_던진다() {
		String tooShort = java.util.Base64.getEncoder().encodeToString(new byte[]{1, 2, 3});

		assertThatThrownBy(() -> cipher.decrypt(tooShort))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("복호화에 실패했습니다.");
	}
}
