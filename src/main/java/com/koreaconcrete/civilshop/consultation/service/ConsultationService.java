package com.koreaconcrete.civilshop.consultation.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koreaconcrete.civilshop.common.api.PageResponse;
import com.koreaconcrete.civilshop.common.audit.AuditService;
import com.koreaconcrete.civilshop.common.domain.ConsultationStatus;
import com.koreaconcrete.civilshop.common.domain.ConsultationType;
import com.koreaconcrete.civilshop.common.domain.ProductStatus;
import com.koreaconcrete.civilshop.common.exception.BusinessException;
import com.koreaconcrete.civilshop.common.security.UserPrincipal;
import com.koreaconcrete.civilshop.consultation.dto.ConsultationDtos.ConsultationReplyRequest;
import com.koreaconcrete.civilshop.consultation.dto.ConsultationDtos.ConsultationRequest;
import com.koreaconcrete.civilshop.consultation.dto.ConsultationDtos.ConsultationResponse;
import com.koreaconcrete.civilshop.consultation.dto.ConsultationDtos.ConsultationStatusRequest;
import com.koreaconcrete.civilshop.consultation.entity.Consultation;
import com.koreaconcrete.civilshop.consultation.repository.ConsultationRepository;
import com.koreaconcrete.civilshop.product.entity.Product;
import com.koreaconcrete.civilshop.product.entity.ProductVariant;
import com.koreaconcrete.civilshop.product.service.ProductService;
import com.koreaconcrete.civilshop.user.entity.User;
import com.koreaconcrete.civilshop.user.service.UserService;

@Service
@Transactional(readOnly = true)
public class ConsultationService {
	private final ConsultationRepository consultationRepository;
	private final ProductService productService;
	private final UserService userService;
	private final AuditService auditService;

	public ConsultationService(
			ConsultationRepository consultationRepository,
			ProductService productService,
			UserService userService,
			AuditService auditService
	) {
		this.consultationRepository = consultationRepository;
		this.productService = productService;
		this.userService = userService;
		this.auditService = auditService;
	}

	@Transactional
	public ConsultationResponse create(UserPrincipal principal, ConsultationRequest request) {
		if (principal == null && !Boolean.TRUE.equals(request.privacyAgreed())) {
			throw BusinessException.badRequest("비회원 상담요청은 개인정보 수집 동의가 필요합니다.");
		}
		Consultation consultation = new Consultation();
		consultation.setType(request.type());
		consultation.setUser(principal == null ? null : userService.getUser(principal.id()));
		Product product = request.productId() == null ? null : productService.getProduct(request.productId());
		ProductVariant variant = request.variantId() == null ? null : productService.getVariant(request.variantId());
		if (product == null && variant != null) {
			product = variant.getProduct();
		}
		if (product != null && variant != null) {
			productService.ensureRequestable(product, variant);
		}
		consultation.setProduct(product);
		consultation.setVariant(variant);
		consultation.setContactName(request.contactName());
		consultation.setContactPhone(request.contactPhone());
		consultation.setMessage(request.message());
		return toResponse(consultationRepository.save(consultation));
	}

	public PageResponse<ConsultationResponse> adminList(ConsultationStatus status, int page, int size) {
		PageRequest pageRequest = PageRequest.of(Math.max(page - 1, 0), size);
		Page<Consultation> consultations = status == null
				? consultationRepository.findAllByOrderByIdDesc(pageRequest)
				: consultationRepository.findByStatusOrderByIdDesc(status, pageRequest);
		return PageResponse.of(consultations, consultations.stream().map(this::toResponse).toList());
	}

	@Transactional
	public ConsultationResponse detail(Long id, UserPrincipal principal) {
		Consultation consultation = getConsultation(id);
		if (principal != null) {
			auditService.log(userService.getUser(principal.id()), "READ_CONSULTATION", "Consultation", id.toString());
		}
		return toResponse(consultation);
	}

	@Transactional
	public ConsultationResponse updateStatus(Long id, ConsultationStatusRequest request) {
		Consultation consultation = getConsultation(id);
		consultation.setStatus(request.status());
		return toResponse(consultation);
	}

	@Transactional
	public ConsultationResponse reply(Long id, ConsultationReplyRequest request) {
		Consultation consultation = getConsultation(id);
		consultation.setAdminMemo(request.adminMemo());
		if (request.status() != null) {
			consultation.setStatus(request.status());
		}
		return toResponse(consultation);
	}

	private Consultation getConsultation(Long id) {
		return consultationRepository.findById(id)
				.orElseThrow(() -> BusinessException.notFound("상담요청을 찾을 수 없습니다."));
	}

	private ConsultationResponse toResponse(Consultation consultation) {
		Product product = consultation.getProduct();
		ProductVariant variant = consultation.getVariant();
		return new ConsultationResponse(
				consultation.getId(),
				consultation.getType(),
				product == null ? null : product.getId(),
				product == null ? null : product.getName(),
				product != null && product.getStatus() == ProductStatus.DELETED,
				variant == null ? null : variant.getId(),
				variant == null ? null : variant.getVariantName(),
				consultation.getContactName(),
				consultation.getContactPhone(),
				consultation.getMessage(),
				consultation.getStatus(),
				consultation.getAdminMemo(),
				consultation.getCreatedAt(),
				consultation.getUpdatedAt()
		);
	}

	public ConsultationRequest typedRequest(ConsultationType type, ConsultationRequest request) {
		return new ConsultationRequest(
				type,
				request.productId(),
				request.variantId(),
				request.contactName(),
				request.contactPhone(),
				request.message(),
				request.privacyAgreed()
		);
	}
}
