package com.koreaconcrete.civilshop.common.notification;

import java.time.LocalDate;

public record QuoteCreatedEvent(
		Long id,
		String requestNo,
		String companyName,
		String contactName,
		String contactPhone,
		String siteAddress,
		LocalDate requestedDeliveryDate,
		String itemSummary,
		String totalAmountLabel
) {
}
