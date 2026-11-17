package com.example.medicalfield_local_service.repository;

import com.example.medicalfield_local_service.model.OutboxSync;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxSyncRepository extends JpaRepository<OutboxSync, UUID> {
    List<OutboxSync> findByProcessadoFalse();
}