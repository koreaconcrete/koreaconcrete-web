package com.koreaconcrete.civilshop.freight.dto;

import java.math.BigDecimal;
import java.util.List;

import com.koreaconcrete.civilshop.common.domain.VehicleType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public final class FreightDtos {
	private FreightDtos() {
	}

	public record LoadingRuleResponse(
			Long id,
			Long productId,
			Long variantId,
			VehicleType vehicleType,
			BigDecimal loadQuantity,
			String loadUnit,
			BigDecimal palletQuantity,
			BigDecimal palletWeightKg,
			String note
	) {
	}

	public record LoadingRuleRequest(
			@NotNull Long productId,
			@NotNull Long variantId,
			@NotNull VehicleType vehicleType,
			@NotNull @DecimalMin(value = "0.01") BigDecimal loadQuantity,
			@NotNull String loadUnit,
			BigDecimal palletQuantity,
			BigDecimal palletWeightKg,
			String note
	) {
	}

	public record FreightRateRuleRequest(
			@NotNull String originRegion,
			@NotNull String destinationRegion,
			@NotNull VehicleType vehicleType,
			@NotNull Integer baseFreightAmount,
			BigDecimal surchargeRate,
			BigDecimal fuelSurchargeRate,
			Boolean active
	) {
	}

	public record FreightRateRuleResponse(
			Long id,
			String originRegion,
			String destinationRegion,
			VehicleType vehicleType,
			Integer baseFreightAmount,
			BigDecimal surchargeRate,
			BigDecimal fuelSurchargeRate,
			Boolean active
	) {
	}

	public record FreightEstimateItemRequest(
			@NotNull Long productId,
			@NotNull Long variantId,
			@NotNull @DecimalMin(value = "0.01") BigDecimal quantity
	) {
	}

	public record FreightEstimateRequest(
			@NotEmpty List<@Valid FreightEstimateItemRequest> items,
			@NotNull String destinationAddress,
			VehicleType preferredVehicleType
	) {
	}

	public record FreightEstimateItemResponse(
			Long productId,
			Long variantId,
			BigDecimal quantity,
			BigDecimal loadQuantity,
			Integer vehicleCount,
			Integer unitFreightAmount,
			Integer freightAmount
	) {
	}

	public record FreightEstimateResponse(
			Long estimateId,
			VehicleType vehicleType,
			Integer baseFreightAmount,
			Integer totalFreightAmount,
			List<FreightEstimateItemResponse> items,
			List<String> notes
	) {
	}
}
