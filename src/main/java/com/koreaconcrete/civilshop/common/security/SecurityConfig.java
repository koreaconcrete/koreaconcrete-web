package com.koreaconcrete.civilshop.common.security;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
				.csrf(csrf -> csrf.disable())
				.cors(cors -> {
				})
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/auth/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/auth/login-id-available").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/categories/**", "/api/v1/products/**", "/api/v1/search/**").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/freight/estimate", "/api/v1/consultations/**").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/quotes").authenticated()
						.requestMatchers("/api/v1/users/me/**", "/api/v1/users/me", "/api/v1/quotes/me", "/api/v1/cart/**").authenticated()
						.requestMatchers(HttpMethod.GET, "/api/v1/quotes/*").authenticated()
						.requestMatchers(HttpMethod.POST, "/api/v1/quotes/*/approve", "/api/v1/quotes/*/cancel").authenticated()
						.requestMatchers("/api/v1/admin/categories/**", "/api/v1/admin/products/**",
								"/api/v1/admin/product-variants/**", "/api/v1/admin/price-books/**",
								"/api/v1/admin/product-prices/**", "/api/v1/admin/loading-rules/**",
								"/api/v1/admin/freight-rate-rules/**")
						.hasAnyAuthority("ROLE_ADMIN", "ROLE_PRODUCT_MANAGER")
						.requestMatchers("/api/v1/admin/quotes/**", "/api/v1/admin/consultations/**")
						.hasAnyAuthority("ROLE_ADMIN", "ROLE_OPERATOR")
						.requestMatchers("/api/v1/admin/**").hasAuthority("ROLE_ADMIN")
						.anyRequest().permitAll()
				)
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
				.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource(
			@Value("${app.cors.allowed-origins:http://localhost:8080}") String allowedOrigins
	) {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList());
		configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Session-Id"));
		configuration.setExposedHeaders(List.of("Authorization"));
		configuration.setAllowCredentials(false);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}
