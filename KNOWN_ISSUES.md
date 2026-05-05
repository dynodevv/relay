# Known Issues

> **Last updated:** 2026-05-05

---

## Critical

_None currently tracked._

---

## Minor

_None currently tracked._

---

## Resolved

| Issue | Resolution |
|---|---|
| **Streaming Responses Are Inconsistent** | **Fixed!** Client-side word-by-word emission guarantees typing effect regardless of server buffering. Markdown renders after streaming completes. |
| **Long Assistant Messages Don't Toggle Action Controls When Tapped Near the Bottom** | **Fixed!** Replaced `Surface`'s built-in `onClick` with `Modifier.clickable` on the inner `Column` so the full bounds of tall content remain tappable. |
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
