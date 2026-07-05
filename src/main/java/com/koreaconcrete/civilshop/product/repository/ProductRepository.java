package com.koreaconcrete.civilshop.product.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.koreaconcrete.civilshop.common.domain.ProductStatus;
import com.koreaconcrete.civilshop.product.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {
	Optional<Product> findBySku(String sku);

	boolean existsByCategoryId(Long categoryId);

	List<Product> findByStatusOrderByIdDesc(ProductStatus status, Pageable pageable);

	List<Product> findByStatusInOrderByIdDesc(Collection<ProductStatus> statuses, Pageable pageable);

	@Query("""
			select p
			from Product p
			where p.category.id = :categoryId
			  and p.status <> :deletedStatus
			order by p.sortOrder asc, p.id desc
			""")
	List<Product> findMovableSiblings(
			@Param("categoryId") Long categoryId,
			@Param("deletedStatus") ProductStatus deletedStatus
	);

	@Query("""
			select distinct p
			from Product p
			left join p.category c
			left join c.parent cp
			left join cp.parent cpp
			left join ProductVariant v on v.product = p
			left join ProductSpec s on s.product = p
			where (:categoryId is null or c.id = :categoryId or cp.id = :categoryId or cpp.id = :categoryId)
			  and (:status is null or p.status = :status)
			  and (:includeDeleted = true or p.status <> :deletedStatus)
			  and (:keywordPattern is null
			    or lower(p.name) like :keywordPattern
			    or lower(coalesce(p.summary, '')) like :keywordPattern
			    or lower(coalesce(p.searchKeywords, '')) like :keywordPattern
			    or lower(c.name) like :keywordPattern
			    or lower(coalesce(v.variantName, '')) like :keywordPattern
			    or lower(coalesce(s.specKey, '')) like :keywordPattern
			    or lower(coalesce(s.specValue, '')) like :keywordPattern)
			""")
	Page<Product> search(
			@Param("keywordPattern") String keywordPattern,
			@Param("categoryId") Long categoryId,
			@Param("status") ProductStatus status,
			@Param("includeDeleted") boolean includeDeleted,
			@Param("deletedStatus") ProductStatus deletedStatus,
			Pageable pageable
	);

	@Query("""
			select distinct p
			from Product p
			left join p.category c
			left join c.parent cp
			left join cp.parent cpp
			left join ProductVariant v on v.product = p
			left join ProductSpec s on s.product = p
			where (:categoryId is null or c.id = :categoryId or cp.id = :categoryId or cpp.id = :categoryId)
			  and p.status in :statuses
			  and (:keywordPattern is null
			    or lower(p.name) like :keywordPattern
			    or lower(coalesce(p.summary, '')) like :keywordPattern
			    or lower(coalesce(p.searchKeywords, '')) like :keywordPattern
			    or lower(c.name) like :keywordPattern
			    or lower(coalesce(v.variantName, '')) like :keywordPattern
			    or lower(coalesce(s.specKey, '')) like :keywordPattern
			    or lower(coalesce(s.specValue, '')) like :keywordPattern)
			""")
	Page<Product> searchByStatuses(
			@Param("keywordPattern") String keywordPattern,
			@Param("categoryId") Long categoryId,
			@Param("statuses") Collection<ProductStatus> statuses,
			Pageable pageable
	);

	@Query("""
			select p
			from Product p
			left join p.category c
			left join c.parent cp
			left join cp.parent cpp
			where (:categoryId is null or c.id = :categoryId or cp.id = :categoryId or cpp.id = :categoryId)
			  and (:status is null or p.status = :status)
			  and (:includeDeleted = true or p.status <> :deletedStatus)
			  and (:keywordPattern is null
			    or lower(p.name) like :keywordPattern
			    or lower(coalesce(p.summary, '')) like :keywordPattern
			    or lower(coalesce(p.searchKeywords, '')) like :keywordPattern
			    or lower(c.name) like :keywordPattern
			    or exists (
			      select 1
			      from ProductVariant v
			      where v.product = p
			        and lower(coalesce(v.variantName, '')) like :keywordPattern
			    )
			    or exists (
			      select 1
			      from ProductSpec s
			      where s.product = p
			        and (
			          lower(coalesce(s.specKey, '')) like :keywordPattern
			          or lower(coalesce(s.specValue, '')) like :keywordPattern
			        )
			    ))
			order by coalesce(cp.sortOrder, c.sortOrder) asc, c.sortOrder asc, p.sortOrder asc, p.id desc
			""")
	Page<Product> searchByDisplayOrder(
			@Param("keywordPattern") String keywordPattern,
			@Param("categoryId") Long categoryId,
			@Param("status") ProductStatus status,
			@Param("includeDeleted") boolean includeDeleted,
			@Param("deletedStatus") ProductStatus deletedStatus,
			Pageable pageable
	);

	@Query("""
			select p
			from Product p
			left join p.category c
			left join c.parent cp
			left join cp.parent cpp
			where (:categoryId is null or c.id = :categoryId or cp.id = :categoryId or cpp.id = :categoryId)
			  and p.status in :statuses
			  and (:keywordPattern is null
			    or lower(p.name) like :keywordPattern
			    or lower(coalesce(p.summary, '')) like :keywordPattern
			    or lower(coalesce(p.searchKeywords, '')) like :keywordPattern
			    or lower(c.name) like :keywordPattern
			    or exists (
			      select 1
			      from ProductVariant v
			      where v.product = p
			        and lower(coalesce(v.variantName, '')) like :keywordPattern
			    )
			    or exists (
			      select 1
			      from ProductSpec s
			      where s.product = p
			        and (
			          lower(coalesce(s.specKey, '')) like :keywordPattern
			          or lower(coalesce(s.specValue, '')) like :keywordPattern
			        )
			    ))
			order by coalesce(cp.sortOrder, c.sortOrder) asc, c.sortOrder asc, p.sortOrder asc, p.id desc
			""")
	Page<Product> searchByDisplayOrderAndStatuses(
			@Param("keywordPattern") String keywordPattern,
			@Param("categoryId") Long categoryId,
			@Param("statuses") Collection<ProductStatus> statuses,
			Pageable pageable
	);

	@Query("""
			select p
			from Product p, SearchLog l
			where p.status = :status
			  and l.createdAt >= :from
			  and (
			    lower(p.name) like lower(concat('%', l.keyword, '%'))
			    or lower(coalesce(p.summary, '')) like lower(concat('%', l.keyword, '%'))
			    or lower(coalesce(p.searchKeywords, '')) like lower(concat('%', l.keyword, '%'))
			    or lower(l.keyword) like lower(concat('%', p.name, '%'))
			  )
			group by p
			order by count(l) desc, max(l.createdAt) desc, p.id desc
			""")
	List<Product> popularBySearchLogs(
			@Param("from") LocalDateTime from,
			@Param("status") ProductStatus status,
			Pageable pageable
	);

	@Query("""
			select p
			from Product p, SearchLog l
			where p.status in :statuses
			  and l.createdAt >= :from
			  and (
			    lower(p.name) like lower(concat('%', l.keyword, '%'))
			    or lower(coalesce(p.summary, '')) like lower(concat('%', l.keyword, '%'))
			    or lower(coalesce(p.searchKeywords, '')) like lower(concat('%', l.keyword, '%'))
			    or lower(l.keyword) like lower(concat('%', p.name, '%'))
			  )
			group by p
			order by count(l) desc, max(l.createdAt) desc, p.id desc
			""")
	List<Product> popularBySearchLogsAndStatuses(
			@Param("from") LocalDateTime from,
			@Param("statuses") Collection<ProductStatus> statuses,
			Pageable pageable
	);
}
