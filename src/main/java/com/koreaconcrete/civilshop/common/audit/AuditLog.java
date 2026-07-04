package com.koreaconcrete.civilshop.common.audit;

import java.time.LocalDateTime;

import com.koreaconcrete.civilshop.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "audit_logs")
public class AuditLog {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "actor_id")
	private User actor;

	@Column(nullable = false, length = 120)
	private String action;

	@Column(nullable = false, length = 120)
	private String resourceType;

	@Column(length = 120)
	private String resourceId;

	@Column(length = 80)
	private String ipAddress;

	@Column(length = 400)
	private String userAgent;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	public AuditLog(User actor, String action, String resourceType, String resourceId, String ipAddress, String userAgent) {
		this.actor = actor;
		this.action = action;
		this.resourceType = resourceType;
		this.resourceId = resourceId;
		this.ipAddress = ipAddress;
		this.userAgent = userAgent;
	}

	@PrePersist
	void prePersist() {
		createdAt = LocalDateTime.now();
	}
}
