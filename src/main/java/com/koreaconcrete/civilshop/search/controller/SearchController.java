package com.koreaconcrete.civilshop.search.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.koreaconcrete.civilshop.common.security.UserPrincipal;
import com.koreaconcrete.civilshop.search.dto.SearchDtos.PopularKeyword;
import com.koreaconcrete.civilshop.search.dto.SearchDtos.RecentKeyword;
import com.koreaconcrete.civilshop.search.service.SearchService;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {
	private final SearchService searchService;

	public SearchController(SearchService searchService) {
		this.searchService = searchService;
	}

	@Operation(summary = "인기 검색어")
	@GetMapping("/popular")
	public List<PopularKeyword> popular() {
		return searchService.popular();
	}

	@Operation(summary = "최근 검색어")
	@GetMapping("/recent")
	public List<RecentKeyword> recent(
			@AuthenticationPrincipal UserPrincipal principal,
			@RequestHeader(value = "X-Session-Id", required = false) String sessionId
	) {
		return searchService.recent(principal == null ? null : principal.id(), sessionId);
	}
}
