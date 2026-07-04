package com.koreaconcrete.civilshop.common.config;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.koreaconcrete.civilshop.category.entity.Category;
import com.koreaconcrete.civilshop.category.repository.CategoryRepository;
import com.koreaconcrete.civilshop.common.domain.FreightPolicy;
import com.koreaconcrete.civilshop.common.domain.ProductMediaType;
import com.koreaconcrete.civilshop.common.domain.ProductStatus;
import com.koreaconcrete.civilshop.common.domain.VatPolicy;
import com.koreaconcrete.civilshop.common.domain.VehicleType;
import com.koreaconcrete.civilshop.freight.entity.DeliveryOption;
import com.koreaconcrete.civilshop.freight.entity.FreightRateRule;
import com.koreaconcrete.civilshop.freight.repository.DeliveryOptionRepository;
import com.koreaconcrete.civilshop.freight.repository.FreightRateRuleRepository;
import com.koreaconcrete.civilshop.pricing.entity.PriceBook;
import com.koreaconcrete.civilshop.pricing.entity.ProductPrice;
import com.koreaconcrete.civilshop.pricing.repository.PriceBookRepository;
import com.koreaconcrete.civilshop.pricing.repository.ProductPriceRepository;
import com.koreaconcrete.civilshop.product.entity.Product;
import com.koreaconcrete.civilshop.product.entity.ProductMedia;
import com.koreaconcrete.civilshop.product.entity.ProductSpec;
import com.koreaconcrete.civilshop.product.entity.ProductVariant;
import com.koreaconcrete.civilshop.product.repository.ProductMediaRepository;
import com.koreaconcrete.civilshop.product.repository.ProductRepository;
import com.koreaconcrete.civilshop.product.repository.ProductSpecRepository;
import com.koreaconcrete.civilshop.product.repository.ProductVariantRepository;
import com.koreaconcrete.civilshop.search.entity.SearchLog;
import com.koreaconcrete.civilshop.search.repository.SearchLogRepository;
import com.koreaconcrete.civilshop.user.entity.Role;
import com.koreaconcrete.civilshop.user.entity.User;
import com.koreaconcrete.civilshop.user.entity.UserRole;
import com.koreaconcrete.civilshop.user.repository.RoleRepository;
import com.koreaconcrete.civilshop.user.repository.UserRepository;
import com.koreaconcrete.civilshop.user.repository.UserRoleRepository;

@Component
@Profile("dev")
public class DataInitializer implements CommandLineRunner {
	private final RoleRepository roleRepository;
	private final UserRepository userRepository;
	private final UserRoleRepository userRoleRepository;
	private final PasswordEncoder passwordEncoder;
	private final CategoryRepository categoryRepository;
	private final ProductRepository productRepository;
	private final ProductVariantRepository productVariantRepository;
	private final ProductSpecRepository productSpecRepository;
	private final ProductMediaRepository productMediaRepository;
	private final PriceBookRepository priceBookRepository;
	private final ProductPriceRepository productPriceRepository;
	private final DeliveryOptionRepository deliveryOptionRepository;
	private final FreightRateRuleRepository freightRateRuleRepository;
	private final SearchLogRepository searchLogRepository;

	public DataInitializer(
			RoleRepository roleRepository,
			UserRepository userRepository,
			UserRoleRepository userRoleRepository,
			PasswordEncoder passwordEncoder,
			CategoryRepository categoryRepository,
			ProductRepository productRepository,
			ProductVariantRepository productVariantRepository,
			ProductSpecRepository productSpecRepository,
			ProductMediaRepository productMediaRepository,
			PriceBookRepository priceBookRepository,
			ProductPriceRepository productPriceRepository,
			DeliveryOptionRepository deliveryOptionRepository,
			FreightRateRuleRepository freightRateRuleRepository,
			SearchLogRepository searchLogRepository
	) {
		this.roleRepository = roleRepository;
		this.userRepository = userRepository;
		this.userRoleRepository = userRoleRepository;
		this.passwordEncoder = passwordEncoder;
		this.categoryRepository = categoryRepository;
		this.productRepository = productRepository;
		this.productVariantRepository = productVariantRepository;
		this.productSpecRepository = productSpecRepository;
		this.productMediaRepository = productMediaRepository;
		this.priceBookRepository = priceBookRepository;
		this.productPriceRepository = productPriceRepository;
		this.deliveryOptionRepository = deliveryOptionRepository;
		this.freightRateRuleRepository = freightRateRuleRepository;
		this.searchLogRepository = searchLogRepository;
	}

