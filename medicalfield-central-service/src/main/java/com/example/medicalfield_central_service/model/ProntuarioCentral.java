package com.example.medicalfield_central_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "prontuario_central")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProntuarioCentral {

    @Id
    private UUID id;

    private String nomePaciente;
    private String documentoPaciente;
    private LocalDate dataNascimentoPaciente;

    private String tipoSanguineo;

    @Column(columnDefinition = "TEXT")
    private String alergias;

    @Column(columnDefinition = "TEXT")
    private String comorbidades;

    @Column(columnDefinition = "TEXT")
    private String medicamentosUsoContinuo;

    private LocalDateTime dataCriacao;
    private LocalDateTime updatedAt;
}