package com.koreaconcrete.civilshop.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koreaconcrete.civilshop.user.entity.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {
	Optional<Role> findByName(String name);
}
