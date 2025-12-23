package com.stack.sellstack.repository;

import com.stack.sellstack.model.entity.EmailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, UUID> {

    Optional<EmailTemplate> findByTemplateKey(String templateKey);

    Optional<EmailTemplate> findByTemplateKeyAndIsActiveTrue(String templateKey);

    List<EmailTemplate> findByCategoryAndIsActiveTrue(String category);

    List<EmailTemplate> findByIsActiveTrue();

    boolean existsByTemplateKey(String templateKey);

    @Query("SELECT et FROM EmailTemplate et WHERE et.isActive = true AND LOWER(et.category) = LOWER(:category)")
    List<EmailTemplate> findActiveByCategory(@Param("category") String category);
}