package com.koreaconcrete.civilshop.common.audit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koreaconcrete.civilshop.user.entity.User;

@Service
public class AuditService {
	private final AuditLogRepository auditLogRepository;

	public AuditService(AuditLogRepository auditLogRepository) {
		this.auditLogRepository = auditLogRepository;
	}

	@Transactional
	public void log(User actor, String action, String resourceType, String resourceId) {
		auditLogRepository.save(new AuditLog(actor, action, resourceType, resourceId, null, null));
	}
}
