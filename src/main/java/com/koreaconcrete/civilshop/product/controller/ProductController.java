package com.koreaconcrete.civilshop.product.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.koreaconcrete.civilshop.common.api.PageResponse;
import com.koreaconcrete.civilshop.product.dto.ProductDtos.ProductDetail;
import com.koreaconcrete.civilshop.product.dto.ProductDtos.ProductListItem;
import com.koreaconcrete.civilshop.product.dto.ProductDtos.RelationResponse;
import com.koreaconcrete.civilshop.product.dto.ProductDtos.VariantResponse;
import com.koreaconcrete.civilshop.product.service.ProductService;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
	private final ProductService productService;

	public ProductController(ProductService productService) {
		this.productService = productService;
	}

	@Operation(summary = "상품 목록")
	@GetMapping
	public PageResponse<ProductListItem> list(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) Long categoryId,
			@RequestParam(defaultValue = "latest") String sort,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestHeader(value = "X-Session-Id", required = false) String sessionId
	) {
		return productService.list(keyword, categoryId, sort, page, size, sessionId);
	}

	@Operation(summary = "상품 검색")
	@GetMapping("/search")
	public PageResponse<ProductListItem> search(
			@RequestParam String keyword,
			@RequestParam(required = false) Long categoryId,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestHeader(value = "X-Session-Id", required = false) String sessionId
	) {
		return productService.list(keyword, categoryId, "latest", page, size, sessionId);
	}

	@Operation(summary = "최근 7일 검색 통계 기반 인기 상품")
	@GetMapping("/popular")
	public List<ProductListItem> popular(@RequestParam(defaultValue = "6") int size) {
		return productService.popularProducts(size);
	}

	@Operation(summary = "상품 상세")
	@GetMapping("/{id}")
	public ProductDetail detail(@PathVariable Long id) {
		return productService.publicDetail(id);
	}

	@Operation(summary = "상품 규격 목록")
	@GetMapping("/{id}/variants")
	public List<VariantResponse> variants(@PathVariable Long id) {
		return productService.publicVariants(id);
	}

	@Operation(summary = "상품 연관/대체 목록")
	@GetMapping("/{id}/relations")
	public List<RelationResponse> relations(@PathVariable Long id) {
		return productService.publicRelations(id);
	}
}
