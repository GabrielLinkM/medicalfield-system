package com.example.medicalfield_central_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FichaSyncRequestDto {

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

    private String medicamentos;
    private String observacoes;

    private String profissionalRegistro;
    private LocalDateTime dataHoraAtendimento;
}