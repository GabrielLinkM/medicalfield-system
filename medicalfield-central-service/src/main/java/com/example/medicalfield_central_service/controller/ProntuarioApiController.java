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

        Set<String> chavesExistentes = new HashSet<>();
        for (ProntuarioCentral p : resultado) {
            if (p.getDocumentoPaciente() != null && !p.getDocumentoPaciente().isBlank()) {
                chavesExistentes.add(p.getDocumentoPaciente().trim());
            }
            if (p.getNomePaciente() != null && !p.getNomePaciente().isBlank()) {
                chavesExistentes.add(p.getNomePaciente().trim().toLowerCase());
            }
        }

        List<FichaAtendimentoCentral> fichas = fichaRepository.findAll(
                Sort.by(Sort.Direction.DESC, "sincronizadoEm")
        );
        for (FichaAtendimentoCentral f : fichas) {
            String cpf = (f.getDocumentoPaciente() != null && !f.getDocumentoPaciente().isBlank())
                    ? f.getDocumentoPaciente().trim()
                    : null;
            String nome = (f.getNomePaciente() != null && !f.getNomePaciente().isBlank())
                    ? f.getNomePaciente().trim()
                    : null;

            if (nome == null) continue;

            boolean jaExiste = (cpf != null && chavesExistentes.contains(cpf))
                    || chavesExistentes.contains(nome.toLowerCase());

            if (!jaExiste) {
                resultado.add(ProntuarioCentral.builder()
                        .id(UUID.randomUUID())
                        .nomePaciente(nome)
                        .documentoPaciente(cpf)
                        .dataCriacao(f.getDataHoraAtendimento())
                        .updatedAt(f.getSincronizadoEm())
                        .build());

                if (cpf != null) chavesExistentes.add(cpf);
                chavesExistentes.add(nome.toLowerCase());
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
        if (prontuario.getId() == null) {
            prontuario.setId(UUID.randomUUID());
        }
        if (prontuario.getDataCriacao() == null) {
            prontuario.setDataCriacao(agora);
        }
        prontuario.setUpdatedAt(agora);
        return ResponseEntity.ok(repository.save(prontuario));
    }
}