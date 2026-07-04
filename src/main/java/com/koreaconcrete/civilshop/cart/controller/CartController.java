package com.koreaconcrete.civilshop.cart.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.koreaconcrete.civilshop.cart.dto.CartDtos.AddCartItemRequest;
import com.koreaconcrete.civilshop.cart.dto.CartDtos.CartResponse;
import com.koreaconcrete.civilshop.cart.dto.CartDtos.CartToQuoteRequest;
import com.koreaconcrete.civilshop.cart.dto.CartDtos.UpdateCartItemRequest;
import com.koreaconcrete.civilshop.cart.service.CartService;
import com.koreaconcrete.civilshop.common.security.UserPrincipal;
import com.koreaconcrete.civilshop.quote.dto.QuoteDtos.QuoteResponse;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/cart")
public class CartController {
	private final CartService cartService;

	public CartController(CartService cartService) {
		this.cartService = cartService;
	}

	@Operation(summary = "장바구니 조회")
	@GetMapping
	public CartResponse get(
			@AuthenticationPrincipal UserPrincipal principal,
			@RequestHeader(value = "X-Session-Id", required = false) String sessionId
	) {
		return cartService.get(principal, sessionId);
	}

	@Operation(summary = "장바구니 품목 추가")
	@PostMapping("/items")
	public CartResponse add(
			@AuthenticationPrincipal UserPrincipal principal,
			@RequestHeader(value = "X-Session-Id", required = false) String sessionId,
			@Valid @RequestBody AddCartItemRequest request
	) {
		return cartService.add(principal, sessionId, request);
	}

	@Operation(summary = "장바구니 품목 수량 수정")
	@PatchMapping("/items/{itemId}")
	public CartResponse update(
			@AuthenticationPrincipal UserPrincipal principal,
			@RequestHeader(value = "X-Session-Id", required = false) String sessionId,
			@PathVariable Long itemId,
			@Valid @RequestBody UpdateCartItemRequest request
	) {
		return cartService.update(principal, sessionId, itemId, request);
	}

	@Operation(summary = "장바구니 품목 삭제")
	@DeleteMapping("/items/{itemId}")
	public void delete(
			@AuthenticationPrincipal UserPrincipal principal,
			@RequestHeader(value = "X-Session-Id", required = false) String sessionId,
			@PathVariable Long itemId
	) {
		cartService.delete(principal, sessionId, itemId);
	}

	@Operation(summary = "장바구니에서 견적요청 생성")
	@PostMapping("/to-quote")
	public QuoteResponse toQuote(
			@AuthenticationPrincipal UserPrincipal principal,
			@RequestHeader(value = "X-Session-Id", required = false) String sessionId,
			@Valid @RequestBody CartToQuoteRequest request
	) {
		return cartService.toQuote(principal, sessionId, request);
	}
}
