package com.koreaconcrete.civilshop.user.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.koreaconcrete.civilshop.common.security.UserPrincipal;
import com.koreaconcrete.civilshop.user.dto.UserDtos.ChangePasswordRequest;
import com.koreaconcrete.civilshop.user.dto.UserDtos.UpdateMeRequest;
import com.koreaconcrete.civilshop.user.dto.UserDtos.UserSummary;
import com.koreaconcrete.civilshop.user.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@Operation(summary = "내 정보 조회")
	@GetMapping("/me")
	public UserSummary me(@AuthenticationPrincipal UserPrincipal principal) {
		return userService.me(principal);
	}

	@Operation(summary = "내 정보 수정")
	@PatchMapping("/me")
	public UserSummary updateMe(
			@AuthenticationPrincipal UserPrincipal principal,
			@Valid @RequestBody UpdateMeRequest request
	) {
		return userService.updateMe(principal, request);
	}

	@Operation(summary = "내 비밀번호 변경")
	@PatchMapping("/me/password")
	public UserSummary changePassword(
			@AuthenticationPrincipal UserPrincipal principal,
			@Valid @RequestBody ChangePasswordRequest request
	) {
		return userService.changePassword(principal, request);
	}
}
