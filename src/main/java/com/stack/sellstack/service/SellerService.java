package com.stack.sellstack.service;

import com.stack.sellstack.exception.BusinessException;
import com.stack.sellstack.exception.ResourceNotFoundException;
import com.stack.sellstack.model.dto.request.AuthRequest;
import com.stack.sellstack.model.entity.Seller;
import com.stack.sellstack.model.enums.SellerStatus;
import com.stack.sellstack.model.enums.VerificationStatus;
import com.stack.sellstack.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class SellerService {

    private final SellerRepository sellerRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionService sessionService;

    @Transactional
    public Seller createSeller(AuthRequest.VerifyRegistrationRequest request) {
        // Validate password match
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("Passwords do not match");
        }

        // Check if seller already exists
        if (sellerRepository.existsByPhone(request.getPhone())) {
            throw new BusinessException("Seller with this phone already exists");
        }

        if (request.getEmail() != null && sellerRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Seller with this email already exists");
        }

        // Create seller
        Seller seller = Seller.builder()
                .phone(request.getPhone())
                .email(request.getEmail())
                .displayName(request.getDisplayName() != null ?
                        request.getDisplayName() :
                        "Seller_" + request.getPhone().substring(request.getPhone().length() - 4))
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .status(SellerStatus.ACTIVE)
                .verificationStatus(VerificationStatus.VERIFIED)
                .totalEarnings(java.math.BigDecimal.ZERO)
                .availableBalance(java.math.BigDecimal.ZERO)
                .totalSales(0)
                .totalProducts(0)
                .ratingCount(0)
                .marketingConsent(false) // Default false, can be updated later
                .termsAcceptedAt(Instant.now())
                .build();

        return sellerRepository.save(seller);
    }

    public Seller findByUsername(String username) {
        return sellerRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found"));
    }

    public Seller findById(UUID id) {
        return sellerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found"));
    }

    public boolean existsByPhone(String phone) {
        return sellerRepository.existsByPhone(phone);
    }

    public boolean existsByEmail(String email) {
        return sellerRepository.existsByEmail(email);
    }

    @Transactional
    public void resetPassword(String phone, String newPassword) {
        Seller seller = sellerRepository.findByPhone(phone)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found"));

        seller.setPasswordHash(passwordEncoder.encode(newPassword));
        sellerRepository.save(seller);

        log.info("Password reset for seller: {}", phone);
    }

    @Transactional
    public void invalidateAllSessions(String phone) {
        Seller seller = sellerRepository.findByPhone(phone)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found"));

        sessionService.invalidateAllSessions(seller.getId());
    }

    @Transactional
    public void updateLastLogin(UUID sellerId) {
        sellerRepository.updateLastLogin(sellerId, Instant.now());
    }

    @Transactional
    public void updateProfile(UUID sellerId, UpdateProfileRequest request) {
        Seller seller = findById(sellerId);

        if (request.getDisplayName() != null) {
            seller.setDisplayName(request.getDisplayName());
        }

        if (request.getFullName() != null) {
            seller.setFullName(request.getFullName());
        }

        if (request.getBio() != null) {
            seller.setBio(request.getBio());
        }

        sellerRepository.save(seller);
    }

    public boolean verifyPassword(Seller seller, String rawPassword) {
        return passwordEncoder.matches(rawPassword, seller.getPasswordHash());
    }

    // Inner class for update profile request
    @lombok.Data
    public static class UpdateProfileRequest {
        private String displayName;
        private String fullName;
        private String bio;
        private String profilePicUrl;
    }
}