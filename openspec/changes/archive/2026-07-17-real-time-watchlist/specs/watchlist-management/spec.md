## ADDED Requirements

### Requirement: Persist the watchlist
The app SHALL persist each watchlist instrument by symbol, display name, and nullable last-known price in local Room storage. Persisted prices SHALL be treated as cached data.

#### Scenario: Add an instrument
- **WHEN** the user adds an instrument from search with a usable quote
- **THEN** the app stores the instrument and quote as its cached last-known price

#### Scenario: Restore after relaunch
- **WHEN** the app is launched after a previous watchlist change
- **THEN** the watchlist contains the previously persisted instruments and their last-known prices

#### Scenario: Add without a usable quote
- **WHEN** the user adds an instrument whose quote is unavailable
- **THEN** the app persists the instrument with a null price and renders an explicit unavailable value rather than zero

### Requirement: Manage watchlist membership
The app SHALL allow the user to remove a watchlist instrument from the row action menu, and the search membership control SHALL reflect the same persisted membership.

#### Scenario: Remove from the watchlist row
- **WHEN** the user opens a watchlist row action menu and chooses the remove action
- **THEN** the instrument is removed from local storage and no longer appears in the watchlist

#### Scenario: Remove from search
- **WHEN** the user unchecks an instrument that is in the watchlist
- **THEN** the instrument is removed from local storage and the watchlist membership becomes unchecked

### Requirement: Filter and sort watchlist content
The watchlist SHALL provide a single-line filter input and Symbol and Price sorting controls. The active sort field SHALL show an ascending or descending indicator, and selecting the active field again SHALL toggle its direction.

#### Scenario: Filter by symbol or name
- **WHEN** the user enters text in the watchlist filter
- **THEN** only instruments whose symbol or display name matches the text case-insensitively are shown

#### Scenario: Toggle sorting
- **WHEN** the user selects Symbol or Price
- **THEN** the list is sorted by that field and the active field displays its current direction

#### Scenario: Sort missing prices
- **WHEN** the watchlist contains an instrument without a price and the user sorts by Price
- **THEN** the missing-price instrument is placed consistently after priced instruments and is not treated as numeric zero

### Requirement: Present watchlist states and price provenance
The watchlist SHALL expose loading, empty, cached, live, and error states. Each available row price SHALL visibly identify whether it is `LIVE` or `CACHED`, and a disconnected stream SHALL cause rows to fall back to cached presentation.

#### Scenario: Empty watchlist
- **WHEN** the watchlist has no instruments after filtering
- **THEN** the content area shows `No results` and directs the user to add instruments from Search

#### Scenario: Cached row
- **WHEN** a row is rendered from the Room snapshot and has a last-known price
- **THEN** the price is labeled `CACHED`

#### Scenario: Live row
- **WHEN** a live price update is received for a watched symbol
- **THEN** the row updates to the new value and labels it `LIVE`

#### Scenario: Loading watchlist
- **WHEN** the initial persisted watchlist is being loaded
- **THEN** the content area shows a progress indicator instead of a partial or misleading empty state

### Requirement: Provide accessible watchlist interactions
The watchlist SHALL expose meaningful semantics and labels for settings, filter clearing, sort controls, row menus, price provenance, and movement indicators. Interactive controls SHALL meet Material touch-target guidance and SHALL not rely on color alone.

#### Scenario: Screen-reader row action
- **WHEN** an accessibility service focuses a watchlist row action
- **THEN** it announces an action that identifies the instrument and that it opens removal options
