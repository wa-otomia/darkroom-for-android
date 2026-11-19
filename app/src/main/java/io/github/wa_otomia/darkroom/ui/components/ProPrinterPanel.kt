package io.github.wa_otomia.darkroom.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.wa_otomia.darkroom.R
import io.github.wa_otomia.darkroom.data.printer.PrintQueue
import io.github.wa_otomia.darkroom.ui.theme.Amber
import io.github.wa_otomia.darkroom.ui.theme.MonoFont
import io.github.wa_otomia.darkroom.ui.theme.Paper
import io.github.wa_otomia.darkroom.ui.theme.PaperDim
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date

/** Snapshot controls live in the queue, independent of the currently selected photo. */
@Composable
fun ProPrinterPanel(queue: PrintQueue, runningJobId: String?) {
    val ui by queue.printerState.collectAsState()
    val paused by queue.pauseReason.collectAsState()
    var review by remember { mutableStateOf(false) }
    val status = ui.status
    val unknown = stringResource(R.string.pro_unknown)
    // One card for the whole panel; each reading is a label/value row (SYUI020-R05).
    DarkroomCard(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            stringResource(R.string.pro_status_title),
            color = Paper,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        val percent = status?.reportedBatteryPercent?.let {
            NumberFormat.getNumberInstance().apply { maximumFractionDigits = 1 }.format(it) + "%"
        } ?: unknown
        Column {
            StatusLine(stringResource(R.string.pro_battery, percent), percent)
            Text(
                stringResource(R.string.pro_battery_note),
                color = PaperDim,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        val charging = when (status?.charging) {
            true -> stringResource(R.string.pro_charging)
            false -> stringResource(R.string.pro_not_charging)
            null -> unknown
        }
        StatusLine(stringResource(R.string.pro_charge_state, charging), charging)
        val usb = status?.usbConnected?.let { if (it) stringResource(R.string.pro_yes) else stringResource(R.string.pro_no) } ?: unknown
        StatusLine(stringResource(R.string.pro_usb, usb), usb)
        ui.observedAt?.takeIf { status != null }?.let {
            val time = DateFormat.getTimeInstance().format(Date(it))
            StatusLine(stringResource(R.string.pro_observed, time), time)
        }
        if (status?.hasFault == true) {
            val text = when (status.errorCode) {
                -7001 -> R.string.pro_cover
                -7103 -> R.string.pro_paper
                -7104, -7105 -> R.string.pro_paper_jam
                -7110 -> R.string.pro_remove_photo
                -7111, -7112, -7114 -> R.string.pro_reload_paper
                -7201 -> R.string.pro_ribbon_end
                -7208 -> R.string.pro_ribbon_install
                -7204, -7205 -> R.string.pro_ribbon_restart
                -7308, -7309 -> R.string.pro_temperature
                -7310, -7311 -> R.string.pro_battery_low
                else -> R.string.pro_unknown_fault
            }
            NoticeCard(stringResource(text), icon = Icons.Outlined.WarningAmber)
            val code = status.errorCode?.toString() ?: unknown
            StatusLine(stringResource(R.string.pro_fault_code, code), code, mono = true)
            if (runningJobId != null && ui.allowResume) {
                TonalButton(stringResource(R.string.pro_resume),
                    enabled = ui.controlPhase == "waiting_for_user") { queue.resume(runningJobId) }
            }
        }
        when (ui.controlPhase) {
            "canceling" -> Text(stringResource(R.string.pro_canceling), color = Amber)
            "resuming" -> Text(stringResource(R.string.pro_resuming), color = Amber)
            "waiting_for_user" -> Text(stringResource(R.string.pro_waiting), color = Amber)
        }
        ui.error?.let { Text(it, color = Amber, fontSize = 12.sp, lineHeight = 16.sp) }
        if (paused != null) {
            Text(stringResource(R.string.pro_queue_paused), color = Amber, fontSize = 12.sp, lineHeight = 16.sp)
            TonalButton(stringResource(R.string.pro_review), enabled = !ui.busy) { review = true }
        }
        TonalButton(stringResource(R.string.pro_refresh), enabled = !ui.busy && runningJobId == null) {
            queue.refreshPrinterStatus()
        }
    }
    if (review) {
        AlertDialog(
            onDismissRequest = { review = false },
            title = { Text(stringResource(R.string.pro_review)) },
            text = { Text(stringResource(R.string.pro_review_warning)) },
            confirmButton = {
                TextButton(onClick = {
                    review = false
                    queue.refreshPrinterStatus(reviewUncertainOutcome = true)
                }) { Text(stringResource(R.string.pro_review_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { review = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }
}

/**
 * One reading as a label/value row. The existing strings carry label and value in one format
 * ("Reported battery: %1$s"), so the label is the formatted text up to the value with the
 * separator trimmed; if a translation puts the value anywhere but the end, the whole sentence is
 * shown as is. TalkBack always reads the full formatted sentence as one item.
 * PaperDim label 5.16:1 and Paper value 14.70:1 on Surface1.
 */
@Composable
private fun StatusLine(full: String, value: String, mono: Boolean = false) {
    val at = if (value.isEmpty()) -1 else full.lastIndexOf(value)
    val label = if (at > 0 && at + value.length == full.length) {
        full.substring(0, at).trimEnd(' ', '\u00A0', ':', '：')
    } else {
        ""
    }
    if (label.isEmpty()) {
        Text(full, color = PaperDim, fontSize = 13.sp, lineHeight = 18.sp)
        return
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = full },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label, color = PaperDim, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.weight(1f))
        Text(
            value,
            color = Paper,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = if (mono) MonoFont else null,
            textAlign = TextAlign.End,
        )
    }
}
