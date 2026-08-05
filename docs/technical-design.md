# Your Money — Technical Design Document

Status: **draft for review, no code written yet**
Source of truth for product: `Konsep Sistem Expense Tracker.dc.html` (18 chapters, the *why*), `design_handoff_your_money/README.md` (tokens + 14 screens, the *what*), `Your Money App.dc.html` (hi-fi visual truth).
Scope of this document: how to build it. Product decisions already made in the design are treated as fixed and are not re-litigated here.

**Project stance**, as already established by the handoff — restated here because it governs several decisions below. This is a personal app for one user's own needs: the design names that user's own wallets (BCA, GoPay 1 & 2, OVO, Cash) and their specific phone. It is distributed as-is; if other people install it, fine, but it is not a supported product and nothing here is designed around unknown users. Where a decision trades hardening for simplicity, that is deliberate, and the trade is recorded rather than defended.

---

## 1. What the product actually is, in engineering terms

Strip the UI away and this is:

> A local, single-user, append-only financial ledger whose primary input is a **text-parsing pipeline over Android notifications**, with a **scheduled nagging state machine** to close the one input gap (cash), and a **manual reconciliation mechanism** to bound the drift that an inferred ledger inevitably accumulates.

Three subsystems carry essentially all the risk:

| Subsystem | Why it's the risk | Failure mode if done badly |
|---|---|---|
| Notification ingestion + parsing | Formats are undocumented, vendor-specific, and change without notice | Silent data loss; the ledger becomes untrustworthy and the app is abandoned |
| Reminder engine | Fights Android's battery/doze/OEM restrictions, on an Infinix specifically | Reminder never fires; cash never recorded; same abandonment |
| Ledger arithmetic | `PINDAH_DOMPET` and `KOREKSI` must not leak into income/expense totals | Expenses appear ~2× real, which the concept doc calls out as the critical bug |

Everything else (screens, settings, PDF, backup) is ordinary app work. Effort should be allocated accordingly — the 14 screens are the *easy* part, because the design already specifies them down to the pixel.

---

## 2. Tech stack

### 2.1 Platform: Android native, Kotlin + Jetpack Compose

This is not a preference, it's forced by the requirements. Every one of these needs native Android API surface, and none has a dependable cross-platform abstraction:

- `NotificationListenerService` — reading other apps' notifications
- Ongoing, self-restoring notification with a custom repeating vibration waveform and a permanently silent channel
- `AlarmManager` exact alarms surviving doze + reboot
- Foreground-app detection for the vibration blacklist
- `PdfDocument` + Storage Access Framework for the monthly PDF into a user-chosen (cloud-synced) folder

React Native or Flutter would mean writing all of the above as native modules anyway, then paying the bridge cost on top. Rejected.

### 2.2 Versions and libraries

| Concern | Choice | Note |
|---|---|---|
| Language | Kotlin 2.x, coroutines + Flow | |
| UI | Jetpack Compose (BOM latest stable), Material 3 as a *base only* | The design is a bespoke dark system; M3 defaults are overridden by a custom theme, see §7 |
| minSdk | **29** (Android 10) | Covers effectively the whole install base for a personal app; avoids a pile of legacy branches. Notification channels (26) and scoped storage semantics are then unconditional |
| targetSdk | Latest stable (36 at time of writing) | Must be current, because notification/alarm/FGS behaviour changes are the things that will break this app |
| DI | Hilt | Enough services (listener, scheduler, vibrator, exporter) to justify it |
| Database | **Room + SQLCipher** | §6 |
| Settings | DataStore (Proto) for settings; `EncryptedSharedPreferences` for PIN hash + attempt counter | |
| Background | `AlarmManager` for the reminder (must be on time); **WorkManager** for backup, PDF export, monthly summary, parser re-runs | Never WorkManager for the daily reminder — it has no exact-time guarantee |
| Navigation | Navigation Compose, type-safe routes (kotlinx.serialization) | Needed for the PIN-gate `pendingDestination` pattern |
| Lists | Paging 3 backed by Room `PagingSource` | Riwayat grows unbounded |
| Time | `java.time` + an injected `Clock` everywhere | See §10 — untestable otherwise |
| Serialization | kotlinx.serialization | Backup JSON, summary payloads, nav args |
| PDF | `android.graphics.pdf.PdfDocument` + `Canvas`/`StaticLayout` | Vector text, selectable, small files. Not a Compose→bitmap screenshot. No encryption layer, so no extra library (§9) |
| Testing | JUnit4, Turbine, MockK, Robolectric, Room in-memory, Compose UI test | §10 |
| Quality gates | ktlint + detekt + R8 full mode | Baseline Profile dropped — see §10 |

### 2.3 Money representation

`Long`, whole rupiah, always. Never `Double`, never `Float`, never `BigDecimal` (unnecessary — IDR has no circulating subunit and every amount in the design is a whole rupiah, including the 2.500 admin fee).

`nominal` is **always non-negative**. Direction is carried by `jenis`, never by the sign. This removes an entire class of sign-flip bugs from the totals code.

---

## 3. Architecture

### 3.1 Layering

Clean-ish three layers with unidirectional data flow. Deliberately unfashionable and boring, because the interesting complexity here is in the domain rules, not in the architecture.

```
ui/          Compose screens + ViewModels.  Knows about domain models only.
domain/      Pure Kotlin. Entities, use cases, parsers, matchers, ledger math.
             ZERO Android imports — this is what makes the risky parts fast to test.
data/        Room, DAOs, DataStore, repositories. Implements domain interfaces.
platform/    NotificationListenerService, AlarmScheduler, VibrationController,
             ForegroundAppMonitor, PdfExporter, BackupManager, KeystoreProvider.
             Implements domain interfaces so the domain never touches the framework.
```

The rule that matters: **the notification parsers and all ledger arithmetic live in `domain/` and have no Android dependency.** They then run as plain JVM unit tests in milliseconds, which is the only way a parser corpus stays maintainable.

### 3.2 Module structure

**Start single-module (`:app`) with enforced package layering.** For a solo project, multi-module Gradle costs more in configuration friction than it returns in build time at this size.

One exception worth taking early if build times bite: extract `:domain` as a pure-Kotlin (non-Android) module. That makes the "no Android imports" rule compiler-enforced rather than a convention, and its tests run without Robolectric. Recommended as soon as the parser corpus exceeds ~30 fixtures.

### 3.3 The ingestion pipeline

This is the most important design decision in the document.

```
NotificationListenerService.onNotificationPosted(sbn)
  │  ① whitelist check on packageName  ← privacy boundary, do this FIRST
  │  ② extract title / text / bigText / postTime / sbn.key
  │  ③ INSERT into raw_notification                    ← persist BEFORE parsing
  └─→ returns in <1ms; no parsing on this thread
        │
        ▼ (background worker)
   ParserRegistry.forPackage(pkg) → List<NotificationParser>
        │
        ├── Success(ParsedTransaction) ─→ Deduplicator ─→ TransferMatcher ─→ transaction
        ├── Ignored (promo, balance-info, OTP)  ─→ mark raw as IGNORED
        └── Unrecognized                         ─→ mark raw as UNRECOGNIZED
                                                    (surfaced in a diagnostics screen)
```

**Persist the raw notification before parsing, always.** Reasons, in order of importance:

