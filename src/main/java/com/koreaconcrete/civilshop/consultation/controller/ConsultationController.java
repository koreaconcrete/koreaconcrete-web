package com.koreaconcrete.civilshop.consultation.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.koreaconcrete.civilshop.common.api.PageResponse;
import com.koreaconcrete.civilshop.common.domain.ConsultationType;
import com.koreaconcrete.civilshop.common.security.UserPrincipal;
import com.koreaconcrete.civilshop.consultation.dto.ConsultationDtos.ConsultationRequest;
import com.koreaconcrete.civilshop.consultation.dto.ConsultationDtos.ConsultationResponse;
import com.koreaconcrete.civilshop.consultation.service.ConsultationService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/consultations")
public class ConsultationController {
	private final ConsultationService consultationService;

	public ConsultationController(ConsultationService consultationService) {
		this.consultationService = consultationService;
	}

	@Operation(summary = "상담요청 생성")
	@PostMapping
	public ConsultationResponse create(
			@AuthenticationPrincipal UserPrincipal principal,
			@Valid @RequestBody ConsultationRequest request
	) {
		return consultationService.create(principal, request);
	}

	@Operation(summary = "내 상담 목록")
	@GetMapping("/me")
	public PageResponse<ConsultationResponse> me(
			@AuthenticationPrincipal UserPrincipal principal,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "20") int size
	) {
		return consultationService.me(principal, page, size);
	}

	@Operation(summary = "문자 상담요청 생성")
	@PostMapping("/sms")
	public ConsultationResponse sms(
			@AuthenticationPrincipal UserPrincipal principal,
			@Valid @RequestBody ConsultationRequest request
	) {
		return consultationService.create(principal, consultationService.typedRequest(ConsultationType.SMS, request));
	}

	@Operation(summary = "전화 상담요청 생성")
	@PostMapping("/call-request")
	public ConsultationResponse call(
			@AuthenticationPrincipal UserPrincipal principal,
			@Valid @RequestBody ConsultationRequest request
	) {
		return consultationService.create(principal, consultationService.typedRequest(ConsultationType.PHONE, request));
	}
}
