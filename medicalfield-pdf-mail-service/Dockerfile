FROM python:3.11-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY . .
EXPOSE 8083
<<<<<<< HEAD
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8083"]
=======
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8083"]
>>>>>>> 6e9023f7f420f8a5d73d9bd5dc998105fb543f3e
