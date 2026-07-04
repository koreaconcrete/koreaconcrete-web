package com.koreaconcrete.civilshop.product.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koreaconcrete.civilshop.product.entity.ProductVariant;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
	List<ProductVariant> findByProductIdOrderByIdAsc(Long productId);

	void deleteByProductId(Long productId);
}