	@Override
	@Transactional
	public void run(String... args) {
		seedRolesAndUsers();
		seedPriceBook();
		seedDeliveryOptions();
		seedFreightRules();
	}

	private void seedRolesAndUsers() {
		for (String roleName : List.of("ROLE_MEMBER", "ROLE_BUSINESS_MEMBER", "ROLE_OPERATOR", "ROLE_PRODUCT_MANAGER", "ROLE_ADMIN")) {
			roleRepository.findByName(roleName).orElseGet(() -> roleRepository.save(new Role(roleName)));
		}
		seedUser("admin", "관리자", "01000000000", "ROLE_ADMIN");
		seedUser("buyer", "홍길동", "01012345678", "ROLE_MEMBER");
	}

	private void seedUser(String email, String name, String phone, String roleName) {
		if (userRepository.existsByEmail(email)) {
			return;
		}
		User user = userRepository.save(new User(email, passwordEncoder.encode("Password1234!"), name, phone));
		Role role = roleRepository.findByName(roleName).orElseThrow();
		userRoleRepository.save(new UserRole(user, role));
	}

	private Map<String, Category> seedCategories() {
		Map<String, Category> roots = new LinkedHashMap<>();
		for (GroupSeed seed : groupSeeds()) {
			roots.put(seed.slug(), saveCategory(null, seed.name(), seed.slug(), seed.imageUrl(), 1, seed.sortOrder()));
		}

		Map<String, Category> byBoardCode = new LinkedHashMap<>();
		for (ProductSeed seed : productSeeds()) {
			Category root = roots.get(seed.groupSlug());
			DetailCategorySeed detailSeed = detailCategoryFor(seed);
			Category detailCategory = saveCategory(
					root,
					detailSeed.name(),
					detailSeed.slug(),
					detailSeed.imageUrl(),
					2,
					detailSeed.sortOrder()
			);
			byBoardCode.put(seed.boardCode(), detailCategory);
			deactivateLegacyProductCategory("hk-" + seed.boardCode());
		}
		return byBoardCode;
	}

	private Category saveCategory(Category parent, String name, String slug, String imageUrl, int depth, int sortOrder) {
		return categoryRepository.findBySlug(slug)
				.map(category -> {
					category.setParent(parent);
					category.setName(name);
					category.setImageUrl(imageUrl);
					category.setDepth(depth);
					category.setSortOrder(sortOrder);
					category.setActive(true);
					return category;
				})
				.orElseGet(() -> {
					Category category = new Category(parent, name, slug, depth, sortOrder, true);
					category.setImageUrl(imageUrl);
					return categoryRepository.save(category);
				});
	}

	private void deactivateLegacyProductCategory(String slug) {
		categoryRepository.findBySlug(slug).ifPresent(category -> {
			category.setActive(false);
			category.setParent(null);
		});
	}

	private PriceBook seedPriceBook() {
		return priceBookRepository.findFirstByDefaultBookTrueOrderByIdDesc()
				.orElseGet(() -> {
					PriceBook book = new PriceBook();
					book.setName("기본 가격표");
					book.setEffectiveFrom(LocalDate.now().withDayOfMonth(1));
					book.setDefaultBook(true);
					return priceBookRepository.save(book);
				});
	}

