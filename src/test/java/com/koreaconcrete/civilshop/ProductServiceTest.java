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
import com.koreaconcrete.civilshop.product.dto.ProductDtos.ProductMoveRequest;
import com.koreaconcrete.civilshop.product.dto.ProductDtos.ProductRequest;
import com.koreaconcrete.civilshop.product.entity.Product;
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
	void quoteOnlyProductsAppearInPublicList() {
		TestFixtures fixtures = TestFixtures.product(
				categoryRepository,
				productRepository,
				productVariantRepository,
				priceBookRepository,
				productPriceRepository,
				"quote-only-product-" + System.nanoTime()
		);
		fixtures.product.setStatus(ProductStatus.QUOTE_ONLY);

		var list = productService.list(fixtures.product.getName(), null, "latest", 1, 20, "test-session");

		assertThat(list.items()).extracting("id").contains(fixtures.product.getId());
	}

	@Test
	void hiddenProductIsBlockedFromPublicDetail() {
		TestFixtures fixtures = TestFixtures.product(
				categoryRepository,
				productRepository,
				productVariantRepository,
				priceBookRepository,
				productPriceRepository,
				"hidden-public-detail-" + System.nanoTime()
		);
		fixtures.product.setStatus(ProductStatus.HIDDEN);

		assertThatThrownBy(() -> productService.publicDetail(fixtures.product.getId()))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("상품을 찾을 수 없습니다");
		assertThat(productService.detail(fixtures.product.getId()).id()).isEqualTo(fixtures.product.getId());
	}

	@Test
	void moveProductUsesAllNonDeletedProductsInSameCategory() {
		TestFixtures fixtures = TestFixtures.product(
				categoryRepository,
				productRepository,
				productVariantRepository,
				priceBookRepository,
				productPriceRepository,
				"move-product-" + System.nanoTime()
		);
		fixtures.product.setSortOrder(10);
		Product hidden = product(fixtures.category, "move-hidden-" + System.nanoTime(), ProductStatus.HIDDEN, 20);
		Product target = product(fixtures.category, "move-target-" + System.nanoTime(), ProductStatus.ON_SALE, 30);
		product(fixtures.category, "move-deleted-" + System.nanoTime(), ProductStatus.DELETED, 25);

		productService.move(target.getId(), new ProductMoveRequest("up"));

		assertThat(productRepository.findMovableSiblings(fixtures.category.getId(), ProductStatus.DELETED))
				.extracting(Product::getId)
				.containsExactly(fixtures.product.getId(), target.getId(), hidden.getId());
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
				null,
				null
		);

		assertThatThrownBy(() -> productService.create(request))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("세부 카테고리");
	}

	private Product product(Category category, String suffix, ProductStatus status, int sortOrder) {
		Product product = new Product();
		product.setCategory(category);
		product.setSku("SKU-" + suffix);
		product.setName("테스트상품-" + suffix);
		product.setSummary("테스트 상품");
		product.setUnit("개");
		product.setStatus(status);
		product.setCustomMade(false);
		product.setSortOrder(sortOrder);
		return productRepository.save(product);
	}
}
