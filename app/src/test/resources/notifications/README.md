# Notification corpus

Real notifications captured on the device, one JSON file each. TDD §11 calls
these the highest-value tests in the project: every parser in M3 is written
against this directory, and every bug report becomes a new file here permanently.

## Rules

- **Captured, never invented.** A file only lands here after the string was seen
  on hardware. Guessed formats are what the M1-before-M3 ordering exists to
  avoid.
- **`text` and `bigText` are verbatim**, including typos, emoji, marketing voice
  and inconsistent separators. Tidying them defeats the purpose.
- **One redaction only: personal names.** The account holder's own name is
  replaced with `NAMA PENERIMA` / `NAMA PENGIRIM`, keeping length and word count
  plausible so the parser still faces the same shape. Amounts, merchant names,
  timestamps and bank-side masking (`***PET **AK ***GSA`) are untouched — they
  are what the parser has to read. Unredacted dumps stay in the gitignored
  `corpus-raw/` at the repo root.
- **`expectedSignal`** is the `TransactionSignal` verdict, asserted today by
  `TransactionSignalTest`. `expectedParse` is deliberately absent until M3
  defines the parse outcome type; adding it now would be inventing an M3 shape
  before the parsers exist.

## What the first four files already taught us

- **BCA and GoPay disagree on everything.** BCA writes `IDR 8,127.00` — comma
  thousands, two decimals, English currency code. GoPay writes `Rp26.374` — dot
  thousands, no decimals. One amount normaliser has to swallow both, and the
  separators mean opposite things between them.
- **GoPay's channel is useless for filtering.** Receipts and ads both arrive on
  `promotional_notifications`. Only the text can tell them apart.
- **BCA prefixes with `Financial Diary:`** and appends `di kategori <X>` — BCA's
  own categorisation, worth keeping out of `keterangan`.
- **There is a live transfer pair in here** (§3.5): `gopay/2026-08-06-transfer-keluar`
  at 23:58:32.0 and `bca/2026-08-06-pemasukan-transfer` at 23:58:34.6 are the two
  sides of the same Rp8.127 — 2.6 seconds apart, one wallet's `KELUAR` and
  another's `MASUK` that must collapse into a single `PINDAH_DOMPET`. This is the
  matcher's first real fixture, and it says the time window can be tight.
- **BCA identifies the sender by masked name, not by bank.** `***PET **AK ***GSA`
  is *Dompet Anak Bangsa* — GoPay's legal entity. So the BCA side of a GoPay
  topup is recognisable by that mask, which is a stronger matching hint than the
  amount alone.
