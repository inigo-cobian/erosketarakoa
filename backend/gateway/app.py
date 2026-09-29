"""REST/JSON gateway — the app's ONLY contract. Fully hides Kafka.

Reads price history and known products from the PriceDB (aggregator's store) and records
item->store links. It returns RAW data; it does NOT compute bargain reasons — the app's
engine does that (Task 3). OpenAPI is served at /openapi.json (FastAPI built-in).
"""
from __future__ import annotations

import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from fastapi import FastAPI, Query  # noqa: E402
from pydantic import BaseModel  # noqa: E402

from aggregator.store import PriceStore, SqlitePriceStore  # noqa: E402


class LinkRequest(BaseModel):
    store: str
    externalProductId: str | None = None
    ean: str | None = None


def create_app(store: PriceStore | None = None) -> FastAPI:
    store = store or SqlitePriceStore()
    app = FastAPI(title="Erosketarako Gateway", version="1.0.0")

    @app.get("/products/search")
    def search_products(q: str | None = Query(default=None), ean: str | None = Query(default=None)):
        return {"products": store.search_products(q=q, ean=ean)}

    @app.post("/items/{item_id}/links", status_code=201)
    def create_link(item_id: str, body: LinkRequest):
        return store.add_link(item_id, body.store, body.externalProductId, body.ean)

    @app.get("/items/{item_id}/prices")
    def item_prices(item_id: str):
        return {"itemId": item_id, "links": store.prices_for_item(item_id)}

    @app.get("/bargains")
    def bargains(since: int = Query(default=0)):
        # Raw recently-changed prices; the app flags bargains, not the gateway.
        return {"since": since, "observations": store.observations_since(since)}

    @app.get("/health")
    def health():
        return {"status": "ok", "now": int(time.time() * 1000)}

    return app


# Module-level app for `uvicorn gateway.app:app`.
app = create_app()
