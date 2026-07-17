## ADDED Requirements

### Requirement: Open and edit settings
The app SHALL provide a settings action in the top-right app bar that opens a dialog containing the text `finnhub.io API key (leave empty for demo mode)`, a plain-text API-key field, and Save and Cancel actions.

#### Scenario: Open settings
- **WHEN** the user activates the settings action
- **THEN** the settings dialog is displayed with the API-key field and Save/Cancel actions

#### Scenario: Cancel settings
- **WHEN** the user edits the API key and activates Cancel
- **THEN** the dialog closes without changing the stored key or backend

### Requirement: Protect and persist the API key
The app SHALL store the API key encrypted at rest using a Keystore-protected key and SHALL expose only the required configuration state to UI consumers.

#### Scenario: Persist a real API key
- **WHEN** the user saves a non-blank API key
- **THEN** the key is recoverable after relaunch through the settings repository, but plaintext key material is not written to the settings store

#### Scenario: Persist demo mode
- **WHEN** the user saves an empty API key
- **THEN** the stored configuration selects demo mode

### Requirement: Hot-swap the active backend
Saving a new API key SHALL update the financial repository while the app is running, close the previous backend connection, clear transient live status, and establish the newly selected backend when its stream conditions are met.

#### Scenario: Switch from demo to real mode
- **WHEN** the user saves a non-blank API key while the app is foregrounded with watched instruments
- **THEN** demo updates stop, the real backend begins connecting, and the common connection state is updated

#### Scenario: Switch from real to demo mode
- **WHEN** the user saves a blank API key
- **THEN** the real connection is closed and the demo backend becomes active without requiring an app restart

### Requirement: Show configuration and authorization banners
The common app shell SHALL show a demo warning when the API key is blank and SHALL show an API-key error banner after an authorization failure. The settings action SHALL remain available from both tabs and all connection states.

#### Scenario: Demo warning
- **WHEN** no API key is configured
- **THEN** the common banner says `Demo mode, set api key in settings`

#### Scenario: API-key error
- **WHEN** the active backend reports an authorization error
- **THEN** the common banner says `API key error, update in settings`

#### Scenario: Settings remains available
- **WHEN** either Watchlist or Search is visible with a banner
- **THEN** the settings action remains accessible in the app bar
