# Price Tracking & Daily Bargain Reports — Implementation Brief

Executable task breakdown for an agent working without the originating conversation.
Read this whole file first, then work top-to-bottom. Each task lists context, the
change, tests, and a demo that must pass before the task is "done."

## Goal

Extend the offline **Erosketarako** shopping-list app so each item can be linked to
products in specific Spanish markets, have its price tracked over time, and surface
"bargains" via a daily on-device notification. Prices are gathered by Python scraper
microservices coordinated over **Kafka (KRaft)**, exposed to the app through a single
REST/JSON API gateway. The app keeps its local-first feel with a Room cache.

**Bargain definition** (rules are combinable, any subset can flag an item):
1. Price drop vs the item's own price history.
2. Price at or below a per-item target price.
3. Cheapest-market comparison across the item's linked stores.

**Daily report:** on-device local notification via WorkManager. No off-device delivery.

## Repository facts (verified — build on these)

- App: Kotlin + Jetpack Compose + Room. Path: `android/app/src/main/java/com/erosketarakoa/app/`.
- Single source of truth: `data/ShoppingRepository.kt`. **All UI reads/writes go through it.** Route every new write through the repository too.
- Room is at **schema version 7**, `data/local/AppDatabase.kt`, `exportSchema = false`.
  Entities today: `ListEntity`, `ItemEntity`. DAOs: `ListDao`, `ItemDao`.
- Migration style is **additive `ALTER TABLE` + new tables**, one `MIGRATION_x_y` object per step
  in `AppDatabase.companion`. Follow it exactly. Do NOT use destructive migrations.
- `ItemEntity` already has `supermarkets` (comma-joined String) and `category` (nullable String).
  There is a hardcoded `SUPERMARKET_OPTIONS` list somewhere in the item UI/data layer — keep it.
- `Clock` (`data/Clock.kt`) provides `newId()` (UUID) and `nowMillis()`. Reuse it; do not add another id/time source.
- Existing tests to extend, not replace:
  - `android/app/src/androidTest/.../data/local/MigrationTest.kt`
  - `android/app/src/androidTest/.../data/local/DaoTest.kt`
  - `android/app/src/androidTest/.../AddItemFlowTest.kt`
- The app currently has **no INTERNET permission and no networking library.** Adding them is a real
  change to its offline identity — scoped strictly to fetching the bargains feed, and only in Phase 4.

## Guardrails / lazy-by-default

- Build only what each task asks. No speculative abstractions (no interface with one impl,
  no factory for one product, no config for a value that never changes).
- Reuse existing patterns (`Clock`, DAO shape, migration shape, repository funnel) before writing new ones.
- Bargain rules live in **exactly one place: the Android app** (Task 3). The gateway serves raw price
  history; it does NOT re-implement the rules. Do not create a second copy server-side.
- Do NOT convert `SUPERMARKET_OPTIONS` into a Room table. A store is a name String on the product link
  until users can add custom stores (not in scope). Keep the hardcoded list.
- Keep scraper adapters isolated so a ToS-risky source can be disabled/swapped without touching the
  app, the gateway, or other adapters.
- Every non-trivial piece of logic ships with at least one runnable test that fails if the logic breaks.
- Verify each task's build/tests before moving on. A command exiting 0 is not proof the feature works —
  check against the demo criteria.

---

## Phase 1 — App-side data model & manual linking (works standalone, no backend)

### Task 1 — Extend local schema for product links, prices, and targets

**Context:** Prices and product-to-store links need real tables. Stores do not (keep `SUPERMARKET_OPTIONS`).

**Change:**
- Add Room entities under `data/local/`:
  - `ProductLinkEntity` — links an item to a store and an external product reference.
    Fields: `id` (UUID String, PK), `itemId` (FK → items.id), `store` (String, from `SUPERMARKET_OPTIONS`),
    `externalProductId` (String?, backend/source product id, null until Phase 4), `ean` (String?), `productName` (String?),
    `updatedAt` (Long), `isDeleted` (Boolean default false). Index on `itemId`.
  - `PriceEntity` — an observed price for a link.
    Fields: `id` (UUID String, PK), `linkId` (FK → product_links.id), `priceCents` (Long — store money as integer cents, never float),
    `currency` (String, default `"EUR"`), `observedAt` (Long), `source` (String), `updatedAt` (Long). Index on `linkId`.
- Add columns to `ItemEntity` via `ALTER TABLE`: `targetPriceCents` (INTEGER, nullable), `barcode` (TEXT, nullable).
  `category` already exists — do not re-add.
- Bump Room to **version 8**, add `MIGRATION_7_8` following the existing style: additive `ALTER TABLE items`
  for the two new columns, plus `CREATE TABLE` for `product_links` and `prices`. Register both new entities and
  the migration in `AppDatabase`.
- Add `ProductLinkDao` and `PriceDao` mirroring the shape of `ItemDao` (upsert, observe, soft-delete where relevant).

