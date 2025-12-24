package com.stack.sellstack.service;

import com.stack.sellstack.exception.BusinessException;
import com.stack.sellstack.exception.ResourceNotFoundException;
import com.stack.sellstack.model.dto.request.SellerRequest;
import com.stack.sellstack.model.dto.response.BalanceTransactionResponse;
import com.stack.sellstack.model.dto.response.PayoutResponse;
import com.stack.sellstack.model.entity.Payout;
import com.stack.sellstack.model.entity.Seller;
import com.stack.sellstack.model.entity.SellerBalance;
import com.stack.sellstack.model.entity.BalanceTransaction;
import com.stack.sellstack.model.enums.TransactionType;
import com.stack.sellstack.model.enums.TransactionStatus;
import com.stack.sellstack.model.enums.PayoutStatus;
import com.stack.sellstack.repository.BalanceTransactionRepository;
import com.stack.sellstack.repository.PayoutRepository;
import com.stack.sellstack.repository.SellerBalanceRepository;
import com.stack.sellstack.repository.SellerRepository;
import com.stack.sellstack.service.payment.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class SellerBalanceService {

    private final SellerBalanceRepository sellerBalanceRepository;
    private final BalanceTransactionRepository transactionRepository;
    private final PayoutRepository payoutRepository;
    private final NotificationService notificationService;
    private final SellerRepository sellerRepository; // Use repository instead of service

    @Transactional
    public SellerBalance createInitialBalance(UUID sellerId) {
        // Use repository instead of service
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found"));

        SellerBalance sellerBalance = SellerBalance.builder()
                .seller(seller)
                .availableBalance(BigDecimal.ZERO)
                .pendingBalance(BigDecimal.ZERO)
                .totalEarnings(BigDecimal.ZERO)
                .build();

        return sellerBalanceRepository.save(sellerBalance);
    }

    public SellerBalance getSellerBalance(UUID sellerId) {
        return sellerBalanceRepository.findBySellerId(sellerId)
                .orElseGet(() -> {
                    log.warn("Balance not found for seller: {}, creating default", sellerId);
                    return createInitialBalanceReturn(sellerId);
                });
    }

    private SellerBalance createInitialBalanceReturn(UUID sellerId) {
        SellerBalance balance = SellerBalance.builder()
                .sellerId(sellerId)
                .availableBalance(BigDecimal.ZERO)
                .pendingBalance(BigDecimal.ZERO)
                .totalEarnings(BigDecimal.ZERO)
                .build();

        return sellerBalanceRepository.save(balance);
    }

    @Transactional
    public BalanceTransaction creditBalance(UUID sellerId, BigDecimal amount,
                                            String reference, String description) {
        // Validate amount
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Amount must be positive");
        }

        // Update balance
        int updated = sellerBalanceRepository.addToBalance(sellerId, amount);
        if (updated == 0) {
            throw new BusinessException("Failed to update balance");
        }

        // Create transaction record
        BalanceTransaction transaction = BalanceTransaction.builder()
                .sellerId(sellerId)
                .amount(amount)
                .type(TransactionType.CREDIT)
                .status(TransactionStatus.COMPLETED)
                .reference(reference)
                .description(description)
                .build();

        transaction = transactionRepository.save(transaction);

        // Send notification
        notificationService.sendBalanceCreditedNotification(sellerId, amount, transaction.getId());

        return transaction;
    }

    @Transactional
    public BalanceTransaction debitBalance(UUID sellerId, BigDecimal amount,
                                           String reference, String description) {
        SellerBalance balance = getSellerBalance(sellerId);

        if (balance.getAvailableBalance().compareTo(amount) < 0) {
            throw new BusinessException("Insufficient balance");
        }

        int updated = sellerBalanceRepository.deductFromBalance(sellerId, amount);
        if (updated == 0) {
            throw new BusinessException("Failed to deduct from balance");
        }

        BalanceTransaction transaction = BalanceTransaction.builder()
                .sellerId(sellerId)
                .amount(amount)
                .type(TransactionType.DEBIT)
                .status(TransactionStatus.COMPLETED)
                .reference(reference)
                .description(description)
                .build();

        return transactionRepository.save(transaction);
    }

    // In SellerBalanceService.java
    @Transactional
    public PayoutResponse requestPayout(UUID sellerId, SellerRequest.PayoutRequest request) {
        SellerBalance balance = getSellerBalance(sellerId);

        // Validate minimum balance
        if (balance.getAvailableBalance().compareTo(request.getAmount()) < 0) {
            throw new BusinessException("Insufficient available balance");
        }

        // Validate minimum payout amount
        if (request.getAmount().compareTo(new BigDecimal("100.00")) < 0) {
            throw new BusinessException("Minimum payout amount is ₹100");
        }

        // Create payout record
        Payout payout = Payout.builder()
                .sellerId(sellerId)
                .amount(request.getAmount())
                .payoutMethod(request.getPayoutMethod())
                .payoutDetails(request.getPayoutDetails())
                .status(PayoutStatus.PENDING)
                .build();

        // You'll need to save this - make sure Payout entity exists
        // payout = payoutRepository.save(payout);

        // For now, return mock response
        PayoutResponse response = PayoutResponse.builder()
                .id(UUID.randomUUID())
                .sellerId(sellerId)
                .amount(request.getAmount())
                .status(PayoutStatus.PENDING)
                .payoutMethod(request.getPayoutMethod())
                .requestedAt(Instant.now())
                .build();

        // Send notification
        // notificationService.sendPayoutRequestedNotification(sellerId, request.getAmount(), response.getId());

        return response;
    }

    public Page<BalanceTransactionResponse> getTransactions(UUID sellerId, String type,
                                                            LocalDate startDate, LocalDate endDate,
                                                            Pageable pageable) {
        return transactionRepository.findBySellerId(sellerId, TransactionType.valueOf(type), startDate, endDate, pageable)
                .map(BalanceTransactionResponse::fromEntity);
    }

    @Transactional
    public SellerBalance adjustBalance(UUID sellerId, SellerRequest.AdjustBalanceRequest request) {
        if (request.getType().equals("CREDIT")) {
            creditBalance(sellerId, request.getAmount(), request.getReferenceId(),
                    "Manual adjustment: " + request.getReason());
        } else if (request.getType().equals("DEBIT")) {
            debitBalance(sellerId, request.getAmount(), request.getReferenceId(),
                    "Manual adjustment: " + request.getReason());
        } else {
            throw new BusinessException("Invalid adjustment type");
        }

        return getSellerBalance(sellerId);
    }

    @Transactional
    public void processSale(UUID sellerId, BigDecimal saleAmount, BigDecimal platformFee,
                            String orderId) {
        BigDecimal sellerEarnings = saleAmount.subtract(platformFee);

        // Add to pending balance first (will be moved to available after processing period)
        sellerBalanceRepository.addToPending(sellerId, sellerEarnings);

        // Record transaction
        BalanceTransaction transaction = BalanceTransaction.builder()
                .sellerId(sellerId)
                .amount(sellerEarnings)
                .type(TransactionType.CREDIT)
                .status(TransactionStatus.PENDING)
                .reference(orderId)
                .description("Sale earnings - Order: " + orderId)
                .build();

        transactionRepository.save(transaction);

        log.info("Sale processed for seller: {}, amount: {}", sellerId, sellerEarnings);
    }

    @Transactional
    public void releasePendingFunds(UUID sellerId, BigDecimal amount, String reference) {
        int updated = sellerBalanceRepository.movePendingToAvailable(sellerId, amount);
        if (updated == 0) {
            throw new BusinessException("Failed to release pending funds");
        }

        // Update transaction status
        transactionRepository.markAsCompleted(reference);

        log.info("Released pending funds for seller: {}, amount: {}", sellerId, amount);
    }
}