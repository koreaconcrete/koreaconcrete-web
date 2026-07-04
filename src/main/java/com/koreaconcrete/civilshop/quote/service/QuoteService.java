package com.koreaconcrete.civilshop.quote.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koreaconcrete.civilshop.common.api.PageResponse;
import com.koreaconcrete.civilshop.common.audit.AuditService;
import com.koreaconcrete.civilshop.common.domain.QuoteStatus;
import com.koreaconcrete.civilshop.common.domain.ProductMediaType;
import com.koreaconcrete.civilshop.common.domain.ProductStatus;
import com.koreaconcrete.civilshop.common.exception.BusinessException;
import com.koreaconcrete.civilshop.common.security.UserPrincipal;
import com.koreaconcrete.civilshop.pricing.service.PricingService;
import com.koreaconcrete.civilshop.product.entity.Product;
import com.koreaconcrete.civilshop.product.entity.ProductVariant;
import com.koreaconcrete.civilshop.product.repository.ProductMediaRepository;
import com.koreaconcrete.civilshop.product.service.ProductService;
import com.koreaconcrete.civilshop.quote.dto.QuoteDtos.AdminQuoteItemRequest;
import com.koreaconcrete.civilshop.quote.dto.QuoteDtos.QuoteItemRequest;
import com.koreaconcrete.civilshop.quote.dto.QuoteDtos.QuoteItemResponse;
import com.koreaconcrete.civilshop.quote.dto.QuoteDtos.QuoteRequestCreate;
import com.koreaconcrete.civilshop.quote.dto.QuoteDtos.QuoteResponse;
import com.koreaconcrete.civilshop.quote.dto.QuoteDtos.QuoteStatusRequest;
import com.koreaconcrete.civilshop.quote.entity.QuoteItem;
import com.koreaconcrete.civilshop.quote.entity.QuoteRequest;
import com.koreaconcrete.civilshop.quote.repository.QuoteItemRepository;
import com.koreaconcrete.civilshop.quote.repository.QuoteRequestRepository;
import com.koreaconcrete.civilshop.user.entity.User;
import com.koreaconcrete.civilshop.user.service.UserService;

@Service
@Transactional(readOnly = true)
public class QuoteService {
	private final QuoteRequestRepository quoteRequestRepository;
	private final QuoteItemRepository quoteItemRepository;
	private final ProductService productService;
	private final ProductMediaRepository productMediaRepository;
	private final PricingService pricingService;
	private final UserService userService;
	private final AuditService auditService;

	public QuoteService(
			QuoteRequestRepository quoteRequestRepository,
			QuoteItemRepository quoteItemRepository,
			ProductService productService,
			ProductMediaRepository productMediaRepository,
			PricingService pricingService,
			UserService userService,
			AuditService auditService
	) {
		this.quoteRequestRepository = quoteRequestRepository;
		this.quoteItemRepository = quoteItemRepository;
		this.productService = productService;
		this.productMediaRepository = productMediaRepository;
		this.pricingService = pricingService;
		this.userService = userService;
		this.auditService = auditService;
	}

	@Transactional
	public QuoteResponse create(UserPrincipal principal, QuoteRequestCreate request) {
		if (principal == null && !Boolean.TRUE.equals(request.privacyAgreed())) {
			throw BusinessException.badRequest("비회원 견적요청은 개인정보 수집 동의가 필요합니다.");
		}
		QuoteRequest quote = new QuoteRequest();
		User user = principal == null ? null : userService.getUser(principal.id());
		quote.setUser(user);
		quote.setRequestNo(generateRequestNo());
		quote.setCompanyName(request.companyName());
		quote.setContactName(request.contactName());
		quote.setContactPhone(request.contactPhone());
		quote.setSiteAddress(request.siteAddress());
		quote.setRequestedDeliveryDate(request.requestedDeliveryDate());
		quote.setMemo(request.memo());
		quote.setPrivacyAgreed(request.privacyAgreed());
		QuoteRequest saved = quoteRequestRepository.save(quote);
		for (QuoteItemRequest itemRequest : request.items()) {
			quoteItemRepository.save(createItem(saved, itemRequest.productId(), itemRequest.variantId(), itemRequest.quantity(), null, null, null, null, null));
		}
		return toResponse(saved);
	}

	public PageResponse<QuoteResponse> me(UserPrincipal principal, int page, int size) {
		Page<QuoteRequest> quotes = quoteRequestRepository.findByUserIdOrderByIdDesc(
				principal.id(),
				PageRequest.of(Math.max(page - 1, 0), size)
		);
		return PageResponse.of(quotes, quotes.stream().map(this::toResponse).toList());
	}

	@Transactional
	public QuoteResponse detail(Long id, UserPrincipal principal) {
		QuoteRequest quote = getQuote(id);
		if (principal != null) {
			ensureOwner(quote, principal);
			auditService.log(userService.getUser(principal.id()), "READ_QUOTE", "QuoteRequest", id.toString());
		}
		return toResponse(quote);
	}

