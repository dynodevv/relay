# Known Issues

> **Last updated:** 2026-05-04

---

## Critical

_None currently tracked._

---

## Minor

### Streaming Responses Are Inconsistent

**Status:** Known issue — non-streaming fallback works reliably  
**Severity:** Low — chat is fully usable via non-streaming fallback

**Description:**  
When sending a message, the response sometimes streams token-by-token in real-time, but most of the time no streaming chunks are received and the full response only appears after generation completes. A non-streaming fallback ensures the response always arrives, but the real-time typing effect is intermittent.

**Symptoms:**
- Occasionally works — you see tokens appear one by one
- Most of the time doesn't work — you wait a few seconds, then the full response pops in at once
- The non-streaming fallback always delivers the complete response

**Possible causes:**
1. SSE (`bodyAsChannel()`) parsing may drop chunks depending on network timing
2. Some providers may send chunks in a format the parser doesn't always catch
3. Ktor's byte channel read might consume partial lines or buffer data

**Next steps (when prioritized):**
- Add debug logging to trace exactly what bytes arrive on each stream
- Consider switching to Ktor's built-in SSE plugin or a more robust parser
- Add retry logic specifically for the streaming path

---

## Resolved

| Issue | Resolution |
|---|---|
| **AI Assistant Messages Appear Empty** | **Fixed!** Triple-layer fix: (1) SSE parser now handles raw JSON lines, (2) content extraction falls back from `delta.content` to `message.content`, (3) replaced Markdown renderer with plain Text composable |
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
