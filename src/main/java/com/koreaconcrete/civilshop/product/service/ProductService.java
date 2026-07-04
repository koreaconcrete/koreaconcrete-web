package com.koreaconcrete.civilshop.product.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.koreaconcrete.civilshop.cart.repository.CartItemRepository;
import com.koreaconcrete.civilshop.category.entity.Category;
import com.koreaconcrete.civilshop.category.service.CategoryService;
import com.koreaconcrete.civilshop.common.api.PageResponse;
import com.koreaconcrete.civilshop.common.domain.ProductMediaType;
import com.koreaconcrete.civilshop.common.domain.ProductStatus;
import com.koreaconcrete.civilshop.common.domain.RelationType;
import com.koreaconcrete.civilshop.common.exception.BusinessException;
import com.koreaconcrete.civilshop.pricing.dto.PricingDtos.PriceSummary;
import com.koreaconcrete.civilshop.pricing.service.PricingService;
import com.koreaconcrete.civilshop.product.dto.ProductDtos.CategoryBrief;
import com.koreaconcrete.civilshop.product.dto.ProductDtos.MediaRequest;
import com.koreaconcrete.civilshop.product.dto.ProductDtos.MediaResponse;
import com.koreaconcrete.civilshop.product.dto.ProductDtos.PriceBrief;
import com.koreaconcrete.civilshop.product.dto.ProductDtos.ProductDetail;
import com.koreaconcrete.civilshop.product.dto.ProductDtos.ProductListItem;
import com.koreaconcrete.civilshop.product.dto.ProductDtos.ProductRequest;
import com.koreaconcrete.civilshop.product.dto.ProductDtos.ProductStatusRequest;
import com.koreaconcrete.civilshop.product.dto.ProductDtos.RelationRequest;
import com.koreaconcrete.civilshop.product.dto.ProductDtos.RelationResponse;
import com.koreaconcrete.civilshop.product.dto.ProductDtos.SpecRequest;
import com.koreaconcrete.civilshop.product.dto.ProductDtos.SpecResponse;
import com.koreaconcrete.civilshop.product.dto.ProductDtos.VariantRequest;
import com.koreaconcrete.civilshop.product.dto.ProductDtos.VariantResponse;
import com.koreaconcrete.civilshop.product.entity.Product;
import com.koreaconcrete.civilshop.product.entity.ProductMedia;
import com.koreaconcrete.civilshop.product.entity.ProductRelation;
import com.koreaconcrete.civilshop.product.entity.ProductSpec;
import com.koreaconcrete.civilshop.product.entity.ProductVariant;
import com.koreaconcrete.civilshop.product.repository.ProductMediaRepository;
import com.koreaconcrete.civilshop.product.repository.ProductRelationRepository;
import com.koreaconcrete.civilshop.product.repository.ProductRepository;
import com.koreaconcrete.civilshop.product.repository.ProductSpecRepository;
import com.koreaconcrete.civilshop.product.repository.ProductVariantRepository;
import com.koreaconcrete.civilshop.search.service.SearchService;

@Service
@Transactional(readOnly = true)
public class ProductService {
	private final ProductRepository productRepository;
	private final ProductVariantRepository productVariantRepository;
	private final ProductSpecRepository productSpecRepository;
	private final ProductMediaRepository productMediaRepository;
	private final ProductRelationRepository productRelationRepository;
	private final CartItemRepository cartItemRepository;
	private final CategoryService categoryService;
	private final PricingService pricingService;
	private final SearchService searchService;

	public ProductService(
			ProductRepository productRepository,
			ProductVariantRepository productVariantRepository,
			ProductSpecRepository productSpecRepository,
			ProductMediaRepository productMediaRepository,
			ProductRelationRepository productRelationRepository,
			CartItemRepository cartItemRepository,
			CategoryService categoryService,
			PricingService pricingService,
			SearchService searchService
	) {
		this.productRepository = productRepository;
		this.productVariantRepository = productVariantRepository;
		this.productSpecRepository = productSpecRepository;
		this.productMediaRepository = productMediaRepository;
		this.productRelationRepository = productRelationRepository;
		this.cartItemRepository = cartItemRepository;
		this.categoryService = categoryService;
		this.pricingService = pricingService;
		this.searchService = searchService;
	}

	@Transactional
	public PageResponse<ProductListItem> list(String keyword, Long categoryId, String sort, int page, int size, String sessionId) {
		ProductStatus status = ProductStatus.ON_SALE;
		Sort sortSpec = "name".equals(sort)
				? Sort.by(Sort.Direction.ASC, "name")
				: Sort.by(Sort.Direction.DESC, "id");
		PageRequest pageRequest = PageRequest.of(
				Math.max(page - 1, 0),
				size,
				sortSpec
		);
		String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
		Page<Product> products = productRepository.search(
				keywordPattern(normalizedKeyword),
				categoryId,
				status,
				false,
				ProductStatus.DELETED,
				pageRequest
		);
		if (StringUtils.hasText(normalizedKeyword)) {
			searchService.log(null, sessionId, normalizedKeyword, (int) products.getTotalElements());
		}
		List<ProductListItem> items = products.stream().map(this::toListItem).toList();
		return PageResponse.of(products, items);
	}

