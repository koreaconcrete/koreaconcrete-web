package com.koreaconcrete.civilshop.auth.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.koreaconcrete.civilshop.auth.dto.AuthDtos.AuthResponse;
import com.koreaconcrete.civilshop.auth.dto.AuthDtos.LoginRequest;
import com.koreaconcrete.civilshop.auth.dto.AuthDtos.SignupRequest;
import com.koreaconcrete.civilshop.auth.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@Operation(summary = "회원가입")
	@PostMapping("/signup")
	public AuthResponse signup(@Valid @RequestBody SignupRequest request) {
		return authService.signup(request);
	}

	@Operation(summary = "로그인")
	@PostMapping("/login")
	public AuthResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
	}

	@Operation(summary = "아이디 중복 확인")
	@GetMapping("/login-id-available")
	public Map<String, Boolean> loginIdAvailable(@RequestParam String loginId) {
		return Map.of("available", authService.isLoginIdAvailable(loginId));
	}

	@Operation(summary = "로그아웃")
	@PostMapping("/logout")
	public void logout() {
	}
}
