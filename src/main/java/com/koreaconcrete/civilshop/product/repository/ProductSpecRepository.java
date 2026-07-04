package com.koreaconcrete.civilshop.product.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koreaconcrete.civilshop.product.entity.ProductSpec;

public interface ProductSpecRepository extends JpaRepository<ProductSpec, Long> {
	List<ProductSpec> findByProductIdOrderBySortOrderAscIdAsc(Long productId);

	void deleteByProductId(Long productId);
}
