package com.example.medicalfield_local_service.dto;

import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponseDto {
    private String token;
    private String tokenType;
    private UUID id;
    private String nome;
    private String registroProfissional;
    private String tipoProfissional;
}