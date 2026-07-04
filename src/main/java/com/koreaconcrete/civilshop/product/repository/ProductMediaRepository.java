package com.koreaconcrete.civilshop.product.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koreaconcrete.civilshop.product.entity.ProductMedia;

public interface ProductMediaRepository extends JpaRepository<ProductMedia, Long> {
	List<ProductMedia> findByProductIdOrderBySortOrderAscIdAsc(Long productId);

	void deleteByProductId(Long productId);
}
