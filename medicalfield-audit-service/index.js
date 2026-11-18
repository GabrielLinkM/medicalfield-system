const express = require('express');
const { Pool } = require('pg');
const crypto = require('crypto');
const cors = require('cors');

const app = express();
app.use(cors());
app.use(express.json());

const PORT = process.env.PORT || 8082;
const API_KEY = process.env.MEDICALFIELD_API_KEY || 'medicalfield-secret-key-2026';

const pool = new Pool({
    host: process.env.DB_HOST || 'localhost',
    port: process.env.DB_PORT || 5432,
    database: process.env.DB_NAME || 'medicalfield_audit',
    user: process.env.DB_USER || 'postgres',
    password: process.env.DB_PASSWORD || 'postgres'
});

// Middleware de autenticacao via X-API-Key
app.use('/api/v1', (req, res, next) => {
    const providedKey = req.header('X-API-Key');
    if (!providedKey || providedKey !== API_KEY) {
        return res.status(401).json({ erro: 'Acesso nao autorizado: X-API-Key ausente ou invalida.' });
    }
    next();
});

// Inicializa a tabela de auditoria automaticamente
async function initDb() {
    await pool.query(`
        CREATE TABLE IF NOT EXISTS audit_logs (
            id SERIAL PRIMARY KEY,
            tipo_evento VARCHAR(100) NOT NULL,
            origem VARCHAR(100) NOT NULL,
            entidade_id VARCHAR(100),
            ator VARCHAR(100),
            detalhes TEXT,
            payload JSONB,
            hash_anterior VARCHAR(64) NOT NULL,
            hash_atual VARCHAR(64) NOT NULL,
            registrado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );
    `);
    console.log('>>> [AUDIT DB] Tabela audit_logs verificada/criada com sucesso.');
}

function calcularHash(dados) {
    return crypto.createHash('sha256').update(JSON.stringify(dados)).digest('hex');
}

// Endpoint para registrar novo evento de auditoria
app.post('/api/v1/auditoria/eventos', async (req, res) => {
    try {
        const { tipoEvento, origem, entidadeId, ator, detalhes, payload } = req.body;

        const ultimo = await pool.query('SELECT hash_atual FROM audit_logs ORDER BY id DESC LIMIT 1');
        const hashAnterior = ultimo.rows.length > 0 ? ultimo.rows[0].hash_atual : '0'.repeat(64);

        const timestamp = new Date().toISOString();
        const hashAtual = calcularHash({
            tipoEvento,
            origem,
            entidadeId,
            ator,
            detalhes,
            hashAnterior,
            timestamp
        });

        const result = await pool.query(
            `INSERT INTO audit_logs (tipo_evento, origem, entidade_id, ator, detalhes, payload, hash_anterior, hash_atual)
             VALUES ($1, $2, $3, $4, $5, $6, $7, $8) RETURNING *`,
            [tipoEvento, origem, entidadeId, ator, detalhes, JSON.stringify(payload || {}), hashAnterior, hashAtual]
        );

        console.log(`>>> [AUDITORIA] Evento '${tipoEvento}' registrado | Entidade: ${entidadeId} | Hash: ${hashAtual.substring(0, 12)}...`);
        res.status(201).json(result.rows[0]);
    } catch (err) {
        console.error('Erro ao registrar auditoria:', err.message);
        res.status(500).json({ erro: 'Falha ao gravar log de auditoria' });
    }
});

// Endpoint para listar todos os registros de auditoria
app.get('/api/v1/auditoria', async (req, res) => {
    try {
        const result = await pool.query('SELECT * FROM audit_logs ORDER BY id DESC');
        const logs = result.rows.map(row => {
            const data = row.registrado_em
                || row.criado_em
                || row.data_hora
                || row.data_registro
                || row.data_evento
                || row.data_criacao
                || row.created_at
                || row.timestamp
                || Object.values(row).find(v => v instanceof Date);

            const evento = row.tipo_evento || row.evento || row.acao;
            const idRef = row.entidade_id || row.ficha_id || row.atendimento_id;
            const hAnt = row.hash_anterior || row.previous_hash;
            const hAtu = row.hash_atual || row.current_hash || row.hash;

            return {
                ...row,
                registradoEm: data,
                dataHora: data,
                criadoEm: data,
                createdAt: data,
                timestamp: data,
                dataRegistro: data,
                dataEvento: data,
                tipoEvento: evento,
                evento: evento,
                entidadeId: idRef,
                fichaId: idRef,
                hashAnterior: hAnt,
                hashAtual: hAtu
            };
        });
        res.json(logs);
    } catch (err) {
        res.status(500).json({ erro: err.message });
    }
});

// Endpoint para validar a integridade da cadeia de hashes
app.get('/api/v1/auditoria/integridade', async (req, res) => {
    try {
        const result = await pool.query('SELECT id, hash_anterior, hash_atual FROM audit_logs ORDER BY id ASC');
        const rows = result.rows;
        let integro = true;

        for (let i = 1; i < rows.length; i++) {
            if (rows[i].hash_anterior !== rows[i - 1].hash_atual) {
                integro = false;
                break;
            }
        }

        res.json({
            totalRegistros: rows.length,
            cadeiaIntegra: integro,
            status: integro ? 'VALIDO - Nenhuma adulteracao detectada' : 'ALERTA - Quebra na cadeia de auditoria'
        });
    } catch (err) {
        res.status(500).json({ erro: err.message });
    }
});

initDb().then(() => {
    app.listen(PORT, () => {
        console.log(`>>> Microsservico de Auditoria (Node.js) rodando na porta ${PORT}`);
    });
});