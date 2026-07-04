package com.koreaconcrete.civilshop.auth.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koreaconcrete.civilshop.auth.dto.AuthDtos.AuthResponse;
import com.koreaconcrete.civilshop.auth.dto.AuthDtos.AuthUser;
import com.koreaconcrete.civilshop.auth.dto.AuthDtos.LoginRequest;
import com.koreaconcrete.civilshop.auth.dto.AuthDtos.SignupRequest;
import com.koreaconcrete.civilshop.common.exception.BusinessException;
import com.koreaconcrete.civilshop.common.security.JwtTokenProvider;
import com.koreaconcrete.civilshop.user.entity.Role;
import com.koreaconcrete.civilshop.user.entity.User;
import com.koreaconcrete.civilshop.user.entity.UserRole;
import com.koreaconcrete.civilshop.user.repository.RoleRepository;
import com.koreaconcrete.civilshop.user.repository.UserRepository;
import com.koreaconcrete.civilshop.user.repository.UserRoleRepository;

@Service
@Transactional(readOnly = true)
public class AuthService {
	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final UserRoleRepository userRoleRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;

	public AuthService(
			UserRepository userRepository,
			RoleRepository roleRepository,
			UserRoleRepository userRoleRepository,
			PasswordEncoder passwordEncoder,
			JwtTokenProvider jwtTokenProvider
	) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.userRoleRepository = userRoleRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtTokenProvider = jwtTokenProvider;
	}

	@Transactional
	public AuthResponse signup(SignupRequest request) {
		if (!Boolean.TRUE.equals(request.privacyAgreed()) || !Boolean.TRUE.equals(request.termsAgreed())) {
			throw BusinessException.badRequest("약관과 개인정보 수집에 동의해주세요.");
		}
		String loginId = request.email().trim();
		if (userRepository.existsByEmail(loginId)) {
			throw new BusinessException("LOGIN_ID_DUPLICATED", "이미 사용 중인 아이디입니다.", HttpStatus.CONFLICT);
		}
		User user = userRepository.save(new User(
				loginId,
				passwordEncoder.encode(request.password()),
				request.name(),
				request.phone()
		));
		Role role = roleRepository.findByName("ROLE_MEMBER").orElseGet(() -> roleRepository.save(new Role("ROLE_MEMBER")));
		userRoleRepository.save(new UserRole(user, role));
		return issue(user);
	}

	@Transactional
	public AuthResponse login(LoginRequest request) {
		User user = userRepository.findByEmail(request.email().trim())
				.orElseThrow(() -> BusinessException.unauthorized("아이디 또는 비밀번호를 확인해주세요."));
		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw BusinessException.unauthorized("아이디 또는 비밀번호를 확인해주세요.");
		}
		user.setLastLoginAt(LocalDateTime.now());
		return issue(user);
	}

	public boolean isLoginIdAvailable(String loginId) {
		if (loginId == null || loginId.isBlank()) {
			return false;
		}
		return !userRepository.existsByEmail(loginId.trim());
	}

	public AuthResponse issue(User user) {
		List<String> roles = userRoleRepository.findRoleNamesByUserId(user.getId());
		String token = jwtTokenProvider.createToken(user.getId(), user.getEmail(), roles);
		return AuthResponse.bearer(token, new AuthUser(user.getId(), user.getEmail(), user.getName(), roles));
	}
}
