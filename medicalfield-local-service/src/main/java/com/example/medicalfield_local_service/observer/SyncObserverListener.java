package com.example.medicalfield_local_service.observer;

import com.example.medicalfield_local_service.service.OutboxSyncService;
import com.example.medicalfield_local_service.simulator.NetworkStatusChangedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class SyncObserverListener {

    private final OutboxSyncService outboxSyncService;

    public SyncObserverListener(OutboxSyncService outboxSyncService) {
        this.outboxSyncService = outboxSyncService;
    }

    @EventListener
    public void handleNetworkStatusChange(NetworkStatusChangedEvent event) {
        if (event.isOnline()) {
            System.out.println(">>> [OBSERVER] Conectividade RESTABELECIDA. Disparando sincronizador Outbox...");
            outboxSyncService.processarFilaPendente();
        } else {
            System.out.println(">>> [OBSERVER] Conectividade PERDIDA. Modo de Operação Local ATIVADO.");
        }
    }
}