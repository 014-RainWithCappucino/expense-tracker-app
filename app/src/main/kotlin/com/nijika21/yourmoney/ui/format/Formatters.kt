package com.nijika21.yourmoney.ui.format

import com.nijika21.yourmoney.domain.model.Jenis
import com.nijika21.yourmoney.domain.money.Rupiah
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Every user-visible string built from data passes through here.
 *
 * The locale is pinned to `id-ID` rather than read from the device: this app has
 * exactly one user and its copy is Indonesian throughout, so a phone set to
 * English would otherwise produce "Wednesday, 6 August" inside Indonesian
 * sentences. Pinning it also keeps these functions deterministic in tests.
 */
private val indonesian: Locale = Locale.forLanguageTag("id-ID")

private val dayFormat = DateTimeFormatter.ofPattern("EEEE, d MMMM", indonesian)
private val clockFormat = DateTimeFormatter.ofPattern("HH:mm", indonesian)
private val dateTimeFormat = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", indonesian)
private val dateFormat = DateTimeFormatter.ofPattern("d MMM yyyy", indonesian)

fun formatDay(millis: Long, zone: ZoneId): String =
    Instant.ofEpochMilli(millis).atZone(zone).format(dayFormat)

fun formatClock(millis: Long, zone: ZoneId): String =
    Instant.ofEpochMilli(millis).atZone(zone).format(clockFormat)

fun formatDateTime(millis: Long, zone: ZoneId): String =
    Instant.ofEpochMilli(millis).atZone(zone).format(dateTimeFormat)

fun formatDate(date: LocalDate): String = date.format(dateFormat)

/**
 * A ledger row's amount, signed by what the row does to its wallet.
 *
 * `PINDAH_DOMPET` gets no sign on purpose. It leaves one wallet and enters
 * another, so `+` or `−` would both be half true, and the row already names both
 * wallets.
 */
fun formatRowAmount(jenis: Jenis, nominal: Long): String = when (jenis) {
    Jenis.PINDAH_DOMPET -> Rupiah.format(nominal)
    else -> Rupiah.signed(jenis.effectOnSource(nominal))
}

/**
 * What a row is titled when the user typed no label.
 *
 * Defaulting the stored `keterangan` to "Tunai" produced rows reading
 * *"Tunai / Tunai · 06:34"* — the wallet name twice, once as the title and once
 * as the meta. So nothing is stored, and the title falls back to the direction,
 * which is the only thing actually known about an unlabelled entry.
 */
fun displayKeterangan(keterangan: String, jenis: Jenis): String =
    keterangan.ifBlank {
        when (jenis) {
            Jenis.MASUK -> "Uang masuk"
            Jenis.KELUAR -> "Pengeluaran"
            Jenis.PINDAH_DOMPET -> "Pindah dompet"
            Jenis.KOREKSI_NAIK, Jenis.KOREKSI_TURUN -> "Koreksi saldo"
        }
    }