	private void seedProduct(Category category, PriceBook priceBook, ProductSeed seed) {
		if (category == null) {
			throw new IllegalStateException("카테고리 시드가 누락되었습니다: " + seed.boardCode());
		}
		Product product = productRepository.findBySku(seed.sku()).orElseGet(Product::new);
		product.setCategory(category);
		product.setSku(seed.sku());
		product.setName(seed.name());
		product.setSummary(seed.summary());
		product.setSearchKeywords(defaultSearchKeywords(seed));
		product.setDescription(seed.description());
		product.setUnit("개");
		product.setOriginCountry("국산");
		product.setManufacturer("한국콘크리트산업");
		product.setCustomMade(false);
		product.setStatus(ProductStatus.ON_SALE);
		product = productRepository.save(product);

		productSpecRepository.findByProductIdOrderBySortOrderAscIdAsc(product.getId()).forEach(productSpecRepository::delete);
		productPriceRepository.findByProductIdOrderByVariantIdAscIdDesc(product.getId()).forEach(productPriceRepository::delete);

		List<ProductVariant> existingVariants = productVariantRepository.findByProductIdOrderByIdAsc(product.getId());
		List<VariantSeed> variants = variantSeeds(seed);
		for (int index = 0; index < variants.size(); index++) {
			VariantSeed variantSeed = variants.get(index);
			ProductVariant variant = index < existingVariants.size() ? existingVariants.get(index) : new ProductVariant();
			variant.setProduct(product);
			variant.setVariantName(variantSeed.name());
			variant.setUnit(variantSeed.unit());
			variant.setWeightKg(variantSeed.weightKg());
			variant.setTwentyFiveTonQuantity(twentyFiveTonQuantity(variantSeed.weightKg()));
			variant.setStatus(ProductStatus.ON_SALE);
			variant = productVariantRepository.save(variant);
			savePrice(priceBook, product, variant, variantSeed.salePrice());
			saveSpec(product, variant, "규격 - " + variantSeed.name(), variantSeed.salePrice() + "원", 10 + index * 10);
		}

		productMediaRepository.findByProductIdOrderBySortOrderAscIdAsc(product.getId()).forEach(productMediaRepository::delete);
		saveMedia(product, seed.thumbnailUrl(), seed.name() + " 대표 썸네일", 10);
		saveMedia(product, seed.imageUrl(), seed.name() + " 원본 규격표", 20);
	}

	private void savePrice(PriceBook priceBook, Product product, ProductVariant variant, int salePrice) {
		ProductPrice price = new ProductPrice();
		price.setPriceBook(priceBook);
		price.setProduct(product);
		price.setVariant(variant);
		price.setSalePrice(salePrice);
		price.setVatPolicy(VatPolicy.VAT_EXCLUDED);
		price.setFreightPolicy(FreightPolicy.FREIGHT_EXCLUDED);
		price.setMinOrderQuantity(BigDecimal.ONE);
		price.setPriceNote("개발용 임의 규격 단가입니다. 실제 견적 시 조정됩니다.");
		productPriceRepository.save(price);
	}

	private void saveMedia(Product product, String url, String altText, int sortOrder) {
		ProductMedia media = new ProductMedia();
		media.setProduct(product);
		media.setType(ProductMediaType.IMAGE);
		media.setUrl(url);
		media.setAltText(altText);
		media.setSortOrder(sortOrder);
		productMediaRepository.save(media);
	}

	private void saveSpec(Product product, ProductVariant variant, String key, String value, int sortOrder) {
		ProductSpec spec = new ProductSpec();
		spec.setProduct(product);
		spec.setVariant(variant);
		spec.setSpecKey(key);
		spec.setSpecValue(value);
		spec.setSortOrder(sortOrder);
		productSpecRepository.save(spec);
	}

