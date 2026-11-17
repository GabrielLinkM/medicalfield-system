package com.example.medicalfield_local_service.service;

import com.example.medicalfield_local_service.model.Medicamento;
import com.example.medicalfield_local_service.repository.MedicamentoRepository;
import com.example.medicalfield_local_service.simulator.NetworkSimulator;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class CatalogoMedicamentoSyncService {

    private final MedicamentoRepository medicamentoRepository;
    private final NetworkSimulator networkSimulator;
    private final RestClient restClient;

    @Value("${central.medicamentos.url:http://177.44.248.70:8081/api/v1/medicamentos}")
    private String centralMedicamentosUrl;

    public CatalogoMedicamentoSyncService(MedicamentoRepository medicamentoRepository,
                                          NetworkSimulator networkSimulator) {
        this.medicamentoRepository = medicamentoRepository;
        this.networkSimulator = networkSimulator;
        this.restClient = RestClient.create();
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void sincronizarCatalogoDaNuvem() {
        if (!networkSimulator.isOnline()) {
            System.out.println(">>> [CATALOGO] Simulador OFFLINE; mantendo medicamentos em cache local.");
            return;
        }

        try {
            MedicamentoCentralDto[] listaNuvem = restClient.get()
                    .uri(centralMedicamentosUrl)
                    .retrieve()
                    .body(MedicamentoCentralDto[].class);

            if (listaNuvem != null) {
                medicamentoRepository.deleteAllInBatch();
                medicamentoRepository.flush();

                List<Medicamento> locais = new ArrayList<>();
                for (MedicamentoCentralDto dto : listaNuvem) {
                    if (dto.getNome() == null || dto.getNome().isBlank()) continue;
                    Medicamento med = new Medicamento();
                    med.setId(dto.getId() != null ? dto.getId() : UUID.randomUUID());
                    med.setNome(dto.getNome());
                    med.setDosagem(dto.getDosagem());
                    locais.add(med);
                }

                medicamentoRepository.saveAllAndFlush(locais);
                System.out.println(">>> [CATALOGO] Persistidos no banco local: " + medicamentoRepository.count() + " medicamento(s).");
            }
        } catch (Exception e) {
            System.err.println(">>> [CATALOGO] Falha ao sincronizar medicamentos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Data
    public static class MedicamentoCentralDto {
        private UUID id;
        private String nome;
        private String dosagem;
    }
}