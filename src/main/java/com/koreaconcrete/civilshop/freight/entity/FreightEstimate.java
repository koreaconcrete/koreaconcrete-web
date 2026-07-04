package com.koreaconcrete.civilshop.freight.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "freight_estimates")
public class FreightEstimate {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id")
	private Product product;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "variant_id")
	private ProductVariant variant;

	@Column(nullable = false)
	private BigDecimal quantity;

	@Column(nullable = false, length = 500)
	private String destinationAddress;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private VehicleType vehicleType;

	@Column(nullable = false)
	private Integer estimatedFreight;

	@Column(nullable = false)
	private Integer unitFreight;

	@Column(nullable = false, columnDefinition = "text")
	private String calculationSnapshot;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@PrePersist
	void prePersist() {
		createdAt = LocalDateTime.now();
	}
}
