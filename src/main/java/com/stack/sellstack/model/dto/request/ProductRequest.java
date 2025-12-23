package com.stack.sellstack.model.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Set;

public class ProductRequest {

    @Data
    public static class CreateProductRequest {
        @NotBlank(message = "Title is required")
        @Size(max = 500, message = "Title must be less than 500 characters")
        private String title;

        @Size(max = 5000, message = "Description must be less than 5000 characters")
        private String description;

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be greater than 0")
        @DecimalMax(value = "9999999.99", message = "Price must be less than 10,000,000")
        private BigDecimal price;

        private BigDecimal discountPrice;

        @NotBlank(message = "Currency is required")
        @Size(min = 3, max = 3, message = "Currency must be 3 characters")
        private String currency;

        private String category;
        private Set<String> tags;
        private String language;

        @NotNull(message = "File is required")
        private MultipartFile file;

        private MultipartFile thumbnail;
        private MultipartFile preview;

        private Boolean allowRefunds;
        private Integer refundDays;
        private Integer maxDownloads;
        private Integer downloadExpiryDays;

        private String metaTitle;
        private String metaDescription;
        private String metaKeywords;
    }

    @Data
    public static class UpdateProductRequest {
        @Size(max = 500, message = "Title must be less than 500 characters")
        private String title;

        @Size(max = 5000, message = "Description must be less than 5000 characters")
        private String description;

        @DecimalMin(value = "0.01", message = "Price must be greater than 0")
        @DecimalMax(value = "9999999.99", message = "Price must be less than 10,000,000")
        private BigDecimal price;

        private BigDecimal discountPrice;
        private String category;
        private Set<String> tags;
        private String language;

        private MultipartFile thumbnail;
        private MultipartFile preview;

        private Boolean allowRefunds;
        private Integer refundDays;
        private Integer maxDownloads;
        private Integer downloadExpiryDays;

        private String metaTitle;
        private String metaDescription;
        private String metaKeywords;
    }

    @Data
    public static class PublishProductRequest {
        @NotNull(message = "Publish status is required")
        private Boolean publish;
    }
}