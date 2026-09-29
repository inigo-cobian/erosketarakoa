# Source adapters

Each adapter turns one price source into normalized `price.observations` messages. Adapters
are **isolated**: they share only `backend/common/kafka_io.py` and the message contract in
`backend/CONTRACT.md`. No adapter imports another. This isolation is the point — a source
with fragile HTML or restrictive terms can be disabled or replaced without touching the app,
the gateway, or any sibling adapter.

## Adapters

- `open_prices/` — Open Food Facts Open Prices API. EAN only. Community data, permissive.
- `mercadona/` — **the reusable template** (a Spanish store). EAN **and** name search.

## The template (`mercadona/`)

Copy the directory and change five things:

1. `SOURCE_ID` — unique id published on `sources.registry`.
2. `STORE_NAME` — human store name that lands in each observation.
3. `SEARCH_URL` — the source endpoint.
4. `fetch(query, opener)` — how to retrieve the raw payload (JSON API or HTML page).
5. `parse_search(payload, ean, observed_at)` — **pure** mapping to observations. Keep it
   pure so it can be tested against a saved fixture with no network.

Everything else (`registration`, `handle_request`, `run`) stays the same, so a new store is
mostly a parser plus a URL.

## Contract every adapter honors

- On startup, publish one `sources.registry` message declaring capabilities
  (`supportsEan`, `supportsNameSearch`).
- Consume `scrape.requests`; handle the target types you support (EAN and/or name), ignore
  the rest by returning zero.
- Publish `price.observations` with **money in integer cents** and `source` set to your id.

## Resilience & Terms of Service

- **Scraping risk.** HTML scrapers break when markup changes and may run against a store's
  ToS. Treat every scraper as best-effort: wrap each request so one failure does not stop the
  consumer loop (see `run()`), and never let a parse error propagate to Kafka.
- **Disable a source.** Stop running its process (or scale its container to zero). The app and
  gateway keep working on whatever history already landed in the PriceDB — nothing else changes.
- **Swap a source.** Replace or fork the adapter directory. Because the only shared surface is
  the message contract, downstream services never notice which adapter produced an observation.
- **Rate limits / politeness.** Add delays or caching inside `fetch` for a specific source;
  this stays local to that adapter.

## Testing

Adapters are tested against **saved fixtures** (`tests/fixtures/`) with an injected `opener`,
so CI never makes live network calls. See `tests/test_open_prices_adapter.py` and
`tests/test_mercadona_adapter.py`.
