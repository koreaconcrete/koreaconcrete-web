package com.koreaconcrete.civilshop.user.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.koreaconcrete.civilshop.common.domain.UserStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class UserDtos {
	private UserDtos() {
	}

	public record UserSummary(
			Long id,
			String email,
			String name,
			String phone,
			UserStatus status,
			List<String> roles,
			LocalDateTime createdAt,
			LocalDateTime lastLoginAt
	) {
	}

	public record UpdateMeRequest(
			@NotBlank(message = "이름을 입력해주세요.")
			@Size(max = 100, message = "이름은 100자 이하로 입력해주세요.")
			String name,
			@Size(max = 40, message = "연락처는 40자 이하로 입력해주세요.")
			String phone
	) {
	}

	public record ChangePasswordRequest(
			@NotBlank(message = "현재 비밀번호를 입력해주세요.")
			String currentPassword,
			@NotBlank(message = "새 비밀번호를 입력해주세요.")
			@Size(min = 8, message = "새 비밀번호는 8자 이상이어야 합니다.")
			String newPassword
	) {
	}

	public record AdminCreateRequest(
			@NotBlank(message = "아이디를 입력해주세요.")
			@Size(min = 4, max = 40, message = "아이디는 4~40자로 입력해주세요.")
			@Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "아이디는 영문, 숫자, 마침표, 밑줄, 하이픈만 사용할 수 있습니다.")
			String loginId,
			@NotBlank(message = "비밀번호를 입력해주세요.")
			@Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
			String password,
			@NotBlank(message = "이름을 입력해주세요.")
			@Size(max = 100, message = "이름은 100자 이하로 입력해주세요.")
			String name,
			@Size(max = 40, message = "연락처는 40자 이하로 입력해주세요.")
			String phone,
			@NotBlank(message = "권한을 선택해주세요.")
			String role
	) {
	}

	public record AdminUserItem(
			Long id,
			String email,
			String name,
			String phone,
			UserStatus status,
			List<String> roles,
			LocalDateTime createdAt,
			LocalDateTime lastLoginAt
	) {
	}
}
