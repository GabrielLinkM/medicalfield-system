package com.example.medicalfield_central_service.config;

import com.example.medicalfield_central_service.model.ProfissionalSaude;
import com.example.medicalfield_central_service.model.TipoProfissional;
import com.example.medicalfield_central_service.repository.ProfissionalSaudeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final ProfissionalSaudeRepository repository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(ProfissionalSaudeRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (repository.count() == 0) {
            String senhaPadrao = passwordEncoder.encode("senha123");

            ProfissionalSaude medico = ProfissionalSaude.builder()
                    .nome("Dra. Maria Silva")
                    .registroProfissional("CRM12345")
                    .tipoProfissional(TipoProfissional.MEDICO)
                    .senha(senhaPadrao)
                    .build();

            ProfissionalSaude enfermeiro = ProfissionalSaude.builder()
                    .nome("Carlos Enfr.")
                    .registroProfissional("COREN98765")
                    .tipoProfissional(TipoProfissional.ENFERMEIRO)
                    .senha(senhaPadrao)
                    .build();

            ProfissionalSaude socorrista = ProfissionalSaude.builder()
                    .nome("Socorrista Roberto")
                    .registroProfissional("SOC55443")
                    .tipoProfissional(TipoProfissional.SOCORRISTA)
                    .senha(senhaPadrao)
                    .build();

            repository.save(medico);
            repository.save(enfermeiro);
            repository.save(socorrista);

            System.out.println("=================================================");
            System.out.println(">>> [DATA INITIALIZER] 3 Usuários de teste cadastrados com sucesso! <<<");
            System.out.println("=================================================");
        } else {
            System.out.println(">>> [DATA INITIALIZER] O banco já possui " + repository.count() + " registro(s).");
        }
    }
}