	public PageResponse<ProductListItem> adminList(String keyword, ProductStatus status, boolean includeDeleted, int page, int size) {
		Page<Product> products = productRepository.search(
				keywordPattern(StringUtils.hasText(keyword) ? keyword.trim() : null),
				null,
				status,
				includeDeleted,
				ProductStatus.DELETED,
				PageRequest.of(Math.max(page - 1, 0), size, Sort.by(Sort.Direction.DESC, "id"))
		);
		return PageResponse.of(products, products.stream().map(this::toListItem).toList());
	}

	public List<ProductListItem> popularProducts(int size) {
		int limit = Math.max(1, Math.min(size, 12));
		Map<Long, Product> products = new LinkedHashMap<>();
		productRepository.popularBySearchLogs(
				LocalDateTime.now().minusDays(7),
				ProductStatus.ON_SALE,
				PageRequest.of(0, limit)
		).forEach(product -> products.put(product.getId(), product));

		if (products.size() < limit) {
			productRepository.findByStatusOrderByIdDesc(ProductStatus.ON_SALE, PageRequest.of(0, limit))
					.forEach(product -> products.putIfAbsent(product.getId(), product));
		}

		return products.values().stream()
				.limit(limit)
				.map(this::toListItem)
				.toList();
	}

	public ProductDetail detail(Long id) {
		return toDetail(getProduct(id));
	}

	public List<VariantResponse> variants(Long productId) {
		return productVariantRepository.findByProductIdOrderByIdAsc(productId).stream()
				.map(this::toVariant)
				.toList();
	}

	public List<RelationResponse> relations(Long productId) {
		return productRelationRepository.findBySourceProductIdOrderBySortOrderAscIdAsc(productId).stream()
				.map(this::toRelation)
				.toList();
	}

	@Transactional
	public ProductDetail create(ProductRequest request) {
		Product product = new Product();
		applyProduct(product, request);
		Product saved = productRepository.save(product);
		replaceExtras(saved, request);
		return toDetail(saved);
	}

	@Transactional
	public ProductDetail update(Long id, ProductRequest request) {
		Product product = getProduct(id);
		applyProduct(product, request);
		replaceExtras(product, request);
		return toDetail(product);
	}

	@Transactional
	public void delete(Long id) {
		Product product = getProduct(id);
		if (product.getStatus() == ProductStatus.DELETED) {
			return;
		}
		applyStatus(product, ProductStatus.DELETED);
	}

	@Transactional
	public ProductDetail updateStatus(Long id, ProductStatusRequest request) {
		Product product = getProduct(id);
		applyStatus(product, request.status());
		return toDetail(product);
	}

	@Transactional
	public VariantResponse createVariant(Long productId, VariantRequest request) {
		Product product = getProduct(productId);
		ProductVariant variant = new ProductVariant();
		variant.setProduct(product);
		applyVariant(variant, request);
		return toVariant(productVariantRepository.save(variant));
	}

	@Transactional
	public VariantResponse updateVariant(Long variantId, VariantRequest request) {
		ProductVariant variant = getVariant(variantId);
		applyVariant(variant, request);
		return toVariant(variant);
	}

	@Transactional
	public void deleteVariant(Long variantId) {
		ProductVariant variant = getVariant(variantId);
		if (variant.getStatus() == ProductStatus.DELETED) {
			return;
		}
		cartItemRepository.deleteByVariantId(variantId);
		variant.setStatus(ProductStatus.DELETED);
	}

	public Product getProduct(Long id) {
		return productRepository.findById(id).orElseThrow(() -> BusinessException.notFound("상품을 찾을 수 없습니다."));
	}

	public ProductVariant getVariant(Long id) {
		return productVariantRepository.findById(id).orElseThrow(() -> BusinessException.notFound("상품 규격을 찾을 수 없습니다."));
	}

	public void ensureVariantBelongs(Product product, ProductVariant variant) {
		if (!variant.getProduct().getId().equals(product.getId())) {
			throw BusinessException.badRequest("상품과 규격 정보가 일치하지 않습니다.");
		}
	}

