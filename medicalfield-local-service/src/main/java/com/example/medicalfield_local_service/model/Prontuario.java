package com.example.medicalfield_local_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "prontuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prontuario {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "paciente_id", unique = true)
    private Paciente paciente;

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