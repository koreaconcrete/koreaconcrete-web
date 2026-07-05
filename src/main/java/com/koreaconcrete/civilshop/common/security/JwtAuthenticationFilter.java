package com.koreaconcrete.civilshop.common.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.koreaconcrete.civilshop.common.domain.UserStatus;
import com.koreaconcrete.civilshop.common.exception.BusinessException;
import com.koreaconcrete.civilshop.user.repository.UserRepository;
import com.koreaconcrete.civilshop.user.repository.UserRoleRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
	private final JwtTokenProvider jwtTokenProvider;
	private final UserRepository userRepository;
	private final UserRoleRepository userRoleRepository;

	public JwtAuthenticationFilter(
			JwtTokenProvider jwtTokenProvider,
			UserRepository userRepository,
			UserRoleRepository userRoleRepository
	) {
		this.jwtTokenProvider = jwtTokenProvider;
		this.userRepository = userRepository;
		this.userRoleRepository = userRoleRepository;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		try {
			String header = request.getHeader("Authorization");
			if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
				UserPrincipal tokenPrincipal = jwtTokenProvider.parse(header.substring(7));
				UserPrincipal principal = currentPrincipal(tokenPrincipal);
				UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
						principal,
						null,
						principal.roles().stream().map(SimpleGrantedAuthority::new).toList()
				);
				SecurityContextHolder.getContext().setAuthentication(authentication);
			}
			filterChain.doFilter(request, response);
		} catch (BusinessException exception) {
			SecurityContextHolder.clearContext();
			response.setStatus(exception.getStatus().value());
			response.setContentType("application/json;charset=UTF-8");
			response.getWriter().write("{\"code\":\"" + exception.getCode() + "\",\"message\":\"" + exception.getMessage() + "\",\"details\":{}}");
		}
	}

	private UserPrincipal currentPrincipal(UserPrincipal tokenPrincipal) {
		var user = userRepository.findById(tokenPrincipal.id())
				.orElseThrow(() -> BusinessException.unauthorized("유효하지 않은 인증 정보입니다."));
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw BusinessException.unauthorized("유효하지 않은 인증 정보입니다.");
		}
		var roles = userRoleRepository.findRoleNamesByUserId(user.getId());
		if (roles.isEmpty()) {
			throw BusinessException.unauthorized("유효하지 않은 인증 정보입니다.");
		}
		return new UserPrincipal(user.getId(), user.getEmail(), roles);
	}
}
