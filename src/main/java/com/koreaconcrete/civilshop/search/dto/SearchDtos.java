package com.koreaconcrete.civilshop.search.dto;

import java.time.LocalDateTime;

public final class SearchDtos {
	private SearchDtos() {
	}

	public record PopularKeyword(String keyword, Long count) {
	}

	public record RecentKeyword(String keyword, Integer resultCount, LocalDateTime createdAt) {
	}
}
