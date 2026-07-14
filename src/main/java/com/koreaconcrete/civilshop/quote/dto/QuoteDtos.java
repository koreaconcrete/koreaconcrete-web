package com.koreaconcrete.civilshop.quote.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.koreaconcrete.civilshop.common.domain.QuoteStatus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
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
			@NotBlank(message = "회사명을 입력해주세요.") String companyName,
			@NotBlank(message = "담당자명을 입력해주세요.") String contactName,
			@NotBlank(message = "연락처를 입력해주세요.") String contactPhone,
			@NotBlank(message = "현장주소를 입력해주세요.") String siteAddress,
			LocalDate requestedDeliveryDate,
			Boolean deliveryDateUndecided,
			String memo,
			@NotNull(message = "개인정보 수집 동의 여부를 확인해주세요.")
			@AssertTrue(message = "개인정보 수집에 동의해주세요.") Boolean privacyAgreed,
			@NotEmpty List<@Valid QuoteItemRequest> items
	) {
		@AssertTrue(message = "희망 납기일을 선택하거나 미정을 체크해주세요.")
		public boolean isDeliveryDateSelected() {
			return requestedDeliveryDate != null || Boolean.TRUE.equals(deliveryDateUndecided);
		}
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
