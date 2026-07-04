package com.koreaconcrete.civilshop.quote.entity;

import java.time.LocalDate;

import com.koreaconcrete.civilshop.common.domain.QuoteStatus;
import com.koreaconcrete.civilshop.common.entity.BaseTimeEntity;
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
@Table(name = "quote_requests")
public class QuoteRequest extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 40)
	private String requestNo;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;

	@Column(nullable = false, length = 160)
	private String companyName;

	@Column(nullable = false, length = 80)
	private String contactName;

	@Column(nullable = false, length = 40)
	private String contactPhone;

	@Column(nullable = false, length = 500)
	private String siteAddress;

	private LocalDate requestedDeliveryDate;

	@Column(columnDefinition = "text")
	private String memo;

	@Column(nullable = false)
	private Boolean privacyAgreed = false;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private QuoteStatus status = QuoteStatus.SUBMITTED;
}
