# Milestones

Source of truth for progress. The statusline reads this file, so keep the markers accurate.

Markers: `[x]` done · `[>]` active · `[ ]` not started.
A milestone is **done only when it runs on the device** — compiling is not done.

- [x] M0 — Skeleton: Gradle, Hilt, theme tokens, Room + SQLCipher, `TimeProvider`
- [x] M1 — Notification listener, raw capture, diagnostics screen. Runs on the phone, real corpus started
  - [x] `SourceRegistry` whitelist + `ContentHasher` dedup
  - [x] `TxNotificationListenerService` + `CaptureRepository`
  - [x] Diagnostics screen with discovery leaderboard
  - [x] `assembleDebug` green, APK produced
  - [x] Runs on the Infinix (Android 15, API 35). DB encrypted, listener bound, leaderboard proven with a synthetic notification
  - [x] Real notifications captured 2026-08-06: 2 GoPay receipts, 1 BCA receipt, 1 GoPay ad — including a GoPay→BCA transfer pair 2.6s apart
  - [x] `com.bca` and `com.gojek.gopay` verified on hardware. myBCA / Gojek / OVO are installed but have not fired yet
  - [x] `TransactionSignal` separates receipts from marketing without discarding anything
- [x] Icon fix — verified on the launcher. The resource was always correct; XOS was serving a stale cached icon
- [>] M2 — Ledger domain + property tests, Home (01), Catat cash (02) incl. note field, transaction detail sheet
  - [x] `BalanceCalculator`, `PeriodTotals`, `DayWindow` + the §11 property tests
  - [x] Wallet seed, `LedgerRepository`, day-window query
  - [x] Home (01) — today's spend, wallet balances, today's rows, nav graph
  - [x] Catat cash (02) — keypad, jenis, wallet, the three-row field group incl. note
  - [x] Transaction detail sheet — note commits on dismiss, delete confirms
  - [>] **Verify all three on the unlocked phone.** They install and cold-start
    without crashing (activity reaches Resumed), but nobody has seen them yet —
    the device locked before there was anything to look at
- [ ] M3 — Parsers against the real corpus, dedup, transfer matcher, admin-fee split
- [ ] M4 — Reminder engine: alarms, FGS, vibration, ongoing notification, blacklist, queue
- [ ] M5 — Reconcile wizard (06), Dompet (07)
- [ ] M6 — Riwayat + Paging (04), monthly summary + PDF (05)
- [ ] M7 — PIN + gate routing (09), lock policy, recovery questions, `FLAG_SECURE`, Pengaturan
- [ ] M8 — Setup wizard (08 + steps 2–8), OEM battery deep links, self-test
- [ ] M9 — Backup/restore, hardening, release checklist
