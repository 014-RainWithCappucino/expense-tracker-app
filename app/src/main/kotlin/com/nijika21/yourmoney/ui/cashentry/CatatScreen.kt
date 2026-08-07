package com.nijika21.yourmoney.ui.cashentry

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nijika21.yourmoney.domain.model.Jenis
import com.nijika21.yourmoney.ui.components.AmountField
import com.nijika21.yourmoney.ui.components.BottomActionBar
import com.nijika21.yourmoney.ui.components.CardGroup
import com.nijika21.yourmoney.ui.components.Keypad
import com.nijika21.yourmoney.ui.components.PrimaryButton
import com.nijika21.yourmoney.ui.components.RowDivider
import com.nijika21.yourmoney.ui.components.ScreenHeader
import com.nijika21.yourmoney.ui.components.SegmentedControl
import com.nijika21.yourmoney.ui.components.TextAction
import com.nijika21.yourmoney.ui.components.pressable
import com.nijika21.yourmoney.ui.theme.YourMoneyTheme

/**
 * Screen 02, rebuilt. The first cut had the amount at the top, the keypad at the
 * bottom and the whole form in between — so every keypress made the eye travel
 * the length of the screen, and Simpan sat below the fold behind a scroll. On a
 * screen whose only job is *amount, save, done*, that is the wrong shape.
 *
 * What it is now, top to bottom, all of it fixed except the middle:
 *
 * 1. Title and Batal — an exit that is reachable without scrolling.
 * 2. The amount, immediately above everything that changes it.
 * 3. Keluar / Masuk as one segmented track, not two free-floating pills.
 * 4. The details card, collapsed to four quiet rows.
 * 5. The keypad, pinned.
 * 6. Simpan, pinned, always visible.
 *
 * Only the middle scrolls, and only on a short screen, so the two things that
 * matter — the number and the save — can never be pushed out of view.
 *
 * The wallet chips are gone. They were four permanent choices for a decision that
 * is "Tunai" almost every time; the wallet is now one row that opens a picker
 * when it is actually wrong.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CatatScreen(
    state: CatatUiState,
    onDigit: (Char) -> Unit,
    onTripleZero: () -> Unit,
    onBackspace: () -> Unit,
    onCursor: (Int) -> Unit,
    onJenis: (Jenis) -> Unit,
    onKeterangan: (String) -> Unit,
    onCatatan: (String) -> Unit,
    onWallet: (String) -> Unit,
    onSimpan: () -> Unit,
    onBatal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = YourMoneyTheme.colors
    val type = YourMoneyTheme.typography
    val dimens = YourMoneyTheme.dimens

    var pilihDompet by remember { mutableStateOf(false) }
    // Two keyboards must never be on screen at once. When a note is being typed
    // the system IME owns the bottom of the screen, so the keypad steps aside.
    val mengetik = WindowInsets.isImeVisible

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimens.screenPadding),
        ) {
            Spacer(Modifier.height(dimens.gapM))
            ScreenHeader(
                title = "Catat tunai",
                action = { TextAction("Batal", onBatal) },
            )

            AmountField(
                digits = state.nominalText,
                cursor = state.cursor,
                onCursor = onCursor,
                modifier = Modifier.padding(top = dimens.gapM, bottom = dimens.gapM),
            )

            SegmentedControl(
                options = listOf("Keluar", "Masuk"),
                selectedIndex = if (state.jenis == Jenis.MASUK) 1 else 0,
                onSelect = { onJenis(if (it == 1) Jenis.MASUK else Jenis.KELUAR) },
            )

            Spacer(Modifier.height(dimens.gapM))

            CardGroup {
                TapRow(
                    label = "Dompet",
                    value = state.wallets.firstOrNull { it.id == state.walletId }?.nama ?: "—",
                    onClick = { pilihDompet = true },
                )
                RowDivider()
                FieldRow(
                    label = "Keterangan",
                    value = state.keterangan,
                    // A hint, not a default. "Tunai" here just echoed the wallet
                    // name and taught nothing about what the field is for.
                    placeholder = "kopi, parkir, …",
                    onValueChange = onKeterangan,
                )
                RowDivider()
                // The third row §6.8 asks for. Unstructured and optional — the
                // ledger's only "what for" signal, and not a category.
                FieldRow(
                    label = "Catatan",
                    value = state.catatan,
                    placeholder = "opsional",
                    onValueChange = onCatatan,
                )
                RowDivider()
                ReadOnlyRow(label = "Waktu", value = state.waktu)
            }

            Spacer(Modifier.height(dimens.gapM))
        }

        if (!mengetik) {
            Keypad(
                onDigit = onDigit,
                onTripleZero = onTripleZero,
                onBackspace = onBackspace,
                modifier = Modifier.padding(horizontal = dimens.screenPadding),
            )
        }

        BottomActionBar {
            PrimaryButton(
                text = if (state.menyimpan) "Menyimpan…" else "Simpan",
                onClick = onSimpan,
                enabled = state.bisaSimpan,
                modifier = Modifier.weight(1f),
            )
        }
    }

    if (pilihDompet) {
        WalletPicker(
            wallets = state.wallets,
            selectedId = state.walletId,
            onPick = {
                onWallet(it)
                pilihDompet = false
            },
            onDismiss = { pilihDompet = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WalletPicker(
    wallets: List<WalletChoice>,
    selectedId: String?,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = YourMoneyTheme.colors
    val dimens = YourMoneyTheme.dimens

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = YourMoneyTheme.shapes.sheet,
    ) {
        Column(Modifier.padding(horizontal = dimens.screenPadding)) {
            Text(
                "Dompet",
                style = YourMoneyTheme.typography.screenTitle,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(dimens.gapM))
            wallets.forEach { wallet ->
                val selected = wallet.id == selectedId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .pressable { onPick(wallet.id) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        wallet.nama,
                        style = YourMoneyTheme.typography.rowTitle,
                        color = if (selected) colors.accentLime else colors.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    if (selected) {
                        Text(
                            "dipakai",
                            style = YourMoneyTheme.typography.caption,
                            color = colors.accentLime,
                        )
                    }
                }
            }
            Spacer(Modifier.height(dimens.gapXl))
        }
    }
}

/** Label left, current value right, opens something on tap. */
@Composable
private fun TapRow(label: String, value: String, onClick: () -> Unit) {
    val colors = YourMoneyTheme.colors
    val type = YourMoneyTheme.typography

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            // Always the first row in Catat's card group, so only the top
            // corners need to match — never the last, never the only row.
            // No shrink either: fixed dividers sit right below it.
            .pressable(shape = YourMoneyTheme.shapes.cardTop, scaleDown = 1f, onClick = onClick)
            .padding(horizontal = YourMoneyTheme.dimens.cardPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = type.label, color = colors.textSecondary)
        Spacer(Modifier.weight(1f))
        Text(value, style = type.body, color = colors.textPrimary)
        Spacer(Modifier.width(YourMoneyTheme.dimens.gapS))
        Text("›", style = type.body, color = colors.textSecondary)
    }
}

