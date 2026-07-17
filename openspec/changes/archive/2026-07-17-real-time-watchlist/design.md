## Context

This is a greenfield Android project. The repository currently contains only the assignment, implementation requirements, and design references. The application must fit a 6-8 hour challenge while still demonstrating layered architecture, persistence, dependency injection, Flow-based UI state, real Finnhub REST/WebSocket integration, and a usable fake-data mode.

The supplied design defines a Material 3 two-tab phone experience with light and dark themes. The common shell contains the title, settings action, connection banner, and tabs. Watchlist rows distinguish `LIVE` and `CACHED` prices; search rows expose a checkbox; settings is an in-app dialog.

## Goals / Non-Goals

**Goals:**

- Deliver the complete happy path in demo mode without network access or an API key.
- Keep Room as the source of persisted watchlist data and expose repository data through lifecycle-aware `StateFlow`s.
- Keep live-stream ownership in the data layer, with explicit lifecycle, subscription, retry, and authorization states.
- Make backend selection and API-key changes observable and safe to apply while the app is running.
- Preserve the supplied visual language while meeting accessibility requirements for touch targets, labels, semantics, and non-color status indicators.
- Make the core behavior testable with fakes and controllable time/randomness.

**Non-Goals:**

- Portfolio value, order placement, authentication of an app user, or account synchronization.
- Historical charts, persisted price history, background streaming, notifications, or widgets.
- Broad asset-class support beyond the symbols returned by Finnhub and the five stock symbols in demo mode.
- Production-grade encrypted database storage; only the API key is required to be protected at rest.
- A non-destructive Room migration strategy for this initial version.

## Decisions

### Layered single-module architecture

Use one Android application module with `data`, `domain`, and `ui` packages. Hilt constructor injection provides repositories and data sources. The UI talks to ViewModels, ViewModels talk to repository interfaces, and data sources remain hidden behind repositories. This is enough separation for a small challenge without the build and coordination cost of multiple Gradle modules.

Alternative considered: direct Compose-to-network/database access was rejected because it would make lifecycle behavior, fake backends, and tests harder to isolate.

### Object-based Navigation 3 routes

Use object routes for `WatchlistRoute` and `SearchRoute`, with a settings route represented as a dialog entry or dialog scene. Keep the tab selection in the navigation back stack so each tab is a real destination while the shared app shell owns the common banner and settings action.

Alternative considered: local `when` state for tabs was rejected because the requirements explicitly call for Navigation 3 and object-based routing.

### Room persistence and cached/live price overlay

Store each watchlist symbol, display name, and nullable last-known price in Room. Room values always render as `CACHED`. The financial repository maintains an in-memory map of live prices and source status over the persisted list. A live update is written back as the new last-known price while the overlay renders it as `LIVE`; clearing the overlay on disconnect immediately returns the row to `CACHED` without losing the latest value.

This avoids storing transient WebSocket state in the database and ensures the next launch has a useful snapshot even after the stream stops.

### Encrypted API-key DataStore

Use a singleton Preferences DataStore for the small settings payload, but encrypt the API-key value before it is written. Generate or retrieve an AES-GCM key from Android Keystore; persist only the nonce and ciphertext in DataStore. Expose the decrypted key through a repository Flow and never pass the raw key into UI state.

Alternative considered: plain Preferences DataStore does not satisfy the requirement. Encrypted SharedPreferences provides encryption but is a poorer fit for the required Flow-based observation and current Android storage guidance.

### Swappable financial backend

Define one financial backend contract with real and demo implementations. The real backend uses Finnhub REST search/quote calls and an OkHttp-compatible WebSocket client. The demo backend contains DOCS ($230), NVDA ($212), AAPL ($327), AMZN ($255), and MSFT ($395), returns fixed search/quote values, and emits a price update every five seconds. Each demo update changes the previous value by a random amount in the required range and clamps it to 90%-110% of the symbol base price. Inject the random source and clock so tests can assert behavior without waiting.

