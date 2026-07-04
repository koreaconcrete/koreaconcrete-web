package com.koreaconcrete.civilshop.search.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koreaconcrete.civilshop.search.dto.SearchDtos.PopularKeyword;
import com.koreaconcrete.civilshop.search.dto.SearchDtos.RecentKeyword;
import com.koreaconcrete.civilshop.search.entity.SearchLog;
import com.koreaconcrete.civilshop.search.repository.SearchLogRepository;
import com.koreaconcrete.civilshop.user.entity.User;

@Service
@Transactional(readOnly = true)
public class SearchService {
	private final SearchLogRepository searchLogRepository;

	public SearchService(SearchLogRepository searchLogRepository) {
		this.searchLogRepository = searchLogRepository;
	}

	@Transactional
	public void log(User user, String sessionId, String keyword, int resultCount) {
		SearchLog log = new SearchLog();
		log.setUser(user);
		log.setSessionId(sessionId == null ? "anonymous" : sessionId);
		log.setKeyword(keyword);
		log.setResultCount(resultCount);
		searchLogRepository.save(log);
	}

	public List<PopularKeyword> popular() {
		return searchLogRepository.popular(LocalDateTime.now().minusDays(7), PageRequest.of(0, 10)).stream()
				.map(row -> new PopularKeyword(row.getKeyword(), row.getCount()))
				.toList();
	}

	public List<RecentKeyword> recent(Long userId, String sessionId) {
		List<SearchLog> logs = userId == null
				? searchLogRepository.findBySessionIdOrderByCreatedAtDesc(sessionId == null ? "anonymous" : sessionId, PageRequest.of(0, 10))
				: searchLogRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 10));
		return logs.stream()
				.map(log -> new RecentKeyword(log.getKeyword(), log.getResultCount(), log.getCreatedAt()))
				.toList();
	}
}
