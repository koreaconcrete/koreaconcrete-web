package com.koreaconcrete.civilshop.category.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.koreaconcrete.civilshop.category.dto.CategoryDtos.CategoryDetail;
import com.koreaconcrete.civilshop.category.dto.CategoryDtos.CategoryNode;
import com.koreaconcrete.civilshop.category.dto.CategoryDtos.UpsertCategoryRequest;
import com.koreaconcrete.civilshop.category.entity.Category;
import com.koreaconcrete.civilshop.category.repository.CategoryRepository;
import com.koreaconcrete.civilshop.common.exception.BusinessException;
import com.koreaconcrete.civilshop.common.storage.ImageStorageService;
import com.koreaconcrete.civilshop.product.repository.ProductRepository;

@Service
@Transactional(readOnly = true)
public class CategoryService {
	private static final int MAX_DEPTH = 2;

	private final CategoryRepository categoryRepository;
	private final ProductRepository productRepository;
	private final ImageStorageService imageStorageService;

	public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository, ImageStorageService imageStorageService) {
		this.categoryRepository = categoryRepository;
		this.productRepository = productRepository;
		this.imageStorageService = imageStorageService;
	}

	public List<CategoryNode> tree(boolean includeInactive) {
		List<Category> categories = includeInactive
				? categoryRepository.findAllByOrderByDepthAscSortOrderAscIdAsc()
				: categoryRepository.findByActiveTrueOrderByDepthAscSortOrderAscIdAsc();
		Map<Long, MutableNode> nodes = new LinkedHashMap<>();
		List<MutableNode> roots = new ArrayList<>();
		for (Category category : categories) {
			MutableNode node = new MutableNode(category);
			nodes.put(category.getId(), node);
			if (category.getParent() == null) {
				roots.add(node);
			} else {
				MutableNode parent = nodes.get(category.getParent().getId());
				if (parent != null) {
					parent.children.add(node);
				}
			}
		}
		return roots.stream().map(MutableNode::toNode).toList();
	}

	public CategoryDetail detail(Long id) {
		return toDetail(getCategory(id));
	}

	@Transactional
	public CategoryDetail create(UpsertCategoryRequest request) {
		Category parent = resolveAllowedParent(request.parentId());
		int depth = parent == null ? 1 : parent.getDepth() + 1;
		Category category = categoryRepository.save(new Category(
				parent,
				request.name(),
				resolveSlug(null, request),
				depth,
				request.sortOrder() == null ? nextSortOrder(parent) : request.sortOrder(),
				request.active() == null || request.active()
		));
		category.setImageUrl(blankToNull(request.imageUrl()));
		return toDetail(category);
	}

	@Transactional
	public CategoryDetail update(Long id, UpsertCategoryRequest request) {
		Category category = getCategory(id);
		Category parent = resolveAllowedParent(request.parentId());
		validateParent(category, parent);
		if (parent != null && categoryRepository.existsByParentId(id)) {
			throw BusinessException.badRequest("하위 카테고리가 있는 카테고리는 세부 카테고리로 이동할 수 없습니다.");
		}
		category.setParent(parent);
		category.setDepth(parent == null ? 1 : parent.getDepth() + 1);
		category.setName(request.name());
		category.setSlug(resolveSlug(category, request));
		String previousImageUrl = category.getImageUrl();
		String nextImageUrl = blankToNull(request.imageUrl());
		category.setImageUrl(nextImageUrl);
		if (request.sortOrder() != null) {
			category.setSortOrder(request.sortOrder());
		}
		category.setActive(request.active() == null || request.active());
		updateChildDepths(category);
		deleteImageIfChanged(previousImageUrl, nextImageUrl);
		return toDetail(category);
	}

	@Transactional
	public void delete(Long id) {
		Category category = getCategory(id);
		if (categoryRepository.existsByParentId(id)) {
			throw BusinessException.badRequest("하위 카테고리가 있는 카테고리는 삭제할 수 없습니다.");
		}
		if (productRepository.existsByCategoryId(id)) {
			throw BusinessException.badRequest("상품이 연결된 카테고리는 삭제할 수 없습니다.");
		}
		String imageUrl = category.getImageUrl();
		categoryRepository.delete(category);
		imageStorageService.delete(imageUrl);
	}

	public Category getCategory(Long id) {
		return categoryRepository.findById(id)
				.orElseThrow(() -> BusinessException.notFound("카테고리를 찾을 수 없습니다."));
	}

	private CategoryDetail toDetail(Category category) {
		return new CategoryDetail(
				category.getId(),
				category.getParent() == null ? null : category.getParent().getId(),
				category.getName(),
				category.getSlug(),
				category.getImageUrl(),
				category.getDepth(),
				category.getSortOrder(),
				category.getActive(),
				category.getCreatedAt(),
				category.getUpdatedAt()
		);
	}

	private String resolveSlug(Category category, UpsertCategoryRequest request) {
		if (StringUtils.hasText(request.slug())) {
			return request.slug().trim();
		}
		if (category != null && StringUtils.hasText(category.getSlug())) {
			return category.getSlug();
		}
		String slug;
		do {
			slug = "cat-" + UUID.randomUUID().toString().substring(0, 8).toLowerCase();
		} while (categoryRepository.findBySlug(slug).isPresent());
		return slug;
	}

	private String blankToNull(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}

	private void deleteImageIfChanged(String previousImageUrl, String nextImageUrl) {
		if (previousImageUrl != null && !previousImageUrl.equals(nextImageUrl)) {
			imageStorageService.delete(previousImageUrl);
		}
	}

	private int nextSortOrder(Category parent) {
		List<Category> siblings = parent == null
				? categoryRepository.findByParentIsNull()
				: categoryRepository.findByParentId(parent.getId());
		return siblings.stream()
				.map(Category::getSortOrder)
				.filter(sortOrder -> sortOrder != null)
				.max(Integer::compareTo)
				.orElse(0) + 10;
	}

	private void validateParent(Category category, Category parent) {
		Category current = parent;
		while (current != null) {
			if (category.getId().equals(current.getId())) {
				throw BusinessException.badRequest("자기 자신이나 하위 카테고리는 상위 카테고리로 지정할 수 없습니다.");
			}
			current = current.getParent();
		}
	}

	private Category resolveAllowedParent(Long parentId) {
		if (parentId == null) {
			return null;
		}
		Category parent = getCategory(parentId);
		if (parent.getDepth() >= MAX_DEPTH || parent.getParent() != null) {
			throw BusinessException.badRequest("카테고리는 최상위와 세부 카테고리까지만 생성할 수 있습니다.");
		}
		return parent;
	}

	private void updateChildDepths(Category parent) {
		for (Category child : categoryRepository.findByParentId(parent.getId())) {
			child.setDepth(parent.getDepth() + 1);
			updateChildDepths(child);
		}
	}

	private static class MutableNode {
		private final Category category;
		private final List<MutableNode> children = new ArrayList<>();

		private MutableNode(Category category) {
			this.category = category;
		}

		private CategoryNode toNode() {
			return new CategoryNode(
					category.getId(),
					category.getParent() == null ? null : category.getParent().getId(),
					category.getName(),
					category.getSlug(),
					category.getImageUrl(),
					category.getDepth(),
					category.getSortOrder(),
					category.getActive(),
					children.stream().map(MutableNode::toNode).toList()
			);
		}
	}
}