	private void seedLargeVariantDemoProduct(Category category, PriceBook priceBook) {
		if (category == null) {
			throw new IllegalStateException("대량 규격 테스트 상품 카테고리가 누락되었습니다.");
		}
		Product product = productRepository.findBySku("HK-DEMO-85").orElseGet(Product::new);
		product.setCategory(category);
		product.setSku("HK-DEMO-85");
		product.setName("대량 규격 테스트 수로관");
		product.setSummary("85개 규격을 가진 상세 화면 검증용 상품입니다.");
		product.setSearchKeywords("대량규격, 85개규격, 테스트, 수로관, 규격표, 페이지");
		product.setDescription("규격이 많은 상품의 상세 화면과 10개 단위 규격 페이지 처리를 확인하기 위한 개발용 상품입니다.");
		product.setUnit("개");
		product.setOriginCountry("국산");
		product.setManufacturer("한국콘크리트산업");
		product.setCustomMade(false);
		product.setStatus(ProductStatus.ON_SALE);
		product = productRepository.save(product);

		productSpecRepository.findByProductIdOrderBySortOrderAscIdAsc(product.getId()).forEach(productSpecRepository::delete);
		productPriceRepository.findByProductIdOrderByVariantIdAscIdDesc(product.getId()).forEach(productPriceRepository::delete);

		List<ProductVariant> existingVariants = productVariantRepository.findByProductIdOrderByIdAsc(product.getId());
		for (int index = 0; index < 85; index++) {
			int width = 300 + index * 10;
			int height = 250 + (index % 5) * 20;
			int price = 19000 + index * 850;
			ProductVariant variant = index < existingVariants.size() ? existingVariants.get(index) : new ProductVariant();
			variant.setProduct(product);
			variant.setVariantName(width + "x" + height + "x1000");
			variant.setWidthMm(BigDecimal.valueOf(width));
			variant.setLengthMm(BigDecimal.valueOf(1000));
			variant.setHeightMm(BigDecimal.valueOf(height));
			variant.setThicknessMm(BigDecimal.valueOf(45 + (index % 4) * 5));
			BigDecimal weightKg = BigDecimal.valueOf(85 + index * 2L);
			variant.setWeightKg(weightKg);
			variant.setTwentyFiveTonQuantity(twentyFiveTonQuantity(weightKg));
			variant.setUnit("개");
			variant.setStatus(ProductStatus.ON_SALE);
			variant = productVariantRepository.save(variant);
			savePrice(priceBook, product, variant, price);
		}
		for (int index = 85; index < existingVariants.size(); index++) {
			ProductVariant variant = existingVariants.get(index);
			variant.setStatus(ProductStatus.DISCONTINUED);
			productVariantRepository.save(variant);
		}

		saveSpec(product, null, "규격 수", "85개", 10);
		saveSpec(product, null, "표시 방식", "10개 단위 페이지", 20);
		productMediaRepository.findByProductIdOrderBySortOrderAscIdAsc(product.getId()).forEach(productMediaRepository::delete);
		saveMedia(product, "assets/images/products/thumbs/hk-gallery03.jpg", "대량 규격 테스트 수로관 대표 썸네일", 10);
		saveMedia(product, "assets/images/products/hk-gallery03.jpg", "대량 규격 테스트 수로관 규격표", 20);
	}

	private void seedDeliveryOptions() {
		saveDeliveryOption("SITE_DELIVERY", "현장도착도", true, "현장 도착 기준 납품 조건");
		saveDeliveryOption("FACTORY_PICKUP", "공장상차도", false, "고객 차량 상차 기준 납품 조건");
	}

	private void saveDeliveryOption(String code, String name, boolean includesFreight, String description) {
		DeliveryOption option = deliveryOptionRepository.findByCode(code).orElseGet(DeliveryOption::new);
		option.setCode(code);
		option.setName(name);
		option.setIncludesFreight(includesFreight);
		option.setDescription(description);
		option.setActive(true);
		deliveryOptionRepository.save(option);
	}

	private void seedFreightRules() {
		if (freightRateRuleRepository.count() > 0) {
			return;
		}
		saveFreightRate("기본", "경기", VehicleType.TWENTY_FIVE_TON, 300000);
		saveFreightRate("기본", "서울", VehicleType.FIVE_TON_AXIS, 220000);
	}

	private void saveFreightRate(String origin, String destination, VehicleType vehicleType, int amount) {
		FreightRateRule rule = new FreightRateRule();
		rule.setOriginRegion(origin);
		rule.setDestinationRegion(destination);
		rule.setVehicleType(vehicleType);
		rule.setBaseFreightAmount(amount);
		rule.setActive(true);
		freightRateRuleRepository.save(rule);
	}

