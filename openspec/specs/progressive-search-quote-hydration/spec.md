# progressive-search-quote-hydration Specification

## Purpose
TBD - created by archiving change progressive-search-quote-hydration. Update Purpose after archive.
## Requirements
### Requirement: Publish search results before quote hydration completes
The Search tab SHALL display instrument matches immediately after a successful instrument-search request and SHALL hydrate quote data independently for each returned symbol. Quote requests SHALL be issued one at a time, and a newer query SHALL cancel or supersede hydration for the previous query.

#### Scenario: Search results precede quotes
- **WHEN** the instrument-search request succeeds while quote requests have not completed
- **THEN** the matching instruments are visible with a pending quote state instead of a global loading or error state

#### Scenario: Hydrate symbols sequentially
- **WHEN** multiple instruments are returned by one search
- **THEN** the app requests at most one symbol quote at a time and updates each corresponding row independently

#### Scenario: Replace an active query
- **WHEN** the user submits a new non-blank query while quote hydration for the previous query is pending
- **THEN** the previous hydration work is cancelled or superseded and its results do not update the new query's rows

### Requirement: Represent per-symbol quote availability
Each search result SHALL expose a quote state of pending, available, unavailable, or retryable. A usable positive quote SHALL be available. A successful response without a usable current price SHALL be unavailable and SHALL display an explicit unavailable value rather than zero.

#### Scenario: Quote becomes available
- **WHEN** a symbol quote request returns a usable current price
- **THEN** that row displays the formatted price and transitions to the available state

#### Scenario: Quote has no usable price
- **WHEN** a symbol quote request succeeds without a usable current price
- **THEN** that row displays an explicit unavailable value and does not display a fabricated zero price

#### Scenario: Symbol quote is unavailable
- **WHEN** a symbol quote request returns a non-retryable symbol-level 4xx response
- **THEN** that row remains visible, transitions to unavailable, and does not fail the other search results

#### Scenario: Quote request is retryable
- **WHEN** a symbol quote request fails with a network error, HTTP 408, HTTP 429, or HTTP 5xx response
- **THEN** that row transitions to retryable, remains unable to be added, and remains eligible for bounded explicit retry

### Requirement: Gate adding on usable quote data
The Search tab SHALL enable adding a result only while its quote state is available. Pending, unavailable, and retryable results SHALL not be addable.

#### Scenario: Pending result cannot be added
- **WHEN** a search result is still waiting for its quote
- **THEN** its membership control is disabled for adding

#### Scenario: Priced result can be added
- **WHEN** a search result has a usable quote and is not in the watchlist
- **THEN** its accessible membership control allows adding and persists the instrument with that quote as cached data

#### Scenario: Unavailable result cannot be added
- **WHEN** a search result has a terminal unavailable or retryable quote state and is not in the watchlist
- **THEN** its membership control does not allow adding

### Requirement: Allow removal independent of quote state
An instrument already in the watchlist SHALL remain removable from Search regardless of whether its current search quote is pending, available, unavailable, or retryable.

#### Scenario: Remove while quote is unavailable
- **WHEN** a watched search result has an unavailable or retryable quote state and the user activates its membership control
- **THEN** the instrument is removed from local watchlist storage

#### Scenario: Membership reflects persistence
- **WHEN** watchlist membership changes while quote hydration is in progress
- **THEN** the result keeps its current quote state while its membership control reflects the persisted membership

### Requirement: Preserve error scope and status
The app SHALL preserve search-level authorization failures as common API-key errors, SHALL keep symbol-level non-retryable quote failures local to their rows, and SHALL preserve the actual HTTP status code when exposing backend errors.

#### Scenario: Search authorization fails
- **WHEN** the instrument-search request returns an authorization failure
- **THEN** the Search tab shows the API error state and the common banner says `API key error, update in settings`

#### Scenario: One symbol quote is forbidden
- **WHEN** instrument search succeeds but one symbol quote request returns HTTP 403
- **THEN** the other search results remain visible, the affected symbol is unavailable, and the entire search is not shown as API Error: 401

#### Scenario: Preserve HTTP status
- **WHEN** a backend request fails with HTTP 403
- **THEN** the propagated error retains status code `403` rather than being converted to `401`

### Requirement: Document quote request rate-limit tradeoffs
The README SHALL document that a search may perform one quote request per returned symbol, that hydration is sequential, and that broad searches can consume rate-limit budget. It SHALL identify potential improvements without claiming they are implemented.

#### Scenario: README explains request volume
- **WHEN** a reviewer reads the Finnhub tradeoffs documentation
- **THEN** it explains per-symbol quote request volume, sequential hydration, rate-limit implications, and potential visible-row limiting, caching, pacing, deduplication, or batch-request improvements
