package com.koreaconcrete.civilshop.common.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.koreaconcrete.civilshop.common.exception.BusinessException;

@Component
public class JwtTokenProvider {
	private static final String HMAC_ALGORITHM = "HmacSHA256";
	private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
	private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

	private final ObjectMapper objectMapper;
	private final String secret;
	private final long expirationSeconds;

	public JwtTokenProvider(
			ObjectMapper objectMapper,
			@Value("${app.jwt.secret:}") String secret,
			@Value("${app.jwt.expiration-seconds:3600}") long expirationSeconds
	) {
		this.objectMapper = objectMapper;
		this.secret = secret;
		this.expirationSeconds = expirationSeconds;
	}

	public String createToken(Long userId, String email, List<String> roles) {
		if (!StringUtils.hasText(secret)) {
			throw BusinessException.badRequest("JWT_SECRET 환경변수를 설정해주세요.");
		}
		try {
			Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
			Instant now = Instant.now();
			Map<String, Object> payload = new LinkedHashMap<>();
			payload.put("sub", email);
			payload.put("uid", userId);
			payload.put("roles", roles);
			payload.put("iat", now.getEpochSecond());
			payload.put("exp", now.plusSeconds(expirationSeconds).getEpochSecond());

			String headerPart = encodeJson(header);
			String payloadPart = encodeJson(payload);
			String signature = sign(headerPart + "." + payloadPart);
			return headerPart + "." + payloadPart + "." + signature;
		} catch (Exception exception) {
			throw BusinessException.badRequest("토큰 생성에 실패했습니다.");
		}
	}

	public UserPrincipal parse(String token) {
		try {
			String[] parts = token.split("\\.");
			if (parts.length != 3) {
				throw BusinessException.unauthorized("유효하지 않은 토큰입니다.");
			}
			String signed = parts[0] + "." + parts[1];
			String expected = sign(signed);
			if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), parts[2].getBytes(StandardCharsets.UTF_8))) {
				throw BusinessException.unauthorized("토큰 서명이 유효하지 않습니다.");
			}
			Map<String, Object> payload = objectMapper.readValue(DECODER.decode(parts[1]), new TypeReference<>() {
			});
			long exp = ((Number) payload.get("exp")).longValue();
			if (Instant.now().getEpochSecond() >= exp) {
				throw BusinessException.unauthorized("토큰이 만료되었습니다.");
			}
			Long userId = ((Number) payload.get("uid")).longValue();
			String email = (String) payload.get("sub");
			@SuppressWarnings("unchecked")
			List<String> roles = (List<String>) payload.get("roles");
			return new UserPrincipal(userId, email, roles);
		} catch (BusinessException exception) {
			throw exception;
		} catch (Exception exception) {
			throw BusinessException.unauthorized("유효하지 않은 토큰입니다.");
		}
	}

	private String encodeJson(Object value) throws Exception {
		return ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
	}

	private String sign(String value) throws Exception {
		if (!StringUtils.hasText(secret)) {
			throw BusinessException.unauthorized("JWT_SECRET 환경변수를 설정해주세요.");
		}
		Mac mac = Mac.getInstance(HMAC_ALGORITHM);
		mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
		return ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
	}
}
