package com.koreaconcrete.civilshop.pricing.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koreaconcrete.civilshop.pricing.entity.PriceBook;

public interface PriceBookRepository extends JpaRepository<PriceBook, Long> {
	Optional<PriceBook> findFirstByDefaultBookTrueOrderByIdDesc();
}