	@Transactional
	public QuoteResponse approve(Long id, UserPrincipal principal) {
		QuoteRequest quote = getQuote(id);
		ensureOwner(quote, principal);
		quote.setStatus(QuoteStatus.APPROVED);
		return toResponse(quote);
	}

	@Transactional
	public QuoteResponse cancel(Long id, UserPrincipal principal) {
		QuoteRequest quote = getQuote(id);
		ensureOwner(quote, principal);
		quote.setStatus(QuoteStatus.REJECTED);
		return toResponse(quote);
	}

	public PageResponse<QuoteResponse> adminList(QuoteStatus status, int page, int size) {
		PageRequest pageRequest = PageRequest.of(Math.max(page - 1, 0), size);
		Page<QuoteRequest> quotes = status == null
				? quoteRequestRepository.findAllByOrderByIdDesc(pageRequest)
				: quoteRequestRepository.findByStatusOrderByIdDesc(status, pageRequest);
		return PageResponse.of(quotes, quotes.stream().map(this::toResponse).toList());
	}

	@Transactional
	public QuoteResponse updateStatus(Long id, QuoteStatusRequest request) {
		QuoteRequest quote = getQuote(id);
		quote.setStatus(request.status());
		return toResponse(quote);
	}

	@Transactional
	public QuoteResponse addAdminItem(Long quoteId, AdminQuoteItemRequest request) {
		QuoteRequest quote = getQuote(quoteId);
		quoteItemRepository.save(createItem(
				quote,
				request.productId(),
				request.variantId(),
				request.quantity(),
				request.unitPrice(),
				request.freightAmount(),
				request.vatAmount(),
				request.totalAmount(),
				request.note()
		));
		return toResponse(quote);
	}

	@Transactional
	public QuoteResponse send(Long quoteId) {
		QuoteRequest quote = getQuote(quoteId);
		quote.setStatus(QuoteStatus.QUOTED);
		return toResponse(quote);
	}

	private QuoteItem createItem(
			QuoteRequest quote,
			Long productId,
			Long variantId,
			BigDecimal quantity,
			Integer unitPrice,
			Integer freightAmount,
			Integer vatAmount,
			Integer totalAmount,
			String note
	) {
		requirePositiveQuantity(quantity);
		Product product = productService.getProduct(productId);
		ProductVariant variant = productService.getVariant(variantId);
		productService.ensureRequestable(product, variant);
		QuoteItem item = new QuoteItem();
		item.setQuoteRequest(quote);
		item.setProduct(product);
		item.setVariant(variant);
		item.setQuantity(quantity);
		item.setUnitPrice(unitPrice == null ? pricingService.priceSnapshot(variantId) : unitPrice);
		item.setFreightAmount(freightAmount);
		item.setVatAmount(vatAmount);
		item.setTotalAmount(totalAmount);
		item.setNote(note);
		return item;
	}

	private QuoteRequest getQuote(Long id) {
		return quoteRequestRepository.findById(id)
				.orElseThrow(() -> BusinessException.notFound("견적요청을 찾을 수 없습니다."));
	}

	private String generateRequestNo() {
		return "QT-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
	}

	private void requirePositiveQuantity(BigDecimal quantity) {
		if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
			throw BusinessException.badRequest("수량은 0보다 커야 합니다.");
		}
	}

	private void ensureOwner(QuoteRequest quote, UserPrincipal principal) {
		if (principal == null || quote.getUser() == null || !quote.getUser().getId().equals(principal.id())) {
			throw BusinessException.forbidden("견적요청에 접근할 권한이 없습니다.");
		}
	}

	private QuoteResponse toResponse(QuoteRequest quote) {
		List<QuoteItemResponse> items = quoteItemRepository.findByQuoteRequestIdOrderByIdAsc(quote.getId()).stream()
				.map(this::toItemResponse)
				.toList();
		return new QuoteResponse(
				quote.getId(),
				quote.getRequestNo(),
				quote.getCompanyName(),
				quote.getContactName(),
				quote.getContactPhone(),
				quote.getSiteAddress(),
				quote.getRequestedDeliveryDate(),
				quote.getMemo(),
				quote.getPrivacyAgreed(),
				quote.getStatus(),
				items,
				quote.getCreatedAt(),
				quote.getUpdatedAt()
		);
	}

	private QuoteItemResponse toItemResponse(QuoteItem item) {
		return new QuoteItemResponse(
				item.getId(),
				item.getProduct().getId(),
				item.getProduct().getName(),
				item.getProduct().getStatus() == ProductStatus.DELETED,
				item.getProduct().getSummary(),
				representativeImageUrl(item.getProduct().getId()),
				item.getVariant().getId(),
				item.getVariant().getVariantName(),
				item.getQuantity(),
				item.getUnitPrice(),
				item.getFreightAmount(),
				item.getVatAmount(),
				item.getTotalAmount(),
				item.getNote()
		);
	}

	private String representativeImageUrl(Long productId) {
		return productMediaRepository.findByProductIdOrderBySortOrderAscIdAsc(productId).stream()
				.filter(media -> media.getType() == ProductMediaType.IMAGE)
				.findFirst()
				.map(media -> media.getUrl())
				.orElse(null);
	}
}
