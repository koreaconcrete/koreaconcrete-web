package com.koreaconcrete.civilshop.admin.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.koreaconcrete.civilshop.common.api.PageResponse;
import com.koreaconcrete.civilshop.common.domain.QuoteStatus;
import com.koreaconcrete.civilshop.quote.dto.QuoteDtos.AdminQuoteItemRequest;
import com.koreaconcrete.civilshop.quote.dto.QuoteDtos.QuoteResponse;
import com.koreaconcrete.civilshop.quote.dto.QuoteDtos.QuoteStatusRequest;
import com.koreaconcrete.civilshop.quote.service.QuoteService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/quotes")
public class AdminQuoteController {
	private final QuoteService quoteService;

	public AdminQuoteController(QuoteService quoteService) {
		this.quoteService = quoteService;
	}

	@Operation(summary = "관리자 견적 목록")
	@GetMapping
	public PageResponse<QuoteResponse> list(
			@RequestParam(required = false) QuoteStatus status,
			@RequestParam(required = false) String bucket,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "20") int size
	) {
		return quoteService.adminList(status, bucket, page, size);
	}

	@Operation(summary = "관리자 견적 상세")
	@GetMapping("/{quoteId}")
	public QuoteResponse detail(@PathVariable Long quoteId) {
		return quoteService.detail(quoteId, null);
	}

	@Operation(summary = "관리자 견적 상태 변경")
	@PatchMapping("/{quoteId}/status")
	public QuoteResponse updateStatus(@PathVariable Long quoteId, @Valid @RequestBody QuoteStatusRequest request) {
		return quoteService.updateStatus(quoteId, request);
	}

	@Operation(summary = "관리자 견적 품목 추가")
	@PostMapping("/{quoteId}/quote-items")
	public QuoteResponse addItem(@PathVariable Long quoteId, @Valid @RequestBody AdminQuoteItemRequest request) {
		return quoteService.addAdminItem(quoteId, request);
	}

	@Operation(summary = "관리자 견적 발송 처리")
	@PostMapping("/{quoteId}/send")
	public QuoteResponse send(@PathVariable Long quoteId) {
		return quoteService.send(quoteId);
	}
}
