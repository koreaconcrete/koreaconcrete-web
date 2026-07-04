package com.koreaconcrete.civilshop.quote.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.koreaconcrete.civilshop.common.domain.QuoteStatus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public final class QuoteDtos {
	private QuoteDtos() {
	}

	public record QuoteItemRequest(
			@NotNull Long productId,
			@NotNull Long variantId,
			@NotNull @DecimalMin(value = "0.01") BigDecimal quantity
	) {
	}

	public record QuoteRequestCreate(
			@NotNull String companyName,
			@NotNull String contactName,
			@NotNull String contactPhone,
			@NotNull String siteAddress,
			LocalDate requestedDeliveryDate,
			String memo,
			@NotNull Boolean privacyAgreed,
			@NotEmpty List<@Valid QuoteItemRequest> items
	) {
	}

	public record QuoteItemResponse(
			Long id,
			Long productId,
			String productName,
			Boolean productDeleted,
			String productSummary,
			String productImageUrl,
			Long variantId,
			String variantName,
			BigDecimal quantity,
			Integer unitPrice,
			Integer freightAmount,
			Integer vatAmount,
			Integer totalAmount,
			String note
	) {
	}

	public record QuoteResponse(
			Long id,
			String requestNo,
			String companyName,
			String contactName,
			String contactPhone,
			String siteAddress,
			LocalDate requestedDeliveryDate,
			String memo,
			Boolean privacyAgreed,
			QuoteStatus status,
			List<QuoteItemResponse> items,
			LocalDateTime createdAt,
			LocalDateTime updatedAt
	) {
	}

	public record QuoteStatusRequest(@NotNull QuoteStatus status) {
	}

	public record AdminQuoteItemRequest(
			@NotNull Long productId,
			@NotNull Long variantId,
			@NotNull @DecimalMin(value = "0.01") BigDecimal quantity,
			Integer unitPrice,
			Integer freightAmount,
			Integer vatAmount,
			Integer totalAmount,
			String note
	) {
	}
}