	public void ensureRequestable(Product product, ProductVariant variant) {
		ensureVariantBelongs(product, variant);
		if (product.getStatus() == ProductStatus.DELETED || variant.getStatus() == ProductStatus.DELETED) {
			throw BusinessException.badRequest("삭제된 상품은 요청할 수 없습니다.");
		}
		if (product.getStatus() == ProductStatus.HIDDEN || product.getStatus() == ProductStatus.DISCONTINUED
				|| product.getStatus() == ProductStatus.DRAFT) {
			throw BusinessException.badRequest("현재 요청할 수 없는 상품입니다.");
		}
		if (variant.getStatus() == ProductStatus.HIDDEN || variant.getStatus() == ProductStatus.DISCONTINUED
				|| variant.getStatus() == ProductStatus.DRAFT) {
			throw BusinessException.badRequest("현재 요청할 수 없는 규격입니다.");
		}
		if (product.getStatus() == ProductStatus.SOLD_OUT || variant.getStatus() == ProductStatus.SOLD_OUT) {
			throw BusinessException.badRequest("품절된 상품은 요청할 수 없습니다.");
		}
	}

	private void applyProduct(Product product, ProductRequest request) {
		Category category = categoryService.getCategory(request.categoryId());
		if (category.getParent() == null || category.getDepth() != 2) {
			throw BusinessException.badRequest("상품은 세부 카테고리에만 연결할 수 있습니다.");
		}
		product.setCategory(category);
		product.setSku(resolveSku(product, request));
		product.setName(request.name());
		product.setSummary(request.summary());
		product.setSearchKeywords(request.searchKeywords());
		product.setDescription(request.description());
		product.setUnit(request.unit());
		product.setOriginCountry(request.originCountry());
		product.setManufacturer(request.manufacturer());
		product.setCustomMade(request.customMade() != null && request.customMade());
		product.setLeadTimeDaysMin(request.leadTimeDaysMin());
		product.setLeadTimeDaysMax(request.leadTimeDaysMax());
		applyStatus(product, request.status() == null ? ProductStatus.DRAFT : request.status());
	}

	private void applyStatus(Product product, ProductStatus status) {
		ProductStatus nextStatus = status == null ? ProductStatus.DRAFT : status;
		if (nextStatus == ProductStatus.DELETED && product.getId() != null) {
			cartItemRepository.deleteByProductId(product.getId());
		}
		product.setStatus(nextStatus);
	}

	private String resolveSku(Product product, ProductRequest request) {
		if (StringUtils.hasText(request.sku())) {
			return request.sku().trim();
		}
		if (StringUtils.hasText(product.getSku())) {
			return product.getSku();
		}
		return "AUTO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
	}

	private String keywordPattern(String keyword) {
		return StringUtils.hasText(keyword) ? "%" + keyword.toLowerCase(Locale.ROOT) + "%" : null;
	}

	private void applyVariant(ProductVariant variant, VariantRequest request) {
		variant.setVariantName(request.variantName());
		variant.setWidthMm(request.widthMm());
		variant.setLengthMm(request.lengthMm());
		variant.setHeightMm(request.heightMm());
		variant.setThicknessMm(request.thicknessMm());
		variant.setWeightKg(request.weightKg());
		variant.setTwentyFiveTonQuantity(request.twentyFiveTonQuantity());
		variant.setUnit(request.unit());
		variant.setBarcode(request.barcode());
		variant.setStatus(request.status() == null ? ProductStatus.ON_SALE : request.status());
	}

	private void replaceExtras(Product product, ProductRequest request) {
		if (request.specs() != null) {
			productSpecRepository.findByProductIdOrderBySortOrderAscIdAsc(product.getId()).forEach(productSpecRepository::delete);
			for (SpecRequest specRequest : request.specs()) {
				ProductSpec spec = new ProductSpec();
				spec.setProduct(product);
				ProductVariant variant = specRequest.variantId() == null ? null : getVariant(specRequest.variantId());
				if (variant != null) {
					ensureVariantBelongs(product, variant);
				}
				spec.setVariant(variant);
				spec.setSpecKey(specRequest.specKey());
				spec.setSpecValue(specRequest.specValue());
				spec.setSortOrder(specRequest.sortOrder() == null ? 0 : specRequest.sortOrder());
				productSpecRepository.save(spec);
			}
		}
		if (request.media() != null) {
			productMediaRepository.findByProductIdOrderBySortOrderAscIdAsc(product.getId()).forEach(productMediaRepository::delete);
			for (MediaRequest mediaRequest : request.media()) {
				ProductMedia media = new ProductMedia();
				media.setProduct(product);
				ProductVariant variant = mediaRequest.variantId() == null ? null : getVariant(mediaRequest.variantId());
				if (variant != null) {
					ensureVariantBelongs(product, variant);
				}
				media.setVariant(variant);
				media.setType(mediaRequest.type() == null ? ProductMediaType.IMAGE : mediaRequest.type());
				media.setUrl(mediaRequest.url());
				media.setAltText(mediaRequest.altText());
				media.setSortOrder(mediaRequest.sortOrder() == null ? 0 : mediaRequest.sortOrder());
				productMediaRepository.save(media);
			}
		}
		if (request.relations() != null) {
			productRelationRepository.findBySourceProductIdOrderBySortOrderAscIdAsc(product.getId()).forEach(productRelationRepository::delete);
			for (RelationRequest relationRequest : request.relations()) {
				ProductRelation relation = new ProductRelation();
				relation.setSourceProduct(product);
				relation.setTargetProduct(getProduct(relationRequest.targetProductId()));
				relation.setRelationType(relationRequest.relationType() == null ? RelationType.RELATED : relationRequest.relationType());
				relation.setSortOrder(relationRequest.sortOrder() == null ? 0 : relationRequest.sortOrder());
				productRelationRepository.save(relation);
			}
		}
	}

