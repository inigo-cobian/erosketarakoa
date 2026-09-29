# Erosketarako backend

Local price-tracking backbone. The Android app talks **only** to the REST gateway
(`gateway/`); Kafka and the source adapters are hidden behind it.

## Layout

- `docker-compose.yml` — single-node Kafka (KRaft) + a one-shot topic creator.
- `CONTRACT.md` — the JSON message schemas (money in cents, timestamps epoch millis UTC).
- `common/kafka_io.py` — shared Kafka helpers (topics, JSON producer/consumer).
- `tests/` — cross-service tests (Kafka smoke test).

## Run

```bash
docker compose up            # brings Kafka online and creates the topics
python3 -m venv .venv && .venv/bin/pip install -r requirements.txt
.venv/bin/python -m pytest   # smoke + service tests
```

The Kafka smoke test skips automatically if no broker is reachable, so unit tests for
the individual services still run without Docker.
