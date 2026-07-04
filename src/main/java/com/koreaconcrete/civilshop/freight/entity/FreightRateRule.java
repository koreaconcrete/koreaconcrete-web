package com.koreaconcrete.civilshop.freight.entity;

import java.math.BigDecimal;

import com.koreaconcrete.civilshop.common.domain.VehicleType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "freight_rate_rules")
public class FreightRateRule {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 80)
	private String originRegion;

	@Column(nullable = false, length = 80)
	private String destinationRegion;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private VehicleType vehicleType;

	@Column(nullable = false)
	private Integer baseFreightAmount;

	private BigDecimal surchargeRate = BigDecimal.ZERO;

	private BigDecimal fuelSurchargeRate = BigDecimal.ZERO;

	@Column(nullable = false)
	private Boolean active = true;
}
