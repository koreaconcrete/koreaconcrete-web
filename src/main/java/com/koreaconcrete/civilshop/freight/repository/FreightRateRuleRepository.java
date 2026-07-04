package com.koreaconcrete.civilshop.freight.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koreaconcrete.civilshop.common.domain.VehicleType;
import com.koreaconcrete.civilshop.freight.entity.FreightRateRule;

public interface FreightRateRuleRepository extends JpaRepository<FreightRateRule, Long> {
	List<FreightRateRule> findByActiveTrueAndVehicleTypeOrderByIdAsc(VehicleType vehicleType);
}
