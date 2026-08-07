package com.nijika21.yourmoney.ui.cashentry

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.nijika21.yourmoney.ui.theme.YourMoneyColors
import com.nijika21.yourmoney.ui.theme.YourMoneyTheme
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * `DatePicker`/`TimePicker` read `MaterialTheme.colorScheme`, which the app
 * otherwise never touches (§7 — everything else reads [YourMoneyTheme]
 * instead). These two dialogs are the one place that boundary gets crossed,
 * so the crossing is scoped as tight as it can be: a scheme built from the
 * app's own tokens, applied only around the dialog's own content.
 */
private fun pickerScheme(colors: YourMoneyColors) = darkColorScheme(
    primary = colors.accentLime,
    onPrimary = colors.accentLimeInk,
    primaryContainer = colors.accentLime,
    onPrimaryContainer = colors.accentLimeInk,
    secondaryContainer = colors.accentLimeDim,
    onSecondaryContainer = colors.textPrimary,
    surface = colors.card,
    onSurface = colors.textPrimary,
    surfaceVariant = colors.cardElevated,
    onSurfaceVariant = colors.textSecondary,
    background = colors.card,
    onBackground = colors.textPrimary,
    outline = colors.border,
)

/** Epoch-day in, `LocalDate` out — the caller never sees the picker's UTC millis. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TanggalPickerDialog(
    initialEpochDay: Long,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = YourMoneyTheme.colors
    // The picker works in UTC start-of-day millis regardless of device zone
    // (M3's own contract) — converting through UTC on both ends is what keeps
    // the selected day from drifting by one near midnight.
    val initialMillis = LocalDate.ofEpochDay(initialEpochDay)
        .atStartOfDay(ZoneOffset.UTC)
        .toInstant()
        .toEpochMilli()
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    MaterialTheme(colorScheme = pickerScheme(colors)) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onConfirm(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    onDismiss()
                }) {
                    Text("Pilih", color = colors.accentLime)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Batal", color = colors.textSecondary)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = colors.card),
        ) {
            DatePicker(state = state, showModeToggle = false)
        }
    }
}

/**
 * `null` in or out means "otomatis" (§6 item 10 / handoff open thread #4) —
 * the dedicated button is separate from Batal, since dismissing without a
 * choice should never silently pick a time.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaktuPickerDialog(
    initialMenit: Int?,
    onConfirm: (Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = YourMoneyTheme.colors
    val fallback = LocalTime.now()
    val startMenit = initialMenit ?: (fallback.hour * 60 + fallback.minute)
    val state = rememberTimePickerState(
        initialHour = startMenit / 60,
        initialMinute = startMenit % 60,
        is24Hour = true,
    )

    MaterialTheme(colorScheme = pickerScheme(colors)) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = colors.card,
            title = { Text("Waktu transaksi", color = colors.textPrimary) },
            text = {
                Column(
                    modifier = Modifier,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TimePicker(state = state)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onConfirm(state.hour * 60 + state.minute)
                    onDismiss()
                }) {
                    Text("Pilih", color = colors.accentLime)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    onConfirm(null)
                    onDismiss()
                }) {
                    Text("Otomatis", color = colors.textSecondary)
                }
            },
        )
    }
}
