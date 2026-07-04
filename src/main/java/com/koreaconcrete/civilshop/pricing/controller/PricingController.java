package com.koreaconcrete.civilshop.pricing.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.koreaconcrete.civilshop.pricing.dto.PricingDtos.PriceSummary;
import com.koreaconcrete.civilshop.pricing.service.PricingService;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/v1/products")
public class PricingController {
	private final PricingService pricingService;

	public PricingController(PricingService pricingService) {
		this.pricingService = pricingService;
	}

	@Operation(summary = "상품 가격 조회")
	@GetMapping("/{id}/prices")
	public List<PriceSummary> productPrices(@PathVariable Long id) {
		return pricingService.productPrices(id);
	}
}
