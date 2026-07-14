package com.koreaconcrete.civilshop.quote.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koreaconcrete.civilshop.common.api.PageResponse;
import com.koreaconcrete.civilshop.common.audit.AuditService;
import com.koreaconcrete.civilshop.common.domain.ProductMediaType;
import com.koreaconcrete.civilshop.common.domain.ProductStatus;
import com.koreaconcrete.civilshop.common.domain.QuoteStatus;
import com.koreaconcrete.civilshop.common.exception.BusinessException;
import com.koreaconcrete.civilshop.common.notification.QuoteCreatedEvent;
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
	private final ApplicationEventPublisher eventPublisher;

	public QuoteService(
			QuoteRequestRepository quoteRequestRepository,
			QuoteItemRepository quoteItemRepository,
			ProductService productService,
			ProductMediaRepository productMediaRepository,
			PricingService pricingService,
			UserService userService,
			AuditService auditService,
			ApplicationEventPublisher eventPublisher
	) {
		this.quoteRequestRepository = quoteRequestRepository;
		this.quoteItemRepository = quoteItemRepository;
		this.productService = productService;
		this.productMediaRepository = productMediaRepository;
		this.pricingService = pricingService;
		this.userService = userService;
		this.auditService = auditService;
		this.eventPublisher = eventPublisher;
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
		QuoteResponse response = toResponse(saved);
		eventPublisher.publishEvent(new QuoteCreatedEvent(
				response.id(),
				response.requestNo(),
				response.companyName(),
				response.contactName(),
				response.contactPhone(),
				response.siteAddress(),
				response.requestedDeliveryDate(),
				quoteItemSummary(response.items()),
				quoteTotalAmountLabel(response.items())
		));
		return response;
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
		return adminList(status, null, page, size);
	}

	public PageResponse<QuoteResponse> adminList(QuoteStatus status, String bucket, int page, int size) {
		PageRequest pageRequest = PageRequest.of(Math.max(page - 1, 0), size);
		Page<QuoteRequest> quotes;
		if (status != null) {
			quotes = quoteRequestRepository.findByStatusOrderByIdDesc(status, pageRequest);
		} else {
			List<QuoteStatus> statuses = statusesForBucket(bucket);
			quotes = statuses.isEmpty()
					? quoteRequestRepository.findAllByOrderByIdDesc(pageRequest)
					: quoteRequestRepository.findByStatusInOrderByIdDesc(statuses, pageRequest);
		}
		return PageResponse.of(quotes, quotes.stream().map(this::toResponse).toList());
	}

	private List<QuoteStatus> statusesForBucket(String bucket) {
		String normalized = bucket == null ? "" : bucket.trim().toUpperCase();
		return switch (normalized) {
			case "NEW" -> List.of(QuoteStatus.SUBMITTED);
			case "ACTIVE" -> List.of(QuoteStatus.SUBMITTED, QuoteStatus.REVIEWING, QuoteStatus.QUOTED, QuoteStatus.NEGOTIATING);
			case "PROCESSING" -> List.of(QuoteStatus.REVIEWING, QuoteStatus.QUOTED, QuoteStatus.NEGOTIATING);
			case "DONE" -> List.of(QuoteStatus.APPROVED, QuoteStatus.REJECTED, QuoteStatus.EXPIRED);
			default -> List.of();
		};
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

	private String quoteItemSummary(List<QuoteItemResponse> items) {
		if (items == null || items.isEmpty()) {
			return "-";
		}
		return items.stream()
				.map((item) -> blankToDash(item.productName()) + " " + quantityLabel(item.quantity()) + "개")
				.collect(java.util.stream.Collectors.joining(", "));
	}

	private String quoteTotalAmountLabel(List<QuoteItemResponse> items) {
		if (items == null || items.isEmpty()) {
			return "-";
		}
		BigDecimal total = BigDecimal.ZERO;
		boolean hasUnpricedItem = false;
		for (QuoteItemResponse item : items) {
			BigDecimal lineAmount = lineAmount(item);
			if (lineAmount == null) {
				hasUnpricedItem = true;
				continue;
			}
			total = total.add(lineAmount);
		}
		if (total.compareTo(BigDecimal.ZERO) <= 0 && hasUnpricedItem) {
			return "견적문의";
		}
		String amount = String.format("%,d원", total.setScale(0, RoundingMode.HALF_UP).longValue());
		return hasUnpricedItem ? amount + " (견적문의 품목 포함)" : amount;
	}

	private BigDecimal lineAmount(QuoteItemResponse item) {
		if (item.totalAmount() != null) {
			return BigDecimal.valueOf(item.totalAmount());
		}
		if (item.unitPrice() == null || item.quantity() == null) {
			return null;
		}
		return BigDecimal.valueOf(item.unitPrice()).multiply(item.quantity());
	}

	private String quantityLabel(BigDecimal quantity) {
		if (quantity == null) {
			return "0";
		}
		return quantity.stripTrailingZeros().toPlainString();
	}

	private String blankToDash(String value) {
		return value == null || value.isBlank() ? "-" : value.trim();
	}

	private String representativeImageUrl(Long productId) {
		return productMediaRepository.findByProductIdOrderBySortOrderAscIdAsc(productId).stream()
				.filter(media -> media.getType() == ProductMediaType.IMAGE)
				.findFirst()
				.map(media -> media.getUrl())
				.orElse(null);
	}
}
