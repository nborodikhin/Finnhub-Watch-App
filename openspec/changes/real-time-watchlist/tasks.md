## 1. Project Setup

- [x] 1.1 Create the initial Android application, package name, minimum SDK, compile SDK, manifest, Gradle wrapper, and edge-to-edge Activity configuration.
- [x] 1.2 Add and pin compatible Kotlin, Compose, Material 3, Navigation 3, Lifecycle, Room, DataStore, Keystore/security, Hilt, networking, serialization, and testing dependencies.
- [x] 1.3 Configure Hilt application startup, ktlint checks/formatting, debug/demo defaults, and a build that can run without a Finnhub API key.

## 2. Domain And Persistence

- [x] 2.1 Define instrument, quote, watchlist, price provenance, backend, request error, and connection-state models with nullable-price handling.
- [x] 2.2 Implement the Room entity, DAO, database, repository mapping, uniqueness by symbol, and the intentionally destructive initial migration.
- [x] 2.3 Implement the Keystore-backed AES-GCM API-key store using a singleton DataStore that persists only encrypted key material.
- [x] 2.4 Add repository interfaces and Hilt bindings so production and test implementations can be swapped without UI changes.

## 3. Financial Backends And Streaming

- [x] 3.1 Implement the demo catalog with DOCS, NVDA, AAPL, AMZN, and MSFT, fixed base quotes, query matching, bounded five-second updates, and injectable clock/randomness.
- [x] 3.2 Implement Finnhub REST search and quote data sources, response mapping, HTTP error-code handling, missing-price handling, and authorization classification.
- [x] 3.3 Implement Finnhub WebSocket connection, subscribe/unsubscribe messages, trade-message parsing, malformed-message handling, close/error mapping, and authorization detection.
- [x] 3.4 Implement the financial repository stream state machine, foreground/watchlist/backend activation conditions, subscription changes, 1/2/4/8-second retry backoff, retry exhaustion, and manual reconnect.
- [x] 3.5 Persist every usable live price as the latest cached snapshot while keeping live provenance transient and clearing it on disconnect, backgrounding, or backend switch.

## 4. ViewModels And UI State

- [x] 4.1 Implement the watchlist ViewModel with lifecycle-safe StateFlow state, filter-by-symbol/name, Symbol/Price sorting, direction toggling, null-price ordering, and row removal.
- [x] 4.2 Implement the search ViewModel with debounced queries, active-backend search, quote results, membership state, clear, loading, empty, error, authorization, and retry behavior.
- [x] 4.3 Implement app/settings state for API-key editing, demo/real backend banners, connection banners, foreground changes, and reconnect actions without exposing raw keys in UI state.

## 5. Compose UI And Navigation

- [x] 5.1 Implement the Material 3 light/dark theme, shared app bar, settings action, common banners, and Navigation 3 object routes for Watchlist and Search.
- [x] 5.2 Implement the Watchlist screen, filter/sort bar, rows, LIVE/CACHED provenance, movement indicators, empty/loading/error states, row removal menu, semantics, and previews.
- [x] 5.3 Implement the Search screen, search bar, quote result rows, accessible membership controls, clear action, empty/loading/API-error/retry states, and previews.
- [x] 5.4 Implement the settings dialog with plain-text entry, exact explanatory label, Save/Cancel behavior, accessible labels, and previews.
- [x] 5.5 Verify edge-to-edge insets, keyboard behavior, touch-target sizes, contrast, TalkBack semantics, and readable state changes across phone light/dark themes.

## 6. Tests And Documentation

- [x] 6.1 Add unit tests for demo data, persistence mapping, encrypted settings round trips, backend selection, missing prices, stream state transitions, backoff, and live-to-cached fallback.
- [x] 6.2 Add Turbine/ViewModel tests for search, watchlist filtering/sorting, membership updates, banners, retry, and settings backend switching.
- [x] 6.3 Add Robolectric Compose tests for key UI states, accessibility labels, row removal, search membership, and settings dialog opening.
- [x] 6.4 Add instrumentation tests for demo search, adding an instrument and seeing its cached price, watchlist restoration/display, and opening settings.
- [x] 6.5 Add README setup/run instructions, demo-mode instructions, API-key guidance, architecture/tradeoffs, Finnhub limitations, destructive migration note, test commands, and tooling/AI assistance disclosure.
- [x] 6.6 Run ktlint, unit tests, Robolectric tests, instrumentation tests where an emulator is available, and a debug build; resolve failures and confirm the demo path works offline.

## 7. Landing

- [ ] 7.1 Implementation Gate - commit the implementation (do not ask), then run `plannotator review`. Annotations are instructions, not suggestions: carry out every one (say so briefly if you disagree, then do it anyway), and sync the specs yourself if the code now does something they do not describe. Re-review until the human approves. Tick ONLY once they have.
- [ ] 7.2 Land - invoke the `openspec-archive-guard` skill, then archive the change and commit. If the repo has a GitHub remote, push the branch and give the human a compare link to open the PR themselves (do not create it); if it is local-only, tell the human it is landed and theirs to merge or squash back into the base branch (see spec-review.md).
