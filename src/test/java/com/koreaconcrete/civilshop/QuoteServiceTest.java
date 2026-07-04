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
import com.koreaconcrete.civilshop.pricing.repository.PriceBookRepository;
import com.koreaconcrete.civilshop.pricing.repository.ProductPriceRepository;
import com.koreaconcrete.civilshop.product.repository.ProductRepository;
import com.koreaconcrete.civilshop.product.repository.ProductVariantRepository;
import com.koreaconcrete.civilshop.common.exception.BusinessException;
import com.koreaconcrete.civilshop.quote.dto.QuoteDtos.QuoteItemRequest;
import com.koreaconcrete.civilshop.quote.dto.QuoteDtos.QuoteRequestCreate;
import com.koreaconcrete.civilshop.quote.repository.QuoteItemRepository;
import com.koreaconcrete.civilshop.quote.service.QuoteService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class QuoteServiceTest {
	@Autowired
	QuoteService quoteService;

	@Autowired
	QuoteItemRepository quoteItemRepository;

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
	void quoteCreationSavesItems() {
		TestFixtures fixtures = TestFixtures.product(
				categoryRepository,
				productRepository,
				productVariantRepository,
				priceBookRepository,
				productPriceRepository,
				"quote-" + System.nanoTime()
		);

		var response = quoteService.create(null, new QuoteRequestCreate(
				"대한건설",
				"김철수",
				"01012345678",
				"경기도 수원시",
				null,
				"테스트",
				true,
				List.of(new QuoteItemRequest(fixtures.product.getId(), fixtures.variant.getId(), new BigDecimal("120")))
		));

		assertThat(response.items()).hasSize(1);
		assertThat(response.requestNo()).startsWith("QT-");
		assertThat(response.requestNo()).doesNotContain("TMP");
		assertThat(quoteItemRepository.findByQuoteRequestIdOrderByIdAsc(response.id())).hasSize(1);
	}

	@Test
	void quoteCreationRejectsMismatchedProductAndVariant() {
		TestFixtures first = TestFixtures.product(
				categoryRepository,
				productRepository,
				productVariantRepository,
				priceBookRepository,
				productPriceRepository,
				"quote-mismatch-a-" + System.nanoTime()
		);
		TestFixtures second = TestFixtures.product(
				categoryRepository,
				productRepository,
				productVariantRepository,
				priceBookRepository,
				productPriceRepository,
				"quote-mismatch-b-" + System.nanoTime()
		);

		assertThatThrownBy(() -> quoteService.create(null, new QuoteRequestCreate(
				"대한건설",
				"김철수",
				"01012345678",
				"경기도 수원시",
				null,
				"테스트",
				true,
				List.of(new QuoteItemRequest(first.product.getId(), second.variant.getId(), new BigDecimal("120")))
		))).isInstanceOf(BusinessException.class);
	}
}
