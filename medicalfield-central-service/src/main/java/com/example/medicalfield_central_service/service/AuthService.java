package com.example.medicalfield_central_service.service;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.medicalfield_central_service.dto.LoginRequest;
import com.example.medicalfield_central_service.dto.LoginResponse;
import com.example.medicalfield_central_service.model.ProfissionalSaude;
import com.example.medicalfield_central_service.repository.ProfissionalSaudeRepository;
import com.example.medicalfield_central_service.security.JwtTokenProvider;

@Service
public class AuthService {

    private final ProfissionalSaudeRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(ProfissionalSaudeRepository repository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    public LoginResponse authenticate(LoginRequest request) {
        ProfissionalSaude profissional = repository.findByRegistroProfissional(request.getRegistroProfissional())
                .orElseThrow(() -> new BadCredentialsException("Credenciais inválidas."));

        if (!passwordEncoder.matches(request.getSenha(), profissional.getSenha())) {
            throw new BadCredentialsException("Credenciais inválidas.");
        }

        String token = tokenProvider.generateToken(profissional);

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .id(profissional.getId())
                .nome(profissional.getNome())
                .registroProfissional(profissional.getRegistroProfissional())
                .tipoProfissional(profissional.getTipoProfissional())
                .build();
    }
}
