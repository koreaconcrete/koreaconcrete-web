package com.koreaconcrete.civilshop.product.entity;

import com.koreaconcrete.civilshop.category.entity.Category;
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
@Table(name = "products")
public class Product extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id")
	private Category category;

	@Column(nullable = false, unique = true, length = 80)
	private String sku;

	@Column(nullable = false, length = 180)
	private String name;

	@Column(length = 500)
	private String summary;

	@Column(columnDefinition = "text")
	private String searchKeywords;

	@Column(columnDefinition = "text")
	private String description;

	@Column(length = 30)
	private String unit;

	@Column(length = 80)
	private String originCountry;

	@Column(length = 120)
	private String manufacturer;

	@Column(nullable = false)
	private Boolean customMade = false;

	private Integer leadTimeDaysMin;

	private Integer leadTimeDaysMax;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private ProductStatus status = ProductStatus.DRAFT;
}
