package com.example.medicalfield_central_service.controller;

import com.example.medicalfield_central_service.model.FichaAtendimentoCentral;
import com.example.medicalfield_central_service.model.MedicamentoCentral;
import com.example.medicalfield_central_service.model.ProntuarioCentral;
import com.example.medicalfield_central_service.repository.FichaAtendimentoCentralRepository;
import com.example.medicalfield_central_service.repository.MedicamentoCentralRepository;
import com.example.medicalfield_central_service.repository.ProntuarioCentralRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
@RequestMapping("/admin")
@SuppressWarnings({"unchecked", "rawtypes"})
public class AdminCentralController {

    private final FichaAtendimentoCentralRepository fichaRepository;
    private final MedicamentoCentralRepository medicamentoRepository;
    private final ProntuarioCentralRepository prontuarioRepository;
    private final RestClient restClient;

    @Value("${audit.base.url:http://localhost:8082/api/v1/auditoria}")
    private String auditBaseUrl;

    @Value("${pdf.base.url:http://localhost:8083/api/v1/documentos}")
    private String pdfBaseUrl;

    public AdminCentralController(FichaAtendimentoCentralRepository fichaRepository,
                                  MedicamentoCentralRepository medicamentoRepository,
                                  ProntuarioCentralRepository prontuarioRepository) {
        this.fichaRepository = fichaRepository;
        this.medicamentoRepository = medicamentoRepository;
        this.prontuarioRepository = prontuarioRepository;
        this.restClient = RestClient.create();
    }

    @GetMapping("/login")
    public String loginAdministrativo() {
        return "admin-login";
    }

    @GetMapping
    public String painelAdministrativo(Model model) {
        List<FichaAtendimentoCentral> atendimentos = fichaRepository.findAll(
                Sort.by(Sort.Direction.DESC, "sincronizadoEm")
        );

        // Agrupa pacientes únicos a partir das fichas sincronizadas
        Map pacientesMap = new LinkedHashMap();
        for (FichaAtendimentoCentral f : atendimentos) {
            String chave = (f.getDocumentoPaciente() != null && !f.getDocumentoPaciente().isBlank())
                    ? f.getDocumentoPaciente()
                    : (f.getNomePaciente() != null ? f.getNomePaciente().toLowerCase() : f.getId().toString());

            if (!pacientesMap.containsKey(chave)) {
                Map p = new HashMap();
                p.put("nome", f.getNomePaciente() != null ? f.getNomePaciente() : "Não identificado");
                p.put("cpf", f.getDocumentoPaciente() != null ? f.getDocumentoPaciente() : "Sem documento");
                p.put("ultimoManchester", f.getProtocoloManchester());
                p.put("ultimoAtendimento", f.getSincronizadoEm());
                p.put("totalAtendimentos", 0);
                pacientesMap.put(chave, p);
            }

            Map existente = (Map) pacientesMap.get(chave);
            existente.put("totalAtendimentos", (int) existente.get("totalAtendimentos") + 1);
        }

        List<ProntuarioCentral> prontuarios = prontuarioRepository.findAll(
                Sort.by(Sort.Direction.DESC, "updatedAt")
        );

        List<MedicamentoCentral> medicamentos = medicamentoRepository.findAll(Sort.by("nome"));

        List logsAuditoria = new ArrayList();
        Map statusIntegridade = new HashMap();
        boolean auditoriaOnline = false;

        try {
            logsAuditoria = restClient.get()
                    .uri(auditBaseUrl)
                    .retrieve()
                    .body(List.class);

            statusIntegridade = restClient.get()
                    .uri(auditBaseUrl + "/integridade")
                    .retrieve()
                    .body(Map.class);

            auditoriaOnline = true;
        } catch (Exception e) {
            statusIntegridade.put("status", "Microsserviço de Auditoria (8082) indisponível");
            statusIntegridade.put("cadeiaIntegra", false);
        }

        model.addAttribute("atendimentos", atendimentos);
        model.addAttribute("pacientes", pacientesMap.values());
        model.addAttribute("prontuarios", prontuarios);
        model.addAttribute("medicamentos", medicamentos);
        model.addAttribute("logsAuditoria", logsAuditoria);
        model.addAttribute("statusIntegridade", statusIntegridade);
        model.addAttribute("auditoriaOnline", auditoriaOnline);

        return "admin-dashboard";
    }

