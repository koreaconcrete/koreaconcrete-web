package com.koreaconcrete.civilshop.pricing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.koreaconcrete.civilshop.common.domain.FreightPolicy;
import com.koreaconcrete.civilshop.common.domain.VatPolicy;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public final class PricingDtos {
	private PricingDtos() {
	}

	public record PriceSummary(
			Long id,
			Long productId,
			Long variantId,
			Integer salePrice,
			VatPolicy vatPolicy,
			FreightPolicy freightPolicy,
			BigDecimal minOrderQuantity,
			String priceNote,
			LocalDateTime createdAt
	) {
	}

	public record PriceBookResponse(
			Long id,
			String name,
			LocalDate effectiveFrom,
			LocalDate effectiveTo,
			Boolean defaultBook,
			LocalDateTime createdAt
	) {
	}

	public record PriceBookRequest(
			@NotBlank String name,
			@NotNull LocalDate effectiveFrom,
			LocalDate effectiveTo,
			Boolean defaultBook
	) {
	}

	public record ProductPriceRequest(
			@NotNull Long priceBookId,
			@NotNull Long productId,
			@NotNull Long variantId,
			@NotNull @Positive Integer salePrice,
			@NotNull VatPolicy vatPolicy,
			@NotNull FreightPolicy freightPolicy,
			@DecimalMin(value = "0.01") BigDecimal minOrderQuantity,
			String priceNote
	) {
	}
}
