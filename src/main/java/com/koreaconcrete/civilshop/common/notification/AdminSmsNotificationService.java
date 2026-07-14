package com.koreaconcrete.civilshop.common.notification;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koreaconcrete.civilshop.common.domain.UserStatus;
import com.koreaconcrete.civilshop.user.repository.UserRepository;

@Service
public class AdminSmsNotificationService {
	private static final Logger log = LoggerFactory.getLogger(AdminSmsNotificationService.class);
	private static final Set<String> NOTIFICATION_ROLES = Set.of("ROLE_ADMIN", "ROLE_OPERATOR");

	private final UserRepository userRepository;
	private final ObjectMapper objectMapper;
	private final HttpClient httpClient;
	private final boolean enabled;
	private final String accessKey;
	private final String secretKey;
	private final String serviceId;
	private final String from;
	private final String endpoint;

	public AdminSmsNotificationService(
			UserRepository userRepository,
			ObjectMapper objectMapper,
			@Value("${sms.enabled:false}") boolean enabled,
			@Value("${sms.naver.access-key:}") String accessKey,
			@Value("${sms.naver.secret-key:}") String secretKey,
			@Value("${sms.naver.service-id:}") String serviceId,
			@Value("${sms.naver.from:}") String from,
			@Value("${sms.naver.endpoint:https://sens.apigw.ntruss.com}") String endpoint
	) {
		this.userRepository = userRepository;
		this.objectMapper = objectMapper;
		this.enabled = enabled;
		this.accessKey = accessKey;
		this.secretKey = secretKey;
		this.serviceId = serviceId;
		this.from = normalizePhone(from);
		this.endpoint = trimTrailingSlash(endpoint);
		this.httpClient = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(5))
				.build();
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onQuoteCreated(QuoteCreatedEvent event) {
		sendToQuoteAndConsultationAdmins(quoteMessage(event));
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onConsultationCreated(ConsultationCreatedEvent event) {
		sendToQuoteAndConsultationAdmins(consultationMessage(event));
	}

	private void sendToQuoteAndConsultationAdmins(String content) {
		if (!enabled) {
			return;
		}
		if (!isConfigured()) {
			log.warn("SMS notification is enabled but Naver SENS configuration is incomplete.");
			return;
		}
		List<String> recipients = adminRecipientPhones();
		if (recipients.isEmpty()) {
			log.info("SMS notification skipped because no active admin/operator phone numbers were found.");
			return;
		}
		for (String recipient : recipients) {
			send(recipient, content);
		}
	}

	private List<String> adminRecipientPhones() {
		return userRepository.findPhoneNumbersByAnyRoleNameInAndStatus(NOTIFICATION_ROLES, UserStatus.ACTIVE).stream()
				.map(this::normalizePhone)
				.filter((phone) -> !phone.isBlank())
				.collect(Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), List::copyOf));
	}

	private void send(String recipient, String content) {
		try {
			String timestamp = String.valueOf(System.currentTimeMillis());
			String path = "/sms/v2/services/" + serviceId + "/messages";
			String requestBody = objectMapper.writeValueAsString(new SensMessageRequest(
					"LMS",
					"COMM",
					"82",
					from,
					content,
					List.of(new SensMessage(recipient, content))
			));
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(endpoint + path))
					.timeout(Duration.ofSeconds(10))
					.header("Content-Type", "application/json; charset=utf-8")
					.header("x-ncp-apigw-timestamp", timestamp)
					.header("x-ncp-iam-access-key", accessKey)
					.header("x-ncp-apigw-signature-v2", signature(timestamp, path))
					.POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
					.build();
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				log.warn("SMS notification failed. status={}, body={}", response.statusCode(), response.body());
			}
		} catch (InterruptedException error) {
			Thread.currentThread().interrupt();
			log.warn("SMS notification interrupted.", error);
		} catch (Exception error) {
			log.warn("SMS notification failed.", error);
		}
	}

	private String signature(String timestamp, String path) {
		try {
			String message = "POST " + path + "\n" + timestamp + "\n" + accessKey;
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			return Base64.getEncoder().encodeToString(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
		} catch (Exception error) {
			throw new IllegalStateException("Failed to create Naver SENS signature.", error);
		}
	}

	private String quoteMessage(QuoteCreatedEvent event) {
		return String.join("\n",
				"[한국콘크리트] 신규 견적요청",
				"번호: " + blankToDash(event.requestNo()),
				"회사: " + blankToDash(event.companyName()),
				"상품: " + blankToDash(event.itemSummary()),
				"총액: " + blankToDash(event.totalAmountLabel()),
				"담당: " + blankToDash(event.contactName()) + " / " + blankToDash(event.contactPhone()),
				"납기: " + deliveryDateLabel(event.requestedDeliveryDate())
		);
	}

	private String consultationMessage(ConsultationCreatedEvent event) {
		String product = event.productName() == null || event.productName().isBlank() ? "일반 상담" : event.productName().trim();
		return String.join("\n",
				"[한국콘크리트] 신규 상담요청",
				"구분: " + consultationTypeLabel(event.type()),
				"상품: " + product,
				"담당: " + blankToDash(event.contactName()) + " / " + blankToDash(event.contactPhone())
		);
	}

	private boolean isConfigured() {
		return !accessKey.isBlank()
				&& !secretKey.isBlank()
				&& !serviceId.isBlank()
				&& !from.isBlank()
				&& !endpoint.isBlank();
	}

	private String normalizePhone(String value) {
		if (value == null) {
			return "";
		}
		String digits = value.replaceAll("\\D", "");
		if (digits.startsWith("82") && digits.length() >= 11) {
			return "0" + digits.substring(2);
		}
		return digits;
	}

	private String trimTrailingSlash(String value) {
		if (value == null || value.isBlank()) {
			return "";
		}
		return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
	}

	private String blankToDash(String value) {
		return value == null || value.isBlank() ? "-" : value.trim();
	}

	private String deliveryDateLabel(LocalDate value) {
		return value == null ? "미정" : value.toString();
	}

	private String consultationTypeLabel(com.koreaconcrete.civilshop.common.domain.ConsultationType type) {
		if (type == null) {
			return "-";
		}
		return switch (type) {
			case PHONE -> "전화 상담";
			case SMS -> "문자 상담";
			case KAKAO -> "카카오 상담";
			case EMAIL -> "이메일 상담";
			case SITE_QNA -> "사이트 문의";
		};
	}

	private record SensMessageRequest(
			String type,
			String contentType,
			String countryCode,
			String from,
			String content,
			List<SensMessage> messages
	) {
	}

	private record SensMessage(
			String to,
			String content
	) {
	}
}
