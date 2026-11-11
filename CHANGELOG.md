# Changelog

All notable changes to this project are documented in this file.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/)
and the project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-11-11

### Added

- Local and global chat channels with per-world radius overrides.
- Mention detection with a configurable sound ping and name highlighting.
- Safe MiniMessage formatting for players, including sprite support.
- Anti-spam: cooldown, duplicate detection, sliding rate limit and caps control.
- Word and link filters with mask/block actions and an allowlist.
- In-memory chat history with `/chat history`.
- Asynchronous chat logging to a file.
- Persistent per-player settings (channel and mention sounds).
- `DefaultChatMessageEvent` API for other plugins.
- English and Russian language files with MiniMessage templates.
- Folia-compatible scheduling and a fully thread-safe chat pipeline.
