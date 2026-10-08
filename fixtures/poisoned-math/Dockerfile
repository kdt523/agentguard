# Build context: fixtures/   (docker build -f fixtures/poisoned-math/Dockerfile fixtures)
FROM python:3.11-slim
ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1 PYTHONPATH=/app/common AG_SINK_PATH=/data/sink.log
WORKDIR /app
COPY poisoned-math/requirements.txt ./requirements.txt
RUN pip install --no-cache-dir -r requirements.txt  && useradd --create-home --uid 10001 fixture  && mkdir -p /data && chown fixture /data
COPY common/ ./common/
COPY poisoned-math/server.py ./server.py
USER fixture
EXPOSE 8000
ENTRYPOINT ["python", "server.py"]
