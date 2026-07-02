# Recognize

The Recognize module wraps the Ping Identity Recognize SDK (formerly Keyless) and provides
integration points for both PingOne DaVinci and PingAM Journey authentication flows.
It exposes a `Recognize` singleton for lifecycle management and a `RecognizeLifecycle` orchestration
module, with `davinci/` and `journey/` sub-packages that register the appropriate
collectors and callbacks automatically via AndroidX App Startup.

Depends on: `foundation:utils`, `foundation:logger`, `foundation:android`,
`foundation:davinci-plugin`, `foundation:journey-plugin`.

## Installation

Add the dependency in your app's `build.gradle.kts`:

```kotlin
implementation("com.pingidentity.sdks:recognize:<version>")
```
