package com.koreaconcrete.civilshop.counterparty.entity;

import com.koreaconcrete.civilshop.common.entity.BaseTimeEntity;

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
@Table(name = "counterparties")
public class Counterparty extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 160)
	private String companyName;

	@Column(length = 40)
	private String businessRegistrationNo;

	@Column(length = 80)
	private String contactName;

	@Column(length = 40)
	private String contactPhone;

	@Column(length = 500)
	private String memo;
}