/**
 * An empty value renders as faint placeholder text rather than a filled value, so
 * an unused note never looks like data (§6.8).
 */
@Composable
private fun FieldRow(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
) {
    val colors = YourMoneyTheme.colors
    val type = YourMoneyTheme.typography
    // Same rule as the detail sheet's note field: the hint is about an empty
    // field, not empty text, so it clears on tap rather than on first keystroke.
    var focused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = YourMoneyTheme.dimens.cardPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = type.label, color = colors.textSecondary)
        Spacer(Modifier.weight(1f))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = type.body.copy(color = colors.textPrimary, textAlign = TextAlign.End),
            cursorBrush = SolidColor(colors.accentLime),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            decorationBox = { inner ->
                if (value.isEmpty() && !focused) {
                    Text(
                        placeholder,
                        style = type.body,
                        color = colors.textMuted,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                inner()
            },
            modifier = Modifier
                .weight(2f)
                .onFocusChanged { focused = it.isFocused },
        )
    }
}

/**
 * The time being recorded, shown but not editable. It states what will actually be
 * saved rather than implying an editing surface that does not exist — back-dating
 * belongs with corrections, not with entry.
 */
@Composable
private fun ReadOnlyRow(label: String, value: String) {
    val colors = YourMoneyTheme.colors
    val type = YourMoneyTheme.typography

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = YourMoneyTheme.dimens.cardPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = type.label, color = colors.textSecondary)
        Spacer(Modifier.weight(1f))
        Text(value, style = type.body, color = colors.textSecondary)
    }
}
