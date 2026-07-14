package com.koreaconcrete.civilshop.cart.service;

import java.util.List;
import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koreaconcrete.civilshop.cart.dto.CartDtos.AddCartItemRequest;
import com.koreaconcrete.civilshop.cart.dto.CartDtos.CartItemResponse;
import com.koreaconcrete.civilshop.cart.dto.CartDtos.CartResponse;
import com.koreaconcrete.civilshop.cart.dto.CartDtos.CartToQuoteRequest;
import com.koreaconcrete.civilshop.cart.dto.CartDtos.UpdateCartItemRequest;
import com.koreaconcrete.civilshop.cart.entity.Cart;
import com.koreaconcrete.civilshop.cart.entity.CartItem;
import com.koreaconcrete.civilshop.cart.repository.CartItemRepository;
import com.koreaconcrete.civilshop.cart.repository.CartRepository;
import com.koreaconcrete.civilshop.common.domain.CartType;
import com.koreaconcrete.civilshop.common.domain.ProductMediaType;
import com.koreaconcrete.civilshop.common.exception.BusinessException;
import com.koreaconcrete.civilshop.common.security.UserPrincipal;
import com.koreaconcrete.civilshop.pricing.service.PricingService;
import com.koreaconcrete.civilshop.product.entity.Product;
import com.koreaconcrete.civilshop.product.entity.ProductMedia;
import com.koreaconcrete.civilshop.product.entity.ProductVariant;
import com.koreaconcrete.civilshop.product.repository.ProductMediaRepository;
import com.koreaconcrete.civilshop.product.service.ProductService;
import com.koreaconcrete.civilshop.quote.dto.QuoteDtos.QuoteItemRequest;
import com.koreaconcrete.civilshop.quote.dto.QuoteDtos.QuoteRequestCreate;
import com.koreaconcrete.civilshop.quote.dto.QuoteDtos.QuoteResponse;
import com.koreaconcrete.civilshop.quote.service.QuoteService;
import com.koreaconcrete.civilshop.user.service.UserService;

@Service
@Transactional(readOnly = true)
public class CartService {
	private final CartRepository cartRepository;
	private final CartItemRepository cartItemRepository;
	private final ProductService productService;
	private final ProductMediaRepository productMediaRepository;
	private final PricingService pricingService;
	private final UserService userService;
	private final QuoteService quoteService;

	public CartService(
			CartRepository cartRepository,
			CartItemRepository cartItemRepository,
			ProductService productService,
			ProductMediaRepository productMediaRepository,
			PricingService pricingService,
			UserService userService,
			QuoteService quoteService
	) {
		this.cartRepository = cartRepository;
		this.cartItemRepository = cartItemRepository;
		this.productService = productService;
		this.productMediaRepository = productMediaRepository;
		this.pricingService = pricingService;
		this.userService = userService;
		this.quoteService = quoteService;
	}

	@Transactional
	public CartResponse get(UserPrincipal principal, String sessionId) {
		Cart cart = getOrCreateCart(principal, sessionId);
		return toResponse(cart);
	}

	@Transactional
	public CartResponse add(UserPrincipal principal, String sessionId, AddCartItemRequest request) {
		requirePositiveQuantity(request.quantity());
		Cart cart = getOrCreateCart(principal, sessionId);
		Product product = productService.getProduct(request.productId());
		ProductVariant variant = productService.getVariant(request.variantId());
		productService.ensureRequestable(product, variant);
		CartItem item = cartItemRepository.findFirstByCartIdAndVariantId(cart.getId(), variant.getId())
				.orElseGet(() -> {
					CartItem created = new CartItem();
					created.setCart(cart);
					created.setProduct(product);
					created.setVariant(variant);
					created.setQuantity(BigDecimal.ZERO);
					return created;
				});
		item.setQuantity(item.getQuantity().add(request.quantity()));
		item.setUnitPriceSnapshot(pricingService.priceSnapshot(variant.getId()));
		cartItemRepository.save(item);
		return toResponse(cart);
	}