	private void seedSearchLogs() {
		if (searchLogRepository.count() > 0) {
			return;
		}
		List.of(
				"맨홀", "맨홀", "맨홀",
				"그레이팅", "그레이팅",
				"수로관", "경계석", "PVC", "PE"
		).forEach(keyword -> {
			SearchLog log = new SearchLog();
			log.setSessionId("seed");
			log.setKeyword(keyword);
			log.setResultCount(3);
			searchLogRepository.save(log);
		});
	}

	private List<GroupSeed> groupSeeds() {
		return List.of(
				new GroupSeed("콘크리트제품", "concrete-products", "assets/images/categories/concrete-products.jpg", 10),
				new GroupSeed("스틸/스텐/주철제품", "steel-cast-products", "assets/images/categories/steel-cast-products.jpg", 20),
				new GroupSeed("화강석제품", "granite-products", "assets/images/categories/granite-products.jpg", 30),
				new GroupSeed("PE/PVC제품", "pe-pvc-products", "assets/images/categories/pe-pvc-products.jpg", 40)
		);
	}

	private List<ProductSeed> productSeeds() {
		return List.of(
				product("concrete-products", "gallery01", "맨홀(인버터,전기/통신)", 10),
				product("concrete-products", "gallery02", "맨홀사다리", 20),
				product("concrete-products", "gallery03", "벤치플륨관(1종,2종,3종)", 30),
				product("concrete-products", "gallery04", "측구수로관", 40),
				product("concrete-products", "gallery05", "수로뚜껑", 50),
				product("concrete-products", "gallery06", "물받이블럭", 60),
				product("concrete-products", "gallery07", "가로등기초", 70),
				product("concrete-products", "gallery08", "경계석", 80),
				product("concrete-products", "gallery09", "보도블럭", 90),
				product("concrete-products", "gallery10", "인조화강블럭", 100),
				product("concrete-products", "gallery11", "점토블럭", 110),
				product("concrete-products", "gallery12", "흄관(원심력/VR)", 120),
				product("concrete-products", "gallery13", "원심력사각수로관", 130),
				product("concrete-products", "gallery14", "돌무늬원형수로관", 140),
				product("concrete-products", "gallery15", "암거", 150),
				product("concrete-products", "gallery16", "보강토", 160),
				product("concrete-products", "gallery17", "콘크리트벽돌/적벽돌", 170),
				product("steel-cast-products", "gallerya01", "주철뚜껑/칼라뚜껑", 210),
				product("steel-cast-products", "gallerya02", "디자인뚜껑", 220),
				product("steel-cast-products", "gallerya03", "수목보호판", 230),
				product("steel-cast-products", "gallerya04", "스틸그레이팅", 240),
				product("steel-cast-products", "gallerya05", "무소음그레이팅", 250),
				product("steel-cast-products", "gallerya06", "특수그레이팅", 260),
				product("steel-cast-products", "gallerya07", "스텐볼라드", 270),
				product("granite-products", "galleryb01", "화강경계석", 310),
				product("granite-products", "galleryb02", "화강강판석", 320),
				product("granite-products", "galleryb03", "사구석", 330),
				product("granite-products", "galleryb04", "볼라드", 340),
				product("pe-pvc-products", "galleryc01", "PE빗물받이/PE배수로/PE맨홀", 410),
				product("pe-pvc-products", "galleryc02", "PE이중벽관", 420),
				product("pe-pvc-products", "galleryc03", "PVC고강도이중관", 430),
				product("pe-pvc-products", "galleryc04", "PVC멀티 오수받이/VG1,VG2", 440)
		);
	}

	private ProductSeed product(String groupSlug, String boardCode, String name, int sortOrder) {
		String summary = name + " 제품입니다. 규격과 현장 조건에 따라 견적이 달라질 수 있습니다.";
		String description = "한국콘크리트산업 제품 자료를 기반으로 등록한 " + name
				+ "입니다. 상세 규격은 제품 이미지 및 규격표를 확인하고, 실제 발주 단가와 운반 조건은 견적 요청 후 안내됩니다.";
		return new ProductSeed(
				groupSlug,
				boardCode,
				"HK-" + boardCode.toUpperCase(),
				name,
				summary,
				description,
				"assets/images/products/thumbs/hk-" + boardCode + ".jpg",
				"assets/images/products/hk-" + boardCode + ".jpg",
				"http://www.hk2922.co.kr/bbs/board.php?bo_table=" + boardCode,
				sortOrder
		);
	}

