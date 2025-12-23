package com.stack.sellstack.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "delivery_analytics",
        indexes = {
                @Index(name = "idx_delivery_analytics_date", columnList = "date"),
                @Index(name = "idx_delivery_analytics_seller_channel_date", columnList = "seller_id, channel, date"),
                @Index(name = "idx_delivery_analytics_channel_date", columnList = "channel, date"),
                @Index(name = "idx_delivery_analytics_created", columnList = "created_at DESC"),
                @Index(name = "idx_delivery_analytics_seller_date", columnList = "seller_id, date DESC")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "unique_daily_aggregation",
                        columnNames = {"date", "channel", "seller_id", "template_key"})
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "channel", nullable = false, length = 20)
    private String channel; // EMAIL, WHATSAPP, SMS

    @Column(name = "template_key", length = 100)
    private String templateKey;

    @Column(name = "seller_id", length = 100)
    private String sellerId;

    @Column(name = "attempted")
    private Long attempted;

    @Column(name = "sent")
    private Long sent;

    @Column(name = "delivered")
    private Long delivered;

    @Column(name = "failed")
    private Long failed;

    @Column(name = "bounced")
    private Long bounced;

    @Column(name = "opened")
    private Long opened;

    @Column(name = "clicked")
    private Long clicked;

    @Column(name = "replied")
    private Long replied; // For WhatsApp

    @Column(name = "avg_delivery_time_ms")
    private Integer avgDeliveryTimeMs;

    @Column(name = "success_rate", precision = 5, scale = 2)
    private BigDecimal successRate;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    private void calculateSuccessRate() {
        if (attempted != null && attempted > 0) {
            double rate = (double) (sent != null ? sent : 0) / attempted * 100;
            this.successRate = BigDecimal.valueOf(rate);
        }
    }
}