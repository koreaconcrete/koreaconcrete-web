package com.koreaconcrete.civilshop.cart.entity;

import java.math.BigDecimal;

import com.koreaconcrete.civilshop.common.entity.BaseTimeEntity;
import com.koreaconcrete.civilshop.freight.entity.DeliveryOption;
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
@Table(name = "cart_items")
public class CartItem extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "cart_id")
	private Cart cart;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id")
	private Product product;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "variant_id")
	private ProductVariant variant;

	@Column(nullable = false)
	private BigDecimal quantity;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "delivery_option_id")
	private DeliveryOption deliveryOption;

	private Integer unitPriceSnapshot;

	@Column(columnDefinition = "text")
	private String freightEstimateSnapshot;
}
