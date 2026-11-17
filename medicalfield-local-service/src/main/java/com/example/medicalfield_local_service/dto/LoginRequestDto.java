package com.example.medicalfield_local_service.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequestDto {
    private String registroProfissional;
    private String senha;
}