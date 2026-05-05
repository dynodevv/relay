# Known Issues

> **Last updated:** 2026-05-04

---

## Critical

_None currently tracked._

---

## Minor

### Streaming Responses Are Inconsistent

**Status:** Fixed — client-side word-by-word emission guarantees a typing effect  
**Severity:** N/A (resolved)

**Description:**  
The server/CDN buffers the full SSE response and delivers it all at once, so the original approach of emitting each server chunk as it arrives produced no visible typing effect. The fix moves pacing entirely to the client side.

**Solution (2026-05-05, round 4 — final):**
- `ChatService.streamResponse()` now collects ALL streaming chunks into a `StringBuilder`, then emits the accumulated text word-by-word with 12ms delays between words. This guarantees a typing effect regardless of whether the server streams token-by-token or returns everything at once.
- Non-streaming fallback still works: if no streaming content is received, it makes a regular request and emits the response word-by-word.
- `ChatViewModel` no longer adds `delay(16)` between chunks — pacing is handled entirely in `ChatService`.
- `MessageBubble` restores the three-state rendering: "Thinking…" when `isStreaming && content.isEmpty()`, plain `Text` when `isStreaming && content.isNotEmpty()` (fast), and `Markdown` when `!isStreaming` (formatted).

**Fixes applied (2026-05-05):**

---

## Resolved

| Issue | Resolution |
|---|---|
| **Streaming Responses Are Inconsistent** | **Fixed!** Client-side word-by-word emission guarantees typing effect regardless of server buffering. Markdown renders after streaming completes. |
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
