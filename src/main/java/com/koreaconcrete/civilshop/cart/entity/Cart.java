package com.koreaconcrete.civilshop.cart.entity;

import com.koreaconcrete.civilshop.common.domain.CartType;
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
@Table(name = "carts")
public class Cart extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;

	@Column(length = 120)
	private String sessionId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private CartType cartType = CartType.QUOTE;
}