**Tests:**
- Extend `MigrationTest`: v7 → v8 preserves existing `lists`/`items` rows and new columns default to null.
- Extend `DaoTest`: CRUD for `ProductLinkDao` and `PriceDao` (insert link, insert prices, observe by item/link, delete).

**Demo:** App builds and runs unchanged. DB now stores links/prices/targets even though nothing populates them yet.

### Task 2 — Manual product-to-store linking and categorization in the UI

**Context:** Users seed data by hand until the backend exists. Everything persists locally.

**Change:**
- Extend the item editor screen so a user can:
  - set a per-item **target price** (entered in euros, stored as `targetPriceCents`),
  - set an optional **barcode/EAN**,
  - pick a **category** (manual; reuse existing category field),
  - **link the item to one or more stores** (from `SUPERMARKET_OPTIONS`) each with a manually entered external product reference.
- Add repository methods on `ShoppingRepository` for creating/updating/removing links and setting the target/barcode.
  All writes go through the repository; generate ids/timestamps via `Clock`.

**Tests:**
- ViewModel/repository unit tests: create/update/remove a link, set/clear target price, set barcode.
- Extend `AddItemFlowTest` (Compose): open an item, set a target, pick a category, link to a store with a reference, reopen and assert persistence.

**Demo:** In the running app, open an item → set target price → pick category → link to "Mercadona" with a product
reference → all persisted and visible on reopen.

### Task 3 — Local bargain engine + "Today's bargains" screen

**Context:** This is the ONLY home for bargain rules. Pure Kotlin, testable without Android.

**Change:**
- Add a bargain engine (plain Kotlin class/functions, no Android deps) that takes an item + its links + price
  history and returns any flags with a reason and the relevant store:
  - **below-target:** latest price ≤ `targetPriceCents`.
  - **price-drop:** latest price < previous observed price for the same link (define "previous" as the prior `observedAt`).
  - **cheapest-market:** across the item's links, the link with the lowest latest price (only meaningful with ≥2 linked stores).
  - Rules combine — one item can carry multiple reasons.
- Add a read-only **"Today's bargains"** Compose screen listing flagged items, each with its reason(s) and store.
  Read through the repository.

**Tests:** Unit-test each rule and their combinations against seeded `PriceEntity` history (deterministic; money in cents).

**Demo:** With hand-seeded price rows, the screen correctly lists drops, below-target items, and cheapest markets.
**End of Phase 1–2 the app is fully usable on manual/seeded data.**

---

## Phase 2 — Daily on-device report

### Task 4 — Daily bargain notification via WorkManager

**Context:** Offline, on-device only. Reuses the Task 3 engine over cached prices.

**Change:**
- Add a daily WorkManager job that runs the bargain engine over cached prices and posts a **summary** local
  notification (e.g. "3 bargains today"). Tapping it opens the Task 3 bargains screen (deep link / intent).
- Create a notifications channel. Add a settings toggle (on/off) and a time-of-day picker for the run.
- Extract the "build the summary from a list of flagged items" step into a pure function so it can be tested
  without WorkManager or Android notification internals.

**Tests:** Unit-test the summary logic (deterministic given seeded flagged items) and the notification content builder.

**Demo:** Trigger the worker manually (or set a near-future time) → a notification appears → tapping deep-links to
the bargains screen. All offline.

---

## Phase 3 — Backend skeleton (local), Kafka + gateway contract

> The app talks ONLY to the REST gateway. Kafka and scrapers are hidden behind it.
> Put all backend code under a new top-level `backend/` directory.

### Task 5 — Stand up Kafka (KRaft) + define topics and the source-adapter contract

**Context:** Kafka is the durable, replayable messaging backbone. Single-node is fine locally.

**Change:**
- Add `backend/docker-compose.yml` running a single-node **Kafka in KRaft mode** (no ZooKeeper).
- Define topics: `scrape.requests`, `price.observations`, `sources.registry`.
- Document a JSON message schema (in `backend/CONTRACT.md`) for:
  - a **scrape request** (target: EAN and/or name, optional store),
  - a **price observation** (store, external product id, ean, name, priceCents, currency, observedAt, source),
  - a **source registration** (source id/name, capabilities: supports EAN? name search?, published on startup).
- Money is integer **cents** everywhere in messages. Timestamps are epoch millis UTC.

**Tests:** A smoke test (script or pytest) that produces and consumes one sample `price.observations` message locally.

**Demo:** `docker compose up` brings Kafka online; a script publishes and reads back a sample price message.

### Task 6 — Aggregator + price DB (server side, NO bargain rules)

**Context:** Stores price history the gateway will serve. Bargain rules stay in the app (Task 3) — do NOT duplicate them here.

