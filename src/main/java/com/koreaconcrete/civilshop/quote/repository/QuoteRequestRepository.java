package com.koreaconcrete.civilshop.quote.repository;

import java.util.Collection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.koreaconcrete.civilshop.common.domain.QuoteStatus;
import com.koreaconcrete.civilshop.quote.entity.QuoteRequest;

public interface QuoteRequestRepository extends JpaRepository<QuoteRequest, Long> {
	long countByCreatedAtAfter(java.time.LocalDateTime createdAt);

	Page<QuoteRequest> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);

	Page<QuoteRequest> findByStatusOrderByIdDesc(QuoteStatus status, Pageable pageable);

	Page<QuoteRequest> findByStatusInOrderByIdDesc(Collection<QuoteStatus> statuses, Pageable pageable);

	Page<QuoteRequest> findAllByOrderByIdDesc(Pageable pageable);
}
