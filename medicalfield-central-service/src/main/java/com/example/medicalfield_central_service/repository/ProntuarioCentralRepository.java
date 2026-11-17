package com.example.medicalfield_central_service.repository;

import com.example.medicalfield_central_service.model.ProntuarioCentral;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProntuarioCentralRepository extends JpaRepository<ProntuarioCentral, UUID> {
    Optional<ProntuarioCentral> findFirstByDocumentoPaciente(String documentoPaciente);
    Optional<ProntuarioCentral> findFirstByNomePacienteIgnoreCase(String nomePaciente);
}