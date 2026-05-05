# Known Issues

> **Last updated:** 2026-05-04

---

## Critical

_None currently tracked._

---

## Minor

### Streaming Responses Are Inconsistent

**Status:** Partially fixed — parser is now significantly more robust; remaining inconsistency is likely server-side buffering  
**Severity:** Low — chat is fully usable via non-streaming fallback

**Description:**  
When sending a message, the response sometimes streams token-by-token in real-time, but most of the time no streaming chunks are received and the full response only appears after generation completes. A non-streaming fallback ensures the response always arrives, but the real-time typing effect is intermittent.

**Symptoms:**
- Occasionally works — you see tokens appear one by one
- Most of the time doesn't work — you wait a few seconds, then the full response pops in at once
- The non-streaming fallback always delivers the complete response

**Fixes applied (2026-05-05):**
1. Rewrote SSE parser to be spec-compliant — properly accumulates multi-line `data:` fields into single events
2. Handle `data:` prefix with or without space (SSE spec allows both)
3. Handle pretty-printed JSON responses when a provider ignores `stream=true` (multi-line JSON accumulation)
4. Strip UTF-8 BOM from line starts
5. Replaced `while (!channel.isClosedForRead)` with `while (true) { readUTF8Line() ?: break }` to avoid race conditions
6. Added comprehensive debug logging (`RelaySSE` / `RelayStream` tags) — no more silent parse failures
7. Log fallback trigger and emitted chunk previews for field diagnosis

**Fixes applied (2026-05-05, round 2):**
8. Switched Ktor engine from Android (`HttpURLConnection`) to OkHttp — OkHttp has significantly better streaming support with smaller response buffering
9. Added anti-buffering headers (`Cache-Control: no-cache`, `X-Accel-Buffering: no`) to discourage proxy buffering
10. Added `yield()` in ViewModel's collect loop to force the Main thread to render UI between chunks
11. Added simulated word-by-word fallback streaming with 12ms delays for a natural typing effect when the server doesn't stream
12. Hybrid SSE parser: single-line events are emitted immediately without waiting for blank lines

**Fixes applied (2026-05-05, round 3):**
13. Force HTTP/1.1 only — HTTP/2 multiplexing can cause proxy/CDN buffering of SSE responses
14. Add `readTimeout(0)` for SSE connections (infinite timeout — servers may pause between tokens)
15. Replace `yield()` with `delay(16)` in ViewModel — `yield()` on `Dispatchers.Main.immediate` may not post to the handler; `delay(16)` forces at least one frame per chunk
16. Fix auto-scroll to trigger on `lastMessage?.content` changes, not just `messages.size`
17. Render streaming messages with plain `Text` instead of `Markdown` — `Markdown` may skip intermediate recompositions due to expensive parsing; switches to `Markdown` only after `isStreaming = false`
18. Add `RelayUI` debug logging in ViewModel to count chunks and track arrival

**Remaining possible causes:**
- Server genuinely does not stream (ignores `stream=true` or buffers response server-side). The simulated fallback provides a typing effect.
- OkHttp or Ktor may still buffer the response body despite HTTP/1.1. Check `tag:RelaySSE` logs to confirm whether chunks arrive incrementally.

**Diagnosing:**
- `tag:RelaySSE`: shows each line read from the SSE stream — if lines appear in a burst, the server is buffering
- `tag:RelayStream`: shows when fallback triggers and how many chars are emitted
- `tag:RelayUI`: shows chunk count and total chars as the ViewModel receives them — if this increments slowly, the UI layer is working

---

## Resolved

| Issue | Resolution |
|---|---|
| **Markdown Rendering Does Not Work** | **Fixed!** Replaced plain `Text` composable with `Markdown` from `multiplatform-markdown-renderer-m3` for assistant messages |
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
