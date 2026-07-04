package com.koreaconcrete.civilshop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.koreaconcrete.civilshop.category.repository.CategoryRepository;
import com.koreaconcrete.civilshop.common.domain.VehicleType;
import com.koreaconcrete.civilshop.common.exception.BusinessException;
import com.koreaconcrete.civilshop.freight.dto.FreightDtos.FreightEstimateItemRequest;
import com.koreaconcrete.civilshop.freight.dto.FreightDtos.FreightEstimateRequest;
import com.koreaconcrete.civilshop.freight.service.FreightService;
import com.koreaconcrete.civilshop.pricing.repository.PriceBookRepository;
import com.koreaconcrete.civilshop.pricing.repository.ProductPriceRepository;
import com.koreaconcrete.civilshop.product.repository.ProductRepository;
import com.koreaconcrete.civilshop.product.repository.ProductVariantRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FreightServiceTest {
	@Autowired
	FreightService freightService;

	@Autowired
	CategoryRepository categoryRepository;

	@Autowired
	ProductRepository productRepository;

	@Autowired
	ProductVariantRepository productVariantRepository;

	@Autowired
	PriceBookRepository priceBookRepository;

	@Autowired
	ProductPriceRepository productPriceRepository;

	@Test
	void fallbackFreightCalculationWorks() {
		TestFixtures fixtures = TestFixtures.product(
				categoryRepository,
				productRepository,
				productVariantRepository,
				priceBookRepository,
				productPriceRepository,
				"freight-" + System.nanoTime()
		);

		var response = freightService.estimate(new FreightEstimateRequest(
				List.of(new FreightEstimateItemRequest(fixtures.product.getId(), fixtures.variant.getId(), new BigDecimal("120"))),
				"강원도 원주시",
				VehicleType.TWENTY_FIVE_TON
		));

		assertThat(response.totalFreightAmount()).isEqualTo(300000);
		assertThat(response.notes()).anyMatch(note -> note.contains("기본 금액"));
	}

	@Test
	void freightCalculationRejectsZeroQuantity() {
		TestFixtures fixtures = TestFixtures.product(
				categoryRepository,
				productRepository,
				productVariantRepository,
				priceBookRepository,
				productPriceRepository,
				"freight-zero-" + System.nanoTime()
		);

		assertThatThrownBy(() -> freightService.estimate(new FreightEstimateRequest(
				List.of(new FreightEstimateItemRequest(fixtures.product.getId(), fixtures.variant.getId(), BigDecimal.ZERO)),
				"강원도 원주시",
				VehicleType.TWENTY_FIVE_TON
		))).isInstanceOf(BusinessException.class);
	}
}
