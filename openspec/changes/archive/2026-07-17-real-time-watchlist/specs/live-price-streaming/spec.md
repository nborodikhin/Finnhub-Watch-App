## ADDED Requirements

### Requirement: Control real-time stream lifetime
The financial data repository SHALL keep a real WebSocket connection active only while the app is foregrounded, the watchlist is non-empty, and a real backend is selected. It SHALL stop the connection when any of those conditions becomes false.

#### Scenario: Start when conditions are met
- **WHEN** the app enters the foreground with a non-empty watchlist and a non-blank API key
- **THEN** the repository starts connecting and subscribes to the current watchlist symbols after the socket is ready

#### Scenario: Stop in the background
- **WHEN** the app leaves the foreground
- **THEN** the repository closes the real stream and stops presenting live overlays

#### Scenario: Stop for an empty watchlist
- **WHEN** the last watchlist instrument is removed
- **THEN** the repository unsubscribes or closes the stream and exposes no active live connection

### Requirement: Apply live price updates
The repository SHALL parse real trade updates, associate each update with its symbol, update the latest known price, and expose the value as live while the stream is healthy.

#### Scenario: Receive a subscribed trade
- **WHEN** the WebSocket receives a valid trade for a subscribed symbol
- **THEN** the corresponding watchlist row updates to that price and is labeled `LIVE`

#### Scenario: Ignore an unknown symbol
- **WHEN** the stream receives a valid trade for a symbol not currently in the watchlist
- **THEN** the repository does not add a new watchlist item

### Requirement: Reconnect with bounded exponential backoff
The repository SHALL expose connection status and retry a failed real stream with delays of 1, 2, 4, and 8 seconds. After those attempts fail, it SHALL remain disconnected until a manual reconnect or a qualifying watchlist/foreground/API-key change occurs.

#### Scenario: Retry a disconnected stream
- **WHEN** an active real stream fails before reaching a healthy state
- **THEN** the repository exposes retrying status and schedules attempts using the required backoff sequence

#### Scenario: Exhaust retries
- **WHEN** all four retry attempts fail
- **THEN** the repository exposes a disconnected state and stops automatic retries

#### Scenario: Manually reconnect
- **WHEN** the user activates the reconnect action while disconnected
- **THEN** the repository resets the retry budget and starts a new connection attempt

### Requirement: Present connection and stale-data states
The common UI SHALL show a connecting notification after two seconds without live updates while reconnection is active. Once automatic retries are exhausted, it SHALL show a reconnect action. When live updates are unavailable, watchlist prices SHALL be labeled `CACHED`.

#### Scenario: Reconnecting notification
- **WHEN** live updates have been unavailable for at least two seconds and another connection attempt is scheduled
- **THEN** the common banner displays `Connecting...`

#### Scenario: Disconnected notification
- **WHEN** the retry budget is exhausted
- **THEN** the common banner offers `Tap to reconnect`

#### Scenario: Fall back to cached values
- **WHEN** the stream disconnects after a live value was displayed
- **THEN** the row keeps the latest known numeric value but changes its provenance label to `CACHED`

### Requirement: Provide demo live updates
When no API key is configured, the demo backend SHALL provide DOCS, NVDA, AAPL, AMZN, and MSFT with base prices of $230, $212, $327, $255, and $395 respectively. It SHALL return fixed quote values and emit simulated updates every five seconds, with each new value bounded to 90%-110% of its base price.

#### Scenario: Demo stream for a watched instrument
- **WHEN** the user watches a predefined demo instrument while the app is foregrounded
- **THEN** the row receives simulated updates and displays the resulting value as `LIVE`

#### Scenario: Demo mode has no network dependency
- **WHEN** the device has no network connection and the API key is blank
- **THEN** searching, adding, and observing demo instruments remain usable

### Requirement: Surface authorization failures
The repository SHALL distinguish authorization failures from transient connection failures and SHALL expose them without continuing an automatic retry loop.

#### Scenario: Unauthorized stream
- **WHEN** Finnhub rejects the WebSocket token or reports an unauthorized API response
- **THEN** the stream enters an authorization error state and the common banner says `API key error, update in settings`