1. **Parsers will be wrong, and they will be wrong later too.** Banks change wording after app updates. With raws stored, fixing a parser means bumping `parserVersion` and re-running it over history to repair the ledger. Without raws, every parser bug is permanent data loss.
2. **You cannot write these parsers without real samples.** The only way to obtain them is to capture on the device over days of real spending. The raw table *is* the corpus source.
3. **Unrecognized notifications become a visible work queue** instead of silent gaps in the ledger.
4. The listener callback runs on a shared system thread and must return immediately; a DB insert on a background dispatcher is the correct amount of work to do there.

**Privacy boundary:** the whitelist check happens before anything is written. Only notifications from the configured BCA/GoPay/OVO packages are ever persisted — never the whole notification stream, even though the permission technically allows it. Raws are purged after 90 days (configurable). This is also what the setup screen promises the user, so it is a correctness requirement, not just good manners.

### 3.4 Parser design

Strategy pattern keyed by package name, exactly as the handoff asks:

```kotlin
interface NotificationParser {
    val id: String              // "bca.transfer.v3"
    val version: Int            // bump on every behaviour change
    val packages: Set<String>
    fun parse(raw: RawNotification): ParseOutcome
}

sealed interface ParseOutcome {
    data class Parsed(val tx: ParsedTransaction) : ParseOutcome
    data object Ignored : ParseOutcome        // promo / OTP / balance info
    data object NotMine : ParseOutcome        // next parser gets a turn
}
```

Each parser is a small ordered list of regex templates plus a shared amount normaliser (`Rp 45.000` / `45.000,00` / `IDR 45000` → `45000L`). The registry tries parsers in order; first `Parsed` or `Ignored` wins.

Every produced transaction stores `parserId` + `parserVersion` + `rawNotificationId`. That triple is what makes a repair migration possible.

Package names to confirm on the actual device (do **not** hardcode from memory — verify with the capture build):
`com.bca` / `com.bca.mybca.omni.android`, `com.gojek.app` / `com.gojek.gopay`, `ovo.id`.

### 3.5 Transfer matching and admin fees

Topup produces two notifications a few seconds apart. Naive in-memory buffering loses them to process death, so:

- Every parsed transaction is written **immediately** with `matchState = PENDING` if it's a candidate (an outflow from a bank wallet, or an inflow to an e-wallet).
- A delayed job at `+120s` (configurable) finalises it.
- If a counterpart arrives first — opposite direction, different wallet, amount within tolerance — merge on the spot into one `PINDAH_DOMPET` row (`walletId` = source, `walletTujuanId` = dest, `nominal` = the **inflow** amount).
- If `outflow − inflow > 0`, emit a second `KELUAR` row for the difference with `keterangan = "biaya admin"`, linked by `groupId`.
- If the window closes unmatched, mark `matchState = UNMATCHED` and raise the one-time question the concept doc describes (§5).

**Edge case the design implies but doesn't name:** ATM withdrawal produces exactly **one** notification (the BCA debit), because Cash has no app. It can never pair. The BCA parser must detect the ATM/tarik-tunai wording and emit `PINDAH_DOMPET(BCA → Cash)` directly, without entering the matcher. Same for any other single-sided transfer. Worth an explicit fixture set.

### 3.6 Deduplication

Duplicate arises two ways and they need different handling:

- **Same notification re-posted/updated** — identical `sbn.key` + identical content hash → ignore at the raw layer.
- **Two genuinely different notifications for one transaction** (e.g. app + SMS-style push) → same `walletId` + same `nominal` + same direction within ±60s → treated as one transaction, second one marked `DUPLICATE` and linked to the first (not deleted, so it stays auditable).

### 3.7 Reminder state machine

```
                 ┌──────────── daily exact alarm at reminderTime ────────────┐
                 ▼                                                            │
        ensureDayRows(lastSeen..today)                                        │
        autoClose(days older than 7d → TIDAK_TERCATAT)                        │
                 ▼                                                            │
        pending = days where status == MENUNGGU                               │
                 ├── empty ────────────────────────────► reschedule for tomorrow
                 └── non-empty                                                │
                        ▼                                                     │
              foreground app in blacklist? ──yes──► hold, poll until exit ────┤
                        │ no                                                  │
                        ▼                                                     │
              start ReminderForegroundService                                 │
                 · post ongoing, silent-channel notification (screen 10)       │
                 · vibrate waveform, repeat until answered, cap vibrateMax     │
                 · re-post on dismiss (Android 14+ allows swiping FGS notifs)  │
                        │                                                      │
        ┌───────────────┼────────────────┬──────────────────┐                 │
        ▼               ▼                ▼                  ▼                 │
   "Jawab sekarang"   snooze 30m/1j/2j  answered in-app   repeatInterval ──────┘
        │                │                   │
        ▼                ▼                   ▼
   PIN → screen 03   re-alarm, day    day → TERCATAT / NOL
                     stays MENUNGGU        service stops
```

Platform notes that constrain this:

- **Exact alarms.** Declare `SCHEDULE_EXACT_ALARM`; check `canScheduleExactAlarms()` and deep-link the user to the grant page during setup step 7. Use `setExactAndAllowWhileIdle`. Reschedule on `BOOT_COMPLETED`, on `TIMEZONE_CHANGED`/`TIME_SET`, and after every fire (chained one-shots, never `setRepeating`).
- **Silent forever, by construction.** Create the channel with `setSound(null, null)` and `IMPORTANCE_HIGH`. Notification channels are immutable after creation, so sound literally cannot be enabled afterwards — which is exactly the "toggle Suara mati dan tidak bisa dinyalakan" requirement in screen 14, implemented by the platform rather than by UI enforcement. If the channel ever needs changing, it must be recreated under a new channel ID.
- **Vibration.** `VibrationEffect.createWaveform(pattern, repeat = 0)` with `USAGE_ALARM` audio attributes so it survives Do-Not-Disturb; cancelled by the service at `vibrateDurationMax`.
- **"Tidak bisa digeser" is weaker than it used to be.** Since Android 14, users can dismiss ongoing foreground-service notifications in most cases. Mitigation: set a `deleteIntent` that immediately re-posts. This matches the design's own stated intent ("digeser pun ia kembali") and stays within platform rules — the design already, correctly, forbids escalating beyond this.
- **OEM battery restriction is the real enemy.** Infinix/Tecno (XOS) and Xiaomi (MIUI) kill background apps aggressively. Setup step 4 exists precisely for this and must deep-link into the OEM autostart/battery pages, with a per-OEM intent table and a graceful fallback to the generic `IGNORE_BATTERY_OPTIMIZATIONS` request. Treat this as a first-class feature, not a nicety — if it's skipped, the app silently stops working and looks broken.

### 3.8 Foreground-app blacklist

Two options, and the choice has consequences beyond code:

| | `UsageStatsManager` (+ `PACKAGE_USAGE_STATS`) | `AccessibilityService` |
|---|---|---|
| Accuracy | Poll-based, ~1–2s lag | Event-driven, instant |
| Battery | Bounded if polling only during an active reminder | Always-on service |
| Play Store | Declarable, moderate friction | Heavy scrutiny; a common rejection cause |
| Privacy surface | App-usage history | Screen content |

**Decided: `UsageStatsManager`, polling at ~2s only while a reminder is actively pending.** The lag is irrelevant here (the question waits anyway) and the battery cost is bounded. Distribution is sideload (§13), so the Play-review column no longer constrains the choice — but `AccessibilityService` is still rejected on privacy-surface grounds alone: it can read screen content, which is far more than this feature needs.

---

## 4. State management

MVVM + UDF. No third-party MVI framework.

