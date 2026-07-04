package com.koreaconcrete.civilshop.user.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.koreaconcrete.civilshop.user.entity.UserRole;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
	@Query("select ur.role.name from UserRole ur where ur.user.id = :userId order by ur.role.name")
	List<String> findRoleNamesByUserId(@Param("userId") Long userId);

	List<UserRole> findByUserId(Long userId);
}