	private DetailCategorySeed detailCategoryFor(ProductSeed seed) {
		return switch (seed.boardCode()) {
			case "gallery01", "gallery02" -> detail("맨홀/부속", "concrete-manhole-parts", "gallery01", 10);
			case "gallery03", "gallery04", "gallery05", "gallery06", "gallery13", "gallery14" ->
					detail("수로관/배수자재", "concrete-drainage", "gallery03", 20);
			case "gallery07", "gallery08" -> detail("경계석/기초", "concrete-curb-base", "gallery08", 30);
			case "gallery09", "gallery10", "gallery11", "gallery17" ->
					detail("블록/벽돌", "concrete-block-brick", "gallery17", 40);
			case "gallery12", "gallery15", "gallery16" ->
					detail("관/암거/보강토", "concrete-pipe-box-retaining", "gallery15", 50);
			case "gallerya01", "gallerya02" -> detail("주철뚜껑/디자인뚜껑", "steel-cast-lids", "gallerya01", 10);
			case "gallerya03", "gallerya07" -> detail("시설물/보호자재", "steel-facility-protection", "gallerya03", 20);
			case "gallerya04", "gallerya05", "gallerya06" -> detail("그레이팅", "steel-grating", "gallerya04", 30);
			case "galleryb01", "galleryb02" -> detail("화강석 경계/판석", "granite-curb-slab", "galleryb01", 10);
			case "galleryb03", "galleryb04" -> detail("화강석 기타", "granite-etc", "galleryb03", 20);
			case "galleryc01" -> detail("PE 배수/맨홀", "pe-drainage-manhole", "galleryc01", 10);
			case "galleryc02", "galleryc03", "galleryc04" -> detail("PE/PVC 관재", "pe-pvc-pipe", "galleryc02", 20);
			default -> detail("기타", seed.groupSlug() + "-etc", seed.boardCode(), 900);
		};
	}

	private DetailCategorySeed detail(String name, String slug, String boardCode, int sortOrder) {
		return new DetailCategorySeed(
				name,
				slug,
				"assets/images/products/thumbs/hk-" + boardCode + ".jpg",
				sortOrder
		);
	}

	private List<VariantSeed> variantSeeds(ProductSeed seed) {
		int basePrice = basePrice(seed);
		if ("steel-cast-products".equals(seed.groupSlug())) {
			return List.of(
					variant("300x1000", "개", "18.0", basePrice),
					variant("400x1000", "개", "24.0", Math.round(basePrice * 1.32f)),
					variant("500x1000", "개", "31.0", Math.round(basePrice * 1.68f))
			);
		}
		if ("granite-products".equals(seed.groupSlug())) {
			return List.of(
					variant("100x100x1000", "개", "27.0", basePrice),
					variant("150x150x1000", "개", "54.0", Math.round(basePrice * 1.42f)),
					variant("200x200x1000", "개", "96.0", Math.round(basePrice * 1.92f))
			);
		}
		if ("pe-pvc-products".equals(seed.groupSlug())) {
			return List.of(
					variant("D200", "개", "4.5", basePrice),
					variant("D300", "개", "8.0", Math.round(basePrice * 1.45f)),
					variant("D400", "개", "13.5", Math.round(basePrice * 1.95f))
			);
		}
		if (seed.name().contains("맨홀")) {
			return List.of(
					variant("900형", "개", "420.0", basePrice),
					variant("1200형", "개", "760.0", Math.round(basePrice * 1.48f)),
					variant("1500형", "개", "1180.0", Math.round(basePrice * 2.05f))
			);
		}
		if (seed.name().contains("관") || seed.name().contains("수로")) {
			return List.of(
					variant("300형", "개", "95.0", basePrice),
					variant("450형", "개", "150.0", Math.round(basePrice * 1.38f)),
					variant("600형", "개", "235.0", Math.round(basePrice * 1.82f))
			);
		}
		return List.of(
				variant("소형", "개", "18.0", basePrice),
				variant("중형", "개", "32.0", Math.round(basePrice * 1.35f)),
				variant("대형", "개", "52.0", Math.round(basePrice * 1.78f))
		);
	}

