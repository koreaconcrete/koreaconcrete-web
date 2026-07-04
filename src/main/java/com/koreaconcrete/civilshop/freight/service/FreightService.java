package com.koreaconcrete.civilshop.freight.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koreaconcrete.civilshop.common.domain.VehicleType;
import com.koreaconcrete.civilshop.common.exception.BusinessException;
import com.koreaconcrete.civilshop.freight.dto.FreightDtos.FreightEstimateItemResponse;
import com.koreaconcrete.civilshop.freight.dto.FreightDtos.FreightEstimateRequest;
import com.koreaconcrete.civilshop.freight.dto.FreightDtos.FreightEstimateResponse;
import com.koreaconcrete.civilshop.freight.dto.FreightDtos.FreightRateRuleRequest;
import com.koreaconcrete.civilshop.freight.dto.FreightDtos.FreightRateRuleResponse;
import com.koreaconcrete.civilshop.freight.dto.FreightDtos.LoadingRuleRequest;
import com.koreaconcrete.civilshop.freight.dto.FreightDtos.LoadingRuleResponse;
import com.koreaconcrete.civilshop.freight.entity.FreightEstimate;
import com.koreaconcrete.civilshop.freight.entity.FreightRateRule;
import com.koreaconcrete.civilshop.freight.entity.LoadingRule;
import com.koreaconcrete.civilshop.freight.repository.FreightEstimateRepository;
import com.koreaconcrete.civilshop.freight.repository.FreightRateRuleRepository;
import com.koreaconcrete.civilshop.freight.repository.LoadingRuleRepository;
import com.koreaconcrete.civilshop.product.entity.Product;
import com.koreaconcrete.civilshop.product.entity.ProductVariant;
import com.koreaconcrete.civilshop.product.service.ProductService;

@Service
@Transactional(readOnly = true)
public class FreightService {
	private static final int FALLBACK_FREIGHT = 300_000;

	private final LoadingRuleRepository loadingRuleRepository;
	private final FreightRateRuleRepository freightRateRuleRepository;
	private final FreightEstimateRepository freightEstimateRepository;
	private final ProductService productService;
	private final ObjectMapper objectMapper;

	public FreightService(
			LoadingRuleRepository loadingRuleRepository,
			FreightRateRuleRepository freightRateRuleRepository,
			FreightEstimateRepository freightEstimateRepository,
			ProductService productService,
			ObjectMapper objectMapper
	) {
		this.loadingRuleRepository = loadingRuleRepository;
		this.freightRateRuleRepository = freightRateRuleRepository;
		this.freightEstimateRepository = freightEstimateRepository;
		this.productService = productService;
		this.objectMapper = objectMapper;
	}

	public List<LoadingRuleResponse> loadingRules(Long productId) {
		return loadingRuleRepository.findByProductIdOrderByVehicleTypeAsc(productId).stream()
				.map(this::toLoadingRuleResponse)
				.toList();
	}

	@Transactional
	public FreightEstimateResponse estimate(FreightEstimateRequest request) {
		Product firstProduct = productService.getProduct(request.items().get(0).productId());
		ProductVariant firstVariant = productService.getVariant(request.items().get(0).variantId());
		productService.ensureVariantBelongs(firstProduct, firstVariant);
		VehicleType vehicleType = chooseVehicle(request);
		FreightRateRule rateRule = findRateRule(request.destinationAddress(), vehicleType);
		boolean fallback = rateRule == null;
		int baseFreight = fallback ? FALLBACK_FREIGHT : rateRule.getBaseFreightAmount();
		List<String> notes = new ArrayList<>();
		notes.add("운반비는 지역, 차량, 현장 진입 조건에 따라 변경될 수 있습니다.");
		if (fallback) {
			notes.add("운반비 룰이 없어 기본 금액으로 계산되었습니다. 관리자 확인이 필요합니다.");
		}

		int totalFreight = 0;
		List<FreightEstimateItemResponse> itemResponses = new ArrayList<>();
		for (var item : request.items()) {
			requirePositiveQuantity(item.quantity());
			Product product = productService.getProduct(item.productId());
			ProductVariant variant = productService.getVariant(item.variantId());
			productService.ensureVariantBelongs(product, variant);
			LoadingRule loadingRule = loadingRuleRepository.findFirstByVariantIdAndVehicleType(item.variantId(), vehicleType)
					.orElseGet(() -> loadingRuleRepository.findFirstByVariantIdOrderByLoadQuantityDesc(item.variantId()).orElse(null));
			BigDecimal loadQuantity = loadingRule == null || loadingRule.getLoadQuantity() == null
					? item.quantity()
					: loadingRule.getLoadQuantity();
			if (loadQuantity.compareTo(BigDecimal.ZERO) <= 0) {
				throw BusinessException.badRequest("상차수량은 0보다 커야 합니다.");
			}
			int vehicleCount = item.quantity().divide(loadQuantity, 0, RoundingMode.CEILING).max(BigDecimal.ONE).intValue();
			int freightAmount = baseFreight * vehicleCount;
			int unitFreight = item.quantity().compareTo(BigDecimal.ZERO) <= 0
					? freightAmount
					: BigDecimal.valueOf(freightAmount).divide(item.quantity(), 0, RoundingMode.CEILING).intValue();
			totalFreight += freightAmount;
			itemResponses.add(new FreightEstimateItemResponse(
					variant.getProduct().getId(),
					variant.getId(),
					item.quantity(),
					loadQuantity,
					vehicleCount,
					unitFreight,
					freightAmount
			));
		}

		FreightEstimate estimate = new FreightEstimate();
		estimate.setProduct(firstProduct);
		estimate.setVariant(firstVariant);
		estimate.setQuantity(request.items().get(0).quantity());
		estimate.setDestinationAddress(request.destinationAddress());
		estimate.setVehicleType(vehicleType);
		estimate.setEstimatedFreight(totalFreight);
		estimate.setUnitFreight(itemResponses.get(0).unitFreightAmount());
		estimate.setCalculationSnapshot(toSnapshot(request, itemResponses, fallback, baseFreight, notes));
		FreightEstimate saved = freightEstimateRepository.save(estimate);

		return new FreightEstimateResponse(saved.getId(), vehicleType, baseFreight, totalFreight, itemResponses, notes);
	}

