package com.stack.sellstack.repository;

import com.stack.sellstack.model.entity.WhatsAppTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WhatsAppTemplateRepository extends JpaRepository<WhatsAppTemplate, UUID> {

    Optional<WhatsAppTemplate> findByTemplateKey(String templateKey);

    Optional<WhatsAppTemplate> findByTemplateKeyAndStatus(String templateKey, String status);

    List<WhatsAppTemplate> findByStatus(String status);

    List<WhatsAppTemplate> findByIsActiveTrue();

    @Query("SELECT wt FROM WhatsAppTemplate wt WHERE wt.status = :status AND wt.isActive = true")
    List<WhatsAppTemplate> findActiveByStatus(@Param("status") String status);

    boolean existsByTemplateKey(String templateKey);
    List<WhatsAppTemplate> findByCategory(String category);
}