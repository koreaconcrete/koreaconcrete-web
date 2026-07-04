package com.koreaconcrete.civilshop.consultation.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.koreaconcrete.civilshop.common.domain.ConsultationStatus;
import com.koreaconcrete.civilshop.consultation.entity.Consultation;

public interface ConsultationRepository extends JpaRepository<Consultation, Long> {
	long countByCreatedAtAfter(java.time.LocalDateTime createdAt);

	Page<Consultation> findAllByOrderByIdDesc(Pageable pageable);

	Page<Consultation> findByStatusOrderByIdDesc(ConsultationStatus status, Pageable pageable);

	boolean existsByProductId(Long productId);
}
