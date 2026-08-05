# Milestones

Source of truth for progress. The statusline reads this file, so keep the markers accurate.

Markers: `[x]` done · `[>]` active · `[ ]` not started.
A milestone is **done only when it runs on the device** — compiling is not done.

- [x] M0 — Skeleton: Gradle, Hilt, theme tokens, Room + SQLCipher, `TimeProvider`
- [>] M1 — Notification listener, raw capture, diagnostics screen. Ship to the phone and let the corpus accumulate
  - [x] `SourceRegistry` whitelist + `ContentHasher` dedup
  - [x] `TxNotificationListenerService` + `CaptureRepository`
  - [x] Diagnostics screen with discovery leaderboard
  - [x] `assembleDebug` green, APK produced
  - [>] Install on the Infinix and confirm capture — blocked on USB debugging
  - [ ] Read real package names off the leaderboard, fix `SourceRegistry`, flip `verified`
- [ ] M2 — Ledger domain + property tests, Home (01), Catat cash (02) incl. note field, transaction detail sheet
- [ ] M3 — Parsers against the real corpus, dedup, transfer matcher, admin-fee split
- [ ] M4 — Reminder engine: alarms, FGS, vibration, ongoing notification, blacklist, queue
- [ ] M5 — Reconcile wizard (06), Dompet (07)
- [ ] M6 — Riwayat + Paging (04), monthly summary + PDF (05)
- [ ] M7 — PIN + gate routing (09), lock policy, recovery questions, `FLAG_SECURE`, Pengaturan
- [ ] M8 — Setup wizard (08 + steps 2–8), OEM battery deep links, self-test
- [ ] M9 — Backup/restore, hardening, release checklist
