package com.koreaconcrete.civilshop.consultation.entity;

import com.koreaconcrete.civilshop.common.domain.ConsultationStatus;
import com.koreaconcrete.civilshop.common.domain.ConsultationType;
import com.koreaconcrete.civilshop.common.entity.BaseTimeEntity;
import com.koreaconcrete.civilshop.product.entity.Product;
import com.koreaconcrete.civilshop.product.entity.ProductVariant;
import com.koreaconcrete.civilshop.user.entity.User;

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
@Table(name = "consultations")
public class Consultation extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private ConsultationType type;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id")
	private Product product;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "variant_id")
	private ProductVariant variant;

	@Column(nullable = false, length = 80)
	private String contactName;

	@Column(nullable = false, length = 40)
	private String contactPhone;

	@Column(columnDefinition = "text")
	private String message;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private ConsultationStatus status = ConsultationStatus.NEW;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "assigned_to_id")
	private User assignedTo;

	@Column(columnDefinition = "text")
	private String adminMemo;
}
