package com.koreaconcrete.civilshop.freight.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koreaconcrete.civilshop.freight.entity.DeliveryOption;

public interface DeliveryOptionRepository extends JpaRepository<DeliveryOption, Long> {
	Optional<DeliveryOption> findByCode(String code);
}
