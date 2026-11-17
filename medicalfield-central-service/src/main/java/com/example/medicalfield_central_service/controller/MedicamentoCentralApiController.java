package com.example.medicalfield_central_service.controller;

import com.example.medicalfield_central_service.model.MedicamentoCentral;
import com.example.medicalfield_central_service.repository.MedicamentoCentralRepository;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/medicamentos")
public class MedicamentoCentralApiController {

    private final MedicamentoCentralRepository repository;

    public MedicamentoCentralApiController(MedicamentoCentralRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<MedicamentoCentral> listarCatalogo() {
        return repository.findAll(Sort.by("nome"));
    }
}