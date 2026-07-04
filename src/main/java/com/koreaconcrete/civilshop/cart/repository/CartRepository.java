package com.koreaconcrete.civilshop.cart.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koreaconcrete.civilshop.cart.entity.Cart;
import com.koreaconcrete.civilshop.common.domain.CartType;

public interface CartRepository extends JpaRepository<Cart, Long> {
	Optional<Cart> findFirstByUserIdAndCartType(Long userId, CartType cartType);

	Optional<Cart> findFirstBySessionIdAndCartType(String sessionId, CartType cartType);
}
