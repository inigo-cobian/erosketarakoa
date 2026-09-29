"""Feed observations through Kafka into the aggregator's PriceDB. Skips without a broker."""
import sys
import time
import uuid
from pathlib import Path

import pytest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from aggregator import service  # noqa: E402
from aggregator.store import SqlitePriceStore  # noqa: E402
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


def test_observations_flow_through_kafka_into_pricedb():
    db = SqlitePriceStore(":memory:")
    ean = f"int-{uuid.uuid4().hex[:8]}"

    producer = kafka_io.make_producer()
    producer.send(kafka_io.TOPIC_SOURCES_REGISTRY, value={
        "sourceId": "itest", "name": "Integration", "supportsEan": True,
        "supportsNameSearch": False, "publishedAt": 1,
    })
    for price, ts in [(200, 100), (180, 200)]:
        producer.send(kafka_io.TOPIC_PRICE_OBSERVATIONS, value={
            "store": "Mercadona", "externalProductId": "x", "ean": ean, "name": "Leche",
            "priceCents": price, "currency": "EUR", "observedAt": ts, "source": "itest",
        })
    producer.flush()
    producer.close()

    # Consume just our messages via the same routing the service uses.
    consumer = kafka_io.make_consumer(
        kafka_io.TOPIC_PRICE_OBSERVATIONS,
        kafka_io.TOPIC_SOURCES_REGISTRY,
        group_id=f"itest-{uuid.uuid4()}",
        consumer_timeout_ms=10000,
    )
    deadline = time.time() + 12
    for message in consumer:
        service.route(db, message.topic, message.value)
        if len(db.observations(ean=ean)) >= 2 and db.sources():
            break
        if time.time() > deadline:
            break
    consumer.close()

    history = db.observations(ean=ean)
    assert [o["priceCents"] for o in history] == [200, 180]
    assert any(s["sourceId"] == "itest" for s in db.sources())
