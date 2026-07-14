package com.koreaconcrete.civilshop.consultation.dto;

import java.time.LocalDateTime;

import com.koreaconcrete.civilshop.common.domain.ConsultationStatus;
import com.koreaconcrete.civilshop.common.domain.ConsultationType;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class ConsultationDtos {
	private ConsultationDtos() {
	}

	public record ConsultationRequest(
			@NotNull ConsultationType type,
			Long productId,
			Long variantId,
			@NotBlank(message = "담당자명을 입력해주세요.") String contactName,
			@NotBlank(message = "연락처를 입력해주세요.") String contactPhone,
			@NotBlank(message = "상담 내용을 입력해주세요.") String message,
			@NotNull(message = "개인정보 수집 동의 여부를 확인해주세요.")
			@AssertTrue(message = "개인정보 수집에 동의해주세요.") Boolean privacyAgreed
	) {
	}

	public record ConsultationResponse(
			Long id,
			ConsultationType type,
			Long productId,
			String productName,
			Boolean productDeleted,
			Long variantId,
			String variantName,
			String contactName,
			String contactPhone,
			String message,
			ConsultationStatus status,
			String adminMemo,
			LocalDateTime createdAt,
			LocalDateTime updatedAt
	) {
	}

	public record ConsultationStatusRequest(@NotNull ConsultationStatus status) {
	}

	public record ConsultationReplyRequest(String adminMemo, ConsultationStatus status) {
	}
}