- **Room `Flow` queries are the single source of truth.** No in-memory mirrors of DB data anywhere. Balance is *derived*, never a stored mutable field (§6.3) — this is what keeps the "≈" honest.
- Each screen has one `ViewModel` exposing `StateFlow<XUiState>` built by `combine(...).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Loading)`.
- UI models are `@Immutable` data classes carrying **already-formatted strings** for money. Formatting lives in one `RupiahFormatter` because the design mandates specific glyphs — `.` thousands separator, U+2212 minus (not hyphen), `+` prefix, `≈` prefix, `tabular-nums`. Scattering `String.format` across composables guarantees these drift.
- One-shot events (navigate, toast, haptic) via `Channel` → `receiveAsFlow()`, not in the state object.
- Settings via DataStore `Flow`, same treatment as DB data.

**Session state is deliberately not persisted**, held in a `@Singleton AppLockManager`:

```kotlin
isUnlocked: StateFlow<Boolean>
pendingDestination: Destination?    // where to go after PIN
backgroundedAt: Instant?            // drives the lockTimeout grace
```

`ProcessLifecycleOwner` records `backgroundedAt` and clears `isUnlocked` once the app has been backgrounded longer than `lockTimeout`. Because the session holder is not persisted, process death forces a PIN automatically — which is exactly the required behaviour (§6.6) rather than a happy accident.

**A trap worth naming:** setup steps 1 and 4 send the user *out* to Android Settings and back, as do the battery-optimisation and notification-access grants. A naive "lock immediately on background" implementation would log the user out mid-setup, every time. The `lockTimeout` grace in §6.6 covers this for free — no separate suppression mechanism is needed, provided the timeout is a duration and not a boolean.

Routing must funnel **every** entry point through the gate destination — launcher icon, notification action, deep link, and resume-after-timeout. The PIN is mandatory (§13, item 6), so there is exactly one shape of graph and one path to test.

Background state owners (not ViewModels, they outlive screens):
- `NotificationIngestService` — always running while permission is granted
- `ReminderScheduler` — daily exact alarm
- `MonthEndWorker`, `WeeklyBackupWorker` — WorkManager, both idempotent by (period key) so a double-fire is harmless

---

## 5. Backend

**There is none, and that is the design.** "Data hanya di HP ini" is stated on the setup screen and in the Pengaturan footer, so it is a promise to the user, not just an architecture choice.

Consequences to accept explicitly:

- No cross-device sync, no web view of the data, no server-side parsing improvements.
- Device loss = data loss, bounded only by the backup file. This makes backup a **core feature**, not a checkbox.
- **No analytics, no crash reporting SDK, no Firebase.** Any of these would quietly break the promise. Crash visibility instead comes from a local `Thread.setDefaultUncaughtExceptionHandler` writing a rotating, redacted log file, viewable in a diagnostics screen and included in the backup. That log is also the fastest way to debug parser failures in the field.

If a backend is ever wanted (sync between two phones, iOS companion), the correct seam is a sync layer over the existing ledger — but it would invalidate the current privacy copy on two screens, so treat it as a product change, not a technical one.

---

## 6. Database and local security

### 6.1 Engine and encryption

**Room over SQLCipher.** The DB holds a complete financial history behind a 6-digit PIN; an unencrypted `.db` file is trivially pulled off a rooted or backed-up device.

Key handling:
- DB passphrase is a **random 256-bit key held in the Android Keystore** (hardware-backed where available), not derived from the PIN.
- The **PIN is a UI gate**, stored as PBKDF2-HMAC-SHA256, ≥200k iterations, 16-byte random salt, in `EncryptedSharedPreferences`, with a persisted failed-attempt counter driving exponential backoff (survives app restart, which an in-memory counter would not).
- Be honest about the strength: a 6-digit PIN is 10⁶. Deriving the DB key from it would be *worse*, because it would make the DB offline-brute-forceable. What the PIN actually buys is described in §6.5.
- **Biometric unlock (screen 13) must default to off, and the screen must say why.** Enrolling a new fingerprint on Android requires only the device lock credential — which, per §6.5, the adversary already has. Turning the toggle on therefore silently reopens the exact hole the app PIN exists to close. This is not a generic "biometrics are convenient" toggle; it is a documented trade-off the user has to opt into knowingly.
- `FLAG_SECURE` on the window. This blanks the app in the recents switcher and blocks screenshots and screen recording. Note it renders a **blank card, not a blur** — a true blur is a custom overlay applied in `onPause` and removed in `onResume`. Recommendation: `FLAG_SECURE` as the actual security measure, optional blur overlay purely for polish.

**Direct consequence for backup:** a hardware-bound Keystore key cannot leave the device, so the backup must be a **logical export** (JSON/CSV), never a copy of the SQLCipher file. A copied `.db` would be unrestorable on a new phone. See §9.

### 6.2 Schema

```sql
wallet(
  id TEXT PK, nama TEXT, jenis TEXT,            -- BANK | EWALLET | CASH
  terhubung INTEGER, saldoAwal INTEGER,
  urutan INTEGER, packageHint TEXT NULL)

transaction(
  id TEXT PK,
  jenis TEXT,                                    -- see 6.3
  nominal INTEGER,                               -- always >= 0
  waktu INTEGER,                                 -- epoch millis
  walletId TEXT, walletTujuanId TEXT NULL,
  keterangan TEXT,                               -- derived: merchant name, or cash label
  catatan TEXT NULL,                             -- user's own free note, optional (§6.8)
  sumber TEXT,                                   -- OTOMATIS | MANUAL
  parserId TEXT NULL, parserVersion INTEGER NULL,
  rawNotificationId TEXT NULL,
  matchState TEXT NULL,                          -- PENDING | MATCHED | UNMATCHED | DUPLICATE
  groupId TEXT NULL,                             -- links transfer + its admin fee
  createdAt INTEGER, editedAt INTEGER NULL, deletedAt INTEGER NULL)

raw_notification(
  id TEXT PK, packageName TEXT, sbnKey TEXT,
  title TEXT, text TEXT, bigText TEXT,
  postTime INTEGER, contentHash TEXT,
  parseState TEXT,                               -- BARU | PARSED | IGNORED | UNRECOGNIZED | DUPLICATE
  parserId TEXT NULL, receivedAt INTEGER)

daily_cash_status(
  date TEXT PK,                                  -- ISO local date
  status TEXT,                                   -- MENUNGGU | TERCATAT | NOL | TIDAK_TERCATAT
  answeredAt INTEGER NULL)

monthly_summary(
  yearMonth TEXT PK, payloadJson TEXT,
  pdfUri TEXT NULL, generatedAt INTEGER)

reconcile_event(
  id TEXT PK, walletId TEXT, waktu INTEGER,
  saldoPrediksi INTEGER, saldoAsli INTEGER,
  selisih INTEGER, transactionId TEXT NULL)
```

Indexes: `transaction(waktu DESC)`, `transaction(walletId, waktu)`, `transaction(walletTujuanId)`, `transaction(jenis)`, `raw_notification(contentHash)`, `raw_notification(packageName, postTime)`, `daily_cash_status(status)`.

**Two additions made during M0/M1 implementation:**

