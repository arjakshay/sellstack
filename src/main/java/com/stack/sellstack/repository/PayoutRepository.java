package com.stack.sellstack.repository;

import com.stack.sellstack.model.entity.Payout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PayoutRepository extends JpaRepository<Payout, UUID> {
    // Basic CRUD operations provided by JpaRepository
}