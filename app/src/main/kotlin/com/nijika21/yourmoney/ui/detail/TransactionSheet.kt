package com.nijika21.yourmoney.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nijika21.yourmoney.ui.components.DangerButton
import com.nijika21.yourmoney.ui.components.SecondaryButton
import com.nijika21.yourmoney.ui.components.jenisColor
import com.nijika21.yourmoney.ui.theme.YourMoneyTheme

/**
 * Hosts [TransactionDetail] in a `ModalBottomSheet`.
 *
 * The contents are a stateless composable taking the state plus callbacks, and the
 * sheet does nothing but host it (§6.8). If corrections outgrow a sheet later,
 * promoting this to a route is a hosting change rather than a rewrite.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionSheetHost(
    state: TransactionSheetState,
    onCatatan: (String) -> Unit,
    onDismiss: () -> Unit,
    onHapus: () -> Unit,
) {
    if (!state.terbuka) return

    // Two problems on the device, both fixed here rather than in the content:
    // the half-height sheet left the delete confirmation clipped at the screen
    // edge until it was dragged up by hand, and the system keyboard covered the
    // note field — the exact hazard §6.8 warns to budget for. Full height plus a
    // scrollable body means the field can always be brought above the keyboard.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = YourMoneyTheme.colors.surface,
        shape = YourMoneyTheme.shapes.sheet,
    ) {
        TransactionDetail(
            state = state,
            onCatatan = onCatatan,
            onHapus = onHapus,
        )
    }
}

@Composable
fun TransactionDetail(
    state: TransactionSheetState,
    onCatatan: (String) -> Unit,
    onHapus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = YourMoneyTheme.colors
    val type = YourMoneyTheme.typography
    val dimens = YourMoneyTheme.dimens

    var mintaKonfirmasi by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = dimens.screenPadding)
            // A text field inside a sheet needs both, or the keyboard covers it.
            .imePadding()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(dimens.gapS),
    ) {
        Text(state.keterangan, style = type.screenTitle, color = colors.textPrimary)
        Text(state.amount, style = type.titleMoney, color = jenisColor(state.jenis))

        Row(horizontalArrangement = Arrangement.spacedBy(dimens.gapS)) {
            Badge(if (state.otomatis) "otomatis" else "dicatat sendiri")
            if (state.sudahDiubah) Badge("pernah diubah")
        }

        Spacer(Modifier.height(dimens.gapS))
        DetailRow("Dompet", state.walletNama)
        state.walletTujuanNama?.let { DetailRow("Ke dompet", it) }
        DetailRow("Waktu", state.waktu)

        Spacer(Modifier.height(dimens.gapM))
        Text("Catatan", style = type.label, color = colors.textSecondary)
        // No Simpan button by design: the note commits when the sheet closes.
        // An explicit save here creates "typed a note, swiped down, lost it".
        BasicTextField(
            value = state.catatanDraft,
            onValueChange = onCatatan,
            textStyle = type.body.copy(color = colors.textPrimary),
            cursorBrush = SolidColor(colors.accentLime),
            decorationBox = { inner ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(colors.card, YourMoneyTheme.shapes.field)
                        .border(dimens.hairline, colors.border, YourMoneyTheme.shapes.field)
                        .padding(dimens.cardPadding),
                ) {
                    if (state.catatanDraft.isEmpty()) {
                        Text(
                            "Buat apa? Opsional — tersimpan sendiri waktu ditutup.",
                            style = type.body,
                            color = colors.textMuted,
                        )
                    }
                    inner()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp),
        )

        Spacer(Modifier.height(dimens.gapM))

        if (mintaKonfirmasi) {
            // Delete moves a balance, so it keeps an explicit confirm (§6.8).
            Text(
                "Hapus transaksi ini? Angkanya keluar dari saldo dan ringkasan, " +
                    "tapi jejaknya tetap tersimpan.",
                style = type.body,
                color = colors.textSecondary,
            )
            Spacer(Modifier.height(dimens.gapS))
            Row(horizontalArrangement = Arrangement.spacedBy(dimens.gapM)) {
                // Not the lime button. Lime is the affirmative accent used for
                // saving; wearing it here would make deleting look like the
                // agreeable choice.
                DangerButton(
                    text = "Ya, hapus",
                    onClick = onHapus,
                    modifier = Modifier.weight(1f),
                )
                SecondaryButton(
                    text = "Batal",
                    onClick = { mintaKonfirmasi = false },
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            SecondaryButton(text = "Hapus transaksi", onClick = { mintaKonfirmasi = true })
        }

        Spacer(Modifier.height(dimens.gapL))
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    val colors = YourMoneyTheme.colors
    val type = YourMoneyTheme.typography

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = type.label, color = colors.textSecondary)
        Spacer(Modifier.weight(1f))
        Text(value, style = type.body, color = colors.textPrimary, textAlign = TextAlign.End)
    }
}

@Composable
private fun Badge(text: String) {
    val colors = YourMoneyTheme.colors
    Text(
        text,
        style = YourMoneyTheme.typography.caption,
        color = colors.textMuted,
        modifier = Modifier
            .background(colors.cardElevated, YourMoneyTheme.shapes.chip)
            .padding(horizontal = YourMoneyTheme.dimens.gapS, vertical = 4.dp),
    )
}
