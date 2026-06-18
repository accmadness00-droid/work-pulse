# WorkPulse Face Service

FastAPI service. Image yuboriladi, service yuzni topadi va embedding qaytaradi.

## Run local

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 5001
```

## Run Docker

```bash
docker build -t workpulse-face-service .
docker run --rm -p 5001:5001 workpulse-face-service
```

## Test

```bash
curl http://localhost:5001/api/v1/health

curl -X POST http://localhost:5001/api/v1/face/embedding \
  -F "file=@/path/to/person.jpg"
```
