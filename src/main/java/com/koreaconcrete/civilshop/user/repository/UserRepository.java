package com.koreaconcrete.civilshop.user.repository;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.koreaconcrete.civilshop.common.domain.UserStatus;
import com.koreaconcrete.civilshop.user.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);

	Page<User> findByStatusNot(UserStatus status, Pageable pageable);

	@Query("""
			select distinct u
			from User u
			join UserRole ur on ur.user = u
			join ur.role r
			where r.name in :roles
			  and u.status <> :deletedStatus
			""")
	Page<User> findByAnyRoleNameInAndStatusNot(
			@Param("roles") Collection<String> roles,
			@Param("deletedStatus") UserStatus deletedStatus,
			Pageable pageable
	);

	@Query("""
			select u
			from User u
			where u.status <> :deletedStatus
			  and not exists (
				select 1
				from UserRole ur
				where ur.user = u
				  and ur.role.name in :roles
			)
			""")
	Page<User> findByNoRoleNameInAndStatusNot(
			@Param("roles") Collection<String> roles,
			@Param("deletedStatus") UserStatus deletedStatus,
			Pageable pageable
	);
}
