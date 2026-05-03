# Relay — Development Roadmap

> **Last updated:** 2026-05-03  
> **Current version:** 1.0.0  
> **Status:** Active development

---

## v1.0.0 — Initial Release

- [x] Multi-provider BYOK support (OpenAI, Gemini, Claude, OpenRouter, Groq)
- [x] Custom OpenAI-compatible provider support
- [x] Model management (manual + API fetch)
- [x] Streaming chat with markdown rendering
- [x] Conversation history with sidebar navigation
- [x] Encrypted API key storage
- [x] Local-first data (Room database)
- [x] Material 3 Expressive UI with dynamic theming
- [x] Settings persistence (theme, dynamic colors)
- [x] GitHub Actions CI/CD for APK builds

---

## v1.1.0 — Chat Experience Polish

### Core Chat Improvements
- [ ] **Image input / vision support** — Attach images from gallery or camera to messages for vision-capable models
- [ ] **Message editing** — Tap to edit sent user messages and regenerate the response from that point
- [ ] **Message branching** — When editing a message, create a new branch instead of overwriting
- [ ] **Copy message content** — Dedicated copy button on each message bubble
- [ ] **Stop generation** — Cancel an in-progress streaming response
- [ ] **Retry / regenerate with feedback** — Better regenerate UX with loading states

### UI/UX
- [ ] **Typing indicator** — Animated "..." while model is thinking (before first token)
- [ ] **Code block syntax highlighting** — Better rendering for code snippets
- [ ] **Message timestamps** — Show sent/received time on long-press or as tooltip
- [ ] **Conversation renaming** — Manually rename chats from the sidebar
- [ ] **Empty state illustrations** — Friendly onboarding when no providers are configured

---

## v1.2.0 — Provider & Model Power-User Features

### Provider Enhancements
- [ ] **Provider presets / quick setup** — One-tap setup for popular providers with pre-filled base URLs
- [ ] **Provider health check** — Test connection button to verify API key + endpoint works
- [ ] **Multiple API keys per provider** — Rotate keys or use different keys for different models
- [ ] **Model grouping / favorites** — Pin frequently used models to the top
- [ ] **Model search & filter** — Search through fetched model lists
- [ ] **Provider icons / logos** — Visual identifiers for each provider (instead of generic cloud icon)

### Model Configuration
- [ ] **Per-model parameters** — Temperature, max tokens, top-p, top-k, presence/frequency penalty
- [ ] **System prompt per conversation** — Custom system instructions for each chat thread
- [ ] **Default model selection** — Remember last used model or set a global default
- [ ] **Model capability auto-detection** — Better heuristics for vision/tools/reasoning from API metadata

---

## v1.3.0 — Advanced Chat Features

### Tools & Function Calling
- [ ] **Tool use support** — Allow models to use built-in tools (calculator, web search, datetime)
- [ ] **Custom tool definitions** — JSON schema editor for user-defined tools
- [ ] **Tool execution sandbox** — Safe execution environment for tool calls

### Reasoning & Thinking
- [ ] **Reasoning display** — Show thinking/reasoning steps separately from final answer (e.g., Claude thinking, o1 reasoning)
- [ ] **Chain-of-thought toggle** — Option to show/hide reasoning tokens

### Message Features
- [ ] **Message search** — Search within a conversation
- [ ] **Message pinning** — Pin important messages to top of conversation
- [ ] **Message reactions** — Quick emoji reactions to messages (for personal organization)

---

## v1.4.0 — Organization & Data

### Conversation Management
- [ ] **Conversation folders** — Organize chats into folders/categories
- [ ] **Conversation tags / labels** — Color-coded tags for quick organization
- [ ] **Bulk operations** — Select multiple conversations to delete, archive, or move
- [ ] **Archive** — Soft-delete conversations to an archive instead of permanent deletion
- [ ] **Conversation templates** — Save prompt templates as reusable starting points

### Export & Backup
- [ ] **Export chat to Markdown** — Share or save conversations as .md files
- [ ] **Export chat to JSON** — Full structured export with metadata
- [ ] **Export all data** — Full app data backup to JSON (conversations, providers, models, settings)
- [ ] **Import data** — Restore from backup JSON
- [ ] **Auto-backup** — Optional periodic backup to local storage

---

## v1.5.0 — Quality of Life

### Accessibility
- [ ] **TalkBack support audit** — Ensure all UI elements have proper content descriptions
- [ ] **Font scaling support** — Better handling of system font size changes
- [ ] **High contrast mode** — Accessibility-friendly color scheme

### Performance
- [ ] **Lazy message loading** — Paginate conversation history for large chats
- [ ] **Image caching** — Cache loaded images in conversations
- [ ] **Offline queue** — Queue messages when offline and send when connection returns

### Widgets & Shortcuts
- [ ] **Quick settings tile** — Toggle dark mode from quick settings
- [ ] **App shortcuts** — Long-press launcher icon to start a new chat
- [ ] **Share target** — Share text from other apps directly into Relay as a new message

---

## v2.0.0 — Major Features (Future)

### Multi-Modal
- [ ] **Voice input** — Speech-to-text for message input
- [ ] **Text-to-speech** — Read aloud assistant responses
- [ ] **File attachments** — PDF, DOCX, TXT file upload for document analysis
- [ ] **Audio input** — Send voice memos / audio files to audio-capable models

### Local AI
- [ ] **Ollama integration** — Connect to local Ollama instances
- [ ] **Local model management** — Download and run small local models on-device (via llama.cpp or similar)
- [ ] **Hybrid mode** — Automatically route to local model when offline, cloud when online

### Sync & Cloud (Optional)
- [ ] **Optional cloud sync** — End-to-end encrypted sync across devices
- [ ] **Web companion** — Optional web interface to view/sync chats
- [ ] **Cross-device handoff** — Continue a conversation on another device

### Community
- [ ] **Prompt marketplace** — Browse and import community prompt templates
- [ ] **Share conversations** — Public link generation for sharing (with privacy controls)

---

## Maybe / Experimental Ideas

> These are speculative and may or may not be implemented depending on feasibility and user demand.

- [ ] **Plugin system** — Allow third-party extensions for custom providers, tools, or UI themes
- [ ] **Split-view / multi-chat** — View two conversations side-by-side on tablets
- [ ] **Collaborative chats** — Share a conversation link for real-time collaboration
- [ ] **AI-assisted organization** — Auto-suggest conversation titles, tags, and folder placement
- [ ] **Usage analytics** — Track token usage, costs, and model preferences (local-only)
- [ ] **Custom themes** — Full theme editor beyond Material You
- [ ] **Biometric lock** — Fingerprint/Face ID to unlock the app
- [ ] **Notification integration** — Push notifications for long-running generations or scheduled prompts

---

## Contributing

Have an idea that's not on this list? Open an issue on GitHub with the `enhancement` label!

## License

MIT
