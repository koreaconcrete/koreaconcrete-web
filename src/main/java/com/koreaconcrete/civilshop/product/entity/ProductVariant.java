package com.koreaconcrete.civilshop.product.entity;

import java.math.BigDecimal;

import com.koreaconcrete.civilshop.common.domain.ProductStatus;
import com.koreaconcrete.civilshop.common.entity.BaseTimeEntity;

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
@Table(name = "product_variants")
public class ProductVariant extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id")
	private Product product;

	@Column(nullable = false, length = 180)
	private String variantName;

	private BigDecimal widthMm;

	private BigDecimal lengthMm;

	private BigDecimal heightMm;

	private BigDecimal thicknessMm;

	private BigDecimal weightKg;

	private BigDecimal twentyFiveTonQuantity;

	@Column(length = 30)
	private String unit;

	@Column(length = 80)
	private String barcode;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private ProductStatus status = ProductStatus.ON_SALE;
}
