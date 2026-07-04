package com.koreaconcrete.civilshop.category.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koreaconcrete.civilshop.category.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {
	List<Category> findAllByOrderByDepthAscSortOrderAscIdAsc();

	List<Category> findByActiveTrueOrderByDepthAscSortOrderAscIdAsc();

	List<Category> findByParentId(Long parentId);

	List<Category> findByParentIsNull();

	boolean existsByParentId(Long parentId);

	Optional<Category> findBySlug(String slug);
}
