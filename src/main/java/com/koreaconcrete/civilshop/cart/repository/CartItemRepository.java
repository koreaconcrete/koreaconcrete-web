package com.koreaconcrete.civilshop.cart.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koreaconcrete.civilshop.cart.entity.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
	List<CartItem> findByCartIdOrderByIdAsc(Long cartId);

	Optional<CartItem> findFirstByCartIdAndVariantId(Long cartId, Long variantId);

	boolean existsByProductId(Long productId);

	void deleteByProductId(Long productId);

	void deleteByVariantId(Long variantId);

	void deleteByCartId(Long cartId);
}
