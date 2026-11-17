package com.example.medicalfield_local_service.controller;

import com.example.medicalfield_local_service.dto.LoginResponseDto;
import com.example.medicalfield_local_service.model.FichaAtendimento;
import com.example.medicalfield_local_service.model.Medicamento;
import com.example.medicalfield_local_service.model.Prontuario;
import com.example.medicalfield_local_service.repository.MedicamentoRepository;
import com.example.medicalfield_local_service.repository.PacienteRepository;
import com.example.medicalfield_local_service.repository.ProntuarioRepository;
import com.example.medicalfield_local_service.service.AtendimentoLocalService;
import com.example.medicalfield_local_service.service.CatalogoMedicamentoSyncService;
import com.example.medicalfield_local_service.service.OutboxSyncService;
import com.example.medicalfield_local_service.simulator.NetworkSimulator;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/atendimento")
public class AtendimentoController {

    private final AtendimentoLocalService atendimentoService;
    private final NetworkSimulator networkSimulator;
    private final OutboxSyncService outboxSyncService;
    private final MedicamentoRepository medicamentoRepository;
    private final PacienteRepository pacienteRepository;
    private final ProntuarioRepository prontuarioRepository;
    private final CatalogoMedicamentoSyncService catalogoMedicamentoSyncService;

    public AtendimentoController(AtendimentoLocalService atendimentoService,
                                 NetworkSimulator networkSimulator,
                                 OutboxSyncService outboxSyncService,
                                 MedicamentoRepository medicamentoRepository,
                                 PacienteRepository pacienteRepository,
                                 ProntuarioRepository prontuarioRepository,
                                 CatalogoMedicamentoSyncService catalogoMedicamentoSyncService) {
        this.atendimentoService = atendimentoService;
        this.networkSimulator = networkSimulator;
        this.outboxSyncService = outboxSyncService;
        this.medicamentoRepository = medicamentoRepository;
        this.pacienteRepository = pacienteRepository;
        this.prontuarioRepository = prontuarioRepository;
        this.catalogoMedicamentoSyncService = catalogoMedicamentoSyncService;
    }

    @GetMapping("/novo")
    public String formularioNovo(@RequestParam(required = false) UUID pacienteId, HttpSession session, Model model) {
        LoginResponseDto usuarioLogado = (LoginResponseDto) session.getAttribute("usuarioLogado");
        if (usuarioLogado == null) {
            return "redirect:/login";
        }

        catalogoMedicamentoSyncService.sincronizarCatalogoDaNuvem();

        FichaAtendimento ficha = new FichaAtendimento();
        ficha.setProntuario(new Prontuario());

        if (pacienteId != null) {
            pacienteRepository.findById(pacienteId).ifPresent(p -> {
                ficha.setPaciente(p);
                ficha.setNomePaciente(p.getNome());
                ficha.setDocumentoPaciente(p.getCpf());
                prontuarioRepository.findByPacienteId(p.getId())
                        .ifPresent(ficha::setProntuario);
            });
        }

        List<Medicamento> medicamentos = medicamentoRepository.findAll(Sort.by("nome"));

        model.addAttribute("ficha", ficha);
        model.addAttribute("usuario", usuarioLogado);
        model.addAttribute("isOnline", networkSimulator.isOnline());
        model.addAttribute("medicamentosEstoque", medicamentos);

        return "atendimento-form";
    }

    @PostMapping("/salvar")
    public String salvarFicha(@ModelAttribute FichaAtendimento ficha,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataNascimentoPaciente,
                              @RequestParam(required = false) List<UUID> medicamentoIds,
                              @RequestParam(required = false) List<Double> medicamentoQtds,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        LoginResponseDto usuarioLogado = (LoginResponseDto) session.getAttribute("usuarioLogado");
        if (usuarioLogado == null) {
            return "redirect:/login";
        }

        try {
            atendimentoService.salvarFichaLocal(
                    ficha,
                    usuarioLogado.getRegistroProfissional(),
                    dataNascimentoPaciente,
                    medicamentoIds,
                    medicamentoQtds
            );
            outboxSyncService.processarFilaPendente();
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("erroEstoque", "Erro ao salvar atendimento: " + e.getMessage());
            return "redirect:/atendimento/novo";
        }

        return "redirect:/pacientes";
    }

    @PostMapping("/simulador/toggle")
    public String toggleConectividade(@RequestParam(defaultValue = "false") boolean online,
                                      @RequestHeader(value = "Referer", required = false) String referer) {
        networkSimulator.setOnline(online);
        if (online) {
            outboxSyncService.processarFilaPendente();
        }
        return "redirect:" + (referer != null ? referer : "/pacientes");
    }
}