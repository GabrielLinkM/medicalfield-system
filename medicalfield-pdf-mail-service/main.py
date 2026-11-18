import os
import smtplib
from datetime import datetime
from email.message import EmailMessage
from typing import Optional
from fastapi import FastAPI, HTTPException, Header, Depends
from fastapi.responses import FileResponse
from pydantic import BaseModel
from fpdf import FPDF

app = FastAPI(title="MedicalField - Servico de PDF e Email")

API_KEY = os.getenv("MEDICALFIELD_API_KEY", "medicalfield-secret-key-2026")

PDF_DIR = "pdfs"
os.makedirs(PDF_DIR, exist_ok=True)


def verificar_api_key(x_api_key: Optional[str] = Header(None, alias="X-API-Key")):
    if not x_api_key or x_api_key != API_KEY:
        raise HTTPException(
            status_code=401,
            detail="Acesso nao autorizado: X-API-Key ausente ou invalida."
        )


class FichaPayload(BaseModel):
    id: str
    nomePaciente: Optional[str] = "Nao identificado"
    documentoPaciente: Optional[str] = None
    pressaoArterial: Optional[str] = None
    frequenciaCardiaca: Optional[int] = None
    frequenciaRespiratoria: Optional[int] = None
    saturacaoOxigenio: Optional[int] = None
    temperaturaCorporal: Optional[float] = None
    glicemiaCapilar: Optional[int] = None
    escalaGlasgow: Optional[int] = None
    classificacaoGlasgow: Optional[str] = None
    protocoloManchester: Optional[str] = None
    protocoloStart: Optional[str] = None
    medicamentos: Optional[str] = None
    observacoes: Optional[str] = None
    profissionalRegistro: Optional[str] = None
    dataHoraAtendimento: Optional[str] = None
    emailDestino: Optional[str] = None


def gerar_pdf_ficha(ficha: FichaPayload) -> str:
    pdf = FPDF()
    pdf.add_page()
    pdf.set_auto_page_break(auto=True, margin=15)

    # Cabecalho
    pdf.set_fill_color(2, 117, 216)
    pdf.set_text_color(255, 255, 255)
    pdf.set_font("Helvetica", "B", 15)
    pdf.cell(0, 12, "MEDICALFIELD - FICHA DE ATENDIMENTO PRE-HOSPITALAR", fill=True, align="C", new_x="LMARGIN", new_y="NEXT")

    pdf.set_text_color(0, 0, 0)
    pdf.set_font("Helvetica", "", 9)
    pdf.cell(0, 7, f"Protocolo ID: {ficha.id}  |  Gerado em: {datetime.now().strftime('%d/%m/%Y %H:%M:%S')}", align="R", new_x="LMARGIN", new_y="NEXT")
    pdf.ln(3)

    # Secao 1: Identificacao
    pdf.set_font("Helvetica", "B", 11)
    pdf.set_fill_color(235, 240, 245)
    pdf.cell(0, 8, " 1. IDENTIFICACAO E ATENDIMENTO", fill=True, new_x="LMARGIN", new_y="NEXT")
    pdf.set_font("Helvetica", "", 10)
    pdf.cell(0, 7, f"Paciente: {ficha.nomePaciente or 'Nao informado'}", new_x="LMARGIN", new_y="NEXT")
    pdf.cell(0, 7, f"CPF: {ficha.documentoPaciente or 'Nao informado'}   |   Data/Hora: {ficha.dataHoraAtendimento or 'N/I'}", new_x="LMARGIN", new_y="NEXT")
    pdf.cell(0, 7, f"Profissional Responsavel: {ficha.profissionalRegistro or 'N/I'}", new_x="LMARGIN", new_y="NEXT")
    pdf.ln(3)

    # Secao 2: Sinais Vitais
    pdf.set_font("Helvetica", "B", 11)
    pdf.cell(0, 8, " 2. SINAIS VITAIS", fill=True, new_x="LMARGIN", new_y="NEXT")
    pdf.set_font("Helvetica", "", 10)
    pdf.cell(0, 7, f"Pressao Arterial: {ficha.pressaoArterial or 'N/I'} mmHg   |   Freq. Cardiaca: {ficha.frequenciaCardiaca or 'N/I'} bpm", new_x="LMARGIN", new_y="NEXT")
    pdf.cell(0, 7, f"Freq. Respiratoria: {ficha.frequenciaRespiratoria or 'N/I'} irpm   |   Saturacao O2: {ficha.saturacaoOxigenio or 'N/I'} %", new_x="LMARGIN", new_y="NEXT")
    pdf.cell(0, 7, f"Temperatura: {ficha.temperaturaCorporal or 'N/I'} C   |   Glicemia: {ficha.glicemiaCapilar or 'N/I'} mg/dL", new_x="LMARGIN", new_y="NEXT")
    pdf.ln(3)

    # Secao 3: Triagem e Glasgow
    pdf.set_font("Helvetica", "B", 11)
    pdf.cell(0, 8, " 3. AVALIACAO NEUROLOGICA E TRIAGEM", fill=True, new_x="LMARGIN", new_y="NEXT")
    pdf.set_font("Helvetica", "", 10)
    pdf.cell(0, 7, f"Escala de Glasgow: {ficha.escalaGlasgow or 'N/I'} ({ficha.classificacaoGlasgow or 'N/I'})", new_x="LMARGIN", new_y="NEXT")
    pdf.cell(0, 7, f"Protocolo Manchester: {ficha.protocoloManchester or 'N/I'}   |   Protocolo START: {ficha.protocoloStart or 'N/I'}", new_x="LMARGIN", new_y="NEXT")
    pdf.ln(3)

    # Secao 4: Medicamentos e Observacoes
    pdf.set_font("Helvetica", "B", 11)
    pdf.cell(0, 8, " 4. MEDICAMENTOS ADMINISTRADOS E CONDUTA", fill=True, new_x="LMARGIN", new_y="NEXT")
    pdf.set_font("Helvetica", "", 10)
    pdf.multi_cell(0, 6, f"Medicamentos: {ficha.medicamentos or 'Nenhum medicamento administrado'}")
    pdf.ln(2)
    pdf.multi_cell(0, 6, f"Observacoes Clinicas: {ficha.observacoes or 'Sem observacoes adicionais.'}")

    caminho_arquivo = os.path.join(PDF_DIR, f"ficha_{ficha.id}.pdf")
    pdf.output(caminho_arquivo)
    return caminho_arquivo


