package com.example.medicalfield_local_service.service;

import com.example.medicalfield_local_service.model.*;
import com.example.medicalfield_local_service.repository.FichaAtendimentoRepository;
import com.example.medicalfield_local_service.repository.MedicamentoRepository;
import com.example.medicalfield_local_service.repository.OutboxSyncRepository;
import com.example.medicalfield_local_service.repository.PacienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AtendimentoLocalService {

    private final FichaAtendimentoRepository atendimentoRepository;
    private final OutboxSyncRepository outboxRepository;
    private final MedicamentoRepository medicamentoRepository;
    private final PacienteRepository pacienteRepository;

    public AtendimentoLocalService(FichaAtendimentoRepository atendimentoRepository,
                                 OutboxSyncRepository outboxRepository,
                                 MedicamentoRepository medicamentoRepository,
                                 PacienteRepository pacienteRepository) {
        this.atendimentoRepository = atendimentoRepository;
        this.outboxRepository = outboxRepository;
        this.medicamentoRepository = medicamentoRepository;
        this.pacienteRepository = pacienteRepository;
    }

    @Transactional
    public FichaAtendimento salvarFichaLocal(FichaAtendimento ficha,
                                             String registroProfissional,
                                             LocalDate dataNascimentoPaciente,
                                             List<UUID> medicamentoIds,
                                             List<Double> medicamentoQtds) {
        if (ficha.getId() == null) {
            ficha.setId(UUID.randomUUID());
        }

        // 1. Normaliza CPF e vincula ou cria o Paciente localmente
        String cpfLimpo = normalizarCpf(ficha.getDocumentoPaciente());
        ficha.setDocumentoPaciente(cpfLimpo);

        if (ficha.getPaciente() != null && ficha.getPaciente().getId() != null) {
            Paciente pacienteExistente = pacienteRepository.findById(ficha.getPaciente().getId()).orElse(null);
            if (pacienteExistente != null) {
                if (pacienteExistente.getCpf() == null && cpfLimpo != null) {
                    pacienteExistente.setCpf(cpfLimpo);
                    pacienteRepository.save(pacienteExistente);
                }
                ficha.setPaciente(pacienteExistente);
            }
        } else {
            Paciente pacienteResolvido = null;
            if (cpfLimpo != null) {
                pacienteResolvido = pacienteRepository.findByCpf(cpfLimpo).orElse(null);
            }
            if (pacienteResolvido == null && ficha.getNomePaciente() != null && !ficha.getNomePaciente().isBlank()) {
                pacienteResolvido = pacienteRepository.save(Paciente.builder()
                        .nome(ficha.getNomePaciente().trim())
                        .cpf(cpfLimpo)
                        .dataNascimento(dataNascimentoPaciente)
                        .build());
            }
            ficha.setPaciente(pacienteResolvido);
        }

        // 2. Formata os medicamentos administrados com volume em ml (sem baixa de estoque)
        List<String> medicamentosUsados = new ArrayList<>();
        if (medicamentoIds != null && !medicamentoIds.isEmpty()) {
            for (int i = 0; i < medicamentoIds.size(); i++) {
                UUID medId = medicamentoIds.get(i);
                if (medId == null) continue;

                Double volume = (medicamentoQtds != null && i < medicamentoQtds.size() && medicamentoQtds.get(i) != null)
                        ? medicamentoQtds.get(i) : 1.0;

                String volumeFormatado = (volume % 1 == 0)
                        ? String.valueOf(volume.intValue())
                        : String.valueOf(volume);

                medicamentoRepository.findById(medId).ifPresent(med -> {
                    String descricao = med.getNome()
                            + (med.getDosagem() != null && !med.getDosagem().isBlank() ? " " + med.getDosagem() : "")
                            + " (" + volumeFormatado + " ml)";
                    medicamentosUsados.add(descricao);
                });
            }
        }

        ficha.setMedicamentos(!medicamentosUsados.isEmpty()
                ? String.join(", ", medicamentosUsados)
                : "Nenhum medicamento administrado");

        // 3. Calcula a Escala de Glasgow
        calcularClassificacaoGlasgow(ficha);

        LocalDateTime agora = LocalDateTime.now();
        if (ficha.getDataHoraAtendimento() == null) {
            ficha.setDataHoraAtendimento(agora);
        }
        ficha.setUpdatedAt(agora);
        ficha.setProfissionalRegistro(registroProfissional);
        ficha.setStatusSync(StatusSync.PENDING);

        FichaAtendimento salva = atendimentoRepository.save(ficha);

        OutboxSync outbox = OutboxSync.builder()
                .atendimentoId(salva.getId())
                .criadoEm(agora)
                .processado(false)
                .build();

        outboxRepository.save(outbox);

        return salva;
    }

    private String normalizarCpf(String cpf) {
        if (cpf == null) return null;
        String digits = cpf.replaceAll("\\D", "");
        return digits.isEmpty() ? null : digits;
    }

    private void calcularClassificacaoGlasgow(FichaAtendimento ficha) {
        int ocular = ficha.getAberturaOcular() != null ? ficha.getAberturaOcular() : 4;
        int verbal = ficha.getRespostaVerbal() != null ? ficha.getRespostaVerbal() : 5;
        int motora = ficha.getRespostaMotora() != null ? ficha.getRespostaMotora() : 6;
        int pupilar = ficha.getReatividadePupilar() != null ? ficha.getReatividadePupilar() : 0;

        int total = ocular + verbal + motora + pupilar;
        ficha.setEscalaGlasgow(total);

        if (total >= 13) {
            ficha.setClassificacaoGlasgow("LEVE");
        } else if (total >= 9) {
            ficha.setClassificacaoGlasgow("MODERADA");
        } else if (total >= 3) {
            ficha.setClassificacaoGlasgow("GRAVE");
        } else {
            ficha.setClassificacaoGlasgow("COMA");
        }
    }

    public List<FichaAtendimento> listarTodas() {
        return atendimentoRepository.findAll();
    }
}