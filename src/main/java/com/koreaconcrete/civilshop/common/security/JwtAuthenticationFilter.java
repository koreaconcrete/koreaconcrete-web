package com.koreaconcrete.civilshop.common.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.koreaconcrete.civilshop.common.exception.BusinessException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
	private final JwtTokenProvider jwtTokenProvider;

	public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
		this.jwtTokenProvider = jwtTokenProvider;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		try {
			String header = request.getHeader("Authorization");
			if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
				UserPrincipal principal = jwtTokenProvider.parse(header.substring(7));
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
}
