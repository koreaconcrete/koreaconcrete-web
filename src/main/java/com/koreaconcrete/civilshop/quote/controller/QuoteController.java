package com.koreaconcrete.civilshop.quote.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.koreaconcrete.civilshop.common.api.PageResponse;
import com.koreaconcrete.civilshop.common.security.UserPrincipal;
import com.koreaconcrete.civilshop.quote.dto.QuoteDtos.QuoteRequestCreate;
import com.koreaconcrete.civilshop.quote.dto.QuoteDtos.QuoteResponse;
import com.koreaconcrete.civilshop.quote.service.QuoteService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/quotes")
public class QuoteController {
	private final QuoteService quoteService;

	public QuoteController(QuoteService quoteService) {
		this.quoteService = quoteService;
	}

	@Operation(summary = "견적요청 생성")
	@PostMapping
	public QuoteResponse create(
			@AuthenticationPrincipal UserPrincipal principal,
			@Valid @RequestBody QuoteRequestCreate request
	) {
		return quoteService.create(principal, request);
	}

	@Operation(summary = "내 견적 목록")
	@GetMapping("/me")
	public PageResponse<QuoteResponse> me(
			@AuthenticationPrincipal UserPrincipal principal,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "20") int size
	) {
		return quoteService.me(principal, page, size);
	}

	@Operation(summary = "견적 상세")
	@GetMapping("/{quoteId}")
	public QuoteResponse detail(@PathVariable Long quoteId, @AuthenticationPrincipal UserPrincipal principal) {
		return quoteService.detail(quoteId, principal);
	}

	@Operation(summary = "견적 승인")
	@PostMapping("/{quoteId}/approve")
	public QuoteResponse approve(@PathVariable Long quoteId, @AuthenticationPrincipal UserPrincipal principal) {
		return quoteService.approve(quoteId, principal);
	}

	@Operation(summary = "견적 취소")
	@PostMapping("/{quoteId}/cancel")
	public QuoteResponse cancel(@PathVariable Long quoteId, @AuthenticationPrincipal UserPrincipal principal) {
		return quoteService.cancel(quoteId, principal);
	}
}
