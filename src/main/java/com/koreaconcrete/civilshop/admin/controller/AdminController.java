package com.koreaconcrete.civilshop.admin.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.koreaconcrete.civilshop.admin.dto.AdminDtos.AuditLogItem;
import com.koreaconcrete.civilshop.admin.dto.AdminDtos.DashboardResponse;
import com.koreaconcrete.civilshop.admin.dto.AdminDtos.SearchLogItem;
import com.koreaconcrete.civilshop.admin.service.AdminService;
import com.koreaconcrete.civilshop.common.api.PageResponse;
import com.koreaconcrete.civilshop.user.dto.UserDtos.AdminCreateRequest;
import com.koreaconcrete.civilshop.user.dto.UserDtos.AdminUserItem;
import com.koreaconcrete.civilshop.user.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
	private final AdminService adminService;
	private final UserService userService;

	public AdminController(AdminService adminService, UserService userService) {
		this.adminService = adminService;
		this.userService = userService;
	}

	@Operation(summary = "관리자 대시보드")
	@GetMapping("/dashboard")
	public DashboardResponse dashboard() {
		return adminService.dashboard();
	}

	@Operation(summary = "관리자 회원 목록")
	@GetMapping("/users")
	public PageResponse<AdminUserItem> users(
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "20") int size
	) {
		return userService.adminUsers(page, size);
	}

	@Operation(summary = "관리자 계정 생성")
	@PostMapping("/users/admins")
	public AdminUserItem createAdmin(@Valid @RequestBody AdminCreateRequest request) {
		return userService.createAdmin(request);
	}

	@Operation(summary = "관리자 검색 로그")
	@GetMapping("/search-logs")
	public PageResponse<SearchLogItem> searchLogs(
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "20") int size
	) {
		return adminService.searchLogs(page, size);
	}

	@Operation(summary = "관리자 감사 로그")
	@GetMapping("/audit-logs")
	public PageResponse<AuditLogItem> auditLogs(
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "20") int size
	) {
		return adminService.auditLogs(page, size);
	}
}