The repository selects demo when the stored key is blank. A non-blank key selects the real backend. Saving a changed key closes the old stream, resets transient live state, and starts the new backend when the foreground/watchlist conditions allow it.

### Explicit stream state machine

The repository exposes `Inactive`, `Connecting`, `Live`, `Retrying`, `Disconnected`, and `Unauthorized` states. A real stream is active only when the process is foregrounded, the watchlist is non-empty, and a real backend is selected. Watchlist changes update subscriptions; foreground changes start or stop the stream; a manual reconnect explicitly restarts a disconnected stream.

Connection attempts use delays of 1, 2, 4, and 8 seconds. While retrying, the UI exposes `Connecting...` only after two seconds without live updates. After the retry budget is exhausted, the repository remains disconnected and exposes a `Tap to reconnect` action. Authorization failures do not consume the retry budget and instead drive the API-key error banner.

### ViewModel and UI state boundaries

Use screen-level ViewModels: a watchlist ViewModel owns local filter/sort state and combines it with watchlist/price flows; a search ViewModel owns query, debounce, request state, quote results, and retry; an app/settings ViewModel owns the common connection banner and API-key actions. Compose collects each `StateFlow` with `collectAsStateWithLifecycle`.

Filter and sort state is local UI state, while watchlist contents, search results, connection status, and settings outcomes are repository-backed state. Missing prices remain nullable and render as an unavailable value rather than as zero.

### Test strategy

Prefer fake repositories/backends over mocks for behavior tests. Unit tests cover demo data, Room mapping, backend selection, stream transitions/backoff, ViewModel reducers, and Flow emissions with Turbine. Robolectric Compose tests cover accessible content and settings dialog behavior. Instrumentation tests cover demo search, adding an item and seeing its cached price, watchlist display, and opening settings. A test-only clock/random source makes the five-second demo loop deterministic.

### Documentation and formatting

Add a README with prerequisites, build/test commands, API-key setup, demo mode instructions, Finnhub plan and market-data limitations, Room's intentionally destructive initial migration, architecture, and tradeoffs. Configure ktlint for Compose-compatible Kotlin formatting and verification.

## Risks / Trade-offs

- **Finnhub rate limits or market closures** -> Keep demo mode fully functional, surface API error codes with retry, and document service limitations.
- **WebSocket protocol or authorization differences** -> Centralize parsing and connection mapping in the real backend and treat malformed/unauthorized messages as explicit stream states.
- **Encrypting a value inside DataStore adds custom crypto code** -> Keep the cryptographic primitive limited to Keystore-backed AES-GCM, never store plaintext, and test round trips plus key rotation/error handling.
- **A live price write can race with a disconnect** -> Make disconnect clear the in-memory overlay and serialize price persistence through the repository scope; persisted data remains a safe cached fallback.
- **Navigation 3 APIs may evolve while the challenge project is built** -> Pin compatible AndroidX versions and isolate route/scene wiring in the app shell.
- **Instrumentation tests are slower and environment-dependent** -> Keep core cases covered by fast fakes/Robolectric and make instrumentation tests use the demo backend with no network dependency.
- **Default destructive Room migration loses future schema data** -> Use it only for this initial version and call the limitation out in README as required.

## Migration Plan

There is no existing application to migrate. Create the initial Android project and Room schema directly. For future schema changes, replace the intentionally destructive migration with explicit Room migrations. Rollback for this initial delivery is uninstalling the app or reverting the project change; no server-side migration is required.

## Open Questions

- What application/package name and minimum Android SDK should the initial project use?
- Which stable versions of Navigation 3, Compose, Kotlin, AGP, and the test libraries are available in the evaluation environment?
- Should a search result with no usable quote remain addable with a null cached price, or should adding be disabled until a quote is available? The design currently favors allowing the item with an unavailable price so missing-price handling is visible.
