package com.koreaconcrete.civilshop.admin.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.koreaconcrete.civilshop.category.dto.CategoryDtos.CategoryDetail;
import com.koreaconcrete.civilshop.category.dto.CategoryDtos.CategoryNode;
import com.koreaconcrete.civilshop.category.dto.CategoryDtos.UpsertCategoryRequest;
import com.koreaconcrete.civilshop.category.service.CategoryService;
import com.koreaconcrete.civilshop.common.storage.ImageStorageService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/categories")
public class AdminCategoryController {
	private final CategoryService categoryService;
	private final ImageStorageService imageStorageService;

	public AdminCategoryController(CategoryService categoryService, ImageStorageService imageStorageService) {
		this.categoryService = categoryService;
		this.imageStorageService = imageStorageService;
	}

	@Operation(summary = "관리자 카테고리 트리")
	@GetMapping("/tree")
	public List<CategoryNode> tree() {
		return categoryService.tree(true);
	}

	@Operation(summary = "관리자 카테고리 생성")
	@PostMapping
	public CategoryDetail create(@Valid @RequestBody UpsertCategoryRequest request) {
		return categoryService.create(request);
	}

	@Operation(summary = "관리자 카테고리 이미지 업로드")
	@PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public Map<String, String> uploadImage(@RequestPart("file") MultipartFile file) {
		return Map.of("url", imageStorageService.store(file, "categories"));
	}

	@Operation(summary = "관리자 카테고리 수정")
	@PatchMapping("/{id}")
	public CategoryDetail update(@PathVariable Long id, @Valid @RequestBody UpsertCategoryRequest request) {
		return categoryService.update(id, request);
	}

	@Operation(summary = "관리자 카테고리 삭제")
	@DeleteMapping("/{id}")
	public void delete(@PathVariable Long id) {
		categoryService.delete(id);
	}
}
