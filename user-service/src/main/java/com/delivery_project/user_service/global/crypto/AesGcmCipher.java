package com.delivery_project.user_service.global.crypto;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * DB에 저장하는 개인정보(name/slackId) 필드 암호화 전용 AES-256-GCM 유틸리티. 로그인용
 * 비밀번호 해시(BCrypt)와는 목적이 다르다 — 여기는 복호화해서 원래 값을 다시 봐야 하는
 * 필드를 다룬다.
 *
 * encrypt()는 매번 다른 암호문을 만들어(랜덤 IV) 의미론적 보안이 더 강하고, 동등성 검사가
 * 필요 없는 필드(name)에 쓴다. encryptDeterministic()은 같은 평문이면 항상 같은 암호문을
 * 만들어서(HMAC로 파생한 IV) DB의 부분 유니크 인덱스·중복확인이 그대로 동작해야 하는
 * 필드(slackId)에 쓴다 — 이 경우 "두 값이 같다"는 사실 자체는 암호문만으로도 드러나지만,
 * 이미 UNIQUE 제약이 있는 필드라 그 사실은 애플리케이션 레벨에서도 어차피 노출돼 있다.
 */
@Component
public class AesGcmCipher {

	private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
	private static final String MAC_ALGORITHM = "HmacSHA256";
	private static final int GCM_IV_LENGTH_BYTES = 12;
	private static final int GCM_TAG_LENGTH_BITS = 128;

	private final SecretKeySpec encryptionKey;
	private final SecretKeySpec ivDerivationKey;
	private final SecureRandom secureRandom = new SecureRandom();

	public AesGcmCipher(@Value("${encryption.key}") String rawKey) {
		byte[] masterKey = sha256(rawKey.getBytes(StandardCharsets.UTF_8));
		// 암호화 키와 IV 파생용 MAC 키를 분리한다 — 같은 키를 두 용도로 재사용하지 않기 위함.
		this.encryptionKey = new SecretKeySpec(hmacSha256(masterKey, "encryption"), "AES");
		this.ivDerivationKey = new SecretKeySpec(hmacSha256(masterKey, "iv-derivation"), MAC_ALGORITHM);
	}

	public String encrypt(String plaintext) {
		byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
		secureRandom.nextBytes(iv);
		return encryptWithIv(plaintext, iv);
	}

	public String encryptDeterministic(String plaintext) {
		byte[] iv = Arrays.copyOf(hmacSha256(ivDerivationKey.getEncoded(), plaintext), GCM_IV_LENGTH_BYTES);
		return encryptWithIv(plaintext, iv);
	}

	public String decrypt(String stored) {
		try {
			byte[] decoded = Base64.getDecoder().decode(stored);
			byte[] iv = Arrays.copyOfRange(decoded, 0, GCM_IV_LENGTH_BYTES);
			byte[] ciphertext = Arrays.copyOfRange(decoded, GCM_IV_LENGTH_BYTES, decoded.length);

			Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
			cipher.init(Cipher.DECRYPT_MODE, encryptionKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
			return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("복호화에 실패했습니다.", e);
		}
	}

	private String encryptWithIv(String plaintext, byte[] iv) {
		try {
			Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
			cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
			byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

			byte[] result = new byte[iv.length + ciphertext.length];
			System.arraycopy(iv, 0, result, 0, iv.length);
			System.arraycopy(ciphertext, 0, result, iv.length, ciphertext.length);
			return Base64.getEncoder().encodeToString(result);
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("암호화에 실패했습니다.", e);
		}
	}

	private static byte[] sha256(byte[] input) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(input);
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", e);
		}
	}

	private static byte[] hmacSha256(byte[] keyBytes, String data) {
		try {
			Mac mac = Mac.getInstance(MAC_ALGORITHM);
			mac.init(new SecretKeySpec(keyBytes, MAC_ALGORITHM));
			return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("HMAC-SHA256을 사용할 수 없습니다.", e);
		}
	}
}
