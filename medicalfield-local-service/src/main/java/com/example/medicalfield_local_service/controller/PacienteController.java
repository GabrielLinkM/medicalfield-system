package com.example.medicalfield_local_service.controller;

import com.example.medicalfield_local_service.dto.LoginResponseDto;
import com.example.medicalfield_local_service.model.FichaAtendimento;
import com.example.medicalfield_local_service.model.Paciente;
import com.example.medicalfield_local_service.repository.FichaAtendimentoRepository;
import com.example.medicalfield_local_service.repository.PacienteRepository;
import com.example.medicalfield_local_service.simulator.NetworkSimulator;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/pacientes")
public class PacienteController {

    private final PacienteRepository pacienteRepository;
    private final FichaAtendimentoRepository fichaRepository;
    private final NetworkSimulator networkSimulator;

    public PacienteController(PacienteRepository pacienteRepository,
                              FichaAtendimentoRepository fichaRepository,
                              NetworkSimulator networkSimulator) {
        this.pacienteRepository = pacienteRepository;
        this.fichaRepository = fichaRepository;
        this.networkSimulator = networkSimulator;
    }

    @GetMapping
    public String listarPacientes(HttpSession session, Model model) {
        LoginResponseDto usuarioLogado = (LoginResponseDto) session.getAttribute("usuarioLogado");
        if (usuarioLogado == null) {
            return "redirect:/login";
        }

        List<Paciente> pacientes = pacienteRepository.findAll();

        Map<UUID, Object> historicos = new HashMap<>();
        for (Paciente p : pacientes) {
            historicos.put(p.getId(), fichaRepository.findByPacienteIdOrderByDataHoraAtendimentoDesc(p.getId()));
        }

        // Busca os atendimentos realizados no dia de hoje
        LocalDate hoje = LocalDate.now();
        LocalDateTime inicioDia = hoje.atStartOfDay();
        LocalDateTime fimDia = hoje.atTime(LocalTime.MAX);
        List<FichaAtendimento> atendimentosHoje = fichaRepository
                .findByDataHoraAtendimentoBetweenOrderByDataHoraAtendimentoDesc(inicioDia, fimDia);

        model.addAttribute("usuario", usuarioLogado);
        model.addAttribute("isOnline", networkSimulator.isOnline());
        model.addAttribute("pacientes", pacientes);
        model.addAttribute("historicos", historicos);
        model.addAttribute("atendimentosHoje", atendimentosHoje);

        return "pacientes-list";
    }

    @PostMapping("/salvar")
    public String cadastrarPacienteRapido(@RequestParam String nome,
                                          @RequestParam(required = false) String cpf,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataNascimento,
                                          @RequestParam(defaultValue = "false") boolean iniciarAtendimento,
                                          HttpSession session,
                                          RedirectAttributes redirectAttributes) {
        LoginResponseDto usuarioLogado = (LoginResponseDto) session.getAttribute("usuarioLogado");
        if (usuarioLogado == null) {
            return "redirect:/login";
        }

        String cpfLimpo = (cpf != null && !cpf.replaceAll("\\D", "").isEmpty())
                ? cpf.replaceAll("\\D", "")
                : null;

        if (cpfLimpo != null && pacienteRepository.findByCpf(cpfLimpo).isPresent()) {
            redirectAttributes.addFlashAttribute("erroPaciente", "Já existe um paciente cadastrado com o CPF informado.");
            return "redirect:/pacientes";
        }

        Paciente novo = pacienteRepository.save(Paciente.builder()
                .nome(nome.trim())
                .cpf(cpfLimpo)
                .dataNascimento(dataNascimento)
                .build());

        if (iniciarAtendimento) {
            return "redirect:/atendimento/novo?pacienteId=" + novo.getId();
        }

        redirectAttributes.addFlashAttribute("sucessoPaciente", "Paciente cadastrado com sucesso!");
        return "redirect:/pacientes";
    }
}