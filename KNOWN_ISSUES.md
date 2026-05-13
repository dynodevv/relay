# Known Issues

> **Last updated:** 2026-05-13

---

## Critical

_No critical issues at this time._

---

## Minor

_No minor issues at this time._

---

## Resolved

| Issue | Resolution |
|---|---|
| **Drag-to-reorder is buggy and hard to use** | **Fixed!** Replaced custom `detectDragGesturesAfterLongPress` implementation with `sh.calvin.reorderable:reorderable` library. Uses actual item positions from `LazyListState`, supports edge auto-scroll, and animates item placement natively. |
| **Gray box still visible inside model cards** | **Fixed!** Switched `AssistChip` from filled `secondaryContainer` background to border-only styling (`outline.copy(alpha = 0.5f)`), eliminating the color layering artifact against the semi-transparent card. |
| **Auto-detection heuristics are incomplete** | **Fixed!** Added `ModelCapabilityDatabase` with exact + prefix matching for known model IDs (OpenAI, Anthropic, Google, Groq, DeepSeek, Mistral, xAI, etc.). Detection now queries the local database first and falls back to heuristics only for unknown models. |
| Streaming Responses Are Inconsistent | **Fixed!** Client-side word-by-word emission guarantees typing effect regardless of server buffering. Markdown renders after streaming completes. |

| Issue | Resolution |
|---|---|
| **Streaming Responses Are Inconsistent** | **Fixed!** Client-side word-by-word emission guarantees typing effect regardless of server buffering. Markdown renders after streaming completes. |
| **Long Assistant Messages Don't Toggle Action Controls When Tapped Near the Bottom** | **Fixed!** Replaced inline tap-to-toggle controls with a long-press context menu (`DropdownMenu` at press location) using `detectTapGestures`. Works for both user and assistant bubbles. |
| **Auto-scroll breaks during long streaming messages** | **Fixed!** Instant scroll during streaming, animated scroll for new messages. Disabled `animateContentSize` on streaming bubbles. Only auto-scrolls when user is near the bottom. |
| **AI Assistant Messages Appear Empty** | **Fixed!** Triple-layer fix: (1) SSE parser now handles raw JSON lines, (2) content extraction falls back from `delta.content` to `message.content`, (3) replaced Markdown renderer with plain Text composable |
| **Google Sans Flex roundness (ROND) not applied everywhere** | **Fixed!** Added `wght` axis to `FontVariation.Settings` for all `GoogleSansFlex` and `GoogleSansCode` entries so variable font weight matching works correctly and the system doesn't fall back to non-rounded system fonts |
| Release build fails due to missing ProGuard rules | Added comprehensive `-keep` and `-dontwarn` rules |
| `ChatRequestDto` serialization error | Applied `kotlinx-serialization` plugin to app module |
| `positionChange()` compilation error | Replaced with manual delta tracking |
| `FontVariation` API compilation error | Added `@OptIn(ExperimentalTextApi::class)` annotation |
| System theme ignores dark mode | Switched to `isSystemInDarkTheme()` inside `setContent` |
| Settings don't persist | Built DataStore-based `SettingsRepository` |
| Edit provider/model does nothing | Added proper edit dialogs |
| Model fetch auto-adds all models | Replaced with searchable multi-select dialog |
| Sidebar swipe gesture unreliable | Implemented custom gesture detector |
| Conversation rename/delete missing | Added dropdown menu with dialogs |

---

## How to Report a New Issue

Open a GitHub issue with:
1. Clear description of the bug
2. Steps to reproduce
3. Expected vs actual behavior
4. Screenshots if applicable
5. Device/Android version info
