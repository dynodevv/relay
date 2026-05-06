# Relay — Development Roadmap

> **Last updated:** 2026-05-06  
> **Current version:** 1.0.0  
> **Status:** Active development

---

## How to Read This Roadmap

This project uses **phases** instead of rigid versions. Each phase builds on the previous one, and when all phases are complete, we reach **v1.0** — a feature-complete, polished app. After v1.0, we move into v2.0 territory (multi-modal, local AI, etc.).

---

## Current State (Pre-v1)

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

## Phase 1: Core Chat Experience

**Goal:** Make chatting feel polished, responsive, and frustration-free.

- [x] **Image input / vision support** — Attach images from gallery or camera to messages for vision-capable models
- [x] **Message editing** — Tap to edit sent user messages and regenerate the response from that point
- [x] **Message branching** — When editing a message, create a new branch instead of overwriting
- [x] **Copy message content** — Dedicated copy button on each message bubble
- [x] **Stop generation** — Cancel an in-progress streaming response
- [x] **Retry / regenerate** — Better regenerate UX with clear loading states
- [x] **Typing indicator** — Animated indicator while model is thinking (before first token)
- [x] **Code block syntax highlighting** — Better rendering for code snippets
- [x] **Message timestamps** — Show sent/received time on long-press or as tooltip
- [x] **Empty state / onboarding** — Friendly guidance when no providers are configured

---

## Phase 2: Provider & Model Polish

**Goal:** Make managing providers and models effortless for power users.

- [x] **Provider health check** — Test connection button to verify API key + endpoint works
- [x] **Provider icons / logos** — Visual identifiers for each provider (instead of generic cloud icon)
- [x] **Provider quick-setup presets** — One-tap setup with pre-filled base URLs
- [x] **Model grouping / favorites** — Pin frequently used models to the top
- [x] **Model search & filter** — Search through fetched model lists
- [x] **Per-model parameters** — Temperature, max tokens, top-p, top-k, presence/frequency penalty
- [x] **System prompt per conversation** — Custom system instructions for each chat thread
- [x] **Default model selection** — Remember last used model or set a global default
- [x] **Model capability auto-detection** — Better heuristics for vision/tools/reasoning from API metadata

---

## Phase 3: Organization & Data

**Goal:** Help users manage hundreds of conversations and feel in control of their data.

- [ ] **Conversation folders** — Organize chats into folders/categories
- [ ] **Conversation tags / labels** — Color-coded tags for quick organization
- [ ] **Conversation renaming** — Manually rename chats from the sidebar
- [ ] **Archive** — Soft-delete conversations to an archive instead of permanent deletion
- [ ] **Bulk operations** — Select multiple conversations to delete, archive, or move
- [ ] **Conversation templates** — Save prompt templates as reusable starting points
- [ ] **Message search** — Search within a conversation
- [ ] **Export chat to Markdown** — Share or save conversations as .md files
- [ ] **Export chat to JSON** — Full structured export with metadata
- [ ] **Export all data** — Full app data backup to JSON (conversations, providers, models, settings)
- [ ] **Import data** — Restore from backup JSON

---

## Phase 4: Quality & Accessibility

**Goal:** Make Relay feel like a first-class Android app that everyone can use.

- [ ] **TalkBack support audit** — Ensure all UI elements have proper content descriptions
- [ ] **Font scaling support** — Better handling of system font size changes
- [ ] **High contrast mode** — Accessibility-friendly color scheme
- [ ] **Lazy message loading** — Paginate conversation history for large chats
- [ ] **Image caching** — Cache loaded images in conversations
- [ ] **Offline queue** — Queue messages when offline and send when connection returns
- [ ] **Quick settings tile** — Toggle dark mode from quick settings
- [ ] **App shortcuts** — Long-press launcher icon to start a new chat
- [ ] **Share target** — Share text from other apps directly into Relay as a new message

---

## v1.0 Milestone

**Relay reaches v1.0 when all four phases above are complete.** At that point, Relay will be a fully-featured, polished, accessible AI chat client that power users can rely on daily.

---

## v2.0+ — Future Territory

> These are major features that fundamentally expand what Relay can do. They are **not** required for v1.0.

### Multi-Modal
- [ ] **Voice input** — Speech-to-text for message input
- [ ] **Text-to-speech** — Read aloud assistant responses
- [ ] **File attachments** — PDF, DOCX, TXT file upload for document analysis
- [ ] **Audio input** — Send voice memos / audio files to audio-capable models

### Local AI
- [ ] **Ollama integration** — Connect to local Ollama instances
- [ ] **Local model management** — Download and run small local models on-device
- [ ] **Hybrid mode** — Automatically route to local model when offline, cloud when online

### Advanced Features
- [ ] **Tool use support** — Allow models to use built-in tools (calculator, web search, datetime)
- [ ] **Custom tool definitions** — JSON schema editor for user-defined tools
- [ ] **Reasoning display** — Show thinking/reasoning steps separately from final answer
- [ ] **Optional cloud sync** — End-to-end encrypted sync across devices
- [ ] **Plugin system** — Allow third-party extensions for custom providers, tools, or UI themes

### Community
- [ ] **Prompt marketplace** — Browse and import community prompt templates
- [ ] **Share conversations** — Public link generation for sharing (with privacy controls)

---

## Contributing

Have an idea that's not on this list? Open an issue on GitHub with the `enhancement` label!

## License

MIT