**Change:**
- Python aggregator service under `backend/aggregator/` that:
  - consumes `price.observations`, writes each to a **PriceDB** (SQLite locally; keep the DB layer swappable to Postgres),
  - keeps full price **history** (append, never overwrite),
  - tracks known sources by consuming `sources.registry` into a simple table/list.
- No bargain computation server-side.

**Tests:** Unit tests for storage (append history, query by product/ean/store) and source tracking; one integration
test feeding observations through Kafka into the DB.

**Demo:** Publish sample observations → they land in PriceDB with history → queryable directly.

### Task 7 — REST/JSON API gateway (the app's only contract)

**Context:** The single surface the app depends on. Fully hides Kafka.

**Change:**
- Gateway service under `backend/gateway/` exposing:
  - `GET /products/search?q=&ean=` — product search by name and/or EAN.
  - `POST /items/{id}/links` — record a product-to-store link.
  - `GET /items/{id}/prices` — price history for an item's links.
  - `GET /bargains?since=` — raw price data / recently changed prices the app's engine can flag.
    (Gateway returns data; it does NOT compute bargain reasons — the app does.)
- Reads from PriceDB/aggregator. Never exposes Kafka topics or internals.
- Publish an **OpenAPI spec** (`backend/gateway/openapi.yaml` or served at `/openapi.json`).

**Tests:** Endpoint contract tests against a seeded PriceDB (each endpoint's shape and status codes).

**Demo:** `curl` the gateway to search a product, link it, and fetch price history.

---

## Phase 4 — First real source adapters

### Task 8 — Open Prices (OFF) source adapter

**Context:** First real source; validates the barcode-matching path end to end. API: `https://prices.openfoodfacts.org/api/v1` (EAN-based).

**Change:**
- Python service under `backend/adapters/open_prices/` that:
  - self-registers on `sources.registry` at startup (capabilities: EAN yes),
  - consumes `scrape.requests` filtered to EAN targets,
  - queries the Open Prices API,
  - publishes normalized `price.observations` (cents, currency, source="open_prices").
- Keep it isolated — no shared code with other adapters beyond the documented message schema.

**Tests:** Unit tests with **mocked** Open Prices responses → correct normalized observations; a contract test that
it registers and responds to a scrape request. No live network in CI.

**Demo:** Request a price by EAN → adapter fetches from Open Prices → observation flows through Kafka → aggregator
stores it → gateway returns it.

### Task 9 — One scraper source adapter (name + EAN) as the reusable template

**Context:** The copy-paste template for all future scrapers. Pick one Spanish store.

**Change:**
- Python scraper under `backend/adapters/<store>/` implementing the same contract (self-register, consume
  `scrape.requests`, publish observations), including **name-search** matching in addition to EAN.
- Document it in `backend/adapters/README.md` as the template, with resilience and ToS notes (how to disable/swap
  a risky source without touching the app or gateway).

**Tests:** Unit tests against **saved fixture** HTML/JSON (no live network in CI) verifying parsing + normalization.

**Demo:** Request a product by name → scraper returns a normalized price → visible via the gateway feed.

---

## Phase 5 — Wire the app to the backend

### Task 10 — Add networking to the app and a remote data source

**Context:** First real change to the app's offline identity — scope it to the bargains/prices feed only.

**Change:**
- Add `INTERNET` permission to the manifest.
- Add Retrofit + OkHttp and a JSON lib (kotlinx.serialization or Moshi — match whatever the project already leans toward).
- Implement a `PriceApi` client for the gateway endpoints and a `RemotePriceDataSource`.
- Keep all of it **isolated behind `ShoppingRepository`** — the UI must not know about Retrofit.

**Tests:** API client tests against a mock web server (e.g. MockWebServer) for each endpoint.

**Demo:** From the app, search the gateway for a product and link an item to a real backend product id.

### Task 11 — Sync gateway data into Room; point the daily worker at it

**Context:** Ties everything together end to end. Offline must still work.

**Change:**
- Extend `ShoppingRepository` to pull `GET /bargains` and `GET /items/{id}/prices` into the Room cache
  (`PriceEntity` / `ProductLinkEntity`).
- Point the Task 4 worker at the freshly synced cached data.
- Offline fallback: if the fetch fails, run the engine over existing cached prices — no crash, no empty screen.
- De-dupe on sync (don't insert duplicate observations for the same link/observedAt).

**Tests:** Repository tests for merge/cache behavior: fresh fetch, offline fallback, dedupe.

**Demo:** Real backend prices appear on item screens; the daily notification reports bargains computed from live,
backend-sourced data end to end.

---

## Definition of done (per task)

- Android tasks: `./gradlew :app:assembleDebug` succeeds; relevant unit tests pass; androidTest passes where extended.
- Backend tasks: the task's tests pass; the demo command/script produces the stated result.
- No orphaned code: each phase ends in something demoable (app usable on manual data after Phase 2; backend
  independently verifiable after Phase 3–4; full loop after Phase 5).
