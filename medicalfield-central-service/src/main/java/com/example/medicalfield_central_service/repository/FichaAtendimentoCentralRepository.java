package com.example.medicalfield_central_service.repository;

import com.example.medicalfield_central_service.model.FichaAtendimentoCentral;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FichaAtendimentoCentralRepository extends JpaRepository<FichaAtendimentoCentral, UUID> {
}