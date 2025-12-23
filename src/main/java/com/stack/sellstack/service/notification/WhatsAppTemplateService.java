package com.stack.sellstack.service.notification;

import com.stack.sellstack.model.dto.request.WhatsAppTemplateRequest;
import com.stack.sellstack.model.dto.response.WhatsAppTemplateResponse;
import com.stack.sellstack.model.entity.WhatsAppTemplate;
import com.stack.sellstack.repository.WhatsAppTemplateRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WhatsAppTemplateService {

    private final WhatsAppTemplateRepository whatsAppTemplateRepository;

    public WhatsAppTemplateResponse createOrUpdateTemplate(WhatsAppTemplateRequest request) {
        WhatsAppTemplate template;

        if (request.getId() != null && !request.getId().isEmpty()) {
            // Update existing template
            template = whatsAppTemplateRepository.findById(UUID.fromString(request.getId()))
                    .orElseThrow(() -> new EntityNotFoundException("WhatsApp template not found with id: " + request.getId()));

            template.setTemplateName(request.getTemplateName());
            template.setLanguageCode(request.getLanguageCode());
            template.setWhatsappTemplateId(request.getWhatsappTemplateId());
            template.setWhatsappNamespace(request.getWhatsappNamespace());
            template.setCategory(request.getCategory());
            template.setHeaderType(request.getHeaderType());
            template.setBodyTemplate(request.getBodyTemplate());
            template.setFooterTemplate(request.getFooterTemplate());
            template.setButtonSchema(request.getButtonSchema());
            template.setVariableSchema(request.getVariableSchema());
            template.setStatus(request.getStatus());
            template.setIsActive(request.getIsActive());

            if ("APPROVED".equals(request.getStatus())) {
                template.setApprovedAt(LocalDateTime.now());
            }

            log.info("Updating WhatsApp template: {}", request.getTemplateKey());
        } else {
            // Create new template
            // Check if template key already exists
            if (whatsAppTemplateRepository.existsByTemplateKey(request.getTemplateKey())) {
                throw new IllegalArgumentException("Template key already exists: " + request.getTemplateKey());
            }

            template = WhatsAppTemplate.builder()
                    .templateName(request.getTemplateName())
                    .templateKey(request.getTemplateKey())
                    .languageCode(request.getLanguageCode())
                    .whatsappTemplateId(request.getWhatsappTemplateId())
                    .whatsappNamespace(request.getWhatsappNamespace())
                    .category(request.getCategory())
                    .headerType(request.getHeaderType())
                    .bodyTemplate(request.getBodyTemplate())
                    .footerTemplate(request.getFooterTemplate())
                    .buttonSchema(request.getButtonSchema())
                    .variableSchema(request.getVariableSchema())
                    .status(request.getStatus() != null ? request.getStatus() : "DRAFT")
                    .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                    .build();

            if ("APPROVED".equals(template.getStatus())) {
                template.setApprovedAt(LocalDateTime.now());
            }

            log.info("Creating new WhatsApp template: {}", request.getTemplateKey());
        }

        WhatsAppTemplate savedTemplate = whatsAppTemplateRepository.save(template);
        return mapToResponse(savedTemplate);
    }

    @Transactional(readOnly = true)
    public List<WhatsAppTemplateResponse> getAllTemplates(String category) {
        List<WhatsAppTemplate> templates;

        if (category != null && !category.isEmpty()) {
            templates = whatsAppTemplateRepository.findByCategory(category);
        } else {
            templates = whatsAppTemplateRepository.findAll();
        }

        return templates.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WhatsAppTemplateResponse getTemplateByKey(String templateKey) {
        WhatsAppTemplate template = whatsAppTemplateRepository.findByTemplateKey(templateKey)
                .orElseThrow(() -> new EntityNotFoundException("WhatsApp template not found with key: " + templateKey));

        return mapToResponse(template);
    }

    public void deleteTemplate(String id) {
        WhatsAppTemplate template = whatsAppTemplateRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new EntityNotFoundException("WhatsApp template not found with id: " + id));

        whatsAppTemplateRepository.delete(template);
        log.info("Deleted WhatsApp template: {}", template.getTemplateKey());
    }

    public WhatsAppTemplateResponse updateStatus(String id, String status, String rejectionReason) {
        WhatsAppTemplate template = whatsAppTemplateRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new EntityNotFoundException("WhatsApp template not found with id: " + id));

        template.setStatus(status);

        if ("APPROVED".equals(status)) {
            template.setApprovedAt(LocalDateTime.now());
            template.setRejectionReason(null);
        } else if ("REJECTED".equals(status)) {
            template.setRejectionReason(rejectionReason);
        }

        WhatsAppTemplate updatedTemplate = whatsAppTemplateRepository.save(template);
        return mapToResponse(updatedTemplate);
    }

    @Transactional(readOnly = true)
    public List<WhatsAppTemplateResponse> getActiveTemplates() {
        return whatsAppTemplateRepository.findByIsActiveTrue()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private WhatsAppTemplateResponse mapToResponse(WhatsAppTemplate template) {
        return WhatsAppTemplateResponse.builder()
                .id(template.getId().toString())
                .templateName(template.getTemplateName())
                .templateKey(template.getTemplateKey())
                .languageCode(template.getLanguageCode())
                .whatsappTemplateId(template.getWhatsappTemplateId())
                .whatsappNamespace(template.getWhatsappNamespace())
                .category(template.getCategory())
                .headerType(template.getHeaderType())
                .bodyTemplate(template.getBodyTemplate())
                .footerTemplate(template.getFooterTemplate())
                .buttonSchema(template.getButtonSchema())
                .variableSchema(template.getVariableSchema())
                .status(template.getStatus())
                .isActive(template.getIsActive())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .approvedAt(template.getApprovedAt())
                .rejectionReason(template.getRejectionReason())
                .build();
    }
}