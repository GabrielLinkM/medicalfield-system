package com.example.medicalfield_local_service.simulator;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NetworkStatusChangedEvent {
    private final boolean online;
}