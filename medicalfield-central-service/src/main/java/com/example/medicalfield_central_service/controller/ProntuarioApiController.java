package com.example.medicalfield_central_service.controller;

import com.example.medicalfield_central_service.model.FichaAtendimentoCentral;
import com.example.medicalfield_central_service.model.ProntuarioCentral;
import com.example.medicalfield_central_service.repository.FichaAtendimentoCentralRepository;
import com.example.medicalfield_central_service.repository.ProntuarioCentralRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping({"/prontuario", "/api/v1/prontuarios"})
public class ProntuarioApiController {

    private final ProntuarioCentralRepository repository;
    private final FichaAtendimentoCentralRepository fichaRepository;

    public ProntuarioApiController(ProntuarioCentralRepository repository,
                                   FichaAtendimentoCentralRepository fichaRepository) {
        this.repository = repository;
        this.fichaRepository = fichaRepository;
    }

    @GetMapping
    public List<ProntuarioCentral> listarTodos() {
        List<ProntuarioCentral> resultado = new ArrayList<>(
                repository.findAll(Sort.by(Sort.Direction.DESC, "updatedAt"))
        );

        Set<String> cpfsExistentes = new HashSet<>();
        Set<String> nomesExistentes = new HashSet<>();

        for (ProntuarioCentral p : resultado) {
            String cpfNorm = normalizarCpf(p.getDocumentoPaciente());
            if (cpfNorm != null) {
                cpfsExistentes.add(cpfNorm);
            }
            if (p.getNomePaciente() != null && !p.getNomePaciente().isBlank()) {
                nomesExistentes.add(p.getNomePaciente().trim().toLowerCase());
            }
        }

        List<FichaAtendimentoCentral> fichas = fichaRepository.findAll(
                Sort.by(Sort.Direction.DESC, "sincronizadoEm")
        );
        for (FichaAtendimentoCentral f : fichas) {
            String cpfNorm = normalizarCpf(f.getDocumentoPaciente());
            String nome = (f.getNomePaciente() != null && !f.getNomePaciente().isBlank())
                    ? f.getNomePaciente().trim()
                    : null;

            if (nome == null) {
                continue;
            }

            // Se possui CPF, verifica duplicidade apenas pelo CPF (preserva homônimos com CPFs diferentes)
            boolean jaExiste = (cpfNorm != null)
                    ? cpfsExistentes.contains(cpfNorm)
                    : nomesExistentes.contains(nome.toLowerCase());

            if (!jaExiste) {
                resultado.add(ProntuarioCentral.builder()
                        .id(UUID.randomUUID())
                        .nomePaciente(nome)
                        .documentoPaciente(cpfNorm)
                        .dataCriacao(f.getDataHoraAtendimento())
                        .updatedAt(f.getSincronizadoEm())
                        .build());

                if (cpfNorm != null) {
                    cpfsExistentes.add(cpfNorm);
                }
                nomesExistentes.add(nome.toLowerCase());
            }
        }

        return resultado;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProntuarioCentral> buscarPorId(@PathVariable UUID id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ProntuarioCentral> salvar(@RequestBody ProntuarioCentral prontuario) {
        LocalDateTime agora = LocalDateTime.now();

        String cpfLimpo = normalizarCpf(prontuario.getDocumentoPaciente());
        prontuario.setDocumentoPaciente(cpfLimpo);

        ProntuarioCentral existente = null;

        if (prontuario.getId() != null) {
            existente = repository.findById(prontuario.getId()).orElse(null);
        }
        if (existente == null && cpfLimpo != null) {
            existente = repository.findFirstByDocumentoPaciente(cpfLimpo).orElse(null);
        }
        if (existente == null && prontuario.getNomePaciente() != null && !prontuario.getNomePaciente().isBlank()) {
            ProntuarioCentral porNome = repository.findFirstByNomePacienteIgnoreCase(prontuario.getNomePaciente().trim()).orElse(null);
            // Só vincula por nome se o registro existente ainda não tiver CPF
            if (porNome != null && (porNome.getDocumentoPaciente() == null || porNome.getDocumentoPaciente().isBlank())) {
                existente = porNome;
            }
        }

        if (existente != null) {
            if (prontuario.getNomePaciente() != null && !prontuario.getNomePaciente().isBlank()) {
                existente.setNomePaciente(prontuario.getNomePaciente().trim());
            }
            if (cpfLimpo != null) {
                existente.setDocumentoPaciente(cpfLimpo);
            }
            if (prontuario.getDataNascimentoPaciente() != null) {
                existente.setDataNascimentoPaciente(prontuario.getDataNascimentoPaciente());
            }
            if (prontuario.getTipoSanguineo() != null && !prontuario.getTipoSanguineo().isBlank()) {
                existente.setTipoSanguineo(prontuario.getTipoSanguineo().trim());
            }
            if (prontuario.getAlergias() != null && !prontuario.getAlergias().isBlank()) {
                existente.setAlergias(prontuario.getAlergias().trim());
            }
            if (prontuario.getComorbidades() != null && !prontuario.getComorbidades().isBlank()) {
                existente.setComorbidades(prontuario.getComorbidades().trim());
            }
            if (prontuario.getMedicamentosUsoContinuo() != null && !prontuario.getMedicamentosUsoContinuo().isBlank()) {
                existente.setMedicamentosUsoContinuo(prontuario.getMedicamentosUsoContinuo().trim());
            }
            existente.setUpdatedAt(agora);
            return ResponseEntity.ok(repository.save(existente));
        }

        if (prontuario.getId() == null) {
            prontuario.setId(UUID.randomUUID());
        }
        if (prontuario.getDataCriacao() == null) {
            prontuario.setDataCriacao(agora);
        }
        prontuario.setUpdatedAt(agora);
        return ResponseEntity.ok(repository.save(prontuario));
    }

    private String normalizarCpf(String cpf) {
        if (cpf == null || cpf.isBlank()) {
            return null;
        }
        String digits = cpf.replaceAll("\\D", "");
        return digits.isEmpty() ? cpf.trim() : digits;
    }
}