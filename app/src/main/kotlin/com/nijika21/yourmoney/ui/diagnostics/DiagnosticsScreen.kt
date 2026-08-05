package com.nijika21.yourmoney.ui.diagnostics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import com.nijika21.yourmoney.domain.capture.CaptureSignal
import com.nijika21.yourmoney.domain.capture.DiscoveredSource
import com.nijika21.yourmoney.domain.capture.SourceRegistry
import com.nijika21.yourmoney.domain.model.RawNotification
import com.nijika21.yourmoney.ui.theme.YourMoneyTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM HH:mm:ss")

/**
 * M1's only screen. Its job is not to look like the product — it is to answer
 * three questions while the corpus accumulates:
 *
 *  1. Is capture actually running? (the listener silently dies on OEM skins)
 *  2. Are the guessed package names in `SourceRegistry` the real ones?
 *  3. What do the notification strings actually say, verbatim?
 *
 * Question 3 is the whole reason M1 ships before M3: parsers written against
 * guessed formats are wasted work.
 */
@Composable
fun DiagnosticsScreen(
    state: DiagnosticsUiState,
    onOpenListenerSettings: () -> Unit,
    onClearDiscovered: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = YourMoneyTheme.colors
    val type = YourMoneyTheme.typography
    val dimens = YourMoneyTheme.dimens

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
            .padding(horizontal = dimens.screenPadding),
        verticalArrangement = Arrangement.spacedBy(dimens.gapM),
    ) {
        item {
            Spacer(Modifier.height(dimens.gapL))
            Text("Diagnostik tangkapan", style = type.screenTitle, color = colors.textPrimary)
            Spacer(Modifier.height(dimens.gapXs))
            Text(
                "M1 — mengumpulkan contoh notifikasi asli. Parser belum ada.",
                style = type.rowMeta,
                color = colors.textMuted,
            )
        }

        item {
            ListenerStatusCard(
                enabled = state.listenerEnabled,
                capturedCount = state.capturedCount,
                onOpenListenerSettings = onOpenListenerSettings,
            )
        }

        item { SectionTitle("Sumber yang dipantau") }

        items(SourceRegistry.candidates, key = { it.packageName }) { candidate ->
            val confirmed = candidate.packageName in state.confirmedCandidates
            CandidateRow(
                packageName = candidate.packageName,
                label = candidate.label,
                confirmed = confirmed,
            )
        }

        if (state.discoveryLeaderboard.isNotEmpty()) {
            item {
                SectionTitle("Paket lain yang terlihat")
                Text(
                    "Hanya nama paket dan jumlahnya yang disimpan — isi notifikasinya " +
                        "tidak pernah dibaca atau ditulis. Daftar ini ada untuk " +
                        "memastikan nama paket di atas benar.",
                    style = type.caption,
                    color = colors.textMuted,
                )
            }
            items(state.discoveryLeaderboard, key = { it.packageName }) { source ->
                DiscoveredRow(source)
            }
            item {
                Text(
                    "Kosongkan daftar",
                    style = type.label,
                    color = colors.accentLime,
                    modifier = Modifier
                        .clickable(onClick = onClearDiscovered)
                        .padding(vertical = dimens.gapS),
                )
            }
        }

        item { SectionTitle("Transaksi (${state.transaksi.size})") }

        if (state.transaksi.isEmpty()) {
            item {
                Text(
                    "Belum ada. Setelah izin aktif, transaksi berikutnya akan muncul di sini.",
                    style = type.body,
                    color = colors.textMuted,
                )
            }
        } else {
            items(state.transaksi, key = { it.raw.id }) { row -> RawRow(row.raw, row.signal) }
        }

        if (state.mungkin.isNotEmpty()) {
            item {
                SectionTitle("Mungkin transaksi (${state.mungkin.size})")
                Text(
                    "Ada nominalnya, tapi tidak ada kata yang bilang transaksinya selesai. " +
                        "Perlu dilihat sendiri.",
                    style = type.caption,
                    color = colors.textMuted,
                )
            }
            items(state.mungkin, key = { it.raw.id }) { row -> RawRow(row.raw, row.signal) }
        }

        if (state.bukan.isNotEmpty()) {
            item {
                SectionTitle("Bukan transaksi (${state.bukan.size})")
                Text(
                    "Iklan dan notifikasi biasa. Tetap disimpan — filternya cuma perkiraan, " +
                        "jadi kalau ada yang salah masuk sini, masih bisa dibaca.",
                    style = type.caption,
                    color = colors.textMuted,
                )
            }
            items(state.bukan, key = { it.raw.id }) { row -> RawRow(row.raw, row.signal) }
        }

        item {
            Spacer(Modifier.height(dimens.gapM))
            Text(
                "${state.capturedCount} notifikasi tersimpan seluruhnya.",
                style = type.caption,
                color = colors.textMuted,
            )
        }

        item { Spacer(Modifier.height(dimens.gapXl)) }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Column {
        Spacer(Modifier.height(YourMoneyTheme.dimens.gapM))
        Text(
            text.uppercase(),
            style = YourMoneyTheme.typography.sectionTitle,
            color = YourMoneyTheme.colors.textSecondary,
        )
        Spacer(Modifier.height(YourMoneyTheme.dimens.gapXs))
    }
}

