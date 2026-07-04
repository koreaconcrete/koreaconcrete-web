package com.koreaconcrete.civilshop;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.koreaconcrete.civilshop.category.entity.Category;
import com.koreaconcrete.civilshop.category.repository.CategoryRepository;
import com.koreaconcrete.civilshop.common.domain.FreightPolicy;
import com.koreaconcrete.civilshop.common.domain.ProductStatus;
import com.koreaconcrete.civilshop.common.domain.VatPolicy;
import com.koreaconcrete.civilshop.pricing.entity.PriceBook;
import com.koreaconcrete.civilshop.pricing.entity.ProductPrice;
import com.koreaconcrete.civilshop.pricing.repository.PriceBookRepository;
import com.koreaconcrete.civilshop.pricing.repository.ProductPriceRepository;
import com.koreaconcrete.civilshop.product.entity.Product;
import com.koreaconcrete.civilshop.product.entity.ProductVariant;
import com.koreaconcrete.civilshop.product.repository.ProductRepository;
import com.koreaconcrete.civilshop.product.repository.ProductVariantRepository;

public class TestFixtures {
	public final Category category;
	public final Product product;
	public final ProductVariant variant;

	private TestFixtures(Category category, Product product, ProductVariant variant) {
		this.category = category;
		this.product = product;
		this.variant = variant;
	}

	public static TestFixtures product(
			CategoryRepository categoryRepository,
			ProductRepository productRepository,
			ProductVariantRepository productVariantRepository,
			PriceBookRepository priceBookRepository,
			ProductPriceRepository productPriceRepository,
			String suffix
	) {
		Category root = new Category(null, "스틸제품-" + suffix, "steel-root-" + suffix, 1, 1, true);
		root = categoryRepository.save(root);
		Category category = new Category(root, "그레이팅-" + suffix, "grating-" + suffix, 2, 1, true);
		category = categoryRepository.save(category);

		Product product = new Product();
		product.setCategory(category);
		product.setSku("SKU-" + suffix);
		product.setName("그레이팅수로용-" + suffix);
		product.setSummary("테스트 상품");
		product.setUnit("조");
		product.setStatus(ProductStatus.ON_SALE);
		product.setCustomMade(false);
		product = productRepository.save(product);

		ProductVariant variant = new ProductVariant();
		variant.setProduct(product);
		variant.setVariantName("300*1000*50_" + suffix);
		variant.setUnit("조");
		variant.setWeightKg(new BigDecimal("18.5"));
		variant.setStatus(ProductStatus.ON_SALE);
		variant = productVariantRepository.save(variant);

		PriceBook book = new PriceBook();
		book.setName("테스트 가격표-" + suffix);
		book.setEffectiveFrom(LocalDate.now());
		book.setDefaultBook(true);
		book = priceBookRepository.save(book);

		ProductPrice price = new ProductPrice();
		price.setPriceBook(book);
		price.setProduct(product);
		price.setVariant(variant);
		price.setSalePrice(32600);
		price.setVatPolicy(VatPolicy.VAT_EXCLUDED);
		price.setFreightPolicy(FreightPolicy.FREIGHT_EXCLUDED);
		price.setMinOrderQuantity(BigDecimal.ONE);
		productPriceRepository.save(price);

		return new TestFixtures(category, product, variant);
	}
}
