package com.koreaconcrete.civilshop.product.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.koreaconcrete.civilshop.common.domain.FreightPolicy;
import com.koreaconcrete.civilshop.common.domain.ProductMediaType;
import com.koreaconcrete.civilshop.common.domain.ProductStatus;
import com.koreaconcrete.civilshop.common.domain.RelationType;
import com.koreaconcrete.civilshop.common.domain.VatPolicy;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class ProductDtos {
	private ProductDtos() {
	}

	public record CategoryBrief(Long id, String name) {
	}

	public record PriceBrief(Long id, Integer salePrice, VatPolicy vatPolicy, FreightPolicy freightPolicy) {
	}

	public record ProductListItem(
			Long id,
			Long categoryId,
			String categoryName,
			String sku,
			String name,
			String summary,
			String searchKeywords,
			String representativeImageUrl,
			String unit,
			ProductStatus status,
			Long representativeVariantId,
			String representativeVariantName,
			List<String> variantNames,
			Integer salePrice,
			VatPolicy vatPolicy,
			FreightPolicy freightPolicy
	) {
	}

	public record VariantResponse(
			Long id,
			String variantName,
			BigDecimal widthMm,
			BigDecimal lengthMm,
			BigDecimal heightMm,
			BigDecimal thicknessMm,
			BigDecimal weightKg,
			BigDecimal twentyFiveTonQuantity,
			String unit,
			String barcode,
			ProductStatus status,
			PriceBrief price
	) {
	}

	public record SpecResponse(Long id, Long variantId, String specKey, String specValue, Integer sortOrder) {
	}

	public record MediaResponse(Long id, Long variantId, ProductMediaType type, String url, String altText, Integer sortOrder) {
	}

	public record RelationResponse(Long id, Long targetProductId, String targetProductName, RelationType relationType, Integer sortOrder) {
	}

	public record ProductDetail(
			Long id,
			String sku,
			String name,
			String summary,
			String searchKeywords,
			String description,
			String unit,
			String originCountry,
			String manufacturer,
			Boolean customMade,
			Integer leadTimeDaysMin,
			Integer leadTimeDaysMax,
			ProductStatus status,
			CategoryBrief category,
			List<VariantResponse> variants,
			List<SpecResponse> specs,
			List<MediaResponse> media,
			List<RelationResponse> relations,
			LocalDateTime createdAt,
			LocalDateTime updatedAt
	) {
	}

	public record ProductRequest(
			@NotNull Long categoryId,
			String sku,
			@NotBlank String name,
			String summary,
			String searchKeywords,
			String description,
			String unit,
			String originCountry,
			String manufacturer,
			Boolean customMade,
			Integer leadTimeDaysMin,
			Integer leadTimeDaysMax,
			ProductStatus status,
			List<SpecRequest> specs,
			List<MediaRequest> media,
			List<RelationRequest> relations
	) {
	}

	public record ProductStatusRequest(@NotNull ProductStatus status) {
	}

	public record VariantRequest(
			@NotBlank String variantName,
			BigDecimal widthMm,
			BigDecimal lengthMm,
			BigDecimal heightMm,
			BigDecimal thicknessMm,
			BigDecimal weightKg,
			BigDecimal twentyFiveTonQuantity,
			String unit,
			String barcode,
			ProductStatus status
	) {
	}

	public record SpecRequest(Long variantId, @NotBlank String specKey, @NotBlank String specValue, Integer sortOrder) {
	}

	public record MediaRequest(Long variantId, ProductMediaType type, @NotBlank String url, String altText, Integer sortOrder) {
	}

	public record RelationRequest(@NotNull Long targetProductId, RelationType relationType, Integer sortOrder) {
	}
}
