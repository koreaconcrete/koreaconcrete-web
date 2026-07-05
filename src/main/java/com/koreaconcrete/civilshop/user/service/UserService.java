package com.koreaconcrete.civilshop.user.service;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koreaconcrete.civilshop.common.api.PageResponse;
import com.koreaconcrete.civilshop.common.domain.UserStatus;
import com.koreaconcrete.civilshop.common.exception.BusinessException;
import com.koreaconcrete.civilshop.common.security.UserPrincipal;
import com.koreaconcrete.civilshop.user.dto.UserDtos.AdminCreateRequest;
import com.koreaconcrete.civilshop.user.dto.UserDtos.AdminUserItem;
import com.koreaconcrete.civilshop.user.dto.UserDtos.ChangePasswordRequest;
import com.koreaconcrete.civilshop.user.dto.UserDtos.UpdateMeRequest;
import com.koreaconcrete.civilshop.user.dto.UserDtos.UserSummary;
import com.koreaconcrete.civilshop.user.entity.Role;
import com.koreaconcrete.civilshop.user.entity.User;
import com.koreaconcrete.civilshop.user.entity.UserRole;
import com.koreaconcrete.civilshop.user.repository.RoleRepository;
import com.koreaconcrete.civilshop.user.repository.UserRepository;
import com.koreaconcrete.civilshop.user.repository.UserRoleRepository;

@Service
@Transactional(readOnly = true)
public class UserService {
	private static final Set<String> CONSOLE_ROLES = Set.of("ROLE_ADMIN", "ROLE_OPERATOR", "ROLE_PRODUCT_MANAGER");

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final UserRoleRepository userRoleRepository;
	private final PasswordEncoder passwordEncoder;

	public UserService(
			UserRepository userRepository,
			RoleRepository roleRepository,
			UserRoleRepository userRoleRepository,
			PasswordEncoder passwordEncoder
	) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.userRoleRepository = userRoleRepository;
		this.passwordEncoder = passwordEncoder;
	}

	public UserSummary me(UserPrincipal principal) {
		return toSummary(getUser(principal.id()));
	}

	@Transactional
	public UserSummary updateMe(UserPrincipal principal, UpdateMeRequest request) {
		User user = getUser(principal.id());
		user.setName(request.name().trim());
		user.setPhone(normalizeBlank(request.phone()));
		return toSummary(user);
	}

	@Transactional
	public UserSummary changePassword(UserPrincipal principal, ChangePasswordRequest request) {
		User user = getUser(principal.id());
		if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
			throw BusinessException.badRequest("현재 비밀번호가 일치하지 않습니다.");
		}
		user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
		return toSummary(user);
	}

	@Transactional
	public AdminUserItem createAdmin(AdminCreateRequest request) {
		String loginId = request.loginId().trim();
		if (userRepository.existsByEmail(loginId)) {
			throw new BusinessException("LOGIN_ID_DUPLICATED", "이미 사용 중인 아이디입니다.", HttpStatus.CONFLICT);
		}
		String roleName = normalizeConsoleRole(request.role());
		User user = userRepository.save(new User(
				loginId,
				passwordEncoder.encode(request.password()),
				request.name().trim(),
				normalizeBlank(request.phone())
		));
		Role role = roleRepository.findByName(roleName).orElseGet(() -> roleRepository.save(new Role(roleName)));
		userRoleRepository.save(new UserRole(user, role));
		return toAdminItem(user);
	}

	public PageResponse<AdminUserItem> adminUsers(int page, int size) {
		return adminUsers(null, page, size);
	}

	public PageResponse<AdminUserItem> adminUsers(String accountType, int page, int size) {
		PageRequest pageRequest = PageRequest.of(Math.max(page - 1, 0), size, Sort.by(Sort.Direction.DESC, "id"));
		Page<User> users = switch (normalizeAccountType(accountType)) {
			case "ADMIN" -> userRepository.findByAnyRoleNameInAndStatusNot(CONSOLE_ROLES, UserStatus.DELETED, pageRequest);
			case "MEMBER" -> userRepository.findByNoRoleNameInAndStatusNot(CONSOLE_ROLES, UserStatus.DELETED, pageRequest);
			default -> userRepository.findByStatusNot(UserStatus.DELETED, pageRequest);
		};
		List<AdminUserItem> items = users.stream()
				.map(this::toAdminItem)
				.toList();
		return PageResponse.of(users, items);
	}

	@Transactional
	public void deleteAdmin(Long id) {
		User user = getUser(id);
		List<String> roles = userRoleRepository.findRoleNamesByUserId(user.getId());
		if (roles.stream().noneMatch(CONSOLE_ROLES::contains)) {
			throw BusinessException.badRequest("관리자 계정만 삭제할 수 있습니다.");
		}
		user.setStatus(UserStatus.DELETED);
	}

	public User getUser(Long id) {
		return userRepository.findById(id).orElseThrow(() -> BusinessException.notFound("회원을 찾을 수 없습니다."));
	}

	public UserSummary toSummary(User user) {
		return new UserSummary(
				user.getId(),
				user.getEmail(),
				user.getName(),
				user.getPhone(),
				user.getStatus(),
				userRoleRepository.findRoleNamesByUserId(user.getId()),
				user.getCreatedAt(),
				user.getLastLoginAt()
		);
	}

	private AdminUserItem toAdminItem(User user) {
		return new AdminUserItem(
				user.getId(),
				user.getEmail(),
				user.getName(),
				user.getPhone(),
				user.getStatus(),
				userRoleRepository.findRoleNamesByUserId(user.getId()),
				user.getCreatedAt(),
				user.getLastLoginAt()
		);
	}

	private String normalizeBlank(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}

	private String normalizeAccountType(String value) {
		return value == null ? "" : value.trim().toUpperCase();
	}

	private String normalizeConsoleRole(String value) {
		String roleName = value == null ? "" : value.trim();
		if (!CONSOLE_ROLES.contains(roleName)) {
			throw BusinessException.badRequest("관리자 계정 권한을 확인해주세요.");
		}
		return roleName;
	}
}
