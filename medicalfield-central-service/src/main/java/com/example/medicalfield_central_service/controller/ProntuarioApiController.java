package com.example.medicalfield_central_service.controller;

import com.example.medicalfield_central_service.model.ProntuarioCentral;
import com.example.medicalfield_central_service.repository.ProntuarioCentralRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/prontuario", "/api/v1/prontuarios"})
public class ProntuarioApiController {

    private final ProntuarioCentralRepository repository;

    public ProntuarioApiController(ProntuarioCentralRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<ProntuarioCentral> listarTodos() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "updatedAt"));
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