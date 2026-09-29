# Backend message contract

All messages are JSON. **Money is integer cents** everywhere (e.g. `199` = 1.99 EUR).
**Timestamps are epoch milliseconds, UTC.** The app never sees these topics — it talks
only to the REST gateway (Task 7). Kafka is the internal, replayable backbone.

## Topics

| Topic | Produced by | Consumed by | Purpose |
|-------|-------------|-------------|---------|
| `scrape.requests` | gateway / operator | source adapters | ask sources to fetch a price |
| `price.observations` | source adapters | aggregator | an observed price for a product at a store |
| `sources.registry` | source adapters (on startup) | aggregator | announce a source and its capabilities |

## `scrape.requests`

A request to fetch a price. Adapters filter to the targets they support (EAN and/or name).

```json
{
  "requestId": "uuid-string",
  "ean": "8410000000000",        // optional; null if searching by name
  "name": "Leche entera 1L",     // optional; null if searching by EAN
  "store": "Mercadona",          // optional; null = any store the adapter covers
  "requestedAt": 1730000000000
}
```

At least one of `ean` / `name` must be present.

## `price.observations`

A single observed price. Append-only history; never overwritten downstream.

```json
{
  "store": "Mercadona",
  "externalProductId": "abc-123", // source/store product id
  "ean": "8410000000000",         // optional
  "name": "Leche entera 1L",      // optional
  "priceCents": 199,              // integer cents, required
  "currency": "EUR",
  "observedAt": 1730000000000,    // epoch millis UTC
  "source": "open_prices"         // which adapter produced it
}
```

## `sources.registry`

Published by each adapter on startup so the aggregator knows what exists.

```json
{
  "sourceId": "open_prices",
  "name": "Open Prices (OFF)",
  "supportsEan": true,
  "supportsNameSearch": false,
  "publishedAt": 1730000000000
}
```

## Notes

- Producers should set the Kafka message key to a stable id where useful (e.g. `ean` or
  `store:externalProductId`) so related records land on the same partition.
- Adapters are isolated: they share only this contract, never code beyond
  `backend/common/kafka_io.py`.

## REST gateway (the app's only surface)

The Android app talks to the gateway only; it never sees Kafka. Full schema at
`GET /openapi.json` (FastAPI). Endpoints:

- `GET /products/search?q=&ean=` — products seen in observations, by name and/or EAN.
- `POST /items/{id}/links` — record a product-to-store link (`store`, `externalProductId?`, `ean?`).
- `GET /items/{id}/prices` — price history for each of an item's links.
- `GET /bargains?since=` — raw observations at/after `since` (epoch millis). The gateway
  returns data only; the app computes bargain reasons.
