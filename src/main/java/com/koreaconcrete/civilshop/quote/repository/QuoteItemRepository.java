package com.koreaconcrete.civilshop.quote.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koreaconcrete.civilshop.quote.entity.QuoteItem;

public interface QuoteItemRepository extends JpaRepository<QuoteItem, Long> {
	List<QuoteItem> findByQuoteRequestIdOrderByIdAsc(Long quoteRequestId);

	boolean existsByProductId(Long productId);
}
