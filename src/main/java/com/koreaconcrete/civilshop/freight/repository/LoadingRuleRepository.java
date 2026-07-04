package com.koreaconcrete.civilshop.freight.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koreaconcrete.civilshop.common.domain.VehicleType;
import com.koreaconcrete.civilshop.freight.entity.LoadingRule;

public interface LoadingRuleRepository extends JpaRepository<LoadingRule, Long> {
	List<LoadingRule> findByProductIdOrderByVehicleTypeAsc(Long productId);

	Optional<LoadingRule> findFirstByVariantIdAndVehicleType(Long variantId, VehicleType vehicleType);

	Optional<LoadingRule> findFirstByVariantIdOrderByLoadQuantityDesc(Long variantId);

	void deleteByProductId(Long productId);
}
