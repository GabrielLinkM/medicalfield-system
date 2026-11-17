package com.example.medicalfield_local_service.simulator;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class NetworkSimulator {

    private final ApplicationEventPublisher publisher;
    private boolean online = true;

    public NetworkSimulator(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public boolean isOnline() {
        return online;
    }

    public synchronized void setOnline(boolean online) {
        if (this.online != online) {
            this.online = online;
            publisher.publishEvent(new NetworkStatusChangedEvent(online));
        }
    }
}