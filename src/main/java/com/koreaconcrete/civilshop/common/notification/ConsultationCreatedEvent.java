package com.koreaconcrete.civilshop.common.notification;

import com.koreaconcrete.civilshop.common.domain.ConsultationType;

public record ConsultationCreatedEvent(
		Long id,
		ConsultationType type,
		String contactName,
		String contactPhone,
		String productName,
		String message
) {
}
