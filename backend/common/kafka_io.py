"""Thin Kafka helpers shared by backend services. JSON messages, cents for money.

Kept deliberately small: topic names, a JSON producer, and a JSON consumer factory.
Everything else (schemas as plain dicts) lives in the calling service so adapters stay
isolated behind the documented contract in backend/CONTRACT.md.
"""
from __future__ import annotations

import json
import os
from typing import Iterator

from kafka import KafkaConsumer, KafkaProducer

# Well-known topics (see backend/CONTRACT.md).
TOPIC_SCRAPE_REQUESTS = "scrape.requests"
TOPIC_PRICE_OBSERVATIONS = "price.observations"
TOPIC_SOURCES_REGISTRY = "sources.registry"


def bootstrap_servers() -> str:
    """Broker address; overridable so tests/CI can point elsewhere."""
    return os.environ.get("KAFKA_BOOTSTRAP", "localhost:9092")


def make_producer(**kwargs) -> KafkaProducer:
    return KafkaProducer(
        bootstrap_servers=bootstrap_servers(),
        value_serializer=lambda v: json.dumps(v).encode("utf-8"),
        key_serializer=lambda k: k.encode("utf-8") if isinstance(k, str) else k,
        **kwargs,
    )


def make_consumer(*topics: str, group_id: str, **kwargs) -> KafkaConsumer:
    return KafkaConsumer(
        *topics,
        bootstrap_servers=bootstrap_servers(),
        group_id=group_id,
        value_deserializer=lambda v: json.loads(v.decode("utf-8")),
        auto_offset_reset=kwargs.pop("auto_offset_reset", "earliest"),
        enable_auto_commit=kwargs.pop("enable_auto_commit", True),
        **kwargs,
    )


def consume(consumer: KafkaConsumer) -> Iterator[dict]:
    """Yield message values (already JSON-decoded)."""
    for message in consumer:
        yield message.value
