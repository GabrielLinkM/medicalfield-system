package com.example.medicalfield_local_service.service;

import com.example.medicalfield_local_service.dto.LoginRequestDto;
import com.example.medicalfield_local_service.dto.LoginResponseDto;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class CentralAuthClientService {

    private final RestTemplate restTemplate;

    @Value("${medicalfield.central-api.url}")
    private String centralApiUrl;

    public CentralAuthClientService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean autenticarEIniciarSessao(String registroProfissional, String senha, HttpSession session) {
        try {
            String url = centralApiUrl + "/auth/login";
            LoginRequestDto request = new LoginRequestDto(registroProfissional, senha);

            LoginResponseDto response = restTemplate.postForObject(url, request, LoginResponseDto.class);

            if (response != null && response.getToken() != null) {
                // Guarda o profissional autenticado na sessão HTTP local do navegador
                session.setAttribute("usuarioLogado", response);
                return true;
            }
        } catch (Exception e) {
            System.err.println(">>> Erro ao conectar ao Serviço Central para Login: " + e.getMessage());
        }
        return false;
    }
}