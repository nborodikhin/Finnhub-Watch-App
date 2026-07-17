## Why

Finnhub symbol search can return instruments whose quote endpoint is unavailable, but the current flow treats one quote failure as a failed search and can report a symbol-level HTTP 403 as API error 401. Search results should remain useful while quote availability is resolved independently, especially because broad queries can consume Finnhub rate-limit budget.

## What Changes

- Show instrument search results as soon as the search endpoint returns, before quote hydration completes.
- Request quotes sequentially for each result and expose independent pending, available, unavailable, and retryable states.
- Enable adding only after a usable quote is received; keep removal available for already-watched symbols regardless of quote state.
- Treat terminal symbol-level 4xx responses as unavailable without failing the whole search; preserve search-level authorization failures as global errors.
- Keep network, 5xx, and rate-limit responses retryable, with adding disabled until a usable quote is received.
- Preserve the actual HTTP status when mapping backend errors instead of converting 403 into 401.
- Add tests for partial results, sequential quote hydration, per-symbol state transitions, membership behavior, cancellation, and retry handling.
- Document the per-symbol quote request tradeoff and potential rate-limit improvements in the README.

## Capabilities

### New Capabilities

- `progressive-search-quote-hydration`: Progressive search result rendering, per-symbol quote availability, add eligibility, and retry semantics.

### Modified Capabilities

No existing main capability specs are present. This change intentionally refines the behavior described by the archived instrument-search and watchlist-management artifacts.

## Impact

- Affects the financial backend error model and REST search/quote orchestration.
- Affects `FinancialRepository`, `SearchViewModel`, search result UI state, membership controls, and related tests.
- Adds no new external dependency or API surface.
- Updates README tradeoffs to explain sequential per-symbol quote requests, rate-limit implications, and possible future optimizations.
