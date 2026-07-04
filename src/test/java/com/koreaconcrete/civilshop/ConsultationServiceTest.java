package com.koreaconcrete.civilshop;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.koreaconcrete.civilshop.common.domain.ConsultationStatus;
import com.koreaconcrete.civilshop.common.domain.ConsultationType;
import com.koreaconcrete.civilshop.consultation.dto.ConsultationDtos.ConsultationRequest;
import com.koreaconcrete.civilshop.consultation.service.ConsultationService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ConsultationServiceTest {
	@Autowired
	ConsultationService consultationService;

	@Test
	void consultationCreationWorks() {
		var response = consultationService.create(null, new ConsultationRequest(
				ConsultationType.SMS,
				null,
				null,
				"김철수",
				"01012345678",
				"상담 부탁드립니다.",
				true
		));

		assertThat(response.id()).isNotNull();
		assertThat(response.status()).isEqualTo(ConsultationStatus.NEW);
	}
}
