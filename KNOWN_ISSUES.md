# Known Issues

> **Last updated:** 2026-05-04

---

## Critical

### AI Assistant Messages Appear Empty

**Status:** Fix pushed — awaiting test  
**Severity:** High — blocks core chat functionality

**Description:**  
When sending a message to an AI model, the API call succeeds (confirmed via OpenRouter logs and title generation works), but the assistant's response bubble in the chat UI remains completely empty. The message is processed on the API side and streaming chunks are received, yet no text is rendered in the message bubble.

**Symptoms:**
- User message appears correctly
- Assistant bubble is rendered (gray background visible) but contains no text
- Message title generation works (indicates API response is received)
- OpenRouter dashboard shows successful API calls

**Attempts so far:**
- v1: Relied on Room database Flow to emit message updates — messages stayed empty
- v2: Restructured ChatViewModel to use `flatMapLatest` on `activeConversationId` — messages still empty
- v3: Rewrote streaming to update UI state directly instead of relying on Room Flow — messages still empty
- **v4 (latest):** Triple-layer fix applied:
  1. SSE parser now handles raw JSON lines (not just `data: ` prefixed)
  2. Content extraction falls back from `delta.content` to `message.content`
  3. Replaced Markdown renderer with plain `Text()` to rule out library issues

**Hypotheses addressed:**
1. ~~Markdown renderer silently failing~~ — Ruled out by switching to plain Text
2. ~~Compose recomposition not triggering~~ — Ruled out by direct UI state updates
3. ~~SSE parsing missing chunks~~ — Addressed by handling both SSE and raw JSON formats
4. ~~Wrong field name in response~~ — Addressed by falling back from `delta` to `message`

**Next steps:**
- Test latest build to verify if any of the three fixes resolved the issue
- If still empty, add runtime debug logging to trace exact chunk values

---

## Minor

_None currently tracked._

---

## Resolved

| Issue | Resolution |
|---|---|
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
