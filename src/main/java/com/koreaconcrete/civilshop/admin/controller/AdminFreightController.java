package com.koreaconcrete.civilshop.admin.controller;

import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.koreaconcrete.civilshop.freight.dto.FreightDtos.FreightRateRuleRequest;
import com.koreaconcrete.civilshop.freight.dto.FreightDtos.FreightRateRuleResponse;
import com.koreaconcrete.civilshop.freight.dto.FreightDtos.LoadingRuleRequest;
import com.koreaconcrete.civilshop.freight.dto.FreightDtos.LoadingRuleResponse;
import com.koreaconcrete.civilshop.freight.service.FreightService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminFreightController {
	private final FreightService freightService;

	public AdminFreightController(FreightService freightService) {
		this.freightService = freightService;
	}

	@Operation(summary = "상차 규칙 생성")
	@PostMapping("/loading-rules")
	public LoadingRuleResponse createLoadingRule(@Valid @RequestBody LoadingRuleRequest request) {
		return freightService.createLoadingRule(request);
	}

	@Operation(summary = "상차 규칙 수정")
	@PatchMapping("/loading-rules/{ruleId}")
	public LoadingRuleResponse updateLoadingRule(@PathVariable Long ruleId, @Valid @RequestBody LoadingRuleRequest request) {
		return freightService.updateLoadingRule(ruleId, request);
	}

	@Operation(summary = "운반비 규칙 생성")
	@PostMapping("/freight-rate-rules")
	public FreightRateRuleResponse createRateRule(@Valid @RequestBody FreightRateRuleRequest request) {
		return freightService.createRateRule(request);
	}

	@Operation(summary = "운반비 규칙 수정")
	@PatchMapping("/freight-rate-rules/{ruleId}")
	public FreightRateRuleResponse updateRateRule(@PathVariable Long ruleId, @Valid @RequestBody FreightRateRuleRequest request) {
		return freightService.updateRateRule(ruleId, request);
	}
}
