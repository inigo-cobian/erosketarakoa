import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from aggregator import service  # noqa: E402
from aggregator.store import SqlitePriceStore  # noqa: E402
from common import kafka_io  # noqa: E402


def make_obs(price_cents, observed_at, ean="8410000000000", store="Mercadona",
             ext="p1", source="open_prices"):
    return {
        "store": store,
        "externalProductId": ext,
        "ean": ean,
        "name": "Leche",
        "priceCents": price_cents,
        "currency": "EUR",
        "observedAt": observed_at,
        "source": source,
    }


def test_append_history_is_ordered_and_never_overwritten():
    db = SqlitePriceStore(":memory:")
    db.append_observation(make_obs(200, 100))
    db.append_observation(make_obs(180, 200))
    db.append_observation(make_obs(190, 150))

    history = db.observations(ean="8410000000000")
    # Full history kept (3 rows), oldest first.
    assert [o["priceCents"] for o in history] == [200, 190, 180]
    assert [o["observedAt"] for o in history] == [100, 150, 200]


def test_query_by_store_and_external_id():
    db = SqlitePriceStore(":memory:")
    db.append_observation(make_obs(200, 100, store="Mercadona", ext="a"))
    db.append_observation(make_obs(300, 100, store="Eroski", ext="b"))

    assert len(db.observations(store="Mercadona")) == 1
    assert db.observations(store="Eroski")[0]["priceCents"] == 300
    assert db.observations(external_product_id="a")[0]["store"] == "Mercadona"


def test_register_source_upsert():
    db = SqlitePriceStore(":memory:")
    db.register_source({
        "sourceId": "open_prices", "name": "Open Prices",
        "supportsEan": True, "supportsNameSearch": False, "publishedAt": 1,
    })
    # Re-announce with changed capabilities -> updated, not duplicated.
    db.register_source({
        "sourceId": "open_prices", "name": "Open Prices v2",
        "supportsEan": True, "supportsNameSearch": True, "publishedAt": 2,
    })
    sources = db.sources()
    assert len(sources) == 1
    assert sources[0]["name"] == "Open Prices v2"
    assert sources[0]["supportsNameSearch"] is True


def test_route_dispatches_by_topic():
    db = SqlitePriceStore(":memory:")
    service.route(db, kafka_io.TOPIC_PRICE_OBSERVATIONS, make_obs(199, 100))
    service.route(db, kafka_io.TOPIC_SOURCES_REGISTRY, {
        "sourceId": "s1", "name": "S1", "supportsEan": True,
        "supportsNameSearch": False, "publishedAt": 1,
    })
    service.route(db, "unknown.topic", {"ignored": True})  # ignored

    assert len(db.observations()) == 1
    assert len(db.sources()) == 1
