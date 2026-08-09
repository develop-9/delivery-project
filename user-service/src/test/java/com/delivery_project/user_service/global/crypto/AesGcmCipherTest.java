package com.delivery_project.user_service.global.crypto;

import static org.assertj.core.api.Assertions.assertThat;

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
}
