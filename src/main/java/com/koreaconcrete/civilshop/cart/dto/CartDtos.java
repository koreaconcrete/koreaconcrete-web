package com.koreaconcrete.civilshop.cart.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public final class CartDtos {
	private CartDtos() {
	}

	public record CartItemResponse(
			Long id,
			Long productId,
			String productName,
			String productSummary,
			String productImageUrl,
			Long variantId,
			String variantName,
			BigDecimal quantity,
			Integer unitPriceSnapshot
	) {
	}

	public record CartResponse(Long id, List<CartItemResponse> items) {
	}

	public record AddCartItemRequest(
			@NotNull Long productId,
			@NotNull Long variantId,
			@NotNull @DecimalMin(value = "0.01") BigDecimal quantity
	) {
	}

	public record UpdateCartItemRequest(@NotNull @DecimalMin(value = "0.01") BigDecimal quantity) {
	}

	public record CartToQuoteRequest(
			@NotNull String companyName,
			@NotNull String contactName,
			@NotNull String contactPhone,
			@NotNull String siteAddress,
			LocalDate requestedDeliveryDate,
			String memo,
			@NotNull Boolean privacyAgreed
	) {
	}
}
