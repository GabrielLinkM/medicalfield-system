-- Senha dos usuários abaixo criptografada com BCrypt: "senha123"
INSERT INTO profissional_saude (id, nome, registro_profissional, tipo_profissional, senha)
VALUES 
    (gen_random_uuid(), 'Dra. Maria Silva', 'CRM12345', 'MEDICO', '$2a$10$E2UPv7arXm3Yp6W/9b5o0e1C5V/0M6Qd6J4H/8sN4v1f8V.p1K/3S'),
    (gen_random_uuid(), 'Carlos Enfr.', 'COREN98765', 'ENFERMEIRO', '$2a$10$E2UPv7arXm3Yp6W/9b5o0e1C5V/0M6Qd6J4H/8sN4v1f8V.p1K/3S'),
    (gen_random_uuid(), 'Socorrista Roberto', 'SOC55443', 'SOCORRISTA', '$2a$10$E2UPv7arXm3Yp6W/9b5o0e1C5V/0M6Qd6J4H/8sN4v1f8V.p1K/3S')
ON CONFLICT (registro_profissional) DO NOTHING;