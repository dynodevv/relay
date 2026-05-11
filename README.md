<p align="center">
  <img src="./assets/relayicon.png" width="120" alt="Relay icon">
</p>

<h1 align="center">Relay</h1>

<p align="center">A modern Android AI chat client with BYOK (Bring Your Own Key) support.</p>

<p align="center">
  <a href="https://deepwiki.com/dynodevv/relay"><img src="https://deepwiki.com/badge.svg" alt="Ask DeepWiki"></a>
</p>

## Features

- **BYOK Support** — Use your own API keys for OpenAI, Anthropic, Google Gemini, OpenRouter, Groq, and any custom OpenAI-compatible provider.
- **Material 3 Expressive** — Lively shapes, smooth animations, and Material You dynamic theming.
- **Multi-Provider** — Switch between AI providers seamlessly.
- **Model Management** — Fetch available models from APIs or manually configure model capabilities.
- **Streaming Responses** — Real-time token-by-token message rendering.
- **Local-First** — All chats and settings stored locally on your device.

## Tech Stack

- Kotlin + Jetpack Compose + Material 3
- MVVM Architecture with Hilt DI
- Room Database for local persistence
- Ktor Client for networking
- Kotlin Coroutines + Flow

## Building

The project uses GitHub Actions for CI/CD. Push to `main` or create a release tag to trigger an APK build.

### Download Prebuilt APKs

Grab the latest APKs from the [GitHub Actions artifacts](https://github.com/dynodevv/relay/actions) or from [Releases](https://github.com/dynodevv/relay/releases).

## Roadmap

See [ROADMAP.md](ROADMAP.md) for planned features and future development.

## Screenshots

<div align="center">

| Chat Interface | Model Fetching |
| :---: | :---: |
| <img src="./assets/chat.png" width="300"> | <img src="./assets/modelfetch.png" width="300"> |

</div>

## License

MIT
