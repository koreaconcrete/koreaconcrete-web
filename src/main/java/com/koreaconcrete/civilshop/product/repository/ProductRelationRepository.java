package com.koreaconcrete.civilshop.product.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koreaconcrete.civilshop.product.entity.ProductRelation;

public interface ProductRelationRepository extends JpaRepository<ProductRelation, Long> {
	List<ProductRelation> findBySourceProductIdOrderBySortOrderAscIdAsc(Long productId);

	void deleteBySourceProductId(Long productId);

	void deleteByTargetProductId(Long productId);
}
