package com.koreaconcrete.civilshop.pricing.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koreaconcrete.civilshop.common.exception.BusinessException;
import com.koreaconcrete.civilshop.pricing.dto.PricingDtos.PriceBookRequest;
import com.koreaconcrete.civilshop.pricing.dto.PricingDtos.PriceBookResponse;
import com.koreaconcrete.civilshop.pricing.dto.PricingDtos.PriceSummary;
import com.koreaconcrete.civilshop.pricing.dto.PricingDtos.ProductPriceRequest;
import com.koreaconcrete.civilshop.pricing.entity.PriceBook;
import com.koreaconcrete.civilshop.pricing.entity.ProductPrice;
import com.koreaconcrete.civilshop.pricing.repository.PriceBookRepository;
import com.koreaconcrete.civilshop.pricing.repository.ProductPriceRepository;
import com.koreaconcrete.civilshop.product.entity.Product;
import com.koreaconcrete.civilshop.product.entity.ProductVariant;
import com.koreaconcrete.civilshop.product.repository.ProductRepository;
import com.koreaconcrete.civilshop.product.repository.ProductVariantRepository;

@Service
@Transactional(readOnly = true)
public class PricingService {
	private final PriceBookRepository priceBookRepository;
	private final ProductPriceRepository productPriceRepository;
	private final ProductRepository productRepository;
	private final ProductVariantRepository productVariantRepository;

	public PricingService(
			PriceBookRepository priceBookRepository,
			ProductPriceRepository productPriceRepository,
			ProductRepository productRepository,
			ProductVariantRepository productVariantRepository
	) {
		this.priceBookRepository = priceBookRepository;
		this.productPriceRepository = productPriceRepository;
		this.productRepository = productRepository;
		this.productVariantRepository = productVariantRepository;
	}

	public List<PriceSummary> productPrices(Long productId) {
		return productPriceRepository.findByProductIdOrderByVariantIdAscIdDesc(productId).stream()
				.map(this::toSummary)
				.toList();
	}

	public List<PriceBookResponse> priceBooks() {
		return priceBookRepository.findAll().stream().map(this::toBookResponse).toList();
	}

	@Transactional
	public PriceBookResponse createBook(PriceBookRequest request) {
		PriceBook book = new PriceBook();
		book.setName(request.name());
		book.setEffectiveFrom(request.effectiveFrom());
		book.setEffectiveTo(request.effectiveTo());
		book.setDefaultBook(request.defaultBook() != null && request.defaultBook());
		return toBookResponse(priceBookRepository.save(book));
	}

	@Transactional
	public PriceSummary createPrice(ProductPriceRequest request) {
		PriceBook book = priceBookRepository.findById(request.priceBookId())
				.orElseThrow(() -> BusinessException.notFound("가격표를 찾을 수 없습니다."));
		Product product = productRepository.findById(request.productId())
				.orElseThrow(() -> BusinessException.notFound("상품을 찾을 수 없습니다."));
		ProductVariant variant = productVariantRepository.findById(request.variantId())
				.orElseThrow(() -> BusinessException.notFound("상품 규격을 찾을 수 없습니다."));
		ensureVariantBelongs(product, variant);
		ProductPrice price = new ProductPrice();
		apply(price, request, book, product, variant);
		return toSummary(productPriceRepository.save(price));
	}

	@Transactional
	public PriceSummary updatePrice(Long priceId, ProductPriceRequest request) {
		ProductPrice price = productPriceRepository.findById(priceId)
				.orElseThrow(() -> BusinessException.notFound("가격을 찾을 수 없습니다."));
		PriceBook book = priceBookRepository.findById(request.priceBookId())
				.orElseThrow(() -> BusinessException.notFound("가격표를 찾을 수 없습니다."));
		Product product = productRepository.findById(request.productId())
				.orElseThrow(() -> BusinessException.notFound("상품을 찾을 수 없습니다."));
		ProductVariant variant = productVariantRepository.findById(request.variantId())
				.orElseThrow(() -> BusinessException.notFound("상품 규격을 찾을 수 없습니다."));
		ensureVariantBelongs(product, variant);
		apply(price, request, book, product, variant);
		return toSummary(price);
	}

	@Transactional
	public void deletePrice(Long priceId) {
		ProductPrice price = productPriceRepository.findById(priceId)
				.orElseThrow(() -> BusinessException.notFound("가격을 찾을 수 없습니다."));
		productPriceRepository.delete(price);
	}

	public Integer priceSnapshot(Long variantId) {
		return productPriceRepository.findFirstByVariantIdOrderByIdDesc(variantId)
				.map(ProductPrice::getSalePrice)
				.orElse(null);
	}

	public PriceSummary latestPrice(Long variantId) {
		return productPriceRepository.findFirstByVariantIdOrderByIdDesc(variantId)
				.map(this::toSummary)
				.orElse(null);
	}

	private void apply(ProductPrice price, ProductPriceRequest request, PriceBook book, Product product, ProductVariant variant) {
		if (request.salePrice() == null || request.salePrice() <= 0) {
			throw BusinessException.badRequest("판매단가는 0보다 커야 합니다.");
		}
		if (request.minOrderQuantity() != null && request.minOrderQuantity().compareTo(java.math.BigDecimal.ZERO) <= 0) {
			throw BusinessException.badRequest("최소 주문수량은 0보다 커야 합니다.");
		}
		price.setPriceBook(book);
		price.setProduct(product);
		price.setVariant(variant);
		price.setSalePrice(request.salePrice());
		price.setVatPolicy(request.vatPolicy());
		price.setFreightPolicy(request.freightPolicy());
		price.setMinOrderQuantity(request.minOrderQuantity() == null ? java.math.BigDecimal.ONE : request.minOrderQuantity());
		price.setPriceNote(request.priceNote());
	}

	private void ensureVariantBelongs(Product product, ProductVariant variant) {
		if (!variant.getProduct().getId().equals(product.getId())) {
			throw BusinessException.badRequest("상품과 규격 정보가 일치하지 않습니다.");
		}
	}

	private PriceSummary toSummary(ProductPrice price) {
		return new PriceSummary(
				price.getId(),
				price.getProduct().getId(),
				price.getVariant().getId(),
				price.getSalePrice(),
				price.getVatPolicy(),
				price.getFreightPolicy(),
				price.getMinOrderQuantity(),
				price.getPriceNote(),
				price.getCreatedAt()
		);
	}

	private PriceBookResponse toBookResponse(PriceBook book) {
		return new PriceBookResponse(
				book.getId(),
				book.getName(),
				book.getEffectiveFrom(),
				book.getEffectiveTo(),
				book.getDefaultBook(),
				book.getCreatedAt()
		);
	}
}
