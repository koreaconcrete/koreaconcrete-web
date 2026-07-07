package com.koreaconcrete.civilshop.admin.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.koreaconcrete.civilshop.pricing.dto.PricingDtos.PriceBookRequest;
import com.koreaconcrete.civilshop.pricing.dto.PricingDtos.PriceBookResponse;
import com.koreaconcrete.civilshop.pricing.dto.PricingDtos.PriceSummary;
import com.koreaconcrete.civilshop.pricing.dto.PricingDtos.ProductPriceRequest;
import com.koreaconcrete.civilshop.pricing.service.PricingService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminPricingController {
	private final PricingService pricingService;

	public AdminPricingController(PricingService pricingService) {
		this.pricingService = pricingService;
	}

	@Operation(summary = "가격표 목록")
	@GetMapping("/price-books")
	public List<PriceBookResponse> priceBooks() {
		return pricingService.priceBooks();
	}

	@Operation(summary = "가격표 생성")
	@PostMapping("/price-books")
	public PriceBookResponse createBook(@Valid @RequestBody PriceBookRequest request) {
		return pricingService.createBook(request);
	}

	@Operation(summary = "상품 가격 생성")
	@PostMapping("/product-prices")
	public PriceSummary createPrice(@Valid @RequestBody ProductPriceRequest request) {
		return pricingService.createPrice(request);
	}

	@Operation(summary = "상품 가격 수정")
	@PatchMapping("/product-prices/{priceId}")
	public PriceSummary updatePrice(@PathVariable Long priceId, @Valid @RequestBody ProductPriceRequest request) {
		return pricingService.updatePrice(priceId, request);
	}

	@Operation(summary = "상품 가격 삭제")
	@DeleteMapping("/product-prices/{priceId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deletePrice(@PathVariable Long priceId) {
		pricingService.deletePrice(priceId);
	}
}