- **`parseState = BARU`**, a fifth value meaning "captured, no parser has looked at it yet". M1 ships before any parser exists (§15), so without it every freshly captured raw would have to be written as `UNRECOGNIZED` — which is the label for *a parser tried and failed*. Conflating the two would make the diagnostics work queue meaningless the moment M3 lands.
- **`discovered_source(packageName PK, count, firstSeen, lastSeen)`**, a table holding **package names and counts only — never notification content**. It exists because every package name in §3.4 is a guess: if a guess is wrong the whitelist silently captures nothing, and M1's whole purpose fails with no visible symptom until M3. Counting non-whitelisted package names is enough for the diagnostics screen to confirm the real ones, while keeping the §3.3 promise that non-whitelisted *content* is never persisted. It is a build-time diagnostic, expected to be switched off once the list is verified.

Schema JSON exported to `schemas/` and committed, so migrations are testable.

### 6.3 `jenis` — five values, not three

The handoff mandates three. Reconciliation needs a fourth concept, and splitting it by direction keeps `nominal` unsigned and the SQL trivial:

| `jenis` | Effect on `walletId` | Effect on `walletTujuanId` | In "Masuk" total | In "Keluar" total |
|---|---|---|---|---|
| `MASUK` | +nominal | — | ✅ | — |
| `KELUAR` | −nominal | — | — | ✅ |
| `PINDAH_DOMPET` | −nominal | +nominal | ❌ | ❌ |
| `KOREKSI_NAIK` | +nominal | — | ❌ | — |
| `KOREKSI_TURUN` | −nominal | — | — | ❌ |

Balance is always computed, never stored:

```sql
SELECT w.saldoAwal
  + IFNULL((SELECT SUM(CASE t.jenis
        WHEN 'MASUK' THEN t.nominal
        WHEN 'KOREKSI_NAIK' THEN t.nominal
        ELSE -t.nominal END)
      FROM `transaction` t
      WHERE t.walletId = w.id AND t.deletedAt IS NULL), 0)
  + IFNULL((SELECT SUM(t.nominal) FROM `transaction` t
      WHERE t.walletTujuanId = w.id
        AND t.jenis = 'PINDAH_DOMPET' AND t.deletedAt IS NULL), 0)
FROM wallet w
```

At this data volume (a few thousand rows a year) the aggregate is sub-millisecond; a cached balance column would be a correctness liability for no measurable gain.

`KOREKSI_*` adjusts the balance but stays out of the headline Masuk/Keluar figures (decided, §13). It surfaces as its own line in the monthly summary, so the accumulated drift is visible rather than buried inside the expense number — which is what keeps "12% lebih hemat dari Juli" meaningful.

### 6.4 Soft delete

`deletedAt` rather than physical delete, everywhere. The design explicitly allows the user to fix a wrongly-marked day, corrections are a first-class concept, and an auditable ledger is the entire point. Nothing is ever hard-deleted except raw notifications past retention.

### 6.5 Threat model

Stated explicitly, because every security decision below follows from it and a generic threat model would produce the wrong design.

**The adversary is a trusted person with routine physical access to the unlocked phone, who already knows the device lock credential, and that credential cannot be changed.** Not a thief, not a remote attacker, not a forensic analyst.

Three consequences, all non-obvious:

1. **Nothing in this app may depend on the Android device lock.** Anything gated on the device credential — `BiometricPrompt` with `DEVICE_CREDENTIAL` allowed, "reset via device lock", fingerprint enrolment — is a free bypass. The app PIN must be an entirely independent secret. (This invalidates device-credential-based PIN recovery, which was under consideration and is rejected.)
2. **Any file the app writes to shared storage is outside the fence, and this is accepted.** Backup archives and monthly PDFs are plaintext (§9), readable by any file manager, so they are a real gap in what the PIN protects — the adversary above never has to open the app at all. **This is a deliberate decision**, not an oversight: encrypting them would cost a passphrase mechanism, an extra PDF library, three more setup screens and a permanent "what if I lose it" failure mode, in exchange for protecting one of two artefacts (the PDF must remain openable on the laptop regardless). Securing these files is delegated to the user, who can place them in a folder they control.

   The one mitigation taken, because it costs nothing: **backups are not left sitting in shared storage.** They are prepared into app-private storage and only leave via an explicit share action (§9), which is what concept §14 describes anyway. That reduces the standing plaintext exposure on the phone to the monthly PDF alone.
3. **There is a ceiling, and the product should not pretend otherwise.** An adversary with full physical access, the device credential, and determination cannot be stopped by any app. What is achievable — and what is actually being asked for — is a reliable barrier against casual, opportunistic snooping by people who share your space. The app's copy should stay in the same honest register it uses for `≈` balances and should not imply more.

**Out of scope by decision:** the user's laptop is single-user and trusted, so material at rest on the laptop is not a threat. This is what makes plaintext PDF export acceptable at the destination end; the exposure that remains is the copy sitting on the phone.

### 6.6 App lock policy

The PIN is **mandatory** — there is no "no lock" mode (§13, item 6). It gates **entry to the app** and **PIN-related settings only** (change PIN, forgotten-PIN flow). It is not re-prompted for ordinary edits — recording cash, answering the reminder, reconciling a wallet all proceed without further authentication once inside.

Re-lock rules:

| Condition | Behaviour |
|---|---|
| Process killed / stopped / device rebooted | **Always** require PIN. No grace, no exception |
| Backgrounded longer than `lockTimeout` | Require PIN |
| Backgrounded shorter than `lockTimeout` | Resume straight into the app |
| Returning from an Android Settings round-trip during setup | Resume straight in (covered by the rule above) |

`lockTimeout` default: **60 seconds**, user-configurable. Deliberately shorter than the 2–5 minutes typical of banking apps, because the adversary here is in the same room — a five-minute grace window means a phone left on a table is open for five minutes. Start strict; the user can loosen it.

This single rule replaces the `suppressLockUntil` mechanism previously proposed for external-intent round-trips (§4). One mechanism instead of two: the grace period covers the setup deep-links to Android Settings for free.

**Forgotten PIN** is handled by three self-written recovery questions — see §6.7. If those fail too, the backstop is `Lupa PIN` → wipe → restore from the most recent backup, which works without any secret because backups are plaintext (§9); cost is bounded at one week of history.