@Composable
private fun ListenerStatusCard(
    enabled: Boolean,
    capturedCount: Long,
    onOpenListenerSettings: () -> Unit,
) {
    val colors = YourMoneyTheme.colors
    val type = YourMoneyTheme.typography
    val dimens = YourMoneyTheme.dimens

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.card, YourMoneyTheme.shapes.card)
            .border(dimens.hairline, colors.border, YourMoneyTheme.shapes.card)
            .padding(dimens.cardPadding),
        verticalArrangement = Arrangement.spacedBy(dimens.gapS),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Dot(if (enabled) colors.success else colors.danger)
            Spacer(Modifier.size(dimens.gapS))
            Text(
                if (enabled) "Akses notifikasi aktif" else "Akses notifikasi MATI",
                style = type.rowTitle,
                color = colors.textPrimary,
            )
        }
        Text(
            if (enabled) {
                "$capturedCount notifikasi tersimpan."
            } else {
                "Tanpa izin ini aplikasi tidak menangkap apa pun."
            },
            style = type.rowMeta,
            color = colors.textSecondary,
        )
        Text(
            if (enabled) "Buka pengaturan" else "Aktifkan sekarang",
            style = type.button,
            color = colors.accentLime,
            modifier = Modifier
                .clickable(onClick = onOpenListenerSettings)
                .padding(vertical = dimens.gapXs),
        )
    }
}

@Composable
private fun CandidateRow(packageName: String, label: String, confirmed: Boolean) {
    val colors = YourMoneyTheme.colors
    val type = YourMoneyTheme.typography
    val dimens = YourMoneyTheme.dimens

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = dimens.gapS),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Dot(if (confirmed) colors.success else colors.textMuted)
        Spacer(Modifier.size(dimens.gapM))
        Column(Modifier.weight(1f)) {
            Text(label, style = type.rowTitle, color = colors.textPrimary)
            Text(
                packageName,
                style = type.rowMeta,
                color = colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            if (confirmed) "terbukti" else "belum",
            style = type.caption,
            color = if (confirmed) colors.success else colors.textMuted,
        )
    }
}

@Composable
private fun DiscoveredRow(source: DiscoveredSource) {
    val colors = YourMoneyTheme.colors
    val type = YourMoneyTheme.typography
    val dimens = YourMoneyTheme.dimens

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = dimens.gapXs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            source.packageName,
            style = type.rowMeta,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.size(dimens.gapS))
        Text("${source.count}", style = type.bodyMoney, color = colors.textMuted)
    }
}

@Composable
private fun RawRow(raw: RawNotification, signal: CaptureSignal) {
    val colors = YourMoneyTheme.colors
    val type = YourMoneyTheme.typography
    val dimens = YourMoneyTheme.dimens

    // Noise is dimmed rather than hidden, so a misclassified receipt is still
    // findable — the whole corpus stays readable either way.
    val headerColor = when (signal) {
        CaptureSignal.TRANSAKSI -> colors.accentLime
        CaptureSignal.MUNGKIN -> colors.warning
        CaptureSignal.BUKAN -> colors.textMuted
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface, YourMoneyTheme.shapes.cardSmall)
            .border(dimens.hairline, colors.border, YourMoneyTheme.shapes.cardSmall)
            .padding(dimens.cardPadding),
        verticalArrangement = Arrangement.spacedBy(dimens.gapXs),
    ) {
        Row {
            Text(
                SourceRegistry.labelFor(raw.packageName) ?: raw.packageName,
                style = type.label,
                color = headerColor,
                modifier = Modifier.weight(1f),
            )
            Text(
                Instant.ofEpochMilli(raw.postTime)
                    .atZone(ZoneId.systemDefault())
                    .format(timeFormat),
                style = type.caption,
                color = colors.textMuted,
            )
        }
        raw.title?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = type.rowTitle, color = colors.textPrimary)
        }
        raw.text?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = type.body, color = colors.textSecondary)
        }
        // bigText is where BCA puts the amount often enough that hiding it
        // would defeat the point of the corpus. Shown verbatim, never trimmed.
        raw.bigText?.takeIf { it.isNotBlank() && it != raw.text }?.let {
            Text(it, style = type.rowMeta, color = colors.textMuted)
        }
        Text(raw.parseState.name, style = type.caption, color = colors.textMuted)
    }
}

@Composable
private fun Dot(color: Color) {
    Spacer(
        Modifier
            .size(8.dp)
            .background(color, CircleShape),
    )
}
