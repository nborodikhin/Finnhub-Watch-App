## 1. Error And Quote State Model

- [ ] 1.1 Add explicit per-symbol quote states for pending, available, unavailable, and retryable outcomes.
- [ ] 1.2 Preserve actual HTTP status codes and classify search-level authorization separately from symbol-level quote failures.
- [ ] 1.3 Treat network, HTTP 408, HTTP 429, and HTTP 5xx quote failures as retryable; treat other symbol-level 4xx failures as terminal unavailable states.

## 2. Progressive Repository And ViewModel Flow

- [ ] 2.1 Split instrument search completion from per-symbol quote lookup so the instrument list can be published immediately.
- [ ] 2.2 Implement a sequential quote-hydration queue with cancellation and active-query protection when the user changes the query.
- [ ] 2.3 Continue hydrating later symbols after an individual unavailable or retryable quote outcome.
- [ ] 2.4 Keep membership state synchronized with the watchlist while preserving each row's quote state.
- [ ] 2.5 Implement bounded explicit retry behavior for unresolved retryable quote rows without an unbounded automatic retry loop.

## 3. Search UI And Membership Behavior

- [ ] 3.1 Render pending, available, unavailable, and retryable quote states independently in search rows.
- [ ] 3.2 Enable adding only for non-member rows with a usable quote, while keeping removal enabled for existing members in every quote state.
- [ ] 3.3 Add accessible labels and state announcements for quote availability, disabled add controls, and removal actions.
- [ ] 3.4 Preserve the common API-key banner for search-level authorization failures without replacing the whole result list for symbol-level quote failures.

## 4. Tests And Documentation

- [ ] 4.1 Add backend and repository tests for HTTP status preservation, symbol-level 403 handling, 200 responses without usable prices, and retryable network/408/429/5xx outcomes.
- [ ] 4.2 Add ViewModel tests for immediate results, sequential hydration, cancellation, partial failures, retry behavior, and membership changes during hydration.
- [ ] 4.3 Add Compose tests for pending, available, unavailable, and retryable rows, including add gating and removal of existing members.
- [ ] 4.4 Fix the existing Robolectric SearchScreenTest runner/configuration and ktlint violations so the full local test and formatting checks pass.
- [ ] 4.5 Update README tradeoffs to explain one quote request per returned symbol, sequential hydration, rate-limit implications, and potential visible-row limiting, caching, pacing, deduplication, or batch-request improvements.

## 5. Landing

- [ ] 5.1 Implementation Gate - commit the implementation (do not ask), then run `plannotator review`. Annotations are instructions, not suggestions: carry out every one (say so briefly if you disagree, then do it anyway), and sync the specs yourself if the code now does something they do not describe. Re-review until the human approves. Tick ONLY once they have.
- [ ] 5.2 Land - invoke the `openspec-archive-guard` skill, then archive the change and commit. If the repo has a GitHub remote, push the branch and give the human a compare link to open the PR themselves (do not create it); if it is local-only, tell the human it is landed and theirs to merge or squash back into the base branch (see spec-review.md).
