"""Open Prices (Open Food Facts) source adapter.

Self-registers on sources.registry, consumes EAN scrape.requests, queries the Open Prices
API, and publishes normalized price.observations (money in cents, source='open_prices').

Isolated: depends only on backend/common/kafka_io and the documented contract — no shared
code with other adapters.

API: https://prices.openfoodfacts.org/api/v1/prices?product_code=<EAN>
"""
from __future__ import annotations

import sys
import time
import urllib.parse
import urllib.request
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[2]))

from common import kafka_io  # noqa: E402

SOURCE_ID = "open_prices"
API_BASE = "https://prices.openfoodfacts.org/api/v1"


def registration() -> dict:
    return {
        "sourceId": SOURCE_ID,
        "name": "Open Prices (OFF)",
        "supportsEan": True,
        "supportsNameSearch": False,
        "publishedAt": int(time.time() * 1000),
    }


def _to_millis(created: str | None) -> int:
    """Open Prices 'created' is ISO-8601 (UTC). Fall back to now on parse failure."""
    if not created:
        return int(time.time() * 1000)
    try:
        from datetime import datetime, timezone

        s = created.replace("Z", "+00:00")
        dt = datetime.fromisoformat(s)
        if dt.tzinfo is None:
            dt = dt.replace(tzinfo=timezone.utc)
        return int(dt.timestamp() * 1000)
    except Exception:
        return int(time.time() * 1000)


def normalize(api_response: dict, ean: str) -> list[dict]:
    """Map an Open Prices /prices response to price.observations. Pure — easy to test.

    Skips rows without a usable price. Money -> integer cents.
    """
    observations = []
    for item in api_response.get("items", []):
        price = item.get("price")
        if price is None:
            continue
        location = item.get("location") or {}
        store = location.get("osm_name") or location.get("osm_brand") or "unknown"
        observations.append({
            "store": store,
            "externalProductId": str(item.get("id")) if item.get("id") is not None else None,
            "ean": item.get("product_code") or ean,
            "name": item.get("product_name") or (item.get("product") or {}).get("product_name"),
            "priceCents": round(float(price) * 100),
            "currency": item.get("currency", "EUR"),
            "observedAt": _to_millis(item.get("created")),
            "source": SOURCE_ID,
        })
    return observations


def fetch(ean: str, opener=urllib.request.urlopen) -> dict:
    """Query the Open Prices API for an EAN. `opener` injectable for tests (no live net)."""
    import json

    params = urllib.parse.urlencode({"product_code": ean, "order_by": "-created", "size": 50})
    url = f"{API_BASE}/prices?{params}"
    with opener(url, timeout=15) as resp:
        return json.loads(resp.read().decode("utf-8"))


def handle_request(request: dict, producer, opener=urllib.request.urlopen) -> int:
    """Handle one scrape.request. Returns the number of observations published."""
    ean = request.get("ean")
    if not ean:
        return 0  # This adapter only supports EAN targets.
    api_response = fetch(ean, opener=opener)
    observations = normalize(api_response, ean)
    for obs in observations:
        producer.send(kafka_io.TOPIC_PRICE_OBSERVATIONS, key=obs.get("ean"), value=obs)
    producer.flush()
    return len(observations)


def run() -> None:
    producer = kafka_io.make_producer()
    # Announce ourselves so the aggregator knows this source exists.
    producer.send(kafka_io.TOPIC_SOURCES_REGISTRY, key=SOURCE_ID, value=registration())
    producer.flush()

    consumer = kafka_io.make_consumer(kafka_io.TOPIC_SCRAPE_REQUESTS, group_id=SOURCE_ID)
    for message in consumer:
        try:
            handle_request(message.value, producer)
        except Exception as exc:  # keep the adapter alive on a single bad request
            print(f"[{SOURCE_ID}] request failed: {exc}", file=sys.stderr)


if __name__ == "__main__":
    run()
