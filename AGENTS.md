# AGENTS.md — Relay

> Compact instruction file for OpenCode sessions working on Relay.

---

## Git Workflow

- **Push automatically.** After finishing changes, stage, commit, and push to `origin/main` without waiting for explicit confirmation. The user expects the repo to stay up to date.
- Use `--no-edit` for simple fixup amends; rewrite commit messages only when the change meaningfully shifts.

## Shell / File Paths

**Always quote every file path** when passing it to shell commands, even if it looks space-free — it prevents word-splitting errors and costs nothing.

## Build & CI

- **Do NOT build locally.** The project has no local Android SDK setup. All builds run via [GitHub Actions](.github/workflows/build.yml).
- Push to `main` triggers the `build` job which produces Debug + Release APKs as artifacts.
- Creating a git tag triggers a GitHub Release with the Release APK attached.
- `./gradlew assembleDebug --no-daemon` is the CI command — don't run it locally.
- **JDK 21** is required (`JavaVersion.VERSION_21`, `jvmTarget = "21"`).

## Tech Stack Quirks

### Kotlin Serialization Plugin
Both the **root** `build.gradle.kts` AND **app** `build.gradle.kts` must apply `kotlin("plugin.serialization")`. If only one has it, `ChatRequestDto` throws "Serializer not found" at runtime.

### Ktor + Content Negotiation
The `HttpClient` has `ContentNegotiation` installed with `kotlinx.serialization.json`. However:
- For **model fetch** (`fetchModels`), read response as `bodyAsText()` then parse manually with `Json.decodeFromString()`. Do NOT use `response.body()` — if the server returns `text/html`, Ktor throws `NoTransformationFoundException`.
- For **streaming chat**, we read `bodyAsChannel()` and parse SSE lines manually.
- **Engine config:** HTTP/1.1 only (not HTTP/2) to avoid proxy buffering, and `readTimeout(0)` because SSE pauses between tokens.

### OpenRouter Headers
OpenRouter requires these headers on every request or it may return HTML errors:
```kotlin
header("HTTP-Referer", "https://github.com/dynodevv/relay")
header("X-Title", "Relay")
```

### Room + Rapid Writes
Room `Flow` does NOT reliably emit during rapid successive `UPDATE` queries. For streaming chat:
- Update the UI state **directly** (in-memory) on every chunk.
- Write to Room **only once** after streaming completes.
- Do NOT write to Room on every chunk — it blocks the coroutine and stalls the UI.

### ProGuard / R8
Release builds (`isMinifyEnabled = true`) will fail without comprehensive `-keep` rules. `gradle.properties` also sets `android.enableR8.fullMode=true`, making obfuscation even more aggressive. The current `proguard-rules.pro` is hard-won — don't trim it without testing a release build.

## Compose Gotchas

- **`positionChange()` is removed** in newer Compose versions. Use manual delta tracking (store previous x/y and subtract).
- **`FontVariation` API** requires `@OptIn(ExperimentalTextApi::class)` on the declaring property.
- **Dynamic theming** reads `isSystemInDarkTheme()` inside `setContent`, not at the call site.

## Architecture

- **Package:** `com.dynodevv.relay`
- **DI:** Hilt with `@HiltAndroidApp` on `RelayApp`. Use `hiltViewModel()` in Compose screens.
- **Navigation:** Compose Navigation in `RelayAppContent.kt`. Routes are in `Routes` object.
- **Database:** Room with 5 entities (`Conversation`, `Message`, `Provider`, `AIModel`, `CapabilityCache`).
- **API layer:** `OpenAICompatibleApi` handles all HTTP. It's OpenAI-compatible, supporting OpenRouter, Groq, Gemini, Claude, etc.
- **Streaming:** `ChatService.streamResponse()` returns `Flow<String>`. It has a **non-streaming fallback** — if no chunks are emitted, it retries with `stream = false`.

## File Locations

| Concern | Location |
|---|---|
| Entry point | `app/src/main/java/com/dynodevv/relay/MainActivity.kt` |
| Application class | `app/src/main/java/com/dynodevv/relay/RelayApp.kt` |
| Navigation / Routes | `app/src/main/java/com/dynodevv/relay/RelayAppContent.kt` |
| DI Module | `app/src/main/java/com/dynodevv/relay/di/AppModule.kt` |
| API Client | `app/src/main/java/com/dynodevv/relay/data/remote/api/OpenAICompatibleApi.kt` |
| Database | `app/src/main/java/com/dynodevv/relay/data/local/AppDatabase.kt` |
| Chat VM | `app/src/main/java/com/dynodevv/relay/ui/chat/ChatViewModel.kt` |
| Chat UI | `app/src/main/java/com/dynodevv/relay/ui/chat/ChatScreen.kt` |
| Fonts | `app/src/main/res/font/` |
| Colors | `app/src/main/java/com/dynodevv/relay/ui/theme/Color.kt` |
| Typography | `app/src/main/java/com/dynodevv/relay/ui/theme/Type.kt` |

## Known Issues

See [KNOWN_ISSUES.md](KNOWN_ISSUES.md) for the current list.

## Roadmap

See [ROADMAP.md](ROADMAP.md) for planned features and upcoming work.

## Adding a Dependency

1. Add to `app/build.gradle.kts` dependencies block.
2. If it's a KotlinX serialization or KSP processor, also verify root `build.gradle.kts` has the plugin.
3. If it has ProGuard issues, add `-keep` / `-dontwarn` rules to `app/proguard-rules.pro`.
4. Commit and push — CI will validate.

## Testing Changes

- There are no local unit tests yet. The only verification is a successful CI build.
- Debug APK from CI is the fastest way to test on a real device.
