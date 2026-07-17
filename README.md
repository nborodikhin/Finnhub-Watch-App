# Finnhub Watch

Finnhub Watch is a small Kotlin/Jetpack Compose app for searching instruments, persisting a watchlist, and displaying cached and live prices.

## Run

Requirements:

- Android SDK 36
- JDK 17 or newer
- A device or emulator running API 26 or newer

Build and install the debug app:

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

GitHub actions are set to run on every commit to main to automatically build the debug apk:
- Go to the Actions tab: https://github.com/nborodikhin/Finnhub-Watch-App/actions
- Click the most recent run of "Build Debug APK" (should be from the workflow push, or triggered by your recent commits).
- Scroll to the bottom of the run summary page — under Artifacts, you'll see debug-apk.
- Click it to download a zip containing app-debug.apk.

The application id is `com.example.finnhubwatch`.

## Demo Mode

The app starts in demo mode when the API key is empty. Open Settings from the top-right app-bar action to enter a Finnhub key or return to demo mode.

Demo mode does not require network access and includes:

- DOCS: $230
- NVDA: $212
- AAPL: $327
- AMZN: $255
- MSFT: $395

Search and quote values are fixed. Watched instruments receive simulated updates every five seconds. Each update changes the previous value by a random amount between -$1,000 and +$1,000, then clamps the result to 90%-110% of its base price. Tests inject the random source and clock.

## Finnhub Mode

With a non-empty API key, search and quote requests use Finnhub REST APIs and watched symbols use the Finnhub WebSocket trade stream. The key is encrypted with an AES-GCM key held by Android Keystore; DataStore contains only the encrypted payload.

Prices use Kotlin `Double` throughout the domain and data layers because Finnhub exposes quote and trade prices as numeric JSON values. Formatting to currency text happens only in the Compose UI.

The free Finnhub plan may have rate limits, delayed or unavailable market data, symbol coverage differences, and WebSocket restrictions. The app surfaces API error codes, authorization errors, missing prices, reconnecting, and exhausted retry states. Demo mode is the reliable way to review the core flow outside market hours or without a valid key.

## Architecture

```text
Compose UI
  -> screen ViewModels and StateFlow
  -> repositories
     -> Room watchlist database
     -> encrypted API-key DataStore
     -> demo backend or Finnhub REST/WebSocket backend
```

The Room database is the source of persisted watchlist snapshots. Live prices are held as an in-memory overlay and written back as the latest cached value. When streaming stops, rows keep their last numeric value but switch from `LIVE` to `CACHED`.

The real stream is active only while the app is foregrounded, the watchlist is non-empty, and a real API key is configured. Reconnect delays are 1, 2, 4, and 8 seconds. After the retry budget is exhausted, the user can request a reconnect from the banner.

Navigation 3 object routes model the Watchlist and Search tabs. Settings is a Material 3 dialog. The project uses Hilt constructor injection, Room, DataStore, Coroutines/Flow, OkHttp, Kotlin serialization, and Material 3.

## Tests And Checks

```bash
./gradlew ktlintCheck
./gradlew :app:testDebugUnitTest
./gradlew :app:compileDebugAndroidTestKotlin
./gradlew :app:connectedDebugAndroidTest
./gradlew :app:assembleDebug
```

Local tests cover demo search/quotes/stream events, cached/live watchlist mapping, sorting, missing prices, and Compose states with Robolectric. Instrumentation tests cover demo search, adding AAPL and seeing its cached watchlist price, and opening Settings. Instrumentation requires a connected API 26+ device or emulator.

## Tradeoffs

- The initial Room schema uses `fallbackToDestructiveMigration()` as an explicit first-version simplification. Future schema changes should add migrations before shipping persistent data changes.
- Only the API key is encrypted at rest. The watchlist database is regular Room storage as required by the challenge.
- Search results are published before quote hydration completes, then one quote request is issued per returned symbol in sequence. Broad queries can consume Finnhub rate-limit budget and delay which rows become addable. Potential improvements include limiting hydration to visible rows, caching and deduplicating quotes, pacing requests, or using a supported batch endpoint.
- There is no background streaming, historical chart, price history, account login, or portfolio calculation.

## Tooling Assistance

Built from:
- the original assignment description: [assignment.md](assignment.md)
- manually crafted description of UI and architecture: [implementation_requirements.md](implementation_requirements.md)
- mock design: [design folder](design/)

UI design - Claude Design
Coding agent - OpenCode running GPT-5.6
android CLI tools
