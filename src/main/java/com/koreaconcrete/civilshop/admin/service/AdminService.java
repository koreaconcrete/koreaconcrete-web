package com.koreaconcrete.civilshop.admin.service;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koreaconcrete.civilshop.admin.dto.AdminDtos.AuditLogItem;
import com.koreaconcrete.civilshop.admin.dto.AdminDtos.DashboardResponse;
import com.koreaconcrete.civilshop.admin.dto.AdminDtos.RecentConsultation;
import com.koreaconcrete.civilshop.admin.dto.AdminDtos.RecentQuote;
import com.koreaconcrete.civilshop.admin.dto.AdminDtos.SearchLogItem;
import com.koreaconcrete.civilshop.common.api.PageResponse;
import com.koreaconcrete.civilshop.common.audit.AuditLog;
import com.koreaconcrete.civilshop.common.audit.AuditLogRepository;
import com.koreaconcrete.civilshop.consultation.entity.Consultation;
import com.koreaconcrete.civilshop.consultation.repository.ConsultationRepository;
import com.koreaconcrete.civilshop.product.repository.ProductRepository;
import com.koreaconcrete.civilshop.quote.entity.QuoteRequest;
import com.koreaconcrete.civilshop.quote.repository.QuoteRequestRepository;
import com.koreaconcrete.civilshop.search.entity.SearchLog;
import com.koreaconcrete.civilshop.search.repository.SearchLogRepository;

@Service
@Transactional(readOnly = true)
public class AdminService {
	private final QuoteRequestRepository quoteRequestRepository;
	private final ConsultationRepository consultationRepository;
	private final ProductRepository productRepository;
	private final SearchLogRepository searchLogRepository;
	private final AuditLogRepository auditLogRepository;

	public AdminService(
			QuoteRequestRepository quoteRequestRepository,
			ConsultationRepository consultationRepository,
			ProductRepository productRepository,
			SearchLogRepository searchLogRepository,
			AuditLogRepository auditLogRepository
	) {
		this.quoteRequestRepository = quoteRequestRepository;
		this.consultationRepository = consultationRepository;
		this.productRepository = productRepository;
		this.searchLogRepository = searchLogRepository;
		this.auditLogRepository = auditLogRepository;
	}

	public DashboardResponse dashboard() {
		var today = LocalDate.now().atStartOfDay();
		return new DashboardResponse(
				quoteRequestRepository.countByCreatedAtAfter(today),
				consultationRepository.countByCreatedAtAfter(today),
				productRepository.count(),
				quoteRequestRepository.findAllByOrderByIdDesc(PageRequest.of(0, 5)).stream().map(this::toRecentQuote).toList(),
				consultationRepository.findAllByOrderByIdDesc(PageRequest.of(0, 5)).stream().map(this::toRecentConsultation).toList()
		);
	}

	public PageResponse<SearchLogItem> searchLogs(int page, int size) {
		Page<SearchLog> logs = searchLogRepository.findAll(PageRequest.of(Math.max(page - 1, 0), size, Sort.by(Sort.Direction.DESC, "id")));
		return PageResponse.of(logs, logs.stream().map(this::toSearchLogItem).toList());
	}

	public PageResponse<AuditLogItem> auditLogs(int page, int size) {
		Page<AuditLog> logs = auditLogRepository.findAll(PageRequest.of(Math.max(page - 1, 0), size, Sort.by(Sort.Direction.DESC, "id")));
		return PageResponse.of(logs, logs.stream().map(this::toAuditLogItem).toList());
	}

	private RecentQuote toRecentQuote(QuoteRequest quote) {
		return new RecentQuote(quote.getId(), quote.getRequestNo(), quote.getCompanyName(), quote.getStatus(), quote.getCreatedAt());
	}

	private RecentConsultation toRecentConsultation(Consultation consultation) {
		return new RecentConsultation(consultation.getId(), consultation.getContactName(), consultation.getStatus(), consultation.getCreatedAt());
	}

	private SearchLogItem toSearchLogItem(SearchLog log) {
		return new SearchLogItem(log.getId(), log.getKeyword(), log.getResultCount(), log.getCreatedAt());
	}

	private AuditLogItem toAuditLogItem(AuditLog log) {
		return new AuditLogItem(log.getId(), log.getAction(), log.getResourceType(), log.getResourceId(), log.getCreatedAt());
	}
}
