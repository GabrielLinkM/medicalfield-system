package com.example.medicalfield_central_service.service;

import com.example.medicalfield_central_service.model.ProfissionalSaude;
import com.example.medicalfield_central_service.repository.ProfissionalSaudeRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class ProfissionalUserDetailsService implements UserDetailsService {

    private final ProfissionalSaudeRepository repository;

    public ProfissionalUserDetailsService(ProfissionalSaudeRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String registroProfissional) throws UsernameNotFoundException {
        ProfissionalSaude medico = repository.findByRegistroProfissional(registroProfissional.trim())
                .orElseThrow(() -> new UsernameNotFoundException("Médico não encontrado: " + registroProfissional));

        return User.builder()
                .username(medico.getRegistroProfissional())
                .password(medico.getSenha())
                .roles("MEDICO")
                .build();
    }
}