package com.koreaconcrete.civilshop.quote.entity;

import java.math.BigDecimal;

import com.koreaconcrete.civilshop.product.entity.Product;
import com.koreaconcrete.civilshop.product.entity.ProductVariant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "quote_items")
public class QuoteItem {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "quote_request_id")
	private QuoteRequest quoteRequest;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id")
	private Product product;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "variant_id")
	private ProductVariant variant;

	@Column(nullable = false)
	private BigDecimal quantity;

	private Integer unitPrice;

	private Integer freightAmount;

	private Integer vatAmount;

	private Integer totalAmount;

	@Column(length = 500)
	private String note;
}
