package com.example.medicalfield_local_service.repository;

import com.example.medicalfield_local_service.model.FichaAtendimento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface FichaAtendimentoRepository extends JpaRepository<FichaAtendimento, UUID> {

    List<FichaAtendimento> findByPacienteIdOrderByDataHoraAtendimentoDesc(UUID pacienteId);

    List<FichaAtendimento> findByDataHoraAtendimentoBetweenOrderByDataHoraAtendimentoDesc(
            LocalDateTime inicio, LocalDateTime fim);
}