	@Transactional
	public CartResponse update(UserPrincipal principal, String sessionId, Long itemId, UpdateCartItemRequest request) {
		requirePositiveQuantity(request.quantity());
		CartItem item = cartItemRepository.findById(itemId)
				.orElseThrow(() -> BusinessException.notFound("장바구니 품목을 찾을 수 없습니다."));
		ensureCartAccess(item.getCart(), principal, sessionId);
		item.setQuantity(request.quantity());
		return toResponse(item.getCart());
	}

	@Transactional
	public void delete(UserPrincipal principal, String sessionId, Long itemId) {
		CartItem item = cartItemRepository.findById(itemId)
				.orElseThrow(() -> BusinessException.notFound("장바구니 품목을 찾을 수 없습니다."));
		ensureCartAccess(item.getCart(), principal, sessionId);
		cartItemRepository.delete(item);
	}

	@Transactional
	public QuoteResponse toQuote(UserPrincipal principal, String sessionId, CartToQuoteRequest request) {
		Cart cart = getOrCreateCart(principal, sessionId);
		List<QuoteItemRequest> items = cartItemRepository.findByCartIdOrderByIdAsc(cart.getId()).stream()
				.map(item -> new QuoteItemRequest(item.getProduct().getId(), item.getVariant().getId(), item.getQuantity()))
				.toList();
		QuoteResponse response = quoteService.create(principal, new QuoteRequestCreate(
				request.companyName(),
				request.contactName(),
				request.contactPhone(),
				request.siteAddress(),
				request.requestedDeliveryDate(),
				request.deliveryDateUndecided(),
				request.memo(),
				request.privacyAgreed(),
				items
		));
		cartItemRepository.deleteByCartId(cart.getId());
		return response;
	}

	private Cart getOrCreateCart(UserPrincipal principal, String sessionId) {
		if (principal != null) {
			return cartRepository.findFirstByUserIdAndCartType(principal.id(), CartType.QUOTE)
					.orElseGet(() -> {
						Cart cart = new Cart();
						cart.setUser(userService.getUser(principal.id()));
						cart.setCartType(CartType.QUOTE);
						return cartRepository.save(cart);
					});
		}
		String key = sessionId == null ? "anonymous" : sessionId;
		return cartRepository.findFirstBySessionIdAndCartType(key, CartType.QUOTE)
				.orElseGet(() -> {
					Cart cart = new Cart();
					cart.setSessionId(key);
					cart.setCartType(CartType.QUOTE);
					return cartRepository.save(cart);
				});
	}

	private void ensureCartAccess(Cart cart, UserPrincipal principal, String sessionId) {
		if (principal != null && cart.getUser() != null && cart.getUser().getId().equals(principal.id())) {
			return;
		}
		if (principal == null && cart.getSessionId() != null && cart.getSessionId().equals(sessionId)) {
			return;
		}
		throw BusinessException.forbidden("장바구니에 접근할 권한이 없습니다.");
	}

	private void requirePositiveQuantity(BigDecimal quantity) {
		if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
			throw BusinessException.badRequest("수량은 0보다 커야 합니다.");
		}
	}

	private CartResponse toResponse(Cart cart) {
		return new CartResponse(
				cart.getId(),
				cartItemRepository.findByCartIdOrderByIdAsc(cart.getId()).stream().map(this::toItemResponse).toList()
		);
	}

	private CartItemResponse toItemResponse(CartItem item) {
		return new CartItemResponse(
				item.getId(),
				item.getProduct().getId(),
				item.getProduct().getName(),
				item.getProduct().getSummary(),
				representativeImageUrl(item.getProduct()),
				item.getVariant().getId(),
				item.getVariant().getVariantName(),
				item.getQuantity(),
				item.getUnitPriceSnapshot()
		);
	}

	private String representativeImageUrl(Product product) {
		return productMediaRepository.findByProductIdOrderBySortOrderAscIdAsc(product.getId()).stream()
				.filter(media -> media.getType() == ProductMediaType.IMAGE)
				.findFirst()
				.map(ProductMedia::getUrl)
				.orElse(null);
	}
}
