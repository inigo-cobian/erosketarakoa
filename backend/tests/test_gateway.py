import sys
from pathlib import Path

from fastapi.testclient import TestClient

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from aggregator.store import SqlitePriceStore  # noqa: E402
from gateway.app import create_app  # noqa: E402


def seeded_client() -> tuple[TestClient, SqlitePriceStore]:
    store = SqlitePriceStore(":memory:")
    # Two observations for the same product at Mercadona (history) + one at Eroski.
    store.append_observation({
        "store": "Mercadona", "externalProductId": "m1", "ean": "8410000000000",
        "name": "Leche entera 1L", "priceCents": 200, "currency": "EUR",
        "observedAt": 100, "source": "open_prices",
    })
    store.append_observation({
        "store": "Mercadona", "externalProductId": "m1", "ean": "8410000000000",
        "name": "Leche entera 1L", "priceCents": 180, "currency": "EUR",
        "observedAt": 300, "source": "open_prices",
    })
    store.append_observation({
        "store": "Eroski", "externalProductId": "e1", "ean": "8410000000000",
        "name": "Leche entera 1L", "priceCents": 210, "currency": "EUR",
        "observedAt": 200, "source": "scraper",
    })
    return TestClient(create_app(store)), store


def test_search_by_name_and_ean():
    client, _ = seeded_client()
    by_name = client.get("/products/search", params={"q": "Leche"})
    assert by_name.status_code == 200
    assert len(by_name.json()["products"]) == 2  # Mercadona + Eroski

    by_ean = client.get("/products/search", params={"ean": "8410000000000"})
    assert by_ean.status_code == 200
    assert all(p["ean"] == "8410000000000" for p in by_ean.json()["products"])


def test_create_link_then_fetch_prices():
    client, _ = seeded_client()
    resp = client.post("/items/item-1/links", json={
        "store": "Mercadona", "externalProductId": "m1", "ean": "8410000000000",
    })
    assert resp.status_code == 201
    assert resp.json()["itemId"] == "item-1"

    prices = client.get("/items/item-1/prices")
    assert prices.status_code == 200
    body = prices.json()
    assert body["itemId"] == "item-1"
    assert len(body["links"]) == 1
    # History for the linked Mercadona product, oldest first.
    cents = [o["priceCents"] for o in body["links"][0]["prices"]]
    assert cents == [200, 180]


def test_bargains_since_filters_by_timestamp():
    client, _ = seeded_client()
    resp = client.get("/bargains", params={"since": 250})
    assert resp.status_code == 200
    obs = resp.json()["observations"]
    # Only the 300-timestamp observation is >= 250.
    assert [o["observedAt"] for o in obs] == [300]


def test_openapi_served():
    client, _ = seeded_client()
    resp = client.get("/openapi.json")
    assert resp.status_code == 200
    paths = resp.json()["paths"]
    assert "/products/search" in paths
    assert "/items/{item_id}/prices" in paths
    assert "/bargains" in paths
