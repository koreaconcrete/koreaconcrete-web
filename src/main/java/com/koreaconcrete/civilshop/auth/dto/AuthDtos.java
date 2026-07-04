package com.koreaconcrete.civilshop.auth.dto;

import java.util.List;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
	private AuthDtos() {
	}

	public record SignupRequest(
			@NotBlank(message = "아이디를 입력해주세요.")
			@Size(min = 4, max = 40, message = "아이디는 4~40자로 입력해주세요.")
			@Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "아이디는 영문, 숫자, 마침표, 밑줄, 하이픈만 사용할 수 있습니다.")
			String email,
			@NotBlank(message = "비밀번호를 입력해주세요.") @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.") String password,
			@NotBlank(message = "이름을 입력해주세요.") String name,
			String phone,
			@NotNull(message = "개인정보 수집 동의 여부를 확인해주세요.") @AssertTrue(message = "개인정보 수집에 동의해주세요.") Boolean privacyAgreed,
			@NotNull(message = "약관 동의 여부를 확인해주세요.") @AssertTrue(message = "약관에 동의해주세요.") Boolean termsAgreed,
			Boolean marketingAgreed
	) {
	}

	public record LoginRequest(
			@NotBlank(message = "아이디를 입력해주세요.") String email,
			@NotBlank(message = "비밀번호를 입력해주세요.") String password
	) {
	}

	public record AuthUser(Long id, String email, String name, List<String> roles) {
	}

	public record AuthResponse(String accessToken, String tokenType, AuthUser user) {
		public static AuthResponse bearer(String accessToken, AuthUser user) {
			return new AuthResponse(accessToken, "Bearer", user);
		}
	}
}