	@Transactional
	public LoadingRuleResponse createLoadingRule(LoadingRuleRequest request) {
		LoadingRule loadingRule = new LoadingRule();
		applyLoadingRule(loadingRule, request);
		return toLoadingRuleResponse(loadingRuleRepository.save(loadingRule));
	}

	@Transactional
	public LoadingRuleResponse updateLoadingRule(Long ruleId, LoadingRuleRequest request) {
		LoadingRule loadingRule = loadingRuleRepository.findById(ruleId)
				.orElseThrow(() -> BusinessException.notFound("상차 규칙을 찾을 수 없습니다."));
		applyLoadingRule(loadingRule, request);
		return toLoadingRuleResponse(loadingRule);
	}

	@Transactional
	public FreightRateRuleResponse createRateRule(FreightRateRuleRequest request) {
		FreightRateRule rule = new FreightRateRule();
		applyRateRule(rule, request);
		return toRateRuleResponse(freightRateRuleRepository.save(rule));
	}

	@Transactional
	public FreightRateRuleResponse updateRateRule(Long ruleId, FreightRateRuleRequest request) {
		FreightRateRule rule = freightRateRuleRepository.findById(ruleId)
				.orElseThrow(() -> BusinessException.notFound("운반비 규칙을 찾을 수 없습니다."));
		applyRateRule(rule, request);
		return toRateRuleResponse(rule);
	}

	private VehicleType chooseVehicle(FreightEstimateRequest request) {
		if (request.preferredVehicleType() != null) {
			return request.preferredVehicleType();
		}
		return loadingRuleRepository.findFirstByVariantIdOrderByLoadQuantityDesc(request.items().get(0).variantId())
				.map(LoadingRule::getVehicleType)
				.orElse(VehicleType.TWENTY_FIVE_TON);
	}

	private FreightRateRule findRateRule(String destinationAddress, VehicleType vehicleType) {
		return freightRateRuleRepository.findByActiveTrueAndVehicleTypeOrderByIdAsc(vehicleType).stream()
				.filter(rule -> destinationAddress != null && destinationAddress.contains(rule.getDestinationRegion()))
				.findFirst()
				.orElse(null);
	}

	private String toSnapshot(FreightEstimateRequest request, List<FreightEstimateItemResponse> items, boolean fallback, int baseFreight, List<String> notes) {
		try {
			Map<String, Object> snapshot = new LinkedHashMap<>();
			snapshot.put("request", request);
			snapshot.put("items", items);
			snapshot.put("fallback", fallback);
			snapshot.put("baseFreight", baseFreight);
			snapshot.put("notes", notes);
			return objectMapper.writeValueAsString(snapshot);
		} catch (Exception exception) {
			return "{}";
		}
	}

	private void applyLoadingRule(LoadingRule loadingRule, LoadingRuleRequest request) {
		requirePositiveQuantity(request.loadQuantity());
		Product product = productService.getProduct(request.productId());
		ProductVariant variant = productService.getVariant(request.variantId());
		productService.ensureVariantBelongs(product, variant);
		loadingRule.setProduct(product);
		loadingRule.setVariant(variant);
		loadingRule.setVehicleType(request.vehicleType());
		loadingRule.setLoadQuantity(request.loadQuantity());
		loadingRule.setLoadUnit(request.loadUnit());
		loadingRule.setPalletQuantity(request.palletQuantity());
		loadingRule.setPalletWeightKg(request.palletWeightKg());
		loadingRule.setNote(request.note());
	}

	private void applyRateRule(FreightRateRule rule, FreightRateRuleRequest request) {
		rule.setOriginRegion(request.originRegion());
		rule.setDestinationRegion(request.destinationRegion());
		rule.setVehicleType(request.vehicleType());
		rule.setBaseFreightAmount(request.baseFreightAmount());
		rule.setSurchargeRate(request.surchargeRate() == null ? BigDecimal.ZERO : request.surchargeRate());
		rule.setFuelSurchargeRate(request.fuelSurchargeRate() == null ? BigDecimal.ZERO : request.fuelSurchargeRate());
		rule.setActive(request.active() == null || request.active());
	}

	private LoadingRuleResponse toLoadingRuleResponse(LoadingRule rule) {
		return new LoadingRuleResponse(
				rule.getId(),
				rule.getProduct().getId(),
				rule.getVariant().getId(),
				rule.getVehicleType(),
				rule.getLoadQuantity(),
				rule.getLoadUnit(),
				rule.getPalletQuantity(),
				rule.getPalletWeightKg(),
				rule.getNote()
		);
	}

	private FreightRateRuleResponse toRateRuleResponse(FreightRateRule rule) {
		return new FreightRateRuleResponse(
				rule.getId(),
				rule.getOriginRegion(),
				rule.getDestinationRegion(),
				rule.getVehicleType(),
				rule.getBaseFreightAmount(),
				rule.getSurchargeRate(),
				rule.getFuelSurchargeRate(),
				rule.getActive()
		);
	}

	private void requirePositiveQuantity(BigDecimal quantity) {
		if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
			throw BusinessException.badRequest("수량은 0보다 커야 합니다.");
		}
	}
}
