## Context

The current search flow combines instrument search and quote hydration into one repository operation. It waits for every quote before publishing results, and an authorization-classified failure from one symbol aborts the entire search. Finnhub search results can include symbols whose quote data is unavailable under the current plan or exchange coverage.

The desired flow is progressive: publish instruments first, then hydrate quote state per symbol. This keeps the search useful, prevents one symbol from poisoning the result set, and makes add eligibility explicit.

## Goals / Non-Goals

**Goals:**

- Render instrument results immediately after the search request succeeds.
- Hydrate quotes sequentially, one symbol at a time, to avoid a burst of requests.
- Track quote state independently for every result.
- Enable adding only after a usable quote is available.
- Keep removal available for existing watchlist members regardless of quote state.
- Preserve search-level authorization failures while treating symbol-level unavailable data as a row state.
- Keep network, timeout, server, and rate-limit failures retryable without enabling additions.
- Document the request-volume tradeoff and future rate-limit improvements in the README.

**Non-Goals:**

- Adding a new quote batch endpoint or changing the Finnhub API contract.
- Persisting transient quote-loading or quote-error state.
- Automatically hydrating quotes for the watchlist outside the existing live-stream flow.
- Adding historical prices or charts.

## Decisions

### Publish instruments before quote hydration

The repository/ViewModel boundary will separate instrument search from per-symbol quote lookup. A successful search publishes the instrument list immediately and initializes each row as pending. Quote hydration then updates rows independently.

This is preferred over keeping the current aggregate `List<SearchResult>` request because aggregate completion hides partial success and makes one symbol failure fatal to the whole query.

### Use a sequential hydration queue

The search ViewModel will process the result symbols in order and issue only one quote request at a time. A new query cancels the active queue, and quote updates must be associated with the active query so stale responses cannot update newer results.

Sequential requests are intentionally simple and reduce request bursts. They do not eliminate rate-limit risk for broad queries, so the README will identify visible-row limiting, caching, deduplication, pacing, and supported batch endpoints as future improvements.

### Model quote state separately from membership

Each result will carry a quote state such as pending, available, unavailable, or retryable, alongside persisted watchlist membership. Membership is not inferred from quote state.

The add/remove control follows this matrix:

| Membership | Quote state | Control |
|---|---|---|
| Not watched | Pending | Disabled |
| Not watched | Available | Add enabled |
| Not watched | Unavailable | Disabled |
| Not watched | Retryable | Disabled |
| Watched | Any state | Remove enabled |

This prevents new null-price entries while allowing users to remove an existing entry even if its current quote cannot be loaded.

### Classify failures by scope and retryability

Search request authorization failures remain query-level errors and continue to drive the common API-key banner. A non-retryable error returned while looking up one symbol becomes that row's unavailable state and does not discard other results.

Network failures, HTTP 5xx responses, HTTP 408, and HTTP 429 remain retryable. Other symbol-level 4xx responses, including symbol or entitlement failures such as 403/404, become unavailable. The backend error model will preserve the actual HTTP status rather than hard-coding 403 as 401.

Retryable quote failures will not enable adding. Retrying unresolved rows is bounded and explicit rather than an unbounded automatic loop, preventing rate-limit amplification.

### Keep transient state out of persistence

Only a successfully added symbol with a usable quote is written as a new watchlist entry. Existing nullable cached-price storage remains unchanged so previously persisted data and stale/offline watchlist states continue to render safely.

### Document the rate-limit tradeoff

The README tradeoffs section will explain that one search can result in one quote request per symbol, even when those requests are sequential. It will name likely future optimizations without presenting them as implemented behavior.

## Risks / Trade-offs

- **Sequential hydration makes broad searches slow** -> Show rows immediately, expose per-row pending state, and document future visible-row limiting and caching improvements.
- **A new query can race with old quote responses** -> Cancel the old hydration job and associate updates with the active query generation.
- **A 403 can mean authorization or symbol entitlement** -> Classify search-level authorization separately from symbol-level quote failure and preserve endpoint/status context.
- **429 is a 4xx but is retryable** -> Treat rate limiting as an explicit retryable category rather than generic terminal 4xx.
- **Retrying can amplify rate limits** -> Use explicit bounded retry behavior and do not retry indefinitely in the background.
- **Existing watchlist entries may have null cached prices** -> Keep nullable persistence and make removal available regardless of quote state.

## Migration Plan

No database migration is required. The change updates in-memory search state and quote orchestration, while retaining the existing nullable cached-price column.

Implementation should first add the per-result state and repository/backend error classification, then update the ViewModel and UI, followed by tests and README documentation. Rollback is a code revert; persisted watchlist data remains compatible.

## Open Questions

- Whether retryable quote rows should expose a per-row retry action, a single retry-all action, or both.
- Whether the UI should show the numeric unavailable status for a symbol or use only a user-facing `Unavailable` label.
