package com.koreaconcrete.civilshop.admin.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.koreaconcrete.civilshop.common.domain.ConsultationStatus;
import com.koreaconcrete.civilshop.common.domain.QuoteStatus;

public final class AdminDtos {
	private AdminDtos() {
	}

	public record DashboardResponse(
			long todayQuoteCount,
			long todayConsultationCount,
			long totalProductCount,
			List<RecentQuote> recentQuotes,
			List<RecentConsultation> recentConsultations
	) {
	}

	public record RecentQuote(Long id, String requestNo, String companyName, QuoteStatus status, LocalDateTime createdAt) {
	}

	public record RecentConsultation(Long id, String contactName, ConsultationStatus status, LocalDateTime createdAt) {
	}

	public record SearchLogItem(Long id, String keyword, Integer resultCount, LocalDateTime createdAt) {
	}

	public record AuditLogItem(Long id, String action, String resourceType, String resourceId, LocalDateTime createdAt) {
	}
}
