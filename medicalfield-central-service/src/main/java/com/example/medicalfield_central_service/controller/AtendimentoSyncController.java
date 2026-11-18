package com.example.medicalfield_central_service.controller;

import com.example.medicalfield_central_service.dto.FichaSyncRequestDto;
import com.example.medicalfield_central_service.model.FichaAtendimentoCentral;
import com.example.medicalfield_central_service.model.ProntuarioCentral;
import com.example.medicalfield_central_service.repository.FichaAtendimentoCentralRepository;
import com.example.medicalfield_central_service.repository.ProntuarioCentralRepository;
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
    private final ProntuarioCentralRepository prontuarioRepository;
    private final RestClient restClient;

    @Value("${audit.service.url:http://localhost:8082/api/v1/auditoria/eventos}")
    private String auditServiceUrl;

    @Value("${pdf.service.url:http://localhost:8083/api/v1/documentos/gerar-e-enviar}")
    private String pdfServiceUrl;

    @Value("${medicalfield.api.key:medicalfield-secret-key-2026}")
    private String apiKey;

    public AtendimentoSyncController(FichaAtendimentoCentralRepository repository,
                                     ProntuarioCentralRepository prontuarioRepository) {
        this.repository = repository;
        this.prontuarioRepository = prontuarioRepository;
        this.restClient = RestClient.create();
    }

    @PostMapping("/sync")
    public ResponseEntity<Void> receberFichaSincronizada(@RequestBody FichaSyncRequestDto dto) {
        LocalDateTime agora = LocalDateTime.now();

        ProntuarioCentral prontuarioCentral = resolverProntuario(dto, agora);

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
                .sincronizadoEm(agora)
                .prontuario(prontuarioCentral)
                .build();

        repository.save(fichaCentral);

        System.out.println(">>> [CENTRAL SYNC] Ficha " + fichaCentral.getId() +
                " (" + fichaCentral.getNomePaciente() + ") e Prontuário " +
                (prontuarioCentral != null ? prontuarioCentral.getId() : "N/A") + " salvos no banco central!");

        notificarAuditoria(fichaCentral);
        notificarGeracaoPdf(fichaCentral);

        return ResponseEntity.ok().build();
    }

    private ProntuarioCentral resolverProntuario(FichaSyncRequestDto dto, LocalDateTime agora) {
        FichaSyncRequestDto.ProntuarioSyncDto pDto = dto.getProntuario();
        FichaSyncRequestDto.PacienteSyncDto pacDto = dto.getPaciente();

        String cpfBruto = (dto.getDocumentoPaciente() != null && !dto.getDocumentoPaciente().isBlank())
                ? dto.getDocumentoPaciente().trim()
                : (pacDto != null ? pacDto.getCpf() : null);

        String cpf = normalizarCpf(cpfBruto);

        String nome = (dto.getNomePaciente() != null && !dto.getNomePaciente().isBlank())
                ? dto.getNomePaciente().trim()
                : (pacDto != null ? pacDto.getNome() : "Não identificado");

        ProntuarioCentral prontuario = null;

        if (pDto != null && pDto.getId() != null) {
            prontuario = prontuarioRepository.findById(pDto.getId()).orElse(null);
        }
        if (prontuario == null && cpf != null) {
            prontuario = prontuarioRepository.findFirstByDocumentoPaciente(cpf).orElse(null);
        }
        if (prontuario == null && nome != null && !nome.isBlank()) {
            ProntuarioCentral porNome = prontuarioRepository.findFirstByNomePacienteIgnoreCase(nome).orElse(null);
            if (porNome != null && (cpf == null || porNome.getDocumentoPaciente() == null || porNome.getDocumentoPaciente().isBlank())) {
                prontuario = porNome;
            }
        }

        if (prontuario == null) {
            prontuario = ProntuarioCentral.builder()
                    .id(pDto != null && pDto.getId() != null ? pDto.getId() : UUID.randomUUID())
                    .dataCriacao(pDto != null && pDto.getDataCriacao() != null ? pDto.getDataCriacao() : agora)
                    .build();
        }

        prontuario.setNomePaciente(nome);
        if (cpf != null) {
            prontuario.setDocumentoPaciente(cpf);
        }
        if (pacDto != null && pacDto.getDataNascimento() != null) {
            prontuario.setDataNascimentoPaciente(pacDto.getDataNascimento());
        }
        if (pDto != null) {
            if (pDto.getTipoSanguineo() != null && !pDto.getTipoSanguineo().isBlank()) {
                prontuario.setTipoSanguineo(pDto.getTipoSanguineo().trim());
            }
            if (pDto.getAlergias() != null) {
                prontuario.setAlergias(pDto.getAlergias().trim());
            }
            if (pDto.getComorbidades() != null) {
                prontuario.setComorbidades(pDto.getComorbidades().trim());
            }
            if (pDto.getMedicamentosUsoContinuo() != null) {
                prontuario.setMedicamentosUsoContinuo(pDto.getMedicamentosUsoContinuo().trim());
            }
        }
        prontuario.setUpdatedAt(agora);

        return prontuarioRepository.save(prontuario);
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
                    .header("X-API-Key", apiKey)
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
                    .header("X-API-Key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ficha)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            System.err.println(">>> [AVISO] Falha ao notificar Microsservico de PDF/Email: " + e.getMessage());
        }
    }

    private String normalizarCpf(String cpf) {
        if (cpf == null || cpf.isBlank()) {
            return null;
        }
        String digits = cpf.replaceAll("\\D", "");
        return digits.isEmpty() ? cpf.trim() : digits;
    }
}