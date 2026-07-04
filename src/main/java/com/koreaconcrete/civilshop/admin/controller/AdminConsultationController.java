package com.koreaconcrete.civilshop.admin.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.koreaconcrete.civilshop.common.api.PageResponse;
import com.koreaconcrete.civilshop.common.domain.ConsultationStatus;
import com.koreaconcrete.civilshop.common.security.UserPrincipal;
import com.koreaconcrete.civilshop.consultation.dto.ConsultationDtos.ConsultationReplyRequest;
import com.koreaconcrete.civilshop.consultation.dto.ConsultationDtos.ConsultationResponse;
import com.koreaconcrete.civilshop.consultation.dto.ConsultationDtos.ConsultationStatusRequest;
import com.koreaconcrete.civilshop.consultation.service.ConsultationService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/consultations")
public class AdminConsultationController {
	private final ConsultationService consultationService;

	public AdminConsultationController(ConsultationService consultationService) {
		this.consultationService = consultationService;
	}

	@Operation(summary = "관리자 상담 목록")
	@GetMapping
	public PageResponse<ConsultationResponse> list(
			@RequestParam(required = false) ConsultationStatus status,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "20") int size
	) {
		return consultationService.adminList(status, page, size);
	}

	@Operation(summary = "관리자 상담 상세")
	@GetMapping("/{id}")
	public ConsultationResponse detail(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
		return consultationService.detail(id, principal);
	}

	@Operation(summary = "관리자 상담 상태 변경")
	@PatchMapping("/{id}/status")
	public ConsultationResponse updateStatus(@PathVariable Long id, @Valid @RequestBody ConsultationStatusRequest request) {
		return consultationService.updateStatus(id, request);
	}

	@Operation(summary = "관리자 상담 답변/메모")
	@PostMapping("/{id}/reply")
	public ConsultationResponse reply(@PathVariable Long id, @RequestBody ConsultationReplyRequest request) {
		return consultationService.reply(id, request);
	}
}
