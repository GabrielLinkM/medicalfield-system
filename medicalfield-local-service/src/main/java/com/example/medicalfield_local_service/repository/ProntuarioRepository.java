package com.example.medicalfield_local_service.repository;

import com.example.medicalfield_local_service.model.Prontuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProntuarioRepository extends JpaRepository<Prontuario, UUID> {
    Optional<Prontuario> findByPacienteId(UUID pacienteId);
    Optional<Prontuario> findByPacienteCpf(String cpf);
}