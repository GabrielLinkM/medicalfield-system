package com.example.medicalfield_local_service.service;

import com.example.medicalfield_local_service.model.OutboxSync;
import com.example.medicalfield_local_service.model.StatusSync;
import com.example.medicalfield_local_service.repository.FichaAtendimentoRepository;
import com.example.medicalfield_local_service.repository.OutboxSyncRepository;
import com.example.medicalfield_local_service.simulator.NetworkSimulator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class OutboxSyncService {

    private final OutboxSyncRepository outboxRepository;
    private final FichaAtendimentoRepository atendimentoRepository;
    private final NetworkSimulator networkSimulator;
    private final RestTemplate restTemplate;
    private final CatalogoMedicamentoSyncService catalogoMedicamentoSyncService;

    @Value("${medicalfield.central-api.url}")
    private String centralApiUrl;

    public OutboxSyncService(OutboxSyncRepository outboxRepository,
                             FichaAtendimentoRepository atendimentoRepository,
                             NetworkSimulator networkSimulator,
                             RestTemplate restTemplate,
                             CatalogoMedicamentoSyncService catalogoMedicamentoSyncService) {
        this.outboxRepository = outboxRepository;
        this.atendimentoRepository = atendimentoRepository;
        this.networkSimulator = networkSimulator;
        this.restTemplate = restTemplate;
        this.catalogoMedicamentoSyncService = catalogoMedicamentoSyncService;
    }

    public void processarFilaPendente() {
        if (!networkSimulator.isOnline()) {
            System.out.println(">>> [OUTBOX] Conexão SIMULADA como OFFLINE. Sincronização pausada.");
            return;
        }

        // Atualiza o catálogo de medicamentos da Central sempre que estiver online
        catalogoMedicamentoSyncService.sincronizarCatalogoDaNuvem();

        List<OutboxSync> pendentes = outboxRepository.findByProcessadoFalse();
        if (pendentes.isEmpty()) {
            return;
        }

        System.out.println(">>> [OUTBOX] Processando " + pendentes.size() + " ficha(s) pendente(s) para o Servidor Central...");

        for (OutboxSync outbox : pendentes) {
            atendimentoRepository.findById(outbox.getAtendimentoId()).ifPresent(ficha -> {
                try {
                    String url = centralApiUrl + "/atendimentos/sync";
                    restTemplate.postForLocation(url, ficha);

                    ficha.setStatusSync(StatusSync.SYNCED);
                    atendimentoRepository.save(ficha);

                    outbox.setProcessado(true);
                    outboxRepository.save(outbox);

                    System.out.println(">>> [OUTBOX] Ficha ID " + ficha.getId() + " sincronizada com SUCESSO!");
                } catch (Exception e) {
                    System.err.println(">>> [OUTBOX] Falha ao enviar ficha " + ficha.getId() + ": " + e.getMessage());
                }
            });
        }
    }
}