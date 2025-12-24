package com.stack.sellstack.service;

import com.stack.sellstack.exception.BusinessException;
import com.stack.sellstack.exception.ResourceNotFoundException;
import com.stack.sellstack.model.dto.request.AuthRequest;
import com.stack.sellstack.model.dto.request.SellerRequest;
import com.stack.sellstack.model.dto.response.PublicSellerResponse;
import com.stack.sellstack.model.entity.Seller;
import com.stack.sellstack.model.enums.SellerStatus;
import com.stack.sellstack.model.enums.VerificationStatus;
import com.stack.sellstack.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class SellerService {

    private final SellerRepository sellerRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionService sessionService;
    private final SellerBalanceService sellerBalanceService;

    @Transactional
    public Seller createSeller(AuthRequest.VerifyRegistrationRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("Passwords do not match");
        }

        if (sellerRepository.existsByPhone(request.getPhone())) {
            throw new BusinessException("Seller with this phone already exists");
        }

        if (request.getEmail() != null && sellerRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Seller with this email already exists");
        }

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
                .marketingConsent(false)
                .termsAcceptedAt(java.time.Instant.now())
                .build();

        Seller savedSeller = sellerRepository.save(seller);

        // Create initial balance record
        sellerBalanceService.createInitialBalance(savedSeller.getId());

        return savedSeller;
    }

    public Seller findByUsername(String username) {
        log.info("🔍 SellerService - Finding seller by username: {}", username);

        // Try to find by phone
        Seller seller = sellerRepository.findByPhone(username)
                .or(() -> sellerRepository.findByEmail(username))
                .orElse(null);

        if (seller != null) {
            log.info("✅ SellerService - Found seller: {} (ID: {})",
                    seller.getPhone(), seller.getId());
        } else {
            log.warn("❌ SellerService - Seller not found for username: {}", username);
        }

        return seller;
    }

    public Seller findById(UUID id) {
        return sellerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found"));
    }

    @Transactional
    public Seller updateProfile(String username, SellerRequest.UpdateProfileRequest request) {
        Seller seller = findByUsername(username);

        if (request.getDisplayName() != null) {
            seller.setDisplayName(request.getDisplayName());
        }

        if (request.getFullName() != null) {
            seller.setFullName(request.getFullName());
        }

        if (request.getBio() != null) {
            seller.setBio(request.getBio());
        }

        if (request.getProfilePicUrl() != null) {
            seller.setProfilePicUrl(request.getProfilePicUrl());
        }

        if (request.getUpiId() != null) {
            seller.setUpiId(request.getUpiId());
        }

        if (request.getGstNumber() != null) {
            seller.setGstNumber(request.getGstNumber());
        }

        if (request.getPanNumber() != null) {
            seller.setPanNumber(request.getPanNumber());
        }

        if (request.getMarketingConsent() != null) {
            seller.setMarketingConsent(request.getMarketingConsent());
        }

        return sellerRepository.save(seller);
    }

    @Transactional
    public void changePassword(String username, SellerRequest.ChangePasswordRequest request) {
        Seller seller = findByUsername(username);

        if (!passwordEncoder.matches(request.getCurrentPassword(), seller.getPasswordHash())) {
            throw new BusinessException("Current password is incorrect");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("New passwords do not match");
        }

        seller.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        sellerRepository.save(seller);

        // Invalidate all sessions for security
        sessionService.invalidateAllSessions(seller.getId());

        log.info("Password changed for seller: {}", username);
    }

    @Transactional
    public Seller updateStatus(UUID sellerId, SellerRequest.UpdateStatusRequest request) {
        Seller seller = findById(sellerId);
        seller.setStatus(request.getStatus());

        if (request.getStatus() == SellerStatus.SUSPENDED || request.getStatus() == SellerStatus.DELETED) {
            // Invalidate all sessions for suspended/deleted sellers
            sessionService.invalidateAllSessions(sellerId);
        }

        return sellerRepository.save(seller);
    }

    public Page<Seller> getAllSellers(SellerStatus status, VerificationStatus verificationStatus,
                                      LocalDate createdAfter, Pageable pageable) {
        if (createdAfter != null) {
            LocalDateTime createdAfterDateTime = createdAfter.atStartOfDay();
            return sellerRepository.findAllWithFilters(status, verificationStatus, createdAfterDateTime, pageable);
        }
        return sellerRepository.findAllWithFilters(status, verificationStatus, null, pageable);
    }

    public PublicSellerResponse getPublicProfile(UUID sellerId) {
        Seller seller = findById(sellerId);

        return PublicSellerResponse.builder()
                .id(seller.getId())
                .displayName(seller.getDisplayName())
                .profilePicUrl(seller.getProfilePicUrl())
                .bio(seller.getBio())
                .totalProducts(seller.getTotalProducts())
                .ratingAvg(seller.getRatingAvg())
                .ratingCount(seller.getRatingCount())
                .createdAt(seller.getCreatedAt())
                .build();
    }

    public PublicSellerResponse getSellerBySlug(String slug) {
        // Implement slug logic based on your needs
        // For now, find by display name or generate slug from display name
        Seller seller = sellerRepository.findByDisplayNameSlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found"));

        return getPublicProfile(seller.getId());
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

        sessionService.invalidateAllSessions(seller.getId());
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
        sellerRepository.updateLastLogin(sellerId, java.time.Instant.now());
    }

    public boolean verifyPassword(Seller seller, String rawPassword) {
        return passwordEncoder.matches(rawPassword, seller.getPasswordHash());
    }

    @Transactional
    public void incrementTotalSales(UUID sellerId, BigDecimal amount) {
        sellerRepository.incrementTotalSales(sellerId, amount);
    }

    @Transactional
    public void incrementTotalProducts(UUID sellerId) {
        sellerRepository.incrementTotalProducts(sellerId);
    }

    @Transactional
    public void updateRating(UUID sellerId, BigDecimal newRating) {
        sellerRepository.updateRating(sellerId, newRating);
    }
}