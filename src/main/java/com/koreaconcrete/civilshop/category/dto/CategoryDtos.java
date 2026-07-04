package com.koreaconcrete.civilshop.category.dto;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotBlank;

public final class CategoryDtos {
	private CategoryDtos() {
	}

	public record CategoryNode(
			Long id,
			Long parentId,
			String name,
			String slug,
			String imageUrl,
			Integer depth,
			Integer sortOrder,
			Boolean active,
			List<CategoryNode> children
	) {
	}

	public record CategoryDetail(
			Long id,
			Long parentId,
			String name,
			String slug,
			String imageUrl,
			Integer depth,
			Integer sortOrder,
			Boolean active,
			LocalDateTime createdAt,
			LocalDateTime updatedAt
	) {
	}

	public record UpsertCategoryRequest(
			Long parentId,
			@NotBlank String name,
			String slug,
			String imageUrl,
			Integer sortOrder,
			Boolean active
	) {
	}
}
