# DefaultChat

[![Build](https://github.com/227375488489/default-chat/actions/workflows/build.yml/badge.svg)](https://github.com/227375488489/default-chat/actions/workflows/build.yml)
[![Java 25](https://img.shields.io/badge/Java-25-orange.svg)](https://adoptium.net/)
[![Paper 1.21.11+](https://img.shields.io/badge/Paper-1.21.11%2B-blue.svg)](https://papermc.io/)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

A modern chat plugin for Paper servers: **local and global channels, sound-only mention pings,
native MiniMessage formatting with sprites, anti-spam, filters, history and a clean developer API.**

One small jar. No dependencies to install. No NMS. No legacy color codes. Everything is written
in MiniMessage and every message can be changed without touching Java code.

> Author: **Slavyanin** · License: MIT · Requires **Paper 1.21.11+** and **Java 25**

---

## Table of contents

- [Why DefaultChat](#why-defaultchat)
- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Commands](#commands)
- [Permissions](#permissions)
- [Configuration](#configuration)
- [Channels](#channels)
- [Mentions](#mentions)
- [MiniMessage, sprites and player formatting](#minimessage-sprites-and-player-formatting)
- [Anti-spam and filters](#anti-spam-and-filters)
- [History and logging](#history-and-logging)
- [Language files](#language-files)
- [Developer API](#developer-api)
- [Performance and threading](#performance-and-threading)
- [Building from source](#building-from-source)
- [FAQ](#faq)
- [License](#license)

---

## Why DefaultChat

Most chat plugins were designed for Spigot and legacy color codes. DefaultChat is built for the
current Paper API line instead, and it stays out of your way.

| | **DefaultChat** | EssentialsX Chat | ChatControl | VentureChat |
|---|---|---|---|---|
| MiniMessage everywhere (config, lang, formats) | ✔ | — (legacy `&` codes) | ~ (newer builds) | — (own format syntax) |
| Sprites and other Adventure object components in chat | ✔ | — | — | — |
| Mention ping with a configurable sound (no action bar, no title) | ✔ | — | ~ | ~ |
| Per-world local chat radius | ✔ | — | ~ | ~ |
| Chat history command out of the box | ✔ | — | ~ (database/log based) | ~ |
| Built-in duplicate detection, rate limit and caps guard | ✔ | — | ✔ | ~ |
| Word and link filter with allowlist | ✔ | ~ | ✔ | ~ |
| Async file logging with a bounded queue | ✔ | — | ✔ (many backends) | ✔ |
| Public event API with mutable viewers | ✔ | ~ | ~ | ~ |
| One jar for 1.21.11 → 26.x | ✔ | ✔ | ~ | ~ |
| Folia-ready scheduling | ✔ | — | ~ | — |
| Free and open source (MIT) | ✔ | ✔ | ~ (paid edition) | ~ (paid edition) |

*Comparison is based on the public feature lists of each project at the time of writing. Those
projects are mature and offer features DefaultChat intentionally does not (for example multi-server
sync, GUI channel browsers or advanced moderation suites). Check their documentation for the
current state — the table is a starting point, not a verdict.*

What makes DefaultChat different in daily use:

- **Everything is MiniMessage.** Colors, gradients, rainbows, transitions, shadows and sprites work
  in chat formats, feedback messages and player input. No `&a`/`§a` translation layer.
- **Mentions ping with a sound only.** No action bar spam, no titles, no chat flooding. Players can
  turn it off with `/chat ping`.
- **The same jar runs on every supported version.** Compiled against the oldest supported API
  (1.21.11) so Paper 1.21.11, 26.1, 26.1.2, 26.2 and 26.3+ all load it unchanged.
- **Thread-safe by design.** Chat is processed on the global region thread, logging is async and
  the configuration is immutable once loaded. It runs on Folia as well as on regular Paper.
- **Small surface, real features.** History, logs, filters, spam control and a developer event —
  without a 200-key config you will never read.

---

## Features

### Channels

- **Local chat** with a radius in blocks and optional per-world overrides.
- **Global chat** triggered by a prefix (`!` by default) or by `/globalchat <message>`.
- Per-player channel memory: `/chat local`, `/chat global`, `/chat toggle`.
- The sender always sees their own message, even with a radius of `1`.
- Optional vanish awareness: vanished players do not receive local messages.

### Mentions

- Player names in a message are detected case-insensitively and highlighted.
- Mentioned players receive a **sound ping** — configurable key, volume, pitch and cooldown.
- No action bar, no title, no extra chat line.
- Players can disable their own ping with `/chat ping`.
- Only players who can actually see the message are pinged.

### Formatting

- Chat formats, hover text, tags and every feedback message are MiniMessage templates.
- Players with `defaultchat.format` can use a **safe subset** of MiniMessage: colors, decorations,
  gradients, rainbows, transitions, shadow colors and sprites. Click, hover, font, selector,
  score, NBT and translatable tags are blocked.
- Player names are clickable and suggest `/msg <name>`.
- Invalid player formatting falls back to literal text instead of breaking the message.

### Moderation

- Per-player cooldown, duplicate detection window and a sliding rate limit.
- Caps guard with `BLOCK` or `IGNORE` actions.
- Word filter with `MASK` or `BLOCK`, plus link blocking with an allowlist.
- Bypass permissions for staff.

### Quality of life

- `/chat history [n]` shows recent messages from an in-memory ring buffer.
- Asynchronous chat logs written to `chat.log` without blocking the server thread.
- Per-player settings persisted in `playerdata.yml` and saved periodically.
- `/chat reload` re-reads the config and language files, with validation.
- English and Russian language files included; adding another language is one file.
- `DefaultChatMessageEvent` for other plugins to inspect, modify, cancel or re-target messages.

---

## Requirements

| Requirement | Value |
|---|---|
| Server | **Paper** (or a Paper fork) — Spigot and CraftBukkit are not supported |
| Minecraft | **1.21.11**, 26.1, 26.1.2, 26.2, 26.3 and newer |
| Java | **25** |
| Folia | Supported (`folia-supported: true`) |
| Dependencies | None — Adventure and MiniMessage ship with Paper |

DefaultChat uses the modern Paper chat event and Adventure components. That is why Spigot is not
supported and why a single build can support the whole modern version range.

---

## Installation

1. Download `DefaultChat-1.0.0.jar` from the releases page (or build it yourself).
2. Put the jar in your server's `plugins/` folder.
3. Start the server. `plugins/DefaultChat/config.yml` and the language files are created.
4. Adjust the configuration and run `/chat reload`.

Everything is optional: with the default config the plugin works out of the box.

---

## Commands

| Command | Description | Permission |
|---|---|---|
| `/chat local` | Switch to local chat | `defaultchat.use` |
| `/chat global` | Switch to global chat | `defaultchat.channel.global` |
| `/chat toggle` | Toggle between local and global | `defaultchat.use` |
| `/chat ping` | Toggle your mention sounds | `defaultchat.mention` |
| `/chat history [n]` | Show the last `n` messages (1–50, default 10) | `defaultchat.history` |
| `/chat reload` | Reload config and language files | `defaultchat.reload` |
| `/globalchat <message>` | Send a message to global chat | `defaultchat.channel.global` |

Aliases: `/dc` and `/defaultchat` for `/chat`; `/g` and `/gc` for `/globalchat`.

---

## Permissions

| Permission | Default | Description |
|---|---|---|
| `defaultchat.use` | everyone | Send chat messages and use `/chat` |
| `defaultchat.channel.global` | everyone | Use global chat (prefix and command) |
| `defaultchat.mention` | everyone | Mention other players and toggle mention sounds |
| `defaultchat.format` | op | Use the safe MiniMessage subset in messages |
| `defaultchat.history` | op | Use `/chat history` |
| `defaultchat.reload` | op | Use `/chat reload` |
| `defaultchat.bypass.spam` | op | Skip cooldown, duplicate and rate limits |
| `defaultchat.bypass.filter` | op | Skip caps, word and link filters |

To let everyone use formatting, set `defaultchat.format` to `true` in your permissions plugin.
Players without it still chat normally — their tags are shown as literal text.

---

## Configuration

`plugins/DefaultChat/config.yml` (abridged):

```yaml
language: en

chat:
  default-channel: local
  max-length: 256
  player-minimessage: true
  clickable-names: true

  local:
    enabled: true
    radius: 100
    world-radius: {}       # e.g. { world_nether: 50, world_the_end: 0 }

  global:
    trigger: "!"
    require-permission: false

  cooldown-ms: 750
  duplicate-window-seconds: 30

  rate-limit:
    messages: 5
    seconds: 5

  caps:
    min-length: 6
    max-ratio: 0.7
    action: BLOCK          # BLOCK or IGNORE

mentions:
  enabled: true
  min-length: 3
  sound: minecraft:entity.experience_orb.pickup
  volume: 0.6
  pitch: 1.4
  cooldown-seconds: 2
  highlight: '<#FFD166><bold>'

filters:
  words: []
  word-mask: "****"
  word-action: MASK        # MASK or BLOCK
  block-links: false
  link-allowlist: [ "example.com" ]

history:
  enabled: true
  size: 100

logging:
  enabled: true
  file: chat.log

privacy:
  respect-vanish: true

storage:
  save-interval-seconds: 300
```

Notes:

- `radius: 0` (or a negative value) means the local channel reaches the whole server.
- `chat.global.trigger: ""` disables prefix-based global chat; `/globalchat` still works.
- Messages longer than `max-length` are rejected with a friendly message.
- All numeric values are clamped to safe ranges on load, so a typo cannot break the plugin.

---

## Channels

**Local** is the default channel. A message is delivered to players in the same world within the
configured radius. World names in `world-radius` are case-insensitive; worlds without an override
use `chat.local.radius`. If local chat is disabled, local messages are delivered like global ones.

**Global** is triggered in one of two ways:

```text
!Hello everyone          → sends "Hello everyone" to global chat
/globalchat Hello        → sends "Hello" to global chat
```

The trigger prefix is stripped only when the player is allowed to use global chat and global chat is
enabled. Otherwise `!` stays part of the message.

If `chat.global.require-permission` is `true`, players without `defaultchat.channel.global` are
quietly kept in local chat. When they explicitly run `/globalchat`, they get a clear error instead.

---

## Mentions

Write a player's name anywhere in your message:

```text
Steve, can you check the farm?
```

- Matching is case-insensitive and limited to whole words; `mysteve` does not ping `Steve`.
- Names shorter than `mentions.min-length` are ignored (useful on servers with 2-letter names
  clashing with common words).
- Mentioned names are highlighted with `mentions.highlight`, which must be a simple MiniMessage
  style (`<#FFD166><bold>`, `<yellow>`, `<gradient:...>`).
- Each mentioned player hears `mentions.sound` once per message, at most once per
  `mentions.cooldown-seconds` per player.
- The ping is a sound only: **no action bar, no title, no extra message**.
- Players toggle it with `/chat ping`; the setting is stored per player.

Mentions only apply to players who receive the message. A player outside the local radius is not
pinged, and the sender never pings themselves.

---

## MiniMessage, sprites and player formatting

Every text in DefaultChat is parsed as [MiniMessage](https://docs.advntr.dev/minimessage/format.html):

```yaml
prefix: '<gradient:#00ff88:#00aaff><bold>Chat</bold></gradient> <dark_gray>» <gray>'
chat:
  format:
    local: '<dark_gray>[<#7BE38B>L<dark_gray>] <white><player><dark_gray> › <gray><message>'
```

Placeholders available in `chat.format.*`:

| Placeholder | Content |
|---|---|
| `<player>` | The sender's name (clickable when enabled) |
| `<message>` | The formatted message body |
| `<channel>` | Channel tag in history lines |

### Sprites

Chat, MOTDs and messages can include vanilla sprites, for example a stone block or a custom atlas
entry. The sprite tag takes the atlas and the sprite:

```text
<sprite:"minecraft:blocks/stone">
<sprite:"minecraft:blocks":"minecraft:stone">
```

The first form uses `minecraft:blocks` as the atlas and `minecraft:blocks/stone` as the sprite, which
is what most item sprites expect. The second form is explicit: atlas first, sprite second.

> **Always quote the arguments.** The unquoted form `<sprite:minecraft:blocks/stone>` is parsed as
> atlas `minecraft:minecraft` and will not render.

Sprites are sent as Adventure object components, so they render on clients that support chat
objects and show a fallback glyph on older clients.

### Player formatting

Players with `defaultchat.format` may format their own messages:

```text
<gradient:#00ff88:#00aaff>hello</gradient>
<red>look</red> <sprite:"minecraft:blocks/stone">
```

The safe subset is limited to visual tags so a player cannot inject clickable commands, fonts,
selectors, scoreboards or NBT. If parsing fails, the original text is shown literally — a broken
tag never deletes a message.

Disable player formatting entirely with `chat.player-minimessage: false`; the permission can then
be removed from all groups.

---

## Anti-spam and filters

The pipeline checks a message in this order:

1. **Length** — longer than `chat.max-length` is rejected.
2. **Caps** — messages with at least `caps.min-length` letters and more than `caps.max-ratio`
   uppercase letters are blocked (or ignored, depending on `caps.action`).
3. **Spam** — cooldown, duplicate window and sliding rate limit.
4. **Words and links** — masked or blocked according to `filters`.

All timings use a monotonic clock and are computed off the main thread where possible. Bypass
permissions:

- `defaultchat.bypass.spam` — skips caps, cooldown, duplicate and rate limit checks.
- `defaultchat.bypass.filter` — skips the word and link filters.

Feedback messages (`spam.cooldown`, `spam.duplicate`, `spam.rate-limit`, `caps.blocked`,
`error.filtered`) are MiniMessage templates in the language file and receive a `<seconds>`
placeholder where relevant.

Link blocking detects `http://`, `https://` and `www.` links. Hosts listed in
`filters.link-allowlist` are always allowed.

---

## History and logging

`/chat history [n]` prints the last messages from an in-memory ring buffer. The buffer is bounded by
`history.size`, contains only what the server actually sent, and is cleared on shutdown. History is
not persisted — it is a staff convenience, not an audit trail.

Chat logs are written asynchronously to `plugins/DefaultChat/chat.log`:

```text
2026-11-11 20:15:02 [LOCAL] Steve: hello
2026-11-11 20:15:09 [GLOBAL] Alex: hello everyone
```

The writer uses a single virtual thread and a bounded queue, so a slow disk can never stall chat.
If the queue is full, log lines are dropped instead of blocking the server. Set
`logging.enabled: false` to disable it.

---

## Language files

`plugins/DefaultChat/lang/en.yml` and `lang/ru.yml` contain every message. Files are copied on first
start and can be edited freely; missing keys fall back to English, so a partial translation is safe.

To add a language:

1. Copy `lang/en.yml` to `lang/<code>.yml`.
2. Translate the values, keeping the MiniMessage tags and placeholders intact.
3. Set `language: <code>` and run `/chat reload`.

The plugin only accepts language codes matching `[a-z0-9_-]{2,16}` and validates every template when
loading; an invalid template is logged once and rendered as plain text instead of breaking chat.

---

## Developer API

Other plugins can listen to `dev.defaultchat.api.DefaultChatMessageEvent`:

```java
@EventHandler(priority = EventPriority.NORMAL)
public void onChat(DefaultChatMessageEvent event) {
    if (event.channel() == ChatChannel.GLOBAL) {
        event.message(event.message().append(Component.text(" ✦")));
    }

    // Remove players who should not receive this message.
    event.viewers().removeIf(viewer -> viewer instanceof Player player && isMuted(player));
}
```

The event is fired on the global region thread after formatting, filtering and cancellation checks.
It is cancellable, the message component is mutable, and `viewers()` is a mutable set — adding an
`Audience` sends the message there, removing one hides it. The console sender is always appended by
the plugin after the event.

Because DefaultChat cancels the vanilla chat event and re-broadcasts its own component, messages are
not signed. Chat reports and signed-message features do not apply to plugin-formatted chat. This is
the same trade-off every formatting plugin makes, and it is what allows per-recipient local chat,
mutable viewers and full MiniMessage output.

---

## Performance and threading

- The async chat event only captures the player and the plain text, then hands work to the global
  region scheduler. All Bukkit, world and player access happens on the owning thread — Folia-safe.
- Configuration and language files are parsed once on load into immutable records and maps. Chat
  handling does no file I/O.
- Logging uses one virtual thread per plugin with a bounded queue (`512` pending lines). Chat never
  waits for the disk.
- Settings are kept in memory and written to `playerdata.yml` on a timer (and on shutdown), not on
  every message.
- Per-player spam state is dropped on quit. The plugin holds no static references to players or
  worlds, so reloads and player churn do not leak.
- So: no timings are spent per message on reflection, NMS, regex compilation or MiniMessage parser
  construction — the parser and the tag resolvers are built once.

If you profile a busy server, expect chat to disappear from the timings report; if it does not,
open an issue with a Spark profile.

---

## Building from source

Requirements: JDK 25 and a network connection for the Paper repository.

```bash
git clone https://github.com/227375488489/default-chat.git
cd default-chat
./gradlew build
```

The jar is written to `build/libs/DefaultChat-1.0.0.jar`.

By default the build compiles against `1.21.11-R0.1-SNAPSHOT`, the oldest supported API. To verify
against the newest API line:

```bash
./gradlew build -PpaperApi=26.3.build.248-stable
```

CI runs both builds on every push and pull request, so a change that breaks the newest Adventure 5
API is caught before release.

Tests live in `src/test/java` and cover the pure logic: mention tokenizing and highlighting, the
spam guard, filters, history and the safe MiniMessage subset. They run with plain JUnit — no server
is started.

---

## FAQ

**Does it work on Spigot or CraftBukkit?**
Paper and Paper forks only. The plugin relies on Paper's modern chat event, Adventure and the region
schedulers.

**Does it work on Folia?**
Yes. `plugin.yml` declares `folia-supported: true` and the scheduler calls are Folia-compatible.

**Why does `!message` not go to global chat?**
Either global chat is disabled, `chat.global.trigger` is empty, or the player lacks
`defaultchat.channel.global`. Without the permission the `!` is kept as part of the local message.

**Why does a mention not ping?**
Check `mentions.enabled`, the name length (`mentions.min-length`), whether the target is in range,
whether the target disabled pings with `/chat ping`, and the per-player cooldown. The sender never
pings themselves.

**Why do sprites show as boxes?**
Make sure both sprite arguments are quoted and the atlas/sprite names exist on the client. Older
clients that do not support chat objects show a fallback glyph.

**Can I use hex colors and gradients in messages?**
Yes — everywhere, including player messages when they have `defaultchat.format`.

**Is chat signed?**
No. Formatted chat is re-broadcast, so chat reports and signatures do not apply. This is required
for local channels and mutable viewers.

**Can I add my own placeholders to the format?**
Not yet. `chat.format.*` supports `<player>`, `<message>` and `<channel>`; anything else is treated
as MiniMessage.

**How do I remove the clickable names?**
Set `chat.clickable-names: false`. Or keep them and install a messaging plugin; the click suggests
`/msg <name>`.

---

## License

MIT — see [LICENSE](LICENSE).

Copyright (c) 2026 **Slavyanin**.

Russian documentation: [README.ru.md](README.ru.md).
