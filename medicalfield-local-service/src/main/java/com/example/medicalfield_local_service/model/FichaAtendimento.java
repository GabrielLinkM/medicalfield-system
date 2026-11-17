package com.example.medicalfield_local_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ficha_atendimento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FichaAtendimento {

    @Id
    private UUID id;

    // 1. Dados do Paciente
    @Column(nullable = false)
    private String nomePaciente;
    private String documentoPaciente;

    // 2. Sinais Vitais (Campos Individuais)
    private String pressaoArterial;         // Pressão Arterial (mmHg)
    private Integer frequenciaCardiaca;     // Frequência Cardíaca (bpm)
    private Integer frequenciaRespiratoria; // Frequência Respiratória (irpm)
    private Integer saturacaoOxigenio;      // Saturação de Oxigênio (%)
    private Double temperaturaCorporal;     // Temperatura Corporal (°C)
    private Integer glicemiaCapilar;        // Glicemia Capilar (mg/dL)

    // 3. Nível de Consciência (Glasgow P-GCS)
    private Integer aberturaOcular;
    private Integer respostaVerbal;
    private Integer respostaMotora;
    private Integer reatividadePupilar;
    private Integer escalaGlasgow;
    private String classificacaoGlasgow;

    // 4. Protocolos de Triagem
    private String protocoloManchester;
    private String protocoloStart;

    // 5 & 6. Tratamento e Observações
    private String medicamentos;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    // Metadados
    private String profissionalRegistro;
    private LocalDateTime dataHoraAtendimento;
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    private StatusSync statusSync;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "paciente_id")
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "prontuario_id")
    private Prontuario prontuario;
}