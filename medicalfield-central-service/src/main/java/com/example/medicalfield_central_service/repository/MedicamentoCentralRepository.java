package com.example.medicalfield_central_service.repository;

import com.example.medicalfield_central_service.model.MedicamentoCentral;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MedicamentoCentralRepository extends JpaRepository<MedicamentoCentral, UUID> {
}