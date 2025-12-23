package com.stack.sellstack.service.notification;

import com.stack.sellstack.model.dto.request.EmailTemplateRequest;
import com.stack.sellstack.model.dto.response.EmailTemplateResponse;
import com.stack.sellstack.model.dto.response.ProcessedTemplate;
import com.stack.sellstack.exception.NotificationException;
import com.stack.sellstack.model.entity.EmailTemplate;
import com.stack.sellstack.repository.EmailTemplateRepository;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailTemplateService {

    private final EmailTemplateRepository emailTemplateRepository;
    private final Configuration freemarkerConfig;

    /**
     * Create or update email template
     */
    @Transactional
    public EmailTemplateResponse createOrUpdateTemplate(EmailTemplateRequest request) {
        EmailTemplate template;

        if (request.getId() != null) {
            // Update existing template
            template = emailTemplateRepository.findById(request.getId())
                    .orElseThrow(() -> new NotificationException("Template not found"));

            template.setTemplateName(request.getTemplateName());
            template.setSubjectTemplate(request.getSubjectTemplate());
            template.setHtmlTemplate(request.getHtmlTemplate());
            template.setPlainTextTemplate(request.getPlainTextTemplate());
            template.setVariables(request.getVariables());
            template.setCategory(request.getCategory());
            template.setPriority(request.getPriority());
            template.setTags(request.getTags());
            template.setUpdatedAt(LocalDateTime.now());
            template.setUpdatedBy(request.getUpdatedBy());

        } else {
            // Create new template
            if (emailTemplateRepository.existsByTemplateKey(request.getTemplateKey())) {
                throw new NotificationException("Template key already exists");
            }

            template = EmailTemplate.builder()
                    .templateKey(request.getTemplateKey())
                    .templateName(request.getTemplateName())
                    .subjectTemplate(request.getSubjectTemplate())
                    .htmlTemplate(request.getHtmlTemplate())
                    .plainTextTemplate(request.getPlainTextTemplate())
                    .variables(request.getVariables())
                    .category(request.getCategory() != null ? request.getCategory() : "TRANSACTIONAL")
                    .priority(request.getPriority() != null ? request.getPriority() : 5)
                    .tags(request.getTags())
                    .isActive(true)
                    .createdBy(request.getCreatedBy())
                    .updatedBy(request.getCreatedBy())
                    .build();
        }

        template = emailTemplateRepository.save(template);

        return mapToResponse(template);
    }

    /**
     * Process template with variables
     */
    @Transactional(readOnly = true)
    public ProcessedTemplate processTemplate(String templateKey, Map<String, Object> variables) {
        EmailTemplate template = emailTemplateRepository
                .findByTemplateKeyAndIsActiveTrue(templateKey)
                .orElseThrow(() -> new NotificationException("Template not found or inactive"));

        try {
            // Process subject template
            String processedSubject = processTemplateString(
                    template.getSubjectTemplate(), variables);

            // Process HTML template
            String processedHtml = processTemplateString(
                    template.getHtmlTemplate(), variables);

            // Process plain text template (if available)
            String processedPlainText = template.getPlainTextTemplate() != null ?
                    processTemplateString(template.getPlainTextTemplate(), variables) : null;

            // Generate plain text from HTML if not provided
            if (processedPlainText == null) {
                processedPlainText = htmlToPlainText(processedHtml);
            }

            return ProcessedTemplate.builder()
                    .subject(processedSubject)
                    .htmlContent(processedHtml)
                    .plainTextContent(processedPlainText)
                    .templateKey(templateKey)
                    .build();

        } catch (Exception e) {
            log.error("Failed to process template: {}", templateKey, e);
            throw new NotificationException("Failed to process template: " + e.getMessage());
        }
    }

    /**
     * Get all active templates
     */
    @Transactional(readOnly = true)
    public List<EmailTemplateResponse> getAllTemplates(String category) {
        List<EmailTemplate> templates;

        if (category != null) {
            templates = emailTemplateRepository.findByCategoryAndIsActiveTrue(category);
        } else {
            templates = emailTemplateRepository.findByIsActiveTrue();
        }

        return templates.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get template by key
     */
    @Transactional(readOnly = true)
    public EmailTemplateResponse getTemplateByKey(String templateKey) {
        EmailTemplate template = emailTemplateRepository
                .findByTemplateKeyAndIsActiveTrue(templateKey)
                .orElseThrow(() -> new NotificationException("Template not found"));

        return mapToResponse(template);
    }

    /**
     * Deactivate template
     */
    @Transactional
    public void deactivateTemplate(String templateKey, String deactivatedBy) {
        EmailTemplate template = emailTemplateRepository
                .findByTemplateKey(templateKey)
                .orElseThrow(() -> new NotificationException("Template not found"));

        template.setIsActive(false);
        template.setUpdatedAt(LocalDateTime.now());
        template.setUpdatedBy(deactivatedBy);

        emailTemplateRepository.save(template);

        log.info("Template deactivated: {}", templateKey);
    }

    /**
     * Process template string with FreeMarker
     */
    private String processTemplateString(String templateString, Map<String, Object> variables)
            throws Exception {
        Template template = new Template(
                "template",
                templateString,
                freemarkerConfig
        );

        StringWriter writer = new StringWriter();
        template.process(variables, writer);

        return writer.toString();
    }

    /**
     * Convert HTML to plain text (basic implementation)
     */
    private String htmlToPlainText(String html) {
        // Remove HTML tags
        String text = html.replaceAll("<[^>]*>", " ");

        // Replace HTML entities
        text = text.replaceAll("&nbsp;", " ")
                .replaceAll("&amp;", "&")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&quot;", "\"")
                .replaceAll("&#39;", "'");

        // Collapse multiple spaces
        text = text.replaceAll("\\s+", " ").trim();

        return text;
    }

    /**
     * Map entity to response DTO
     */
    private EmailTemplateResponse mapToResponse(EmailTemplate template) {
        return EmailTemplateResponse.builder()
                .id(template.getId())
                .templateKey(template.getTemplateKey())
                .templateName(template.getTemplateName())
                .subjectTemplate(template.getSubjectTemplate())
                .htmlTemplate(template.getHtmlTemplate())
                .plainTextTemplate(template.getPlainTextTemplate())
                .variables(template.getVariables())
                .category(template.getCategory())
                .priority(template.getPriority())
                .tags(template.getTags())
                .isActive(template.getIsActive())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .createdBy(template.getCreatedBy())
                .updatedBy(template.getUpdatedBy())
                .build();
    }
}