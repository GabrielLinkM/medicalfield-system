package com.example.medicalfield_central_service.controller;

import com.example.medicalfield_central_service.dto.FichaSyncRequestDto;
import com.example.medicalfield_central_service.model.FichaAtendimentoCentral;
import com.example.medicalfield_central_service.repository.FichaAtendimentoCentralRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/atendimentos")
public class AtendimentoSyncController {

    private final FichaAtendimentoCentralRepository repository;
    private final RestClient restClient;

    @Value("${audit.service.url:http://localhost:8082/api/v1/auditoria/eventos}")
    private String auditServiceUrl;

    @Value("${pdf.service.url:http://localhost:8083/api/v1/documentos/gerar-e-enviar}")
    private String pdfServiceUrl;

    public AtendimentoSyncController(FichaAtendimentoCentralRepository repository) {
        this.repository = repository;
        this.restClient = RestClient.create();
    }

    @PostMapping("/sync")
    public ResponseEntity<Void> receberFichaSincronizada(@RequestBody FichaSyncRequestDto dto) {
        FichaAtendimentoCentral fichaCentral = FichaAtendimentoCentral.builder()
                .id(dto.getId() != null ? dto.getId() : UUID.randomUUID())
                .nomePaciente(dto.getNomePaciente())
                .documentoPaciente(dto.getDocumentoPaciente())
                .pressaoArterial(dto.getPressaoArterial())
                .frequenciaCardiaca(dto.getFrequenciaCardiaca())
                .frequenciaRespiratoria(dto.getFrequenciaRespiratoria())
                .saturacaoOxigenio(dto.getSaturacaoOxigenio())
                .temperaturaCorporal(dto.getTemperaturaCorporal())
                .glicemiaCapilar(dto.getGlicemiaCapilar())
                .aberturaOcular(dto.getAberturaOcular())
                .respostaVerbal(dto.getRespostaVerbal())
                .respostaMotora(dto.getRespostaMotora())
                .reatividadePupilar(dto.getReatividadePupilar())
                .escalaGlasgow(dto.getEscalaGlasgow())
                .classificacaoGlasgow(dto.getClassificacaoGlasgow())
                .protocoloManchester(dto.getProtocoloManchester())
                .protocoloStart(dto.getProtocoloStart())
                .medicamentos(dto.getMedicamentos())
                .observacoes(dto.getObservacoes())
                .profissionalRegistro(dto.getProfissionalRegistro())
                .dataHoraAtendimento(dto.getDataHoraAtendimento())
                .sincronizadoEm(LocalDateTime.now())
                .build();

        repository.save(fichaCentral);

        System.out.println(">>> [CENTRAL SYNC] Ficha " + fichaCentral.getId() +
                " (" + fichaCentral.getNomePaciente() + ") salva no banco central!");

        // 1. Dispara evento para o Microsservico de Auditoria (Node.js - Porta 8082)
        notificarAuditoria(fichaCentral);

        // 2. Dispara geracao de PDF e envio de E-mail (Python - Porta 8083)
        notificarGeracaoPdf(fichaCentral);

        return ResponseEntity.ok().build();
    }

    private void notificarAuditoria(FichaAtendimentoCentral ficha) {
        try {
            Map<String, Object> eventoAudit = Map.of(
                    "tipoEvento", "FICHA_ATENDIMENTO_SINCRONIZADA",
                    "origem", "medicalfield-central-service",
                    "entidadeId", ficha.getId().toString(),
                    "ator", ficha.getProfissionalRegistro() != null ? ficha.getProfissionalRegistro() : "DESCONHECIDO",
                    "detalhes", "Paciente: " + ficha.getNomePaciente() + " | Manchester: " + ficha.getProtocoloManchester(),
                    "payload", ficha
            );

            restClient.post()
                    .uri(auditServiceUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(eventoAudit)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            System.err.println(">>> [AVISO] Falha ao notificar Microsservico de Auditoria: " + e.getMessage());
        }
    }

    private void notificarGeracaoPdf(FichaAtendimentoCentral ficha) {
        try {
            restClient.post()
                    .uri(pdfServiceUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ficha)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            System.err.println(">>> [AVISO] Falha ao notificar Microsservico de PDF/Email: " + e.getMessage());
        }
    }
}