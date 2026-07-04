package com.koreaconcrete.civilshop.pricing.entity;

import java.math.BigDecimal;

import com.koreaconcrete.civilshop.common.domain.FreightPolicy;
import com.koreaconcrete.civilshop.common.domain.VatPolicy;
import com.koreaconcrete.civilshop.common.entity.BaseTimeEntity;
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
@Table(name = "product_prices")
public class ProductPrice extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "price_book_id")
	private PriceBook priceBook;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id")
	private Product product;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "variant_id")
	private ProductVariant variant;

	@Column(nullable = false)
	private Integer salePrice;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private VatPolicy vatPolicy = VatPolicy.VAT_EXCLUDED;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private FreightPolicy freightPolicy = FreightPolicy.FREIGHT_EXCLUDED;

	@Column(nullable = false)
	private BigDecimal minOrderQuantity = BigDecimal.ONE;

	@Column(length = 500)
	private String priceNote;
}
