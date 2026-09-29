"""Mercadona source adapter — the reusable TEMPLATE for store scrapers.

Copy this directory to add a new store. It implements the full contract:
  * self-register on sources.registry at startup,
  * consume scrape.requests (both EAN and name-search targets),
  * publish normalized price.observations (money in cents, source='<store>').

Isolated: depends only on backend/common/kafka_io. No shared code with other adapters
beyond the documented message contract, so a ToS-risky source can be disabled or swapped
without touching the app, the gateway, or any sibling adapter (see backend/adapters/README.md).

`parse_search` is a PURE function over the source's response payload — swap `fetch` for a
different store's transport/parsing and keep the same shape.

--- To adapt for a new store, change: SOURCE_ID, STORE_NAME, SEARCH_URL, fetch(), parse_search(). ---
"""
from __future__ import annotations

import json
import sys
import time
import urllib.parse
import urllib.request
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[2]))

from common import kafka_io  # noqa: E402

SOURCE_ID = "mercadona"
STORE_NAME = "Mercadona"
# Mercadona's storefront search API returns JSON (no HTML scraping needed here). A store
# that only serves HTML would parse markup in parse_search instead — same output shape.
SEARCH_URL = "https://tienda.mercadona.es/api/products/search"


def registration() -> dict:
    return {
        "sourceId": SOURCE_ID,
        "name": STORE_NAME,
        "supportsEan": True,
        "supportsNameSearch": True,
        "publishedAt": int(time.time() * 1000),
    }


def parse_search(payload: dict, ean: str | None, observed_at: int | None = None) -> list[dict]:
    """Map a store search payload -> price.observations. PURE (testable on fixtures).

    Expects payload like {"products": [{"id","name","ean"?,"price_cents"|"price"}...]}.
    Money is normalized to integer cents; rows without a usable price are skipped.
    """
    ts = observed_at if observed_at is not None else int(time.time() * 1000)
    observations = []
    for product in payload.get("products", []):
        cents = _price_cents(product)
        if cents is None:
            continue
        observations.append({
            "store": STORE_NAME,
            "externalProductId": str(product.get("id")) if product.get("id") is not None else None,
            "ean": product.get("ean") or ean,
            "name": product.get("name") or product.get("display_name"),
            "priceCents": cents,
            "currency": product.get("currency", "EUR"),
            "observedAt": ts,
            "source": SOURCE_ID,
        })
    return observations


def _price_cents(product: dict) -> int | None:
    """Accept either integer cents or a float/string euro amount; return cents or None."""
    if product.get("price_cents") is not None:
        return int(product["price_cents"])
    price = product.get("price")
    if price is None:
        return None
    try:
        return round(float(price) * 100)
    except (TypeError, ValueError):
        return None


def fetch(query: str, opener=urllib.request.urlopen) -> dict:
    """Query the store by name/EAN string. `opener` injectable so tests never hit the network."""
    url = f"{SEARCH_URL}?{urllib.parse.urlencode({'query': query})}"
    with opener(url, timeout=15) as resp:
        return json.loads(resp.read().decode("utf-8"))


def handle_request(request: dict, producer, opener=urllib.request.urlopen) -> int:
    """Handle one scrape.request (EAN or name). Returns number of observations published."""
    ean = request.get("ean")
    name = request.get("name")
    query = ean or name
    if not query:
        return 0
    payload = fetch(query, opener=opener)
    observations = parse_search(payload, ean=ean)
    for obs in observations:
        producer.send(kafka_io.TOPIC_PRICE_OBSERVATIONS, key=obs.get("ean") or obs["store"], value=obs)
    producer.flush()
    return len(observations)


def run() -> None:
    producer = kafka_io.make_producer()
    producer.send(kafka_io.TOPIC_SOURCES_REGISTRY, key=SOURCE_ID, value=registration())
    producer.flush()

    consumer = kafka_io.make_consumer(kafka_io.TOPIC_SCRAPE_REQUESTS, group_id=SOURCE_ID)
    for message in consumer:
        try:
            handle_request(message.value, producer)
        except Exception as exc:  # one bad request must not kill the adapter
            print(f"[{SOURCE_ID}] request failed: {exc}", file=sys.stderr)


if __name__ == "__main__":
    run()
