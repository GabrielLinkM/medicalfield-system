package com.example.medicalfield_local_service.config;

import com.example.medicalfield_local_service.model.*;
import com.example.medicalfield_local_service.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class PacienteDataInitializer implements CommandLineRunner {

    private final PacienteRepository pacienteRepository;
    private final FichaAtendimentoRepository fichaRepository;

    public PacienteDataInitializer(PacienteRepository pacienteRepository, FichaAtendimentoRepository fichaRepository) {
        this.pacienteRepository = pacienteRepository;
        this.fichaRepository = fichaRepository;
    }

    @Override
    public void run(String... args) {
        if (pacienteRepository.count() == 0) {
            // Paciente 1
            Paciente p1 = pacienteRepository.save(Paciente.builder()
                    .nome("João Carlos da Silva")
                    .cpf("12345678901")
                    .dataNascimento(LocalDate.of(1985, 5, 12))
                    .build());

            // Paciente 2
            Paciente p2 = pacienteRepository.save(Paciente.builder()
                    .nome("Maria Aparecida Souza")
                    .cpf("98765432100")
                    .dataNascimento(LocalDate.of(1962, 11, 28))
                    .build());

            // Paciente 3
            Paciente p3 = pacienteRepository.save(Paciente.builder()
                    .nome("Lucas Oliveira Santos")
                    .cpf("45678912344")
                    .dataNascimento(LocalDate.of(2001, 3, 15))
                    .build());

            // Histórico antigo para João Carlos
            fichaRepository.save(FichaAtendimento.builder()
                    .id(java.util.UUID.randomUUID())
                    .paciente(p1)
                    .nomePaciente(p1.getNome())
                    .documentoPaciente(p1.getCpf())
                    .pressaoArterial("140x90")
                    .frequenciaCardiaca(95)
                    .escalaGlasgow(15)
                    .classificacaoGlasgow("LEVE")
                    .protocoloManchester("AMARELO")
                    .protocoloStart("AMARELO")
                    .medicamentos("Soro Fisiológico 0.9% (1x)")
                    .observacoes("Atendimento anterior por pico hipertensivo em via pública.")
                    .profissionalRegistro("CRM12345")
                    .dataHoraAtendimento(LocalDateTime.now().minusDays(15))
                    .updatedAt(LocalDateTime.now().minusDays(15))
                    .statusSync(StatusSync.SYNCED)
                    .build());

            System.out.println(">>> [PACIENTES INITIALIZER] Pacientes e históricos de teste cadastrados com sucesso!");
        }
    }
}