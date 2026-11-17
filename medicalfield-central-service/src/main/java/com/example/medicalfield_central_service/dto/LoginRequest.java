package com.example.medicalfield_central_service.dto;

import jakarta.validation.constraints.NotBlank;

public class LoginRequest {

    @NotBlank(message = "O registro profissional (CRM/COREN/ID) é obrigatório.")
    private String registroProfissional;

    @NotBlank(message = "A senha é obrigatória.")
    private String senha;

    public LoginRequest() {
    }

    public LoginRequest(String registroProfissional, String senha) {
        this.registroProfissional = registroProfissional;
        this.senha = senha;
    }

    public String getRegistroProfissional() {
        return registroProfissional;
    }

    public void setRegistroProfissional(String registroProfissional) {
        this.registroProfissional = registroProfissional;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }
}