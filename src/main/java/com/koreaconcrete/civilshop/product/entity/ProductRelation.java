package com.koreaconcrete.civilshop.product.entity;

import com.koreaconcrete.civilshop.common.domain.RelationType;

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
@Table(name = "product_relations")
public class ProductRelation {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "source_product_id")
	private Product sourceProduct;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "target_product_id")
	private Product targetProduct;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private RelationType relationType = RelationType.RELATED;

	@Column(nullable = false)
	private Integer sortOrder = 0;
}
