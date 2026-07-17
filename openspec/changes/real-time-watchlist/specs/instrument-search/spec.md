## ADDED Requirements

### Requirement: Search for instruments
The Search tab SHALL provide a text input, debounce input changes before requesting results, and query the active financial backend for matching instruments. A blank query SHALL not issue a remote search request.

#### Scenario: Search in demo mode
- **WHEN** the user enters a query matching one of the predefined demo instruments
- **THEN** the matching instrument appears in the search results without requiring network access

#### Scenario: Search loading
- **WHEN** a non-blank query has been submitted and the backend request is in progress
- **THEN** the content area shows a progress indicator

#### Scenario: Clear the query
- **WHEN** the user activates the clear action on a non-empty search query
- **THEN** the query is cleared and the result content returns to the blank-query state

### Requirement: Show quote-backed search results
Each search result SHALL show its symbol, display name, and the current quote returned by the quote API. Search prices SHALL not be labeled `LIVE` or `CACHED` because Search does not subscribe to the live stream.

#### Scenario: Search result has a quote
- **WHEN** an instrument search result has a usable quote
- **THEN** the result displays the quote as a formatted price and provides its watchlist membership control

#### Scenario: Search result has no quote
- **WHEN** the quote API returns no usable price for a search result
- **THEN** the result displays an explicit unavailable value and does not display a fabricated zero price

### Requirement: Add and remove search results
The Search tab SHALL provide an accessible checkbox or equivalent membership control for every result. Adding a result SHALL persist its instrument and available quote as cached watchlist data.

#### Scenario: Add from search
- **WHEN** the user checks a search result that is not in the watchlist
- **THEN** the instrument is persisted, its available quote becomes the cached watchlist price, and the control remains checked

#### Scenario: Existing membership
- **WHEN** a search result is already in the watchlist
- **THEN** its membership control is checked when rendered

### Requirement: Handle search errors and empty results
The Search tab SHALL show `No results` when a successful query returns no instruments. Request failures SHALL show `API Error: <code>` and an accessible retry action. Authorization failures SHALL also be exposed through the common API-key error banner.

#### Scenario: No search matches
- **WHEN** a non-blank search completes successfully with no matches
- **THEN** the content area shows `No results`

#### Scenario: Retry an API error
- **WHEN** a search request fails with an API or request error
- **THEN** the content area shows the error code and a retry action that repeats the failed query

#### Scenario: Unauthorized search
- **WHEN** the backend reports an authorization failure during search
- **THEN** the common banner says `API key error, update in settings`
