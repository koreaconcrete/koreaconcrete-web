package com.koreaconcrete.civilshop.freight.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.koreaconcrete.civilshop.freight.dto.FreightDtos.FreightEstimateRequest;
import com.koreaconcrete.civilshop.freight.dto.FreightDtos.FreightEstimateResponse;
import com.koreaconcrete.civilshop.freight.dto.FreightDtos.LoadingRuleResponse;
import com.koreaconcrete.civilshop.freight.service.FreightService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class FreightController {
	private final FreightService freightService;

	public FreightController(FreightService freightService) {
		this.freightService = freightService;
	}

	@Operation(summary = "상품 상차 규칙 조회")
	@GetMapping("/products/{id}/loading-rules")
	public List<LoadingRuleResponse> loadingRules(@PathVariable Long id) {
		return freightService.loadingRules(id);
	}

	@Operation(summary = "운반비 예상 계산")
	@PostMapping("/freight/estimate")
	public FreightEstimateResponse estimate(@Valid @RequestBody FreightEstimateRequest request) {
		return freightService.estimate(request);
	}
}
