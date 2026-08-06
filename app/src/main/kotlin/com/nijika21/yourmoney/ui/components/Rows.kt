package com.nijika21.yourmoney.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nijika21.yourmoney.domain.model.Jenis
import com.nijika21.yourmoney.ui.theme.YourMoneyTheme

/**
 * A ledger row, with its money already formatted (§4). Nothing here computes;
 * the ViewModel decides every string so the glyph rules live in one place.
 */
@Immutable
data class TxRowUi(
    val id: String,
    val keterangan: String,
    /** `GoPay 1 · 09:12` — always fully visible. */
    val walletAndTime: String,
    /** The user's note, or null. Truncates before [walletAndTime] ever does. */
    val catatan: String?,
    val amount: String,
    val jenis: Jenis,
)

@Composable
fun jenisColor(jenis: Jenis): Color {
    val colors = YourMoneyTheme.colors
    return when (jenis) {
        Jenis.MASUK -> colors.masuk
        Jenis.KELUAR -> colors.keluar
        Jenis.PINDAH_DOMPET -> colors.pindah
        Jenis.KOREKSI_NAIK, Jenis.KOREKSI_TURUN -> colors.koreksi
    }
}

/**
 * Two lines, never three (§6.8). The note is appended to the meta line and
 * truncates with an ellipsis, which keeps every row the same height — the scan
 * rhythm the design specifies precisely. A row without a note is identical to
 * one built before notes existed.
 */
@Composable
fun TxRow(row: TxRowUi, onClick: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = YourMoneyTheme.colors
    val type = YourMoneyTheme.typography
    val dimens = YourMoneyTheme.dimens

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .pressable { onClick(row.id) }
            .padding(vertical = dimens.gapM),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                row.keterangan,
                style = type.rowTitle,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row {
                // Unweighted, so it can never be squeezed by a long note.
                Text(row.walletAndTime, style = type.rowMeta, color = colors.textMuted)
                if (row.catatan != null) {
                    Text(" · ", style = type.rowMeta, color = colors.textMuted)
                    Text(
                        row.catatan,
                        style = type.rowMeta,
                        color = colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
            }
        }
        Spacer(Modifier.size(dimens.gapM))
        Text(row.amount, style = type.bodyMoney, color = jenisColor(row.jenis))
    }
}

/** A wallet plus its predicted balance. The `≈` is the honesty (§6.3). */
@Immutable
data class WalletRowUi(
    val id: String,
    val nama: String,
    val saldo: String,
    val terhubung: Boolean,
)

@Composable
fun WalletRow(row: WalletRowUi, modifier: Modifier = Modifier) {
    val colors = YourMoneyTheme.colors
    val type = YourMoneyTheme.typography
    val dimens = YourMoneyTheme.dimens

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(row.nama, style = type.rowTitle, color = colors.textPrimary)
            if (!row.terhubung) {
                Text("dicatat sendiri", style = type.caption, color = colors.textSecondary)
            }
        }
        Text(row.saldo, style = type.bodyMoney, color = colors.textPrimary)
    }
}
