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

**Remaining possible causes:**
- Some providers or network paths buffer the entire HTTP response before delivering it to the client (OS-level or mobile-network buffering). This is outside app control.
- Some providers may ignore `stream=true` and return a non-streaming response; the fallback handles this.

**Next steps:**
- Monitor logs from real-world usage (filter `tag:RelaySSE` or `tag:RelayStream`) to confirm parser handles all provider formats
- If logs show chunks arriving but UI not updating, investigate further upstream (Ktor engine / network stack)

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
