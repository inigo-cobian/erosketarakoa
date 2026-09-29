"""PriceDB storage. SQLite locally, kept behind a small interface so it can be swapped
for Postgres later without touching the consumer loop or the gateway.

Bargain rules do NOT live here — this only stores raw, append-only price history and the
set of known sources. Money is integer cents; timestamps are epoch millis UTC.
"""
from __future__ import annotations

import sqlite3
from typing import Protocol


class PriceStore(Protocol):
    """Storage contract the aggregator and gateway depend on (not the concrete DB)."""

    def append_observation(self, obs: dict) -> None: ...
    def register_source(self, source: dict) -> None: ...
    def observations(self, ean: str | None = None, store: str | None = None,
                     external_product_id: str | None = None) -> list[dict]: ...
    def sources(self) -> list[dict]: ...
    def search_products(self, q: str | None = None, ean: str | None = None) -> list[dict]: ...
    def add_link(self, item_id: str, store: str, external_product_id: str | None,
                 ean: str | None) -> dict: ...
    def links_for_item(self, item_id: str) -> list[dict]: ...
    def observations_since(self, since: int) -> list[dict]: ...


class SqlitePriceStore:
    """SQLite-backed PriceStore. Pass ':memory:' for tests."""

    def __init__(self, path: str = "pricedb.sqlite3"):
        # check_same_thread=False so a consumer thread and a request thread can share it.
        self._conn = sqlite3.connect(path, check_same_thread=False)
        self._conn.row_factory = sqlite3.Row
        self._init_schema()

    def _init_schema(self) -> None:
        self._conn.executescript(
            """
            CREATE TABLE IF NOT EXISTS observations (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                store TEXT NOT NULL,
                external_product_id TEXT,
                ean TEXT,
                name TEXT,
                price_cents INTEGER NOT NULL,
                currency TEXT NOT NULL DEFAULT 'EUR',
                observed_at INTEGER NOT NULL,
                source TEXT NOT NULL
            );
            CREATE INDEX IF NOT EXISTS idx_obs_ean ON observations(ean);
            CREATE INDEX IF NOT EXISTS idx_obs_store ON observations(store);
            CREATE INDEX IF NOT EXISTS idx_obs_ext ON observations(external_product_id);

            CREATE TABLE IF NOT EXISTS sources (
                source_id TEXT PRIMARY KEY,
                name TEXT,
                supports_ean INTEGER NOT NULL DEFAULT 0,
                supports_name_search INTEGER NOT NULL DEFAULT 0,
                published_at INTEGER
            );

            CREATE TABLE IF NOT EXISTS links (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                item_id TEXT NOT NULL,
                store TEXT NOT NULL,
                external_product_id TEXT,
                ean TEXT
            );
            CREATE INDEX IF NOT EXISTS idx_links_item ON links(item_id);
            """
        )
        self._conn.commit()

    def append_observation(self, obs: dict) -> None:
        # Append only — history is never overwritten.
        self._conn.execute(
            """
            INSERT INTO observations
                (store, external_product_id, ean, name, price_cents, currency, observed_at, source)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """,
            (
                obs["store"],
                obs.get("externalProductId"),
                obs.get("ean"),
                obs.get("name"),
                int(obs["priceCents"]),
                obs.get("currency", "EUR"),
                int(obs["observedAt"]),
                obs["source"],
            ),
        )
        self._conn.commit()

    def register_source(self, source: dict) -> None:
        # Upsert: a source re-announcing on restart updates its capabilities.
        self._conn.execute(
            """
            INSERT INTO sources (source_id, name, supports_ean, supports_name_search, published_at)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT(source_id) DO UPDATE SET
                name=excluded.name,
                supports_ean=excluded.supports_ean,
                supports_name_search=excluded.supports_name_search,
                published_at=excluded.published_at
            """,
            (
                source["sourceId"],
                source.get("name"),
                1 if source.get("supportsEan") else 0,
                1 if source.get("supportsNameSearch") else 0,
                source.get("publishedAt"),
            ),
        )
        self._conn.commit()

    def observations(self, ean=None, store=None, external_product_id=None) -> list[dict]:
        clauses, params = [], []
        if ean is not None:
            clauses.append("ean = ?")
            params.append(ean)
        if store is not None:
            clauses.append("store = ?")
            params.append(store)
        if external_product_id is not None:
            clauses.append("external_product_id = ?")
            params.append(external_product_id)
        where = f"WHERE {' AND '.join(clauses)}" if clauses else ""
        rows = self._conn.execute(
            f"SELECT * FROM observations {where} ORDER BY observed_at ASC", params
        ).fetchall()
        return [self._obs_row(r) for r in rows]

    def sources(self) -> list[dict]:
        rows = self._conn.execute("SELECT * FROM sources ORDER BY source_id ASC").fetchall()
        return [
            {
                "sourceId": r["source_id"],
                "name": r["name"],
                "supportsEan": bool(r["supports_ean"]),
                "supportsNameSearch": bool(r["supports_name_search"]),
                "publishedAt": r["published_at"],
            }
            for r in rows
        ]

    def search_products(self, q=None, ean=None) -> list[dict]:
        """Distinct products seen in observations, matched by name substring and/or EAN."""
        clauses, params = [], []
        if ean is not None:
            clauses.append("ean = ?")
            params.append(ean)
        if q is not None and q.strip():
            clauses.append("name LIKE ?")
            params.append(f"%{q.strip()}%")
        where = f"WHERE {' AND '.join(clauses)}" if clauses else ""
        rows = self._conn.execute(
            f"""
            SELECT store, external_product_id, ean, name, MAX(observed_at) AS last_seen
            FROM observations {where}
            GROUP BY store, external_product_id, ean, name
            ORDER BY last_seen DESC
            """,
            params,
        ).fetchall()
        return [
            {
                "store": r["store"],
                "externalProductId": r["external_product_id"],
                "ean": r["ean"],
                "name": r["name"],
            }
            for r in rows
        ]

    def add_link(self, item_id, store, external_product_id, ean) -> dict:
        cur = self._conn.execute(
            "INSERT INTO links (item_id, store, external_product_id, ean) VALUES (?, ?, ?, ?)",
            (item_id, store, external_product_id, ean),
        )
        self._conn.commit()
        return {
            "id": cur.lastrowid,
            "itemId": item_id,
            "store": store,
            "externalProductId": external_product_id,
            "ean": ean,
        }

    def links_for_item(self, item_id) -> list[dict]:
        rows = self._conn.execute(
            "SELECT * FROM links WHERE item_id = ? ORDER BY id ASC", (item_id,)
        ).fetchall()
        return [
            {
                "id": r["id"],
                "itemId": r["item_id"],
                "store": r["store"],
                "externalProductId": r["external_product_id"],
                "ean": r["ean"],
            }
            for r in rows
        ]

    def prices_for_item(self, item_id: str) -> list[dict]:
        """Observation history for every store link of an item (matched by ext id or EAN)."""
        result: list[dict] = []
        for link in self.links_for_item(item_id):
            if link["externalProductId"]:
                obs = self.observations(
                    store=link["store"], external_product_id=link["externalProductId"]
                )
            elif link["ean"]:
                obs = self.observations(ean=link["ean"], store=link["store"])
            else:
                obs = []
            result.append({"link": link, "prices": obs})
        return result

    def observations_since(self, since: int) -> list[dict]:
        rows = self._conn.execute(
            "SELECT * FROM observations WHERE observed_at >= ? ORDER BY observed_at ASC",
            (since,),
        ).fetchall()
        return [self._obs_row(r) for r in rows]

    @staticmethod
    def _obs_row(r: sqlite3.Row) -> dict:
        return {
            "store": r["store"],
            "externalProductId": r["external_product_id"],
            "ean": r["ean"],
            "name": r["name"],
            "priceCents": r["price_cents"],
            "currency": r["currency"],
            "observedAt": r["observed_at"],
            "source": r["source"],
        }

    def close(self) -> None:
        self._conn.close()
