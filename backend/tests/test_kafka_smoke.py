"""Smoke test: produce and consume one sample price.observations message.

Requires a running broker (docker compose up). Skips cleanly if none is reachable,
so the rest of the backend test suite still runs in CI without Kafka.
"""
import sys
import time
import uuid
from pathlib import Path

import pytest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from common import kafka_io  # noqa: E402


def _broker_up() -> bool:
    try:
        from kafka import KafkaAdminClient

        admin = KafkaAdminClient(bootstrap_servers=kafka_io.bootstrap_servers(), request_timeout_ms=3000)
        admin.list_topics()
        admin.close()
        return True
    except Exception:
        return False


pytestmark = pytest.mark.skipif(not _broker_up(), reason="no Kafka broker reachable")


def test_produce_and_consume_price_observation():
    observation = {
        "store": "Mercadona",
        "externalProductId": "smoke-1",
        "ean": "8410000000000",
        "name": "Leche entera 1L",
        "priceCents": 199,
        "currency": "EUR",
        "observedAt": int(time.time() * 1000),
        "source": "smoke_test",
    }

    producer = kafka_io.make_producer()
    producer.send(kafka_io.TOPIC_PRICE_OBSERVATIONS, key=observation["ean"], value=observation)
    producer.flush()
    producer.close()

    consumer = kafka_io.make_consumer(
        kafka_io.TOPIC_PRICE_OBSERVATIONS,
        group_id=f"smoke-{uuid.uuid4()}",
        consumer_timeout_ms=10000,
    )
    received = None
    for value in kafka_io.consume(consumer):
        if value.get("externalProductId") == "smoke-1":
            received = value
            break
    consumer.close()

    assert received is not None, "did not read back the produced observation"
    assert received["priceCents"] == 199
    assert received["source"] == "smoke_test"