	private ProductListItem toListItem(Product product) {
		List<ProductVariant> variants = productVariantRepository.findByProductIdOrderByIdAsc(product.getId()).stream()
				.filter(variant -> variant.getStatus() != ProductStatus.HIDDEN
						&& variant.getStatus() != ProductStatus.DISCONTINUED
						&& variant.getStatus() != ProductStatus.DELETED)
				.toList();
		ProductVariant representative = variants.stream()
				.findFirst()
				.orElse(null);
		List<String> variantNames = variants.stream()
				.map(ProductVariant::getVariantName)
				.toList();
		PriceSummary price = representative == null ? null : pricingService.latestPrice(representative.getId());
		String representativeImageUrl = productMediaRepository.findByProductIdOrderBySortOrderAscIdAsc(product.getId()).stream()
				.filter(media -> media.getType() == ProductMediaType.IMAGE)
				.findFirst()
				.map(ProductMedia::getUrl)
				.orElse(null);
		return new ProductListItem(
				product.getId(),
				product.getCategory().getId(),
				product.getCategory().getName(),
				product.getSku(),
				product.getName(),
				product.getSummary(),
				product.getSearchKeywords(),
				representativeImageUrl,
				product.getUnit(),
				product.getStatus(),
				representative == null ? null : representative.getId(),
				representative == null ? null : representative.getVariantName(),
				variantNames,
				price == null ? null : price.salePrice(),
				price == null ? null : price.vatPolicy(),
				price == null ? null : price.freightPolicy()
		);
	}

	private ProductDetail toDetail(Product product) {
		return new ProductDetail(
				product.getId(),
				product.getSku(),
				product.getName(),
				product.getSummary(),
				product.getSearchKeywords(),
				product.getDescription(),
				product.getUnit(),
				product.getOriginCountry(),
				product.getManufacturer(),
				product.getCustomMade(),
				product.getLeadTimeDaysMin(),
				product.getLeadTimeDaysMax(),
				product.getStatus(),
				new CategoryBrief(product.getCategory().getId(), product.getCategory().getName()),
				variants(product.getId()),
				productSpecRepository.findByProductIdOrderBySortOrderAscIdAsc(product.getId()).stream().map(this::toSpec).toList(),
				productMediaRepository.findByProductIdOrderBySortOrderAscIdAsc(product.getId()).stream().map(this::toMedia).toList(),
				relations(product.getId()),
				product.getCreatedAt(),
				product.getUpdatedAt()
		);
	}

	private VariantResponse toVariant(ProductVariant variant) {
		PriceSummary price = pricingService.latestPrice(variant.getId());
		PriceBrief priceBrief = price == null ? null : new PriceBrief(price.id(), price.salePrice(), price.vatPolicy(), price.freightPolicy());
		return new VariantResponse(
				variant.getId(),
				variant.getVariantName(),
				variant.getWidthMm(),
				variant.getLengthMm(),
				variant.getHeightMm(),
				variant.getThicknessMm(),
				variant.getWeightKg(),
				variant.getTwentyFiveTonQuantity(),
				variant.getUnit(),
				variant.getBarcode(),
				variant.getStatus(),
				priceBrief
		);
	}

	private SpecResponse toSpec(ProductSpec spec) {
		return new SpecResponse(
				spec.getId(),
				spec.getVariant() == null ? null : spec.getVariant().getId(),
				spec.getSpecKey(),
				spec.getSpecValue(),
				spec.getSortOrder()
		);
	}

	private MediaResponse toMedia(ProductMedia media) {
		return new MediaResponse(
				media.getId(),
				media.getVariant() == null ? null : media.getVariant().getId(),
				media.getType(),
				media.getUrl(),
				media.getAltText(),
				media.getSortOrder()
		);
	}

	private RelationResponse toRelation(ProductRelation relation) {
		return new RelationResponse(
				relation.getId(),
				relation.getTargetProduct().getId(),
				relation.getTargetProduct().getName(),
				relation.getRelationType(),
				relation.getSortOrder()
		);
	}
}
