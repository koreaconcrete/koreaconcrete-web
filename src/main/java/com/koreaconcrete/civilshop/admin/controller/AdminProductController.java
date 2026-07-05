package com.koreaconcrete.civilshop.admin.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;

import com.koreaconcrete.civilshop.common.api.PageResponse;
import com.koreaconcrete.civilshop.common.domain.ProductStatus;
import com.koreaconcrete.civilshop.product.dto.ProductDtos.ProductDetail;
import com.koreaconcrete.civilshop.product.dto.ProductDtos.ProductListItem;
import com.koreaconcrete.civilshop.product.dto.ProductDtos.ProductMoveRequest;
import com.koreaconcrete.civilshop.product.dto.ProductDtos.ProductRequest;
import com.koreaconcrete.civilshop.product.dto.ProductDtos.ProductSortOrderRequest;
import com.koreaconcrete.civilshop.product.dto.ProductDtos.ProductStatusRequest;
import com.koreaconcrete.civilshop.product.dto.ProductDtos.VariantRequest;
import com.koreaconcrete.civilshop.product.dto.ProductDtos.VariantResponse;
import com.koreaconcrete.civilshop.product.service.ProductService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminProductController {
	private final ProductService productService;

	public AdminProductController(ProductService productService) {
		this.productService = productService;
	}

	@Operation(summary = "관리자 상품 목록")
	@GetMapping("/products")
	public PageResponse<ProductListItem> products(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) Long categoryId,
			@RequestParam(required = false) ProductStatus status,
			@RequestParam(defaultValue = "false") boolean includeDeleted,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "20") int size
	) {
		return productService.adminList(keyword, categoryId, status, includeDeleted, page, size);
	}

	@Operation(summary = "관리자 상품 상세")
	@GetMapping("/products/{id}")
	public ProductDetail detail(@PathVariable Long id) {
		return productService.detail(id);
	}

	@Operation(summary = "관리자 상품 생성")
	@PostMapping("/products")
	public ProductDetail create(@Valid @RequestBody ProductRequest request) {
		return productService.create(request);
	}

	@Operation(summary = "관리자 상품 수정")
	@PatchMapping("/products/{id}")
	public ProductDetail update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
		return productService.update(id, request);
	}

	@Operation(summary = "관리자 상품 삭제")
	@DeleteMapping("/products/{id}")
	public void delete(@PathVariable Long id) {
		productService.delete(id);
	}

	@Operation(summary = "관리자 상품 상태 변경")
	@PatchMapping("/products/{id}/status")
	public ProductDetail updateStatus(@PathVariable Long id, @Valid @RequestBody ProductStatusRequest request) {
		return productService.updateStatus(id, request);
	}

	@Operation(summary = "관리자 상품 표시 순서 변경")
	@PatchMapping("/products/{id}/sort-order")
	public ProductDetail updateSortOrder(@PathVariable Long id, @Valid @RequestBody ProductSortOrderRequest request) {
		return productService.updateSortOrder(id, request);
	}

	@Operation(summary = "관리자 상품 위/아래 순서 이동")
	@PatchMapping("/products/{id}/move")
	public ProductDetail move(@PathVariable Long id, @Valid @RequestBody ProductMoveRequest request) {
		return productService.move(id, request);
	}

	@Operation(summary = "관리자 상품 규격 생성")
	@PostMapping("/products/{id}/variants")
	public VariantResponse createVariant(@PathVariable Long id, @Valid @RequestBody VariantRequest request) {
		return productService.createVariant(id, request);
	}

	@Operation(summary = "관리자 상품 규격 수정")
	@PatchMapping("/product-variants/{variantId}")
	public VariantResponse updateVariant(@PathVariable Long variantId, @Valid @RequestBody VariantRequest request) {
		return productService.updateVariant(variantId, request);
	}

	@Operation(summary = "관리자 상품 규격 삭제")
	@DeleteMapping("/product-variants/{variantId}")
	public void deleteVariant(@PathVariable Long variantId) {
		productService.deleteVariant(variantId);
	}
}
