"""Aggregator service: consume price.observations and sources.registry into the PriceDB.

No bargain computation here — the app owns the rules. This only records raw history and
tracks known sources.
"""
from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from common import kafka_io  # noqa: E402
from aggregator.store import PriceStore, SqlitePriceStore  # noqa: E402


def route(store: PriceStore, topic: str, value: dict) -> None:
    """Apply one Kafka message to the store. Pure routing — easy to unit-test."""
    if topic == kafka_io.TOPIC_PRICE_OBSERVATIONS:
        store.append_observation(value)
    elif topic == kafka_io.TOPIC_SOURCES_REGISTRY:
        store.register_source(value)
    # Unknown topics are ignored on purpose.


def run(store: PriceStore | None = None, group_id: str = "aggregator") -> None:
    store = store or SqlitePriceStore()
    consumer = kafka_io.make_consumer(
        kafka_io.TOPIC_PRICE_OBSERVATIONS,
        kafka_io.TOPIC_SOURCES_REGISTRY,
        group_id=group_id,
    )
    for message in consumer:
        route(store, message.topic, message.value)


if __name__ == "__main__":
    run()
