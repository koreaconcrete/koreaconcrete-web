package com.koreaconcrete.civilshop.freight.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koreaconcrete.civilshop.freight.entity.FreightEstimate;

public interface FreightEstimateRepository extends JpaRepository<FreightEstimate, Long> {
	boolean existsByProductId(Long productId);
}
