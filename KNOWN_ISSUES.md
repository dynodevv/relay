# Known Issues

> **Last updated:** 2026-05-05

---

## Critical

_None currently tracked._

---

## Minor

### Long Assistant Messages Don't Toggle Action Controls When Tapped Near the Bottom

**Status:** Open  
**Severity:** Low — controls still work by tapping the upper portion of the message

**Description:**  
When an assistant message bubble is very long (exceeding a certain height), tapping the lower portion of the bubble no longer toggles the copy/regenerate/delete action controls. Tapping the upper portion of the same message still works. The clickable area of the `Surface` appears to not cover the full height of long content.

**Possible fixes:**
- Investigate whether the `Surface` onClick gesture area gets clipped for tall composables
- Move action controls to a context menu / long-press menu instead of inline toggle
- Try wrapping the message content in a separate clickable modifier with `clickable` instead of relying on Surface's built-in onClick

---

## Resolved

| Issue | Resolution |
|---|---|
| **Streaming Responses Are Inconsistent** | **Fixed!** Client-side word-by-word emission guarantees typing effect regardless of server buffering. Markdown renders after streaming completes. |
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
