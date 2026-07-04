package com.koreaconcrete.civilshop.common.api;

import java.util.List;

import org.springframework.data.domain.Page;

public record PageResponse<T>(
		List<T> items,
		int page,
		int size,
		long total,
		boolean hasNext
) {
	public static <T> PageResponse<T> of(Page<?> page, List<T> items) {
		return new PageResponse<>(
				items,
				page.getNumber() + 1,
				page.getSize(),
				page.getTotalElements(),
				page.hasNext()
		);
	}

	public static <T> PageResponse<T> of(List<T> items) {
		return new PageResponse<>(items, 1, items.size(), items.size(), false);
	}
}
