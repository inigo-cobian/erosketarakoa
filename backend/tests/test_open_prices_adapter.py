import io
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from adapters.open_prices import adapter  # noqa: E402
from common import kafka_io  # noqa: E402

FIXTURE = Path(__file__).parent / "fixtures" / "open_prices_nutella.json"


def load_fixture() -> dict:
    return json.loads(FIXTURE.read_text())


def test_normalize_maps_prices_to_cents_and_skips_missing():
    obs = adapter.normalize(load_fixture(), ean="3017620422003")
    # 3 items, one has null price -> 2 observations.
    assert len(obs) == 2
    first = obs[0]
    assert first["priceCents"] == 468  # 4.68 EUR -> cents
    assert first["store"] == "Carrefour Express"
    assert first["ean"] == "3017620422003"
    assert first["source"] == "open_prices"
    assert first["currency"] == "EUR"
    # created ISO -> epoch millis UTC (2026-09-13T10:54:52Z)
    assert first["observedAt"] == 1789296892063
    # product_name falls back to product.product_name when the row's is null.
    assert obs[1]["name"] == "Nutella"


def test_registration_declares_ean_capability():
    reg = adapter.registration()
    assert reg["sourceId"] == "open_prices"
    assert reg["supportsEan"] is True
    assert reg["supportsNameSearch"] is False


class FakeProducer:
    def __init__(self):
        self.sent = []

    def send(self, topic, key=None, value=None):
        self.sent.append((topic, key, value))

    def flush(self):
        pass


def fake_opener_factory(payload: dict):
    def opener(url, timeout=None):
        # crude context manager returning the JSON bytes
        class Ctx(io.BytesIO):
            def __enter__(self):
                return self

            def __exit__(self, *a):
                return False

        return Ctx(json.dumps(payload).encode("utf-8"))

    return opener


def test_handle_request_publishes_normalized_observations():
    producer = FakeProducer()
    n = adapter.handle_request(
        {"ean": "3017620422003"},
        producer,
        opener=fake_opener_factory(load_fixture()),
    )
    assert n == 2
    topics = {t for t, _, _ in producer.sent}
    assert topics == {kafka_io.TOPIC_PRICE_OBSERVATIONS}
    assert all(v["source"] == "open_prices" for _, _, v in producer.sent)


def test_handle_request_ignores_non_ean_targets():
    producer = FakeProducer()
    n = adapter.handle_request({"name": "Nutella"}, producer, opener=fake_opener_factory({}))
    assert n == 0
    assert producer.sent == []
