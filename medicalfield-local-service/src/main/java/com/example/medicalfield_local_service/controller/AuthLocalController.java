package com.example.medicalfield_local_service.controller;

import com.example.medicalfield_local_service.service.CentralAuthClientService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthLocalController {

    private final CentralAuthClientService centralAuthClientService;

    public AuthLocalController(CentralAuthClientService centralAuthClientService) {
        this.centralAuthClientService = centralAuthClientService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String processLogin(@RequestParam String registroProfissional,
                               @RequestParam String senha,
                               HttpSession session,
                               Model model) {

        boolean sucesso = centralAuthClientService.autenticarEIniciarSessao(registroProfissional, senha, session);

        if (sucesso) {
            // CORRIGIDO: Redireciona para a ROTA do PacienteController, que carregará o HTML 'pacientes-list.html'
            return "redirect:/pacientes";
        }

        model.addAttribute("erro", "Credenciais inválidas ou Serviço Central indisponível para o primeiro login.");
        return "login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}