Two mechanisms are permanently excluded and should not be reintroduced: **email reset** (needs a backend, and the recovery mailbox is signed in on the adversary's own phone) and **device-credential or biometric reset** (the adversary knows the device credential, §6.5).

Note that the plaintext backup makes the archive a de facto master key — anyone holding it can restore the ledger onto their own device under a PIN of their choosing. That is the exposure already accepted in §6.5 consequence 2, and it is why backups live in app-private storage rather than a browsable folder (§9).

### 6.7 PIN reset: three self-written recovery questions

Three questions **written entirely by the user**, not chosen from a template list — templates ("nama ibu kandung", "SD pertama") are guessable and often publicly findable, which is the failure mode self-written questions avoid. Fully custom in content, with one structural rule: each must be phrased as a question (§6.7, *Question authoring*). Created during setup, answered to reset a forgotten PIN. No backend, no new dependency.

#### Storage: encrypted, not hashed

Answers are stored **AES-256-GCM encrypted under a non-extractable, hardware-backed Keystore key**, not as one-way hashes.

This looks like a downgrade and isn't, for two reasons specific to this design:

- **Hashing buys very little here.** The value of a slow hash is resistance to offline brute force, but the answer space is small (a word or two), so a determined offline attacker gets through regardless. Meanwhile the real adversary in §6.5 is a family member who *knows the answer* and never brute-forces anything. What actually defends this path is rate limiting, which works identically either way.
- **Encryption buys the thing that matters: typo tolerance.** A hash can only ever answer "exactly equal or not". Being able to decrypt is what makes fuzzy comparison possible at all, and a recovery path that rejects `Kucing Oren ` because of a trailing space is worse than no recovery path.

The Keystore key is hardware-backed and cannot be exported, so an adversary with the phone still cannot read the answers. Against the threat model that is actually being defended, this is equivalent protection with a far more forgiving failure mode.

#### Matching: normalise, then allow near-misses

Comparison runs on both sides through the same pipeline:

1. Unicode **NFKC** normalisation
2. Lowercase with `Locale.ROOT` (never the device locale — Turkish-style dotless-i rules would silently break matching)
3. Strip diacritics
4. Trim, then collapse internal whitespace runs to a single space

That alone handles capitalisation, stray spaces and paste artefacts. On top of it, an answer is accepted when the normalised strings are equal **or** within a Levenshtein distance of `max(1, length / 5)` — one typo on a short answer, proportionally more on a long one. Punctuation is deliberately *not* stripped, because normalisation and fuzzy distance already absorb it and stripping it would cut entropy for no additional benefit.

Levenshtein is ~20 lines. No library, no dependency added by this feature.

#### Threshold and rate limiting

- **All three must match.** With fuzzy matching plus retries, requiring three is barely more onerous than two, and 2-of-3 would mean any single well-guessed answer pair opens the app. If this proves annoying in practice it is a one-constant change.
- Shares the PIN's exponential backoff. Failures **never** lock the path permanently — a permanent lockout would recreate exactly the outcome this feature exists to prevent. Delays grow and then plateau.
- No fallback to device credential, ever (§6.5).

#### Question authoring

Questions are entirely the user's to write — no template list, no suggested wording, no vetting of what makes a "good" question. The only constraint is **structural: the text must be a question.** Validation is deliberately minimal — non-empty, a sensible minimum length, and ending in `?` (trailing whitespace tolerated). Nothing about content. A statement or a bare label is rejected with a plain message; anything phrased as a question is accepted as written.

Two setup-time affordances, both one-off and both about reproducibility rather than instruction:

- **Live normalised preview** — as the user types `Kucing Oren`, the field shows *"akan dicocokkan sebagai: kucing oren"*, so what will be compared is visible while it can still be changed.
- **Re-type each answer once**, which catches an answer that can't be reproduced at the moment it's created rather than a year later.

No recurring prompts, no periodic checks, no reminders to review the answers. Remembering them is the user's business, and the app doesn't nag about it.

Questions and answers can be changed at any time from Pengaturan → KEAMANAN, gated behind the current PIN.

#### Setup step count stays at 8

Question creation is folded into **step 6 ("Buat PIN")**, which becomes *"Buat PIN & pertanyaan pemulihan"*, rather than becoming a ninth step. It belongs there conceptually — both establish how you get into the app — and it leaves the progress indicator untouched.

#### Accepted trade-off

Recorded plainly, once: people close to the user are the population best able to answer personal questions about them, and they are also the adversary in §6.5. Self-written questions can be chosen to defeat this, but nothing in the app can enforce that they were. This is a known, accepted weakening in exchange for a recovery path that needs no stored secret. The setup screen's fragile-answer warning is the only place the product can nudge against it.

### 6.8 Free-text note on every transaction

An optional, free-form note (`catatan`) on **every** transaction, Online and Cash alike. Always optional, never blocks saving, content entirely up to the user.

**This is not a category, and it doesn't reopen that decision.** Categories were rejected because they are a required, structured choice at entry time that adds work without changing any decision (concept §1). A note is unstructured, optional, and written only when the user actually wants to. What it does do is fill the gap the concept doc itself admits in §17 — *"aplikasi bisa menjawab 'berapa yang keluar' tapi tidak 'keluar untuk apa'"* — without building a taxonomy.

#### `keterangan` and `catatan` are different fields, and must stay separate

| Field | Source | Mutable by user | Survives a parser re-run |
|---|---|---|---|
| `keterangan` | Derived — merchant/destination from the notification, or the short cash label | Editable | **No.** Regenerated from `raw_notification` |
| `catatan` | Always user-authored | Editable | **Yes.** Never touched by parsing |

Collapsing these into one field would be a real data-loss bug, not a style choice: §3.3 re-runs parsers over stored raw notifications whenever a parser is fixed, and that pass rewrites `keterangan`. A note sharing that field would be silently wiped by a routine parser fix. **The re-parse routine must never write to `catatan`.**

Setting or editing a note stamps `editedAt`. Notes are included in `backup.json` and in the `transactions.csv` column set (§9), and in the monthly PDF where present — they are the only "what for" signal the ledger has.

#### Where notes appear

- **Cash entry (screen 02)** — a third row in the existing field group, below "Keterangan" and above "Waktu", same row pattern as the other two (flex space-between, 17px vertical padding, divider). Empty-state value reads as placeholder text in `text/faint` rather than a filled value, so an unused note doesn't look like data.
- **Online transactions** — no entry moment exists, since they arrive without user involvement. A note is added afterwards by tapping the row. See the transaction detail sheet in §14.
- **Transaction rows (Home, Riwayat)** — appended to the existing meta line rather than adding a third line: `GoPay 1 · 09:12 · beli kopi buat rapat`. The note truncates with an ellipsis so `Dompet · Jam` is never pushed out. This keeps row height and rhythm exactly as designed, and rows without a note are pixel-identical to the current spec. Full text lives in the detail sheet.

  > Alternative considered: a dedicated third line, shown only when a note exists. Rejected as the default because it makes list rows two different heights and disrupts the scan rhythm the design specifies precisely. Easy to switch later if truncation proves too tight in practice.

#### Editing surface: a bottom sheet, not a screen

Tapping a row opens a `ModalBottomSheet` over Home or Riwayat. There is **no separate detail route**, and the note is edited in place inside that sheet — no second hop, no dedicated note dialog.

Three reasons, in order of weight:

1. **Context stays on screen.** An Online row is identified only by merchant, wallet and time. Annotating it with the list still visible behind the scrim removes the misattribution risk that a full-screen push creates — two Indomaret charges on the same day are indistinguishable once the list is gone.
2. **A route costs more than it returns.** A navigation destination brings a back-stack entry, saved scroll position on return, process-death restoration of a half-typed note, and a deep-link surface. That is a lot of machinery for one text field and three actions.
3. **The return path is the common case.** After writing a note the user is finished; a sheet dismisses to the same list at the same scroll offset, which is what they want.

The sheet is also where correction, delete (§6.4) and the unmatched-transfer question (§3.5) live, so it is not a note-only surface — which is exactly why a bare note dialog was rejected. Build the contents as a **stateless composable taking the transaction plus callbacks**, and let the sheet merely host it. If corrections later outgrow a sheet, promoting it to a route becomes a hosting change rather than a rewrite.

**Save semantics differ by field, deliberately:**

| Field | Commit | Why |
|---|---|---|
| `catatan` | On dismiss, no Simpan button | No invalid state, nothing destructive. An explicit save creates the failure mode "typed a note, swiped down, lost it" |
| `nominal`, `walletId`, `jenis`, delete | Explicit confirm | These move balances (§6.3) |

`editedAt` is stamped only when the trimmed note actually differs from what was stored, so opening and closing a sheet without typing must not dirty the row. No PIN re-prompt at any point — §6.6 gates app entry only.

Two implementation costs to budget for rather than discover: a text field inside `ModalBottomSheet` needs `imePadding()` on the content plus edge-to-edge insets configured properly, or the keyboard covers it on some OEM skins; and the draft note must be held in the ViewModel's `SavedStateHandle`, not in `remember`, so a rotation or process death mid-typing does not discard it.

---

## 7. Design system implementation

The handoff gives oklch values with "≈ hex" approximations and warns to re-verify. Compose has no oklch, so:

- Convert oklch → sRGB **once**, deterministically, and commit the generated `Color.kt` alongside the source oklch values in a comment. Don't paste the approximate hexes; a wrong conversion on the lime accent will be visible on every primary button in the app.
- Wrap tokens in a custom `YourMoneyTheme` exposing `LocalColors`/`LocalTypography` via `CompositionLocal`, keyed by the handoff's token names (`surface/card`, `accent/lime-ink`, …) so the mapping back to the design stays one-to-one.
- Material 3 components are used only where they happen to fit; most of these screens are simpler as bespoke composables than as fought-with M3 overrides. Do not adopt `MaterialTheme.colorScheme` naming — it doesn't match the token set and the translation layer would be pure noise.
- Font: **Plus Jakarta Sans variable font, bundled** (not `google-fonts` runtime download — the handoff says so, and offline start matters). Variable file covers 400/500/600/700/800 in one asset.
- `tabular-nums` on every money glyph: `TextStyle(fontFeatureSettings = "tnum")`.
- The 38px "frame radius" and its shadow are artefacts of the design canvas and must **not** ship.

---

## 8. Folder structure

```
your-money/
├── docs/
│   ├── technical-design.md              ← this file
│   └── design-handoff/                  ← unzipped design bundle, committed for reference
├── app/src/main/kotlin/com/nijika21/yourmoney/
│   ├── YourMoneyApp.kt
│   ├── di/
│   ├── ui/
│   │   ├── theme/                       Color.kt (generated), Type.kt, Dimens.kt, Shapes.kt
│   │   ├── components/                  MoneyText, WalletRow, TxRow, PrimaryButton,
│   │   │                                SecondaryButton, ToggleSwitch, SegmentedControl,
│   │   │                                Chip, ProgressSegments, PinDots, Keypad, TabBar
│   │   ├── home/                        01
│   │   ├── cashentry/                   02
│   │   ├── reminder/                    03, 11  (pengingat + konfirmasi)
│   │   ├── history/                     04
│   │   ├── summary/                     05
│   │   ├── reconcile/                   06
│   │   ├── wallets/                     07
│   │   ├── setup/                       08 + steps 2..8
│   │   ├── lock/                        09 + Lupa PIN (questions → new PIN),
│   │   │                                wipe/restore fallback
│   │   ├── queue/                       12
│   │   ├── settings/                    13, 14
│   │   ├── diagnostics/                 unrecognised notifications, logs, parser lab
│   │   └── nav/                         NavGraph.kt, Destination.kt, PinGate.kt
│   ├── domain/                          ← no Android imports
│   │   ├── model/                       Wallet, Transaction, Jenis, DailyCashStatus, Summary
│   │   ├── ledger/                      BalanceCalculator, PeriodTotals, SummaryBuilder
│   │   ├── parser/                      NotificationParser, ParserRegistry,
│   │   │                                BcaParser, GopayParser, OvoParser, AmountNormalizer
│   │   ├── ingest/                      Deduplicator, TransferMatcher
│   │   ├── reminder/                    ReminderStateMachine, QueueBuilder, DayRoller
│   │   ├── recovery/                    AnswerNormalizer, FuzzyMatcher, QuestionFormatCheck
│   │   └── usecase/
│   ├── data/
│   │   ├── db/                          YourMoneyDatabase, entities, DAOs, migrations
│   │   ├── settings/                    DataStore schemas
│   │   └── repository/
│   └── platform/
│       ├── notification/                TxNotificationListenerService, ReminderNotifier,
│       │                                ReminderForegroundService, channels
│       ├── alarm/                       ReminderScheduler, BootReceiver, TimeChangeReceiver
│       ├── vibration/                   VibrationController
│       ├── usage/                       ForegroundAppMonitor
│       ├── oem/                         OemBatterySettingsIntents (Infinix/Xiaomi/…)
│       ├── export/                      PdfExporter, BackupWriter, BackupReader
│       └── security/                    KeystoreProvider, PinHasher, AppLockManager,
│                                        RecoveryAnswerStore (Keystore-encrypted)
├── app/src/test/                        JVM unit tests
│   └── resources/notifications/{bca,gopay,ovo}/*.json   ← the parser corpus
├── app/src/androidTest/                 Room migration + Compose UI tests
└── .github/workflows/                   build.yml, release.yml
```

---

## 9. Files, PDF and backup

**Neither artefact is encrypted or password-protected.** Decided in §13 item 4: the complexity of a passphrase mechanism (extra KDF, an extra PDF library, additional setup steps and screens, and a permanent lose-it-and-you-are-locked-out failure mode) is not worth it here. Securing these files is delegated to the user, who is free to keep them in an ordinary folder or one they protect themselves. The resulting gap in what the PIN covers is stated and accepted in §6.5, consequence 2.

**Monthly PDF** — generated by a WorkManager job on the last day of the month, idempotent per `yearMonth`. Written via SAF to a user-picked tree URI (`pdfFolderUri`), persisted with `takePersistableUriPermission`. Name `YYYY-MM.pdf`. Drawn with `PdfDocument` + `Canvas`/`StaticLayout` so text stays vector and selectable. Plain PDF, no encryption layer, so `android.graphics.pdf` alone is sufficient and no additional library is needed.

The auto-save-to-synced-folder behaviour from concept §13 works exactly as designed: the file appears on the laptop by itself and opens with no prompt.

**Backup** — weekly WorkManager job producing a **logical export**, for the Keystore reason in §6.1:
- `backup.json` — full fidelity, the thing `restore` actually reads (schema-versioned).
- `transactions.csv` — the "buka sebagai spreadsheet di laptop" requirement.
- Bundled as one plain `.zip`. The ZIP is packaging only, not protection — it keeps the two files together so a restore can't be handed half an archive.

**Where backups live.** Prepared into **app-private storage** (`filesDir/backups/`), not a browsable shared folder, and emitted only when the user explicitly shares or saves them via the share sheet / SAF. This is what concept §14 already describes — *"aplikasi hanya menyiapkan filenya, kamu yang memilih ke mana"* — and it happens to be the one no-cost mitigation available now that the file is plaintext: nothing sits permanently readable on the phone. Rotate, keeping the last 4 weekly archives.

**Restore** — reads `backup.json`, validates schema version, and is **transactional and all-or-nothing**. Offer merge-vs-replace explicitly; silent merge on a ledger produces duplicates that are very hard to unpick afterwards. Restoring on a new device (or after a forgotten-PIN wipe, §6.6) prompts for a new PIN as part of the flow.

**The laptop-folder limitation** is a product constraint the design already commits to explaining honestly (cable copy, or point the folder at a cloud-synced directory). No engineering work can remove it; the setup screen just needs the guidance copy.

---

## 10. Performance strategy

Budgets, so there's something to test against:

| Path | Budget | How |
|---|---|---|
| `onNotificationPosted` → return | < 1 ms | Whitelist check only; DB insert dispatched to background. Parsing never inline |
| Cold start → PIN screen interactive | < 400 ms | PIN screen renders **before** the DB is opened; SQLCipher open (~50–150 ms) runs concurrently with PIN entry, so it's free |
| PIN dot fill | Next frame, no animation delay | The handoff calls this out explicitly |
| Home render after unlock | < 200 ms | One combined Room `Flow`; no N+1 per-wallet queries |
| Riwayat scroll | 60 fps | Paging 3, `LazyColumn` with stable keys, pre-formatted immutable UI models |

Other measures:
- Balance via SQL aggregate (§6.3). Revisit only past ~50k transactions, which at realistic volume is decades away.
- No polling anywhere except `ForegroundAppMonitor`, and only while a reminder is actively pending.
- WorkManager jobs constrained to charging/idle where timing permits (backup yes; month-end summary no, it's time-anchored).
- R8 full mode; ~5 font weights collapsed into one variable font file. **No Baseline Profile** — it is a per-release generation-and-verification chore, and the startup budget above is already met on the one device this app runs on. Revisit only if cold start actually misses its budget there.
- Raw notification retention purge (90 days) keeps the largest-growing table bounded.

---

## 11. Testing strategy

Priority follows risk from §1 — parsers and ledger math get near-total coverage, UI gets targeted coverage, and the platform-fighting parts get a manual checklist because they cannot be honestly automated.

**1. Parser corpus — the highest-value tests in the project.**
`src/test/resources/notifications/<source>/*.json`, each fixture a captured real notification plus its expected `ParseOutcome`. Parameterised JUnit runs the whole corpus. Every bug report and every new bank wording becomes a new fixture, permanently. Target: every fixture green, and the corpus is the definition of "the parser works".

One non-parser test belongs here because this is where the risk lives: **re-running parsers over stored raws must rewrite `keterangan` and leave `catatan` untouched** (§6.8). Cheap to assert, and it protects user-written notes from being wiped by a routine parser fix.

**2. Ledger property tests.** Beyond examples, assert invariants over generated transaction sets:
- `PINDAH_DOMPET` is balance-neutral across the wallet set (this is the exact bug the concept doc warns about — property tests catch the regression that example tests miss).
- Sum of wallet balances == sum of `saldoAwal` + net(MASUK) − net(KELUAR) + net(KOREKSI).
- Period totals never include `PINDAH_DOMPET`.

**3. Time-dependent logic.** `Clock` is injected everywhere — no direct `System.currentTimeMillis()` or `LocalDate.now()` in domain code. Otherwise the transfer-match window, the 7-day auto-close, the midnight rollover and the month-end trigger are all untestable. Fake clock in tests, real clock via Hilt in production.

**4. Room.** In-memory DB for DAO tests; committed schema JSON with migration tests on every version bump.

**5. ViewModels.** Turbine + `TestDispatcher`, asserting emitted state sequences.

**6. Compose UI — two flows deserve real tests, both behavioural rather than visual:**
- **PIN gate routing:** entry from notification/deep link/icon always lands on PIN first, then continues to the *original* destination.
- **The "tidak ada pengeluaran" path:** asserting it takes two deliberate steps and cannot complete in one tap. This is a product invariant the design argues for at length; a test stops it being "simplified" later.

**6b. Security invariants — cheap tests that protect §6.5 from erosion:**
- Lock policy: process death always demands the PIN; background under `lockTimeout` does not; background over it does. Driven by the injected `Clock`.
- Backup round-trip: export → restore reproduces the ledger exactly (balances, day statuses, transfer groupings), and a truncated or schema-mismatched archive is rejected whole rather than partially applied.
- **Answer matching (§6.7) — a fixture corpus, same pattern as the parsers.** Each case is `stored answer → attempted answer → expected accept/reject`, covering capitalisation, leading and trailing spaces, doubled internal spaces, diacritics, single-character typos, transpositions, and near-miss answers that must still be **rejected** so the fuzzy threshold doesn't quietly drift into accepting anything. Pure functions, no Android, instant to run.
- Lowercasing uses `Locale.ROOT`: an explicit test under a Turkish locale, because the dotted-i rule is the classic way this silently breaks.
- Negative test: no code path reaches unlock via `DEVICE_CREDENTIAL`. A plain unit test asserting the unlock entry points is enough. (An architecture-test framework — Konsist/ArchUnit — was considered and dropped: a whole dependency and rule DSL to catch one thing that a single test and a code review already catch.)

**7. Platform behaviour — manual, on the real Infinix.** Alarm firing under doze, after reboot, under battery saver, with the OEM autostart toggle off; notification non-dismissibility; vibration under DND; blacklist suppression. Automation here produces false confidence.

The setup wizard's step 8 ("tes sekali") is effectively a built-in integration test — build it as a **reusable diagnostics screen**, not a one-shot onboarding step, so it can be re-run whenever the phone updates and quietly breaks something.

**Coverage intent:** none, deliberately. There is no percentage target on `domain/` or anywhere else. A coverage number is a proxy that this project does not need: the risk here is concentrated in the parser corpus, the ledger invariants and the answer-matching corpus above, and those are enumerated by name. Chasing a percentage past them buys tests for trivial code and a false sense of the rest.

---

## 12. Deployment strategy

Distribution is sideload (§13), which removes store review entirely and leaves three things that actually matter: reproducible builds, safe key custody, and painless updates.

**Build & signing**
- `./gradlew assembleRelease`, R8 full mode, resource shrinking.
- Upload-key style release keystore, **stored outside the repo and backed up off-device**. Losing it means no user can ever upgrade in place — they'd have to uninstall and lose data. Treat the keystore backup with the same seriousness as the data backup.
- `versionCode` = monotonically increasing integer derived from the build; `versionName` semver. The Pengaturan footer renders `versionName` ("Your Money · versi 1.0 · data hanya di HP ini").

**CI — GitHub Actions**
- `build.yml` on every push: ktlint, detekt, unit tests (domain + parser corpus), Room migration tests, assemble debug. This is the gate that keeps the parser corpus honest.
- `release.yml` on tag `v*`: decode keystore from encrypted secrets, `assembleRelease`, attach the signed APK to a GitHub Release with generated notes.

**Updates — Obtainium**
Point Obtainium at the GitHub Releases page. It polls, notifies, and installs new versions with no store involved. This is the closest thing to Play auto-update available for a sideloaded personal app, and it's why releases must be tagged and signed consistently rather than built ad hoc.

**Two build variants**
- `debug` — the M1 capture build: notification listener, raw storage, diagnostics screen, parser lab. Runs on the phone from week 1.
- `release` — full app. Diagnostics screen stays available but moves behind Pengaturan, because it's the fastest field-debugging tool when a bank changes its notification wording.

**Observability without a backend**
No crash SDK (§5). Instead: `Thread.setDefaultUncaughtExceptionHandler` writes to a rotating, redacted local log (amounts and merchant names stripped); the diagnostics screen reads it; the backup zip includes it. That log plus the `raw_notification` table is enough to diagnose essentially every failure this app can have.

**Release checklist** (the parts automation can't cover)
Fresh-install setup on the real Infinix · reboot, confirm the alarm re-registers · overnight doze test, confirm the reminder fires · battery-saver on, confirm ingestion survives · restore from the previous version's backup · confirm in-place upgrade keeps the DB and the Keystore key.

---

## 13. Decisions

1. **Distribution: sideload / personal.** ✅ Decided. Signed APK via GitHub Releases, auto-update through Obtainium. Consequences: `UsageStatsManager` is safe to use (§3.8), `SCHEDULE_EXACT_ALARM` needs no store justification, and the `specialUse` foreground-service type needs no review. No Data Safety form, no permissions declaration. If this ever moves to Play, §3.8 and the FGS declaration are the first things to revisit.
2. **`KOREKSI` is excluded from headline Masuk/Keluar totals.** ✅ Decided. It still adjusts the balance, and it appears as its own line in the monthly summary so the drift stays visible instead of hiding inside the expense figure. Keeps month-over-month comparison meaningful.
3. **Real notification corpus first.** ✅ Decided. M1 ships a capture-only build to the phone in week 1; parsers (M3) are written against real captured strings, never guessed formats. See §15.
4. **Backup files and exported PDFs are not encrypted.** ✅ Decided. No passphrase, no KDF, no derived subkeys, no verifier. The recovery-passphrase mechanism previously specified in §6.7 is removed from the document entirely, and PdfBox-Android is dropped — plain `PdfDocument` suffices. Rationale: the complexity (an extra dependency, extra setup steps, several new screens, and a permanent lose-it-and-you're-locked-out failure mode) outweighs the benefit, given the PDF has to stay openable on the laptop regardless. File security outside the app is the user's to manage. The accepted gap is stated in §6.5 consequence 2; the one free mitigation taken is in §9.
5. **Forgotten PIN → three self-written recovery questions**, with wipe-and-restore as the backstop. ✅ Decided (§6.7). Chosen knowingly over the alternatives after the trade-off was put on the table: close contacts are the population best able to answer personal questions, and that is also the adversary in §6.5 — accepted in exchange for a recovery path needing no stored secret. The typo and capitalisation problem is solved rather than tolerated: answers are Keystore-encrypted instead of hashed, which is what makes normalisation plus fuzzy matching possible (§6.7). Email reset and device-credential reset stay permanently excluded. No new dependency, and setup stays at 8 steps.
6. **The PIN is mandatory.** ✅ Decided, final. Not optional, no enable/disable toggle in Pengaturan, no `LockMethod = NONE` path. This preserves concept §10 ("Tidak ada jalan masuk lain") and screen 09's stated role as the single entrance, and it keeps the nav graph to one shape. `LockMethod` in §4 collapses to a constant — keep the gate destination in the graph, drop the branch.
7. **Threat model written down explicitly** (§6.5) and the app lock policy derived from it (§6.6). ✅ Decided. Notably: no security mechanism in this app may lean on the Android device lock, and biometric unlock defaults to off *with an explanation on screen 13*, because enrolling a fingerprint needs only the device credential the adversary already has.
8. **`applicationId` is `com.nijika21.yourmoney`.** ✅ Decided. It matches the GitHub handle that hosts the Releases page Obtainium points at (§12), so identity is consistent across the one distribution channel this app has. Now fixed: changing it after the first install forces an uninstall/reinstall and loses the database, because the Keystore key that unwraps the SQLCipher passphrase is scoped to the app.
9. **Ceremonial rigor stripped.** ✅ Decided. Removed: the >90% `domain/` coverage target (§11), the Konsist/ArchUnit architecture test (§11 item 6b), the Baseline Profile (§2.2, §10, §12), and the separate formal doze-soak phase (§15 M9 — the §12 release checklist covers it). Kept, because each was argued for on its own merits rather than as process: the parser golden-fixture corpus, the ledger property tests, the answer-matching corpus, and raw-notification storage. The distinction that decided each case: does it catch a bug this app will actually have, or does it certify that work happened?

## 14. Gaps found in the design

Not blockers, but they need answers before the relevant screen is built:

- **Setup steps 2–8 have no screens.** Only step 1 is designed. Step 3 (per-source notification checklist) and step 4 (OEM battery settings) are the highest-risk screens in the app and currently exist only as prose.
- **No transaction detail surface — now blocking, not optional.** Corrections are a core concept and soft delete is in the schema, but nothing in the design exposes either. The free-text note (§6.8) makes this mandatory rather than nice-to-have: an Online transaction arrives with no user involvement, so tapping the row is the *only* moment a note can ever be attached to it. Form is settled — a bottom sheet, note edited in place, per §6.8. What is still undesigned is its **contents and copy**: full `keterangan`, editable `catatan`, amount, wallet, time, `jenis`, source badge, and the edit/delete actions.
- **Note row on screen 02 (Catat cash)** — the field group grows from two rows to three (§6.8). Placeholder copy for the empty state is not specified in the design.
- **No screen for the unmatched-transfer question** ("aplikasi bertanya sekali", concept §5).
- **No diagnostics/unrecognised-notification screen.** Operationally necessary; see §3.3.
- **Reminder default time conflicts:** 20:00 in the concept doc, README and screens 03/12/14; **21:00** on screen 07. Assuming 20:00.
- **Setup progress segments:** 7 in the design, 8 steps in the spec. The README already flags this; assuming 8.
- **Destination wallet unknown** for a detected topup (e.g. topup to a wallet not configured in the app) — undefined. Suggest: keep as `KELUAR` and offer to add the wallet.

New screens implied by the §13 decisions. Setup stays at **8 steps** — question creation folds into step 6 — so the progress indicator reverts to the original 7-vs-8 discrepancy noted above and nothing further changes there.

- **Recovery question creation**, inside setup step 6 after the PIN is confirmed: three question fields, three answer fields, each answer re-typed once, with the live normalised preview (§6.7).
- **Lupa PIN** — answer the three questions → set a new PIN. Screen 09 has the link and nothing behind it.
- **Wipe-and-restore fallback**, reached from Lupa PIN when the questions can't be answered: states plainly what is lost, shows the date of the last backup, and requires a hard confirmation.
- **Change recovery questions** in Pengaturan → KEAMANAN, gated behind the current PIN.
- **Biometric toggle warning copy** on screen 13. The row exists; the explanation of why it is off by default does not, and per §6.1 that explanation is the whole point of the row.
- **Lock timeout row** in Pengaturan → KEAMANAN (§6.6), currently unrepresented.

## 15. Suggested build order

Ordered so that the long-lead item (collecting real notification data) starts first and runs in the background.

| Phase | Contents |
|---|---|
| **M0** | Project skeleton, Hilt, theme + token conversion, design-system component gallery, Room + SQLCipher, `Clock` injection |
| **M1** | **Notification listener + raw capture + diagnostics screen. Ship to the phone immediately** and let the corpus accumulate while M2–M4 are built |
| **M2** | Ledger domain + property tests, Home (01), Catat cash (02) incl. note field, transaction detail sheet (§6.8, §14) |
| **M3** | Parsers against the real corpus, dedup, transfer matcher, admin-fee split |
| **M4** | Reminder engine: alarms, FGS, vibration, ongoing notification, blacklist, queue (03, 10, 11, 12) |
| **M5** | Reconcile wizard (06), Dompet (07) |
| **M6** | Riwayat + Paging (04), monthly summary + PDF (05) |
| **M7** | PIN + gate routing (09), lock policy (§6.6), recovery questions + Lupa PIN (§6.7), `FLAG_SECURE`, Pengaturan (13, 14) |
| **M8** | Setup wizard (08 + steps 2–8, incl. question creation inside step 6), OEM battery deep links, self-test |
| **M9** | Backup/restore, hardening. The release checklist in §12 is the reliability check — there is no separate formal soak phase |

M1 before M3 is the point of the ordering: parsers written against guessed formats are wasted work, and the only way to get real formats is calendar time on the actual device.
