package com.stack.sellstack.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.stack.sellstack.model.entity.base.BaseAuditEntity;
import com.stack.sellstack.model.enums.PaymentStatus;
import com.stack.sellstack.model.enums.PurchaseStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "purchases",
        indexes = {
                @Index(name = "idx_purchases_order_id", columnList = "order_id"),
                @Index(name = "idx_purchases_buyer_id", columnList = "buyer_id"),
                @Index(name = "idx_purchases_seller_id", columnList = "seller_id"),
                @Index(name = "idx_purchases_product_id", columnList = "product_id"),
                @Index(name = "idx_purchases_status", columnList = "status"),
                @Index(name = "idx_purchases_payment_status", columnList = "payment_status"),
                @Index(name = "idx_purchases_created_at", columnList = "created_at DESC")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_purchases_order_id", columnNames = "order_id")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Purchase extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "order_id", nullable = false, length = 100)
    private String orderId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false, foreignKey = @ForeignKey(name = "fk_purchases_buyer"))
    private Buyer buyer;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false, foreignKey = @ForeignKey(name = "fk_purchases_seller"))
    private Seller seller;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, foreignKey = @ForeignKey(name = "fk_purchases_product"))
    private Product product;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private PurchaseStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 50)
    private PaymentStatus paymentStatus;

    @Column(name = "payment_id", length = 100)
    private String paymentId;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "razorpay_order_id", length = 100)
    private String razorpayOrderId;

    @Column(name = "razorpay_payment_id", length = 100)
    private String razorpayPaymentId;

    @Column(name = "razorpay_signature", length = 255)
    private String razorpaySignature;

    @Column(name = "download_count")
    private Integer downloadCount;

    @Column(name = "last_downloaded_at")
    private Instant lastDownloadedAt;

    @Column(name = "download_expires_at")
    private Instant downloadExpiresAt;

    @Column(name = "is_refunded")
    private Boolean isRefunded;

    @Column(name = "refund_amount", precision = 10, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "refund_reason", length = 500)
    private String refundReason;

    @Column(name = "refunded_at")
    private Instant refundedAt;

    @Column(name = "invoice_url", length = 1000)
    private String invoiceUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Set<String> metadata = new HashSet<>();

    @PrePersist
    @PreUpdate
    private void setDefaults() {
        if (currency == null) {
            currency = "INR";
        }
        if (status == null) {
            status = PurchaseStatus.PENDING;
        }
        if (paymentStatus == null) {
            paymentStatus = PaymentStatus.PENDING;
        }
        if (downloadCount == null) {
            downloadCount = 0;
        }
        if (isRefunded == null) {
            isRefunded = false;
        }
    }
}