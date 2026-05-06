# Known Issues

> **Last updated:** 2026-05-06

---

## Critical

| Issue | Notes |
|---|---|
| **Drag-to-reorder is buggy and hard to use** | The long-press drag handle gesture conflicts with scroll and doesn't feel native. Drag offset calculations are approximate and reorder jumps are unreliable. Needs a proper reorderable LazyColumn implementation (e.g. `androidx.compose.foundation.lazy.items` with `Modifier.draggable` + `LazyListState` animated scroll) or a third-party reorderable library. |

---

## Minor

| Issue | Notes |
|---|---|
| **Gray box still visible inside model cards** | The `AssistChip` composable inside `ModelCard` still renders a slightly lighter gray background. The `Card` → `Box` refactor didn't fully eliminate the visual artifact. Likely caused by `surfaceVariant.copy(alpha = 0.5f)` on the outer card combined with `secondaryContainer` on the chip. Needs a unified background color or border-only chip styling. |
| **Auto-detection heuristics are incomplete** | Vision detection is solid, but Tools and Reasoning still miss many models. Provider APIs (OpenRouter, etc.) don't consistently expose capabilities in a standard format. **Proposed fix:** Build or integrate a local capability database (similar to LiteLLM's model pricing/capability JSON) that maps model IDs → capabilities, falling back to heuristics only for unknown models. This would be a significant data layer addition. |

---

## Resolved

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
