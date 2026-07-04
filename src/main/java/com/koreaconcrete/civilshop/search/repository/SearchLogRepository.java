package com.koreaconcrete.civilshop.search.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.koreaconcrete.civilshop.search.entity.SearchLog;

public interface SearchLogRepository extends JpaRepository<SearchLog, Long> {
	@Query("""
			select s.keyword as keyword, count(s) as count
			from SearchLog s
			where s.createdAt >= :from
			group by s.keyword
			order by count(s) desc, max(s.createdAt) desc
			""")
	List<KeywordCount> popular(@Param("from") LocalDateTime from, Pageable pageable);

	List<SearchLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

	List<SearchLog> findBySessionIdOrderByCreatedAtDesc(String sessionId, Pageable pageable);

	interface KeywordCount {
		String getKeyword();

		Long getCount();
	}
}
