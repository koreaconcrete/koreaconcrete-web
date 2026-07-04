package com.koreaconcrete.civilshop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.koreaconcrete.civilshop.category.entity.Category;
import com.koreaconcrete.civilshop.category.repository.CategoryRepository;
import com.koreaconcrete.civilshop.common.domain.ProductStatus;
import com.koreaconcrete.civilshop.common.exception.BusinessException;
import com.koreaconcrete.civilshop.pricing.repository.PriceBookRepository;
import com.koreaconcrete.civilshop.pricing.repository.ProductPriceRepository;
import com.koreaconcrete.civilshop.product.dto.ProductDtos.ProductRequest;
import com.koreaconcrete.civilshop.product.repository.ProductRepository;
import com.koreaconcrete.civilshop.product.repository.ProductVariantRepository;
import com.koreaconcrete.civilshop.product.service.ProductService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProductServiceTest {
	@Autowired
	ProductService productService;

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
	void productListAndDetailWork() {
		TestFixtures fixtures = TestFixtures.product(
				categoryRepository,
				productRepository,
				productVariantRepository,
				priceBookRepository,
				productPriceRepository,
				"product-" + System.nanoTime()
		);

		var list = productService.list("그레이팅", null, "latest", 1, 20, "test-session");
		var detail = productService.detail(fixtures.product.getId());

		assertThat(list.items()).isNotEmpty();
		assertThat(detail.id()).isEqualTo(fixtures.product.getId());
		assertThat(detail.variants()).hasSize(1);
		assertThat(detail.variants().get(0).price().salePrice()).isEqualTo(32600);
	}

	@Test
	void deleteVariantMarksVariantDeleted() {
		TestFixtures fixtures = TestFixtures.product(
				categoryRepository,
				productRepository,
				productVariantRepository,
				priceBookRepository,
				productPriceRepository,
				"delete-variant-" + System.nanoTime()
		);

		productService.deleteVariant(fixtures.variant.getId());

		assertThat(productVariantRepository.findById(fixtures.variant.getId()))
				.get()
				.extracting("status")
				.isEqualTo(ProductStatus.DELETED);
	}

	@Test
	void adminListHidesDeletedProductsUntilIncluded() {
		TestFixtures fixtures = TestFixtures.product(
				categoryRepository,
				productRepository,
				productVariantRepository,
				priceBookRepository,
				productPriceRepository,
				"deleted-product-" + System.nanoTime()
		);
		productService.delete(fixtures.product.getId());

		var hidden = productService.adminList(fixtures.product.getName(), null, false, 1, 20);
		var included = productService.adminList(fixtures.product.getName(), null, true, 1, 20);

		assertThat(hidden.items()).isEmpty();
		assertThat(included.items()).extracting("id").contains(fixtures.product.getId());
	}

	@Test
	void productCannotBeAttachedToRootCategory() {
		Category root = categoryRepository.save(new Category(
				null,
				"최상위-" + System.nanoTime(),
				"root-" + System.nanoTime(),
				1,
				1,
				true
		));

		ProductRequest request = new ProductRequest(
				root.getId(),
				null,
				"최상위 연결 상품",
				"요약",
				null,
				"상세",
				"개",
				null,
				null,
				false,
				null,
				null,
				ProductStatus.ON_SALE,
				null,
				null,
				null
		);

		assertThatThrownBy(() -> productService.create(request))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("세부 카테고리");
	}
}
