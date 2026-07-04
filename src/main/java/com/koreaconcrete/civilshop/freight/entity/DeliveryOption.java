package com.koreaconcrete.civilshop.freight.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "delivery_options")
public class DeliveryOption {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 60)
	private String code;

	@Column(nullable = false, length = 120)
	private String name;

	@Column(nullable = false)
	private Boolean includesFreight = false;

	@Column(length = 500)
	private String description;

	@Column(nullable = false)
	private Boolean active = true;
}
