package com.stack.sellstack.util;

import com.stack.sellstack.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class SlugGenerator {

    private final ProductRepository productRepository;

    public String generateUniqueSlug(String title) {
        String baseSlug = generateSlug(title);
        String uniqueSlug = baseSlug;
        int counter = 1;

        // Check if slug already exists
        while (productRepository.findBySlug(uniqueSlug).isPresent()) {
            uniqueSlug = baseSlug + "-" + counter;
            counter++;

            // Safety limit
            if (counter > 100) {
                uniqueSlug = baseSlug + "-" + UUID.randomUUID().toString().substring(0, 8);
                break;
            }
        }

        return uniqueSlug;
    }

    public String generateSlug(String text) {
        if (StringUtils.isBlank(text)) {
            return "";
        }

        // Normalize and remove accents
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        // Convert to lowercase
        normalized = normalized.toLowerCase();

        // Replace non-alphanumeric with hyphens
        normalized = normalized.replaceAll("[^a-z0-9\\s-]", "");

        // Replace multiple spaces/hyphens with single hyphen
        normalized = normalized.replaceAll("[\\s-]+", "-");

        // Trim hyphens from start and end
        normalized = normalized.replaceAll("^-|-$", "");

        // Limit length
        if (normalized.length() > 100) {
            normalized = normalized.substring(0, 100);
            // Ensure it doesn't end with hyphen
            normalized = normalized.replaceAll("-+$", "");
        }

        return normalized;
    }
}