package com.example.medicalfield_central_service.dto;

import com.example.medicalfield_central_service.model.TipoProfissional;
import java.util.UUID;

public class LoginResponse {

    private String token;
    private String tokenType;
    private UUID id;
    private String nome;
    private String registroProfissional;
    private TipoProfissional tipoProfissional;

    public LoginResponse() {
    }

    public LoginResponse(String token, String tokenType, UUID id, String nome, String registroProfissional, TipoProfissional tipoProfissional) {
        this.token = token;
        this.tokenType = tokenType;
        this.id = id;
        this.nome = nome;
        this.registroProfissional = registroProfissional;
        this.tipoProfissional = tipoProfissional;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String token;
        private String tokenType;
        private UUID id;
        private String nome;
        private String registroProfissional;
        private TipoProfissional tipoProfissional;

        public Builder token(String token) { this.token = token; return this; }
        public Builder tokenType(String tokenType) { this.tokenType = tokenType; return this; }
        public Builder id(UUID id) { this.id = id; return this; }
        public Builder nome(String nome) { this.nome = nome; return this; }
        public Builder registroProfissional(String registroProfissional) { this.registroProfissional = registroProfissional; return this; }
        public Builder tipoProfissional(TipoProfissional tipoProfissional) { this.tipoProfissional = tipoProfissional; return this; }

        public LoginResponse build() {
            return new LoginResponse(token, tokenType, id, nome, registroProfissional, tipoProfissional);
        }
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getRegistroProfissional() { return registroProfissional; }
    public void setRegistroProfissional(String registroProfissional) { this.registroProfissional = registroProfissional; }

    public TipoProfissional getTipoProfissional() { return tipoProfissional; }
    public void setTipoProfissional(TipoProfissional tipoProfissional) { this.tipoProfissional = tipoProfissional; }
}