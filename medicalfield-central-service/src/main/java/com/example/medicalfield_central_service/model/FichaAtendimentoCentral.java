package com.example.medicalfield_central_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ficha_atendimento_central")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FichaAtendimentoCentral {

    @Id
    private UUID id;

    private String nomePaciente;
    private String documentoPaciente;

    private String pressaoArterial;
    private Integer frequenciaCardiaca;
    private Integer frequenciaRespiratoria;
    private Integer saturacaoOxigenio;
    private Double temperaturaCorporal;
    private Integer glicemiaCapilar;

    private Integer aberturaOcular;
    private Integer respostaVerbal;
    private Integer respostaMotora;
    private Integer reatividadePupilar;
    private Integer escalaGlasgow;
    private String classificacaoGlasgow;

    private String protocoloManchester;
    private String protocoloStart;

    @Column(columnDefinition = "TEXT")
    private String medicamentos;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    private String profissionalRegistro;
    private LocalDateTime dataHoraAtendimento;
    private LocalDateTime sincronizadoEm;
}