    @PostMapping("/medicamentos")
    public String cadastrarMedicamento(@RequestParam String nome,
                                       @RequestParam(required = false) String dosagem,
                                       RedirectAttributes redirectAttributes) {
        MedicamentoCentral med = MedicamentoCentral.builder()
                .nome(nome.trim())
                .dosagem(dosagem != null && !dosagem.isBlank() ? dosagem.trim() : null)
                .build();
        medicamentoRepository.save(med);
        redirectAttributes.addFlashAttribute("mensagemSucesso", "Medicamento adicionado ao catálogo!");
        return "redirect:/admin";
    }

    @PostMapping("/medicamentos/{id}/excluir")
    public String excluirMedicamento(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        medicamentoRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("mensagemSucesso", "Medicamento removido do catálogo.");
        return "redirect:/admin";
    }

    @PostMapping("/atendimentos/enviar-email")
    public String enviarEmailAtendimento(@RequestParam UUID fichaId,
                                         @RequestParam String emailDestino,
                                         RedirectAttributes redirectAttributes) {
        try {
            FichaAtendimentoCentral ficha = fichaRepository.findById(fichaId)
                    .orElseThrow(() -> new IllegalArgumentException("Ficha não encontrada."));

            Map payload = new HashMap();
            payload.put("id", ficha.getId().toString());
            payload.put("nomePaciente", ficha.getNomePaciente());
            payload.put("documentoPaciente", ficha.getDocumentoPaciente());
            payload.put("pressaoArterial", ficha.getPressaoArterial());
            payload.put("frequenciaCardiaca", ficha.getFrequenciaCardiaca());
            payload.put("frequenciaRespiratoria", ficha.getFrequenciaRespiratoria());
            payload.put("saturacaoOxigenio", ficha.getSaturacaoOxigenio());
            payload.put("temperaturaCorporal", ficha.getTemperaturaCorporal());
            payload.put("glicemiaCapilar", ficha.getGlicemiaCapilar());
            payload.put("escalaGlasgow", ficha.getEscalaGlasgow());
            payload.put("classificacaoGlasgow", ficha.getClassificacaoGlasgow());
            payload.put("protocoloManchester", ficha.getProtocoloManchester());
            payload.put("protocoloStart", ficha.getProtocoloStart());
            payload.put("medicamentos", ficha.getMedicamentos());
            payload.put("observacoes", ficha.getObservacoes());
            payload.put("profissionalRegistro", ficha.getProfissionalRegistro());
            payload.put("dataHoraAtendimento", ficha.getDataHoraAtendimento() != null ? ficha.getDataHoraAtendimento().toString() : null);
            payload.put("emailDestino", emailDestino);

            Map resposta = restClient.post()
                    .uri(pdfBaseUrl + "/gerar-e-enviar")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(Map.class);

            String statusEmail = resposta != null ? String.valueOf(resposta.get("emailStatus")) : "Enviado";
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Processado para " + emailDestino + ": " + statusEmail);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensagemErro", "Falha ao acionar serviço de PDF/E-mail (8083): " + e.getMessage());
        }

        return "redirect:/admin";
    }

    @GetMapping("/atendimentos/{id}/pdf")
    public ResponseEntity<byte[]> visualizarPdf(@PathVariable UUID id) {
        try {
            byte[] pdfBytes = restClient.get()
                    .uri(pdfBaseUrl + "/" + id + "/pdf")
                    .retrieve()
                    .body(byte[].class);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.inline().filename("ficha_" + id + ".pdf").build());

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}