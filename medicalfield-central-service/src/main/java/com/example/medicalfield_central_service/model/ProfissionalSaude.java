package com.example.medicalfield_central_service.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "profissional_saude")
public class ProfissionalSaude {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nome;

    @Column(name = "registro_profissional", nullable = false, unique = true)
    private String registroProfissional;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_profissional", nullable = false)
    private TipoProfissional tipoProfissional;

    @Column(nullable = false)
    private String senha;

    public ProfissionalSaude() {
    }

    public ProfissionalSaude(UUID id, String nome, String registroProfissional, TipoProfissional tipoProfissional, String senha) {
        this.id = id;
        this.nome = nome;
        this.registroProfissional = registroProfissional;
        this.tipoProfissional = tipoProfissional;
        this.senha = senha;
    }

    // Pattern Builder manual
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private String nome;
        private String registroProfissional;
        private TipoProfissional tipoProfissional;
        private String senha;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder nome(String nome) { this.nome = nome; return this; }
        public Builder registroProfissional(String registroProfissional) { this.registroProfissional = registroProfissional; return this; }
        public Builder tipoProfissional(TipoProfissional tipoProfissional) { this.tipoProfissional = tipoProfissional; return this; }
        public Builder senha(String senha) { this.senha = senha; return this; }

        public ProfissionalSaude build() {
            return new ProfissionalSaude(id, nome, registroProfissional, tipoProfissional, senha);
        }
    }

    // Getters e Setters manuais
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getRegistroProfissional() { return registroProfissional; }
    public void setRegistroProfissional(String registroProfissional) { this.registroProfissional = registroProfissional; }

    public TipoProfissional getTipoProfissional() { return tipoProfissional; }
    public void setTipoProfissional(TipoProfissional tipoProfissional) { this.tipoProfissional = tipoProfissional; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
}