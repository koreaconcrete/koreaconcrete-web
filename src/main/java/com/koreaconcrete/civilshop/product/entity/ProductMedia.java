package com.koreaconcrete.civilshop.product.entity;

import com.koreaconcrete.civilshop.common.domain.ProductMediaType;

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
@Table(name = "product_media")
public class ProductMedia {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id")
	private Product product;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "variant_id")
	private ProductVariant variant;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private ProductMediaType type = ProductMediaType.IMAGE;

	@Column(nullable = false, length = 600)
	private String url;

	@Column(length = 200)
	private String altText;

	@Column(nullable = false)
	private Integer sortOrder = 0;
}
