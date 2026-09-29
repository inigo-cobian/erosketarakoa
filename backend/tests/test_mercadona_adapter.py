import io
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from adapters.mercadona import adapter  # noqa: E402
from common import kafka_io  # noqa: E402

FIXTURE = Path(__file__).parent / "fixtures" / "mercadona_search.json"


def load_fixture() -> dict:
    return json.loads(FIXTURE.read_text())


def test_parse_handles_euros_and_cents_and_skips_missing():
    obs = adapter.parse_search(load_fixture(), ean=None, observed_at=1000)
    # 3 products, one has null price -> 2 observations.
    assert len(obs) == 2
    milk, oil = obs[0], obs[1]
    assert milk["priceCents"] == 89          # 0.89 EUR float -> cents
    assert milk["ean"] == "8480000123456"
    assert milk["store"] == "Mercadona"
    assert milk["source"] == "mercadona"
    assert milk["observedAt"] == 1000
    assert oil["priceCents"] == 799          # already integer cents
    assert oil["name"] == "Aceite de oliva virgen extra 1 L"  # display_name fallback


def test_registration_declares_ean_and_name_search():
    reg = adapter.registration()
    assert reg["sourceId"] == "mercadona"
    assert reg["supportsEan"] is True
    assert reg["supportsNameSearch"] is True


class FakeProducer:
    def __init__(self):
        self.sent = []

    def send(self, topic, key=None, value=None):
        self.sent.append((topic, key, value))

    def flush(self):
        pass


def fake_opener(payload: dict):
    def opener(url, timeout=None):
        class Ctx(io.BytesIO):
            def __enter__(self):
                return self

            def __exit__(self, *a):
                return False

        return Ctx(json.dumps(payload).encode("utf-8"))

    return opener


def test_handle_request_by_name_publishes_observations():
    producer = FakeProducer()
    n = adapter.handle_request({"name": "leche"}, producer, opener=fake_opener(load_fixture()))
    assert n == 2
    assert {t for t, _, _ in producer.sent} == {kafka_io.TOPIC_PRICE_OBSERVATIONS}


def test_handle_request_by_ean_publishes_observations():
    producer = FakeProducer()
    n = adapter.handle_request(
        {"ean": "8480000123456"}, producer, opener=fake_opener(load_fixture())
    )
    assert n == 2


def test_handle_request_without_target_returns_zero():
    producer = FakeProducer()
    assert adapter.handle_request({}, producer, opener=fake_opener({})) == 0
    assert producer.sent == []
