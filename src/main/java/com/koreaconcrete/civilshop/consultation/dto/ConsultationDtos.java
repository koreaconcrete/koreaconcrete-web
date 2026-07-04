package com.koreaconcrete.civilshop.consultation.dto;

import java.time.LocalDateTime;

import com.koreaconcrete.civilshop.common.domain.ConsultationStatus;
import com.koreaconcrete.civilshop.common.domain.ConsultationType;

import jakarta.validation.constraints.NotNull;

public final class ConsultationDtos {
	private ConsultationDtos() {
	}

	public record ConsultationRequest(
			@NotNull ConsultationType type,
			Long productId,
			Long variantId,
			@NotNull String contactName,
			@NotNull String contactPhone,
			String message,
			Boolean privacyAgreed
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
