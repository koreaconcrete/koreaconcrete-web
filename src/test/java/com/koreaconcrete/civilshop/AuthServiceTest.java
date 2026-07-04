package com.koreaconcrete.civilshop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.koreaconcrete.civilshop.auth.dto.AuthDtos.LoginRequest;
import com.koreaconcrete.civilshop.auth.dto.AuthDtos.SignupRequest;
import com.koreaconcrete.civilshop.auth.service.AuthService;
import com.koreaconcrete.civilshop.category.repository.CategoryRepository;
import com.koreaconcrete.civilshop.common.security.UserPrincipal;
import com.koreaconcrete.civilshop.pricing.repository.PriceBookRepository;
import com.koreaconcrete.civilshop.pricing.repository.ProductPriceRepository;
import com.koreaconcrete.civilshop.product.repository.ProductRepository;
import com.koreaconcrete.civilshop.product.repository.ProductVariantRepository;
import com.koreaconcrete.civilshop.quote.dto.QuoteDtos.QuoteItemRequest;
import com.koreaconcrete.civilshop.quote.dto.QuoteDtos.QuoteRequestCreate;
import com.koreaconcrete.civilshop.quote.service.QuoteService;
import com.koreaconcrete.civilshop.user.dto.UserDtos.AdminCreateRequest;
import com.koreaconcrete.civilshop.user.dto.UserDtos.ChangePasswordRequest;
import com.koreaconcrete.civilshop.user.repository.UserRepository;
import com.koreaconcrete.civilshop.user.service.UserService;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class AuthServiceTest {
	@Autowired
	AuthService authService;

	@Autowired
	UserService userService;

	@Autowired
	UserRepository userRepository;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Autowired
	MockMvc mockMvc;

	@Autowired
	QuoteService quoteService;

	@Autowired
	CategoryRepository categoryRepository;

	@Autowired
	ProductRepository productRepository;

	@Autowired
	ProductVariantRepository productVariantRepository;

	@Autowired
	PriceBookRepository priceBookRepository;

	@Autowired
	ProductPriceRepository productPriceRepository;

	@Test
	void signupHashesPassword() {
		String loginId = "buyer-" + System.nanoTime();
		authService.signup(new SignupRequest(loginId, "Password1234!", "홍길동", "01012345678", true, true, false));

		var user = userRepository.findByEmail(loginId).orElseThrow();
		assertThat(user.getPasswordHash()).isNotEqualTo("Password1234!");
		assertThat(passwordEncoder.matches("Password1234!", user.getPasswordHash())).isTrue();
	}

	@Test
	void loginIssuesJwt() {
		String loginId = "login-" + System.nanoTime();
		authService.signup(new SignupRequest(loginId, "Password1234!", "홍길동", "01012345678", true, true, false));

		var response = authService.login(new LoginRequest(loginId, "Password1234!"));

		assertThat(response.accessToken()).isNotBlank();
		assertThat(response.tokenType()).isEqualTo("Bearer");
	}

	@Test
	void loginIdAvailabilityReflectsExistingUser() {
		String loginId = "available-" + System.nanoTime();

		assertThat(authService.isLoginIdAvailable(loginId)).isTrue();
		authService.signup(new SignupRequest(loginId, "Password1234!", "홍길동", "01012345678", true, true, false));

		assertThat(authService.isLoginIdAvailable(loginId)).isFalse();
	}

	@Test
	void userCanChangeOwnPassword() {
		String loginId = "password-" + System.nanoTime();
		var auth = authService.signup(new SignupRequest(loginId, "Password1234!", "홍길동", "01012345678", true, true, false));
		var principal = new UserPrincipal(auth.user().id(), auth.user().email(), auth.user().roles());

		userService.changePassword(principal, new ChangePasswordRequest("Password1234!", "Password5678!"));

		var user = userRepository.findByEmail(loginId).orElseThrow();
		assertThat(passwordEncoder.matches("Password1234!", user.getPasswordHash())).isFalse();
		assertThat(passwordEncoder.matches("Password5678!", user.getPasswordHash())).isTrue();
	}

	@Test
	void createAdminAddsSelectedRole() {
		String loginId = "admin-" + System.nanoTime();

		var created = userService.createAdmin(new AdminCreateRequest(loginId, "Password1234!", "관리자", "01000000000", "ROLE_ADMIN"));

		assertThat(created.email()).isEqualTo(loginId);
		assertThat(created.roles()).containsExactly("ROLE_ADMIN");
	}

	@Test
	void createOperatorAddsOperatorRole() {
		String loginId = "operator-" + System.nanoTime();

		var created = userService.createAdmin(new AdminCreateRequest(loginId, "Password1234!", "운영자", "01000000000", "ROLE_OPERATOR"));

		assertThat(created.email()).isEqualTo(loginId);
		assertThat(created.roles()).containsExactly("ROLE_OPERATOR");
	}

	@Test
	void adminApiRejectsAnonymousUser() throws Exception {
		mockMvc.perform(post("/api/v1/admin/categories")
						.contentType("application/json")
						.content("{\"name\":\"x\",\"slug\":\"x\"}"))
				.andExpect(status().is4xxClientError());
	}

	@Test
	void invalidJwtReturnsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/v1/users/me")
						.header("Authorization", "Bearer invalid-token"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void quoteDetailRequiresAuthentication() throws Exception {
		TestFixtures fixtures = TestFixtures.product(
				categoryRepository,
				productRepository,
				productVariantRepository,
				priceBookRepository,
				productPriceRepository,
				"anonymous-quote-" + System.nanoTime()
		);
		var quote = quoteService.create(null, quoteRequest(fixtures));

		mockMvc.perform(get("/api/v1/quotes/" + quote.id()))
				.andExpect(status().is4xxClientError());
	}

	@Test
	void quoteOwnerCanReadOwnQuote() throws Exception {
		TestFixtures fixtures = TestFixtures.product(
				categoryRepository,
				productRepository,
				productVariantRepository,
				priceBookRepository,
				productPriceRepository,
				"owned-quote-" + System.nanoTime()
		);
		String loginId = "owner-" + System.nanoTime();
		var auth = authService.signup(new SignupRequest(loginId, "Password1234!", "견적소유자", "01099998888", true, true, false));
		var principal = new UserPrincipal(auth.user().id(), auth.user().email(), auth.user().roles());
		var quote = quoteService.create(principal, quoteRequest(fixtures));

		mockMvc.perform(get("/api/v1/quotes/" + quote.id())
						.header("Authorization", "Bearer " + auth.accessToken()))
				.andExpect(status().isOk());
	}

	private QuoteRequestCreate quoteRequest(TestFixtures fixtures) {
		return new QuoteRequestCreate(
				"대한건설",
				"김철수",
				"01012345678",
				"경기도 수원시",
				null,
				"테스트",
				true,
				List.of(new QuoteItemRequest(fixtures.product.getId(), fixtures.variant.getId(), new BigDecimal("12")))
		);
	}
}
