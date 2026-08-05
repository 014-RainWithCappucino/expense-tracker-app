package com.nijika21.yourmoney.ui.cashentry

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import com.nijika21.yourmoney.domain.model.Jenis
import com.nijika21.yourmoney.ui.components.Keypad
import com.nijika21.yourmoney.ui.components.PrimaryButton
import com.nijika21.yourmoney.ui.components.SecondaryButton
import com.nijika21.yourmoney.ui.components.SectionHeader
import com.nijika21.yourmoney.ui.theme.YourMoneyTheme

/**
 * Screen 02. Cash has no notification, so this is the only way it enters the
 * ledger — which makes speed the whole design goal: amount, save, done. Every
 * other field is optional and can stay untouched.
 */
@Composable
fun CatatScreen(
    state: CatatUiState,
    onDigit: (Char) -> Unit,
    onTripleZero: () -> Unit,
    onBackspace: () -> Unit,
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            // Without this the keyboard covers the note field on some OEM skins
            // — the cost §6.8 says to budget for rather than discover.
            .imePadding()
            .padding(horizontal = dimens.screenPadding),
        verticalArrangement = Arrangement.spacedBy(dimens.gapS),
    ) {
        Spacer(Modifier.height(dimens.gapL))
        Text("Catat tunai", style = type.screenTitle, color = colors.textPrimary)

        Text(
            state.nominalDisplay,
            style = type.displayMoney,
            color = if (state.nominalText.isEmpty()) colors.textMuted else colors.textPrimary,
            modifier = Modifier.padding(vertical = dimens.gapM),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(dimens.gapS)) {
            JenisChip("Keluar", state.jenis == Jenis.KELUAR) { onJenis(Jenis.KELUAR) }
            JenisChip("Masuk", state.jenis == Jenis.MASUK) { onJenis(Jenis.MASUK) }
        }

        SectionHeader("Dompet")
        Row(
            horizontalArrangement = Arrangement.spacedBy(dimens.gapS),
            modifier = Modifier.fillMaxWidth(),
        ) {
            for (wallet in state.wallets) {
                JenisChip(
                    label = wallet.nama,
                    selected = wallet.id == state.walletId,
                    modifier = Modifier.weight(1f),
                ) { onWallet(wallet.id) }
            }
        }

        SectionHeader("Rincian")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.card, YourMoneyTheme.shapes.card)
                .border(dimens.hairline, colors.border, YourMoneyTheme.shapes.card)
                .padding(horizontal = dimens.cardPadding),
        ) {
            FieldRow(
                label = "Keterangan",
                value = state.keterangan,
                placeholder = "Tunai",
                onValueChange = onKeterangan,
            )
            Divider()
            // The third row (§6.8). A note is unstructured and optional — it is
            // the ledger's only "what for" signal, and deliberately not a category.
            FieldRow(
                label = "Catatan",
                value = state.catatan,
                placeholder = "opsional",
                onValueChange = onCatatan,
            )
            Divider()
            ReadOnlyRow(label = "Waktu", value = state.waktu)
        }

        Spacer(Modifier.height(dimens.gapM))
        Keypad(onDigit = onDigit, onTripleZero = onTripleZero, onBackspace = onBackspace)

        Spacer(Modifier.height(dimens.gapM))
        PrimaryButton(
            text = if (state.menyimpan) "Menyimpan…" else "Simpan",
            onClick = onSimpan,
            enabled = state.bisaSimpan,
        )
        SecondaryButton(text = "Batal", onClick = onBatal)
        Spacer(Modifier.height(dimens.gapXl))
    }
}

@Composable
private fun JenisChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = YourMoneyTheme.colors
    val shape = YourMoneyTheme.shapes.chip

    Text(
        text = label,
        style = YourMoneyTheme.typography.label,
        color = if (selected) colors.accentLimeInk else colors.textSecondary,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = modifier
            .height(YourMoneyTheme.dimens.touchTarget)
            .background(if (selected) colors.accentLime else colors.card, shape)
            .border(
                YourMoneyTheme.dimens.hairline,
                if (selected) colors.accentLime else colors.border,
                shape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = YourMoneyTheme.dimens.gapS)
            .wrapContentHeight(Alignment.CenterVertically),
    )
}

/**
 * Label left, value right, same pattern for every row. An empty value renders as
 * faint placeholder text rather than a filled value, so an unused note never
 * looks like data (§6.8).
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(YourMoneyTheme.dimens.touchTarget),
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
                if (value.isEmpty()) {
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = colors.textMuted)) { append(placeholder) }
                        },
                        style = type.body,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                inner()
            },
            modifier = Modifier.weight(2f),
        )
    }
}

/**
 * The time being recorded, shown but not editable. It states what will actually
 * be saved rather than implying an editing surface that does not exist yet —
 * back-dating belongs with corrections, not with entry.
 */
@Composable
private fun ReadOnlyRow(label: String, value: String) {
    val colors = YourMoneyTheme.colors
    val type = YourMoneyTheme.typography

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(YourMoneyTheme.dimens.touchTarget),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = type.label, color = colors.textSecondary)
        Spacer(Modifier.weight(1f))
        Text(value, style = type.body, color = colors.textSecondary)
    }
}

@Composable
private fun Divider() {
    Spacer(
        Modifier
            .fillMaxWidth()
            .height(YourMoneyTheme.dimens.hairline)
            .background(YourMoneyTheme.colors.border),
    )
}