def enviar_email_com_pdf(destinatario: str, ficha: FichaPayload, caminho_pdf: str) -> str:
    smtp_host = os.getenv("SMTP_HOST", "")
    smtp_port = int(os.getenv("SMTP_PORT", "587"))
    smtp_user = os.getenv("SMTP_USER", "")
    smtp_pass = os.getenv("SMTP_PASS", "")

    if not smtp_host or not smtp_user:
        msg = f"[SIMULACAO SMTP] E-mail preparado para '{destinatario}' com anexo '{caminho_pdf}' (configure SMTP_HOST/SMTP_USER para envio real)."
        print(f">>> [PDF-MAIL] {msg}")
        return msg

    email = EmailMessage()
    email["Subject"] = f"[MedicalField] Prontuario de Atendimento - {ficha.nomePaciente}"
    email["From"] = smtp_user
    email["To"] = destinatario
    email.set_content(
        f"Segue em anexo o prontuario digital do atendimento pre-hospitalar.\n\n"
        f"ID da Ficha: {ficha.id}\n"
        f"Paciente: {ficha.nomePaciente}\n"
        f"Classificacao Manchester: {ficha.protocoloManchester}\n"
        f"Glasgow: {ficha.escalaGlasgow} ({ficha.classificacaoGlasgow})\n"
    )

    with open(caminho_pdf, "rb") as f:
        email.add_attachment(
            f.read(),
            maintype="application",
            subtype="pdf",
            filename=f"Prontuario_{ficha.id}.pdf"
        )

    with smtplib.SMTP(smtp_host, smtp_port) as server:
        server.starttls()
        server.login(smtp_user, smtp_pass)
        server.send_message(email)

    msg = f"E-mail enviado com sucesso para {destinatario}"
    print(f">>> [PDF-MAIL] {msg}")
    return msg


@app.post("/api/v1/documentos/gerar-e-enviar", dependencies=[Depends(verificar_api_key)])
def processar_ficha(ficha: FichaPayload):
    try:
        caminho_pdf = gerar_pdf_ficha(ficha)
        destinatario = ficha.emailDestino or os.getenv("DEFAULT_HOSPITAL_EMAIL", "regulacao@hospital-destino.com.br")
        status_email = enviar_email_com_pdf(destinatario, ficha, caminho_pdf)

        print(f">>> [PDF-MAIL] PDF gerado em: {caminho_pdf}")
        return {
            "status": "PROCESSADO",
            "fichaId": ficha.id,
            "pdfUrl": f"http://localhost:8083/api/v1/documentos/{ficha.id}/pdf",
            "emailStatus": status_email
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/v1/documentos/{ficha_id}/pdf", dependencies=[Depends(verificar_api_key)])
def baixar_pdf(ficha_id: str):
    caminho_pdf = os.path.join(PDF_DIR, f"ficha_{ficha_id}.pdf")
    if not os.path.exists(caminho_pdf):
        raise HTTPException(status_code=404, detail="PDF nao encontrado para esta ficha.")
    return FileResponse(caminho_pdf, media_type="application/pdf", filename=f"ficha_{ficha_id}.pdf")


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8083)