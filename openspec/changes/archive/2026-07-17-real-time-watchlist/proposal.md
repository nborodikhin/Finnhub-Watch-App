## Why

The repository currently contains the assignment, implementation requirements, and visual design, but no Android project. This change establishes the complete real-time watchlist experience so reviewers can search instruments, persist a watchlist, observe cached and live prices, and run the core flow without a Finnhub account through demo mode.

## What Changes

- Create a Kotlin Android application using Jetpack Compose, Material 3, Coroutines/Flow, dependency injection, and Navigation 3 object-based routes.
- Add a two-tab Watchlist/Search experience matching the supplied light and dark designs, including loading, empty, error, cached, live, connecting, and reconnect states.
- Add persistent watchlist storage with Room and encrypted API-key storage.
- Add instrument search and quote retrieval through Finnhub REST APIs.
- Add lifecycle-aware Finnhub WebSocket streaming with subscription management, reconnection backoff, manual reconnect, and hot-swappable API-key configuration.
- Add a documented demo backend with the five specified instruments, fixed quotes, bounded simulated five-second price updates, and an explanatory demo banner.
- Add accessible interaction semantics, previews for major Compose UI blocks, required unit/Robolectric/instrumentation coverage, ktlint checks, and README documentation for setup, tradeoffs, limitations, and demo mode.

## Capabilities

### New Capabilities

- `watchlist-management`: Persisted watchlist items, cached prices, filtering, sorting, add/remove interactions, and watchlist UI states.
- `instrument-search`: Debounced instrument search, quote display, add/remove controls, empty/loading/error states, and retry behavior.
- `live-price-streaming`: Real-time and demo price updates, connection lifecycle, source labels, subscriptions, reconnect backoff, and stale-data behavior.
- `api-key-settings`: Settings dialog, encrypted API-key persistence, demo/real backend selection, authorization errors, and runtime backend switching.

### Modified Capabilities

None. This repository has no existing product capability specifications.

## Impact

- Adds the Android project, application module, resources, Compose UI, ViewModels, repositories, Room database, encrypted settings store, and test suites.
- Integrates Finnhub REST search/quote endpoints and Finnhub WebSocket trade streaming; behavior remains usable when the service is unavailable through demo mode and cached data.
- Adds AndroidX Compose, Material 3, Navigation 3, Lifecycle, Room, DataStore/security, Hilt, networking, serialization, and testing dependencies as appropriate.
- Requires README documentation for API-key handling, Finnhub plan/market-data limitations, demo mode, default destructive Room migration, build commands, and test commands.