	private int basePrice(ProductSeed seed) {
		if ("steel-cast-products".equals(seed.groupSlug())) {
			return 38000 + (seed.sortOrder() - 200) * 170;
		}
		if ("granite-products".equals(seed.groupSlug())) {
			return 24000 + (seed.sortOrder() - 300) * 140;
		}
		if ("pe-pvc-products".equals(seed.groupSlug())) {
			return 18000 + (seed.sortOrder() - 400) * 150;
		}
		return 22000 + seed.sortOrder() * 130;
	}

	private VariantSeed variant(String name, String unit, String weightKg, int salePrice) {
		return new VariantSeed(name, unit, new BigDecimal(weightKg), salePrice);
	}

	private BigDecimal twentyFiveTonQuantity(BigDecimal weightKg) {
		if (weightKg == null || weightKg.compareTo(BigDecimal.ZERO) <= 0) {
			return null;
		}
		return BigDecimal.valueOf(25000).divide(weightKg, 0, RoundingMode.DOWN);
	}

	private String defaultSearchKeywords(ProductSeed seed) {
		StringBuilder keywords = new StringBuilder();
		addKeyword(keywords, seed.name());
		addKeyword(keywords, seed.name().replaceAll("[()/,]", " "));
		addKeyword(keywords, seed.boardCode());
		addKeyword(keywords, switch (seed.groupSlug()) {
			case "concrete-products" -> "콘크리트 토목자재 맨홀 수로관 블럭 벽돌 흄관 암거";
			case "steel-cast-products" -> "스틸 스텐 주철 그레이팅 뚜껑 볼라드";
			case "granite-products" -> "화강석 경계석 판석 사구석 볼라드";
			case "pe-pvc-products" -> "PE PVC 피이 피브이씨 배수관 오수받이 빗물받이";
			default -> "토목자재";
		});
		if (seed.name().contains("맨홀")) {
			addKeyword(keywords, "맨홀뚜껑 전기맨홀 통신맨홀 인버터 집수정");
		}
		if (seed.name().contains("수로") || seed.name().contains("측구")) {
			addKeyword(keywords, "수로관 배수로 측구 배수");
		}
		if (seed.name().contains("그레이팅")) {
			addKeyword(keywords, "그레이팅 배수로덮개 스틸그레이팅 스텐그레이팅");
		}
		if (seed.name().contains("경계석")) {
			addKeyword(keywords, "도로경계석 보차도경계석 화강경계석");
		}
		if (seed.name().contains("PVC")) {
			addKeyword(keywords, "PVC 피브이씨 오수관 배수관");
		}
		if (seed.name().contains("PE")) {
			addKeyword(keywords, "PE 피이 폴리에틸렌 배수관");
		}
		return keywords.toString();
	}

	private void addKeyword(StringBuilder keywords, String value) {
		if (value == null || value.isBlank()) {
			return;
		}
		if (!keywords.isEmpty()) {
			keywords.append(", ");
		}
		keywords.append(value.trim().replaceAll("\\s+", " "));
	}

	private record GroupSeed(String name, String slug, String imageUrl, int sortOrder) {
	}

	private record DetailCategorySeed(String name, String slug, String imageUrl, int sortOrder) {
	}

	private record ProductSeed(
			String groupSlug,
			String boardCode,
			String sku,
			String name,
			String summary,
			String description,
			String thumbnailUrl,
			String imageUrl,
			String sourceUrl,
			int sortOrder
	) {
	}

	private record VariantSeed(String name, String unit, BigDecimal weightKg, int salePrice) {
	}
}
