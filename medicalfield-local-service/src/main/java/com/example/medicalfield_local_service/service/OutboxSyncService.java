package com.example.medicalfield_local_service.service;

import com.example.medicalfield_local_service.model.OutboxSync;
import com.example.medicalfield_local_service.model.Paciente;
import com.example.medicalfield_local_service.model.Prontuario;
import com.example.medicalfield_local_service.model.StatusSync;
import com.example.medicalfield_local_service.repository.FichaAtendimentoRepository;
import com.example.medicalfield_local_service.repository.OutboxSyncRepository;
import com.example.medicalfield_local_service.repository.PacienteRepository;
import com.example.medicalfield_local_service.repository.ProntuarioRepository;
import com.example.medicalfield_local_service.simulator.NetworkSimulator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class OutboxSyncService {

    private final OutboxSyncRepository outboxRepository;
    private final FichaAtendimentoRepository atendimentoRepository;
    private final PacienteRepository pacienteRepository;
    private final ProntuarioRepository prontuarioRepository;
    private final NetworkSimulator networkSimulator;
    private final RestTemplate restTemplate;
    private final CatalogoMedicamentoSyncService catalogoMedicamentoSyncService;

    @Value("${medicalfield.central-api.url}")
    private String centralApiUrl;

    public OutboxSyncService(OutboxSyncRepository outboxRepository,
                             FichaAtendimentoRepository atendimentoRepository,
                             PacienteRepository pacienteRepository,
                             ProntuarioRepository prontuarioRepository,
                             NetworkSimulator networkSimulator,
                             RestTemplate restTemplate,
                             CatalogoMedicamentoSyncService catalogoMedicamentoSyncService) {
        this.outboxRepository = outboxRepository;
        this.atendimentoRepository = atendimentoRepository;
        this.pacienteRepository = pacienteRepository;
        this.prontuarioRepository = prontuarioRepository;
        this.networkSimulator = networkSimulator;
        this.restTemplate = restTemplate;
        this.catalogoMedicamentoSyncService = catalogoMedicamentoSyncService;
    }

    public void processarFilaPendente() {
        sincronizarTudo();
    }

    public String sincronizarTudo() {
        if (!networkSimulator.isOnline()) {
            System.out.println(">>> [OUTBOX] Conexão SIMULADA como OFFLINE. Sincronização pausada.");
            return "Simulador em modo OFFLINE. Ative o modo ONLINE para sincronizar.";
        }

        // 1. Atualiza o catálogo de medicamentos da Central
        catalogoMedicamentoSyncService.sincronizarCatalogoDaNuvem();

        // 2. Sincroniza pacientes e prontuários (bidirecional)
        int[] resultadoPacientes = sincronizarPacientesComCentral();
        int enviadosCentral = resultadoPacientes[0];
        int recebidosCentral = resultadoPacientes[1];

        // 3. Envia fichas de atendimento pendentes na fila Outbox
        int fichasSincronizadas = 0;
        List<OutboxSync> pendentes = outboxRepository.findByProcessadoFalse();
        if (!pendentes.isEmpty()) {
            System.out.println(">>> [OUTBOX] Processando " + pendentes.size() + " ficha(s) pendente(s) para o Servidor Central...");

            for (OutboxSync outbox : pendentes) {
                var fichaOpt = atendimentoRepository.findById(outbox.getAtendimentoId());
                if (fichaOpt.isPresent()) {
                    var ficha = fichaOpt.get();
                    try {
                        String url = centralApiUrl + "/atendimentos/sync";
                        restTemplate.postForLocation(url, ficha);

                        ficha.setStatusSync(StatusSync.SYNCED);
                        atendimentoRepository.save(ficha);

                        outbox.setProcessado(true);
                        outboxRepository.save(outbox);
                        fichasSincronizadas++;

                        System.out.println(">>> [OUTBOX] Ficha ID " + ficha.getId() + " sincronizada com SUCESSO!");
                    } catch (Exception e) {
                        System.err.println(">>> [OUTBOX] Falha ao enviar ficha " + ficha.getId() + ": " + e.getMessage());
                    }
                }
            }
        }

        return String.format(
                "Sincronização concluída! Pacientes enviados: %d | Pacientes recebidos da Central: %d | Atendimentos enviados: %d",
                enviadosCentral, recebidosCentral, fichasSincronizadas
        );
    }

    @SuppressWarnings("unchecked")
    public int[] sincronizarPacientesComCentral() {
        int enviados = 0;
        int recebidos = 0;

        if (!networkSimulator.isOnline()) {
            return new int[]{0, 0};
        }

        String urlProntuarios = centralApiUrl + "/prontuarios";
        Map<String, Map<String, Object>> mapaCentralPorChave = new HashMap<>();

        // Passo A: Busca pacientes/prontuários da Central e importa para o banco local
        try {
            Map<String, Object>[] listaCentral = restTemplate.getForObject(urlProntuarios, Map[].class);
            List<Paciente> locaisAtuais = new ArrayList<>(pacienteRepository.findAll());

            if (listaCentral != null) {
                for (Map<String, Object> item : listaCentral) {
                    try {
                        String nomeCentral = item.get("nomePaciente") != null ? item.get("nomePaciente").toString().trim() : null;
                        String cpfCentral = normalizarCpf(item.get("documentoPaciente") != null ? item.get("documentoPaciente").toString() : null);

                        if (nomeCentral == null || nomeCentral.isBlank()) {
                            continue;
                        }

                        String chave = (cpfCentral != null) ? cpfCentral : nomeCentral.toLowerCase();
                        mapaCentralPorChave.put(chave, item);

                        Paciente pacienteLocal = localizarPacienteCompativel(locaisAtuais, nomeCentral, cpfCentral);

                        LocalDate dataNasc = null;
                        if (item.get("dataNascimentoPaciente") != null && !item.get("dataNascimentoPaciente").toString().isBlank()) {
                            try {
                                dataNasc = LocalDate.parse(item.get("dataNascimentoPaciente").toString().substring(0, 10));
                            } catch (Exception ignored) {
                            }
                        }

                        if (pacienteLocal == null) {
                            pacienteLocal = pacienteRepository.save(Paciente.builder()
                                    .nome(nomeCentral)
                                    .cpf(cpfCentral)
                                    .dataNascimento(dataNasc)
                                    .build());
                            locaisAtuais.add(pacienteLocal);
                            recebidos++;
                        } else {
                            boolean alterou = false;
                            if (pacienteLocal.getCpf() == null && cpfCentral != null) {
                                pacienteLocal.setCpf(cpfCentral);
                                alterou = true;
                            }
                            if (pacienteLocal.getDataNascimento() == null && dataNasc != null) {
                                pacienteLocal.setDataNascimento(dataNasc);
                                alterou = true;
                            }
                            if (alterou) {
                                pacienteRepository.save(pacienteLocal);
                            }
                        }

                        atualizarProntuarioLocalSeNecessario(pacienteLocal, item);
                    } catch (Exception itemEx) {
                        System.err.println(">>> [SYNC PACIENTES] Erro ao processar registro individual da Central: " + itemEx.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println(">>> [SYNC PACIENTES] Falha ao consultar pacientes na Central: " + e.getMessage());
        }

        // Passo B: Envia todos os pacientes locais (e seus prontuários) para a Central
        List<Paciente> pacientesLocais = pacienteRepository.findAll();
        String agoraIso = LocalDateTime.now().toString();

        for (Paciente p : pacientesLocais) {
            try {
                String cpfLimpo = normalizarCpf(p.getCpf());
                String chave = (cpfLimpo != null) ? cpfLimpo : p.getNome().trim().toLowerCase();
                Map<String, Object> existenteCentral = mapaCentralPorChave.get(chave);

                Optional<Prontuario> prontLocalOpt = prontuarioRepository.findByPacienteId(p.getId());

                Map<String, Object> payload = new HashMap<>();
                if (existenteCentral != null && existenteCentral.get("id") != null) {
                    payload.put("id", existenteCentral.get("id"));
                } else {
                    payload.put("id", prontLocalOpt.map(Prontuario::getId).orElse(p.getId()));
                }

                payload.put("nomePaciente", p.getNome());
                payload.put("documentoPaciente", cpfLimpo);
                payload.put("dataNascimentoPaciente", p.getDataNascimento() != null
                        ? p.getDataNascimento().toString()
                        : (existenteCentral != null ? existenteCentral.get("dataNascimentoPaciente") : null));

                payload.put("tipoSanguineo", escolherValor(
                        prontLocalOpt.map(Prontuario::getTipoSanguineo).orElse(null),
                        existenteCentral != null ? existenteCentral.get("tipoSanguineo") : null
                ));
                payload.put("alergias", escolherValor(
                        prontLocalOpt.map(Prontuario::getAlergias).orElse(null),
                        existenteCentral != null ? existenteCentral.get("alergias") : null
                ));
                payload.put("comorbidades", escolherValor(
                        prontLocalOpt.map(Prontuario::getComorbidades).orElse(null),
                        existenteCentral != null ? existenteCentral.get("comorbidades") : null
                ));
                payload.put("medicamentosUsoContinuo", escolherValor(
                        prontLocalOpt.map(Prontuario::getMedicamentosUsoContinuo).orElse(null),
                        existenteCentral != null ? existenteCentral.get("medicamentosUsoContinuo") : null
                ));

                payload.put("dataCriacao", (existenteCentral != null && existenteCentral.get("dataCriacao") != null)
                        ? existenteCentral.get("dataCriacao")
                        : agoraIso);
                payload.put("updatedAt", agoraIso);

                restTemplate.postForEntity(urlProntuarios, payload, Map.class);
                enviados++;
            } catch (Exception e) {
                System.err.println(">>> [SYNC PACIENTES] Falha ao enviar paciente " + p.getNome() + ": " + e.getMessage());
            }
        }

        return new int[]{enviados, recebidos};
    }

    /**
     * Garante que homônimos (mesmo nome, CPFs diferentes) NÃO sejam mesclados.
     */
    private Paciente localizarPacienteCompativel(List<Paciente> locais, String nomeCentral, String cpfCentral) {
        if (cpfCentral != null) {
            // 1. Busca exata pelo CPF normalizado
            for (Paciente p : locais) {
                String cpfLocal = normalizarCpf(p.getCpf());
                if (cpfCentral.equals(cpfLocal)) {
                    return p;
                }
            }
            // 2. Só vincula por nome se o registro local ainda NÃO tiver CPF preenchido
            for (Paciente p : locais) {
                String cpfLocal = normalizarCpf(p.getCpf());
                if (cpfLocal == null && p.getNome() != null && p.getNome().trim().equalsIgnoreCase(nomeCentral)) {
                    return p;
                }
            }
            return null;
        }

        // Quando o paciente da Central não possui CPF, busca por nome entre registros sem CPF (ou qualquer homônimo se único)
        for (Paciente p : locais) {
            String cpfLocal = normalizarCpf(p.getCpf());
            if (cpfLocal == null && p.getNome() != null && p.getNome().trim().equalsIgnoreCase(nomeCentral)) {
                return p;
            }
        }
        for (Paciente p : locais) {
            if (p.getNome() != null && p.getNome().trim().equalsIgnoreCase(nomeCentral)) {
                return p;
            }
        }
        return null;
    }

    private void atualizarProntuarioLocalSeNecessario(Paciente paciente, Map<String, Object> itemCentral) {
        String tipoSanguineo = itemCentral.get("tipoSanguineo") != null ? itemCentral.get("tipoSanguineo").toString() : null;
        String alergias = itemCentral.get("alergias") != null ? itemCentral.get("alergias").toString() : null;
        String comorbidades = itemCentral.get("comorbidades") != null ? itemCentral.get("comorbidades").toString() : null;
        String usoContinuo = itemCentral.get("medicamentosUsoContinuo") != null ? itemCentral.get("medicamentosUsoContinuo").toString() : null;

        boolean temDados = (tipoSanguineo != null && !tipoSanguineo.isBlank())
                || (alergias != null && !alergias.isBlank())
                || (comorbidades != null && !comorbidades.isBlank())
                || (usoContinuo != null && !usoContinuo.isBlank());

        if (!temDados) {
            return;
        }

        Prontuario pront = prontuarioRepository.findByPacienteId(paciente.getId()).orElseGet(() -> {
            Prontuario novo = new Prontuario();
            UUID idCentral = null;
            if (itemCentral.get("id") != null) {
                try {
                    idCentral = UUID.fromString(itemCentral.get("id").toString());
                } catch (Exception ignored) {
                }
            }
            novo.setId(idCentral != null ? idCentral : UUID.randomUUID());
            novo.setPaciente(paciente);
            return novo;
        });

        if (pront.getId() == null) {
            pront.setId(UUID.randomUUID());
        }
        if (tipoSanguineo != null && !tipoSanguineo.isBlank()) pront.setTipoSanguineo(tipoSanguineo);
        if (alergias != null && !alergias.isBlank()) pront.setAlergias(alergias);
        if (comorbidades != null && !comorbidades.isBlank()) pront.setComorbidades(comorbidades);
        if (usoContinuo != null && !usoContinuo.isBlank()) pront.setMedicamentosUsoContinuo(usoContinuo);

        prontuarioRepository.save(pront);
    }

    private String normalizarCpf(String cpf) {
        if (cpf == null || cpf.isBlank()) {
            return null;
        }
        String digits = cpf.replaceAll("\\D", "");
        return digits.isEmpty() ? cpf.trim() : digits;
    }

    private Object escolherValor(String local, Object central) {
        if (local != null && !local.isBlank()) {
            return local;
        }
        return central;
    }
}