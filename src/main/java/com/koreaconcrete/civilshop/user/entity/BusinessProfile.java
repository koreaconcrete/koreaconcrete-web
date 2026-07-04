package com.koreaconcrete.civilshop.user.entity;

import com.koreaconcrete.civilshop.common.domain.CreditStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "business_profiles")
public class BusinessProfile {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id")
	private User user;

	@Column(length = 160)
	private String companyName;

	@Column(length = 40)
	private String businessRegistrationNo;

	@Column(length = 80)
	private String ceoName;

	@Column(length = 80)
	private String businessType;

	@Column(length = 80)
	private String businessItem;

	@Column(length = 190)
	private String taxEmail;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private CreditStatus creditStatus = CreditStatus.NORMAL;
}
