package com.koreaconcrete.civilshop.pricing.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koreaconcrete.civilshop.pricing.entity.ProductPrice;

public interface ProductPriceRepository extends JpaRepository<ProductPrice, Long> {
	List<ProductPrice> findByProductIdOrderByVariantIdAscIdDesc(Long productId);

	Optional<ProductPrice> findFirstByVariantIdOrderByIdDesc(Long variantId);

	void deleteByProductId(Long productId);
}
