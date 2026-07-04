package com.koreaconcrete.civilshop.freight.entity;

import java.math.BigDecimal;

import com.koreaconcrete.civilshop.common.domain.VehicleType;
import com.koreaconcrete.civilshop.product.entity.Product;
import com.koreaconcrete.civilshop.product.entity.ProductVariant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "loading_rules")
public class LoadingRule {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id")
	private Product product;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "variant_id")
	private ProductVariant variant;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private VehicleType vehicleType;

	@Column(nullable = false)
	private BigDecimal loadQuantity;

	@Column(nullable = false, length = 30)
	private String loadUnit;

	private BigDecimal palletQuantity;

	private BigDecimal palletWeightKg;

	@Column(length = 500)
	private String note;
}
