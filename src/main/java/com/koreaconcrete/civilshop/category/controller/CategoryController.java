package com.koreaconcrete.civilshop.category.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.koreaconcrete.civilshop.category.dto.CategoryDtos.CategoryDetail;
import com.koreaconcrete.civilshop.category.dto.CategoryDtos.CategoryNode;
import com.koreaconcrete.civilshop.category.service.CategoryService;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {
	private final CategoryService categoryService;

	public CategoryController(CategoryService categoryService) {
		this.categoryService = categoryService;
	}

	@Operation(summary = "카테고리 트리")
	@GetMapping("/tree")
	public List<CategoryNode> tree() {
		return categoryService.tree(false);
	}

	@Operation(summary = "카테고리 상세")
	@GetMapping("/{id}")
	public CategoryDetail detail(@PathVariable Long id) {
		return categoryService.detail(id);
	}
}
