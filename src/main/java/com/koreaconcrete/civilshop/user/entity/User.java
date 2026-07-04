package com.koreaconcrete.civilshop.user.entity;

import java.time.LocalDateTime;

import com.koreaconcrete.civilshop.common.domain.UserStatus;
import com.koreaconcrete.civilshop.common.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "users")
public class User extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 190)
	private String email;

	@Column(nullable = false)
	private String passwordHash;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(length = 40)
	private String phone;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private UserStatus status = UserStatus.ACTIVE;

	private LocalDateTime lastLoginAt;

	public User(String email, String passwordHash, String name, String phone) {
		this.email = email;
		this.passwordHash = passwordHash;
		this.name = name;
		this.phone = phone;
	}
}
