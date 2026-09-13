package io.github.wa_otomia.darkroom.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.wa_otomia.darkroom.R
import io.github.wa_otomia.darkroom.data.printer.PrintQueue
import io.github.wa_otomia.darkroom.ui.theme.Amber
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.pro_status_title), color = Paper)
        val percent = status?.reportedBatteryPercent?.let {
            NumberFormat.getNumberInstance().apply { maximumFractionDigits = 1 }.format(it) + "%"
        } ?: unknown
        Text(stringResource(R.string.pro_battery, percent), color = PaperDim, fontSize = 12.sp)
        Text(stringResource(R.string.pro_battery_note), color = PaperDim, fontSize = 11.sp)
        val charging = when (status?.charging) {
            true -> stringResource(R.string.pro_charging)
            false -> stringResource(R.string.pro_not_charging)
            null -> unknown
        }
        Text(stringResource(R.string.pro_charge_state, charging), color = PaperDim, fontSize = 12.sp)
        val usb = status?.usbConnected?.let { if (it) stringResource(R.string.pro_yes) else stringResource(R.string.pro_no) } ?: unknown
        Text(stringResource(R.string.pro_usb, usb), color = PaperDim, fontSize = 12.sp)
        ui.observedAt?.takeIf { status != null }?.let {
            Text(stringResource(R.string.pro_observed, DateFormat.getTimeInstance().format(Date(it))),
                color = PaperDim, fontSize = 11.sp)
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
            Text(stringResource(text), color = Amber, fontSize = 13.sp)
            Text(stringResource(R.string.pro_fault_code, status.errorCode?.toString() ?: unknown), color = PaperDim, fontSize = 11.sp)
            if (runningJobId != null && ui.allowResume) {
                GhostButton(stringResource(R.string.pro_resume),
                    enabled = ui.controlPhase == "waiting_for_user") { queue.resume(runningJobId) }
            }
        }
        when (ui.controlPhase) {
            "canceling" -> Text(stringResource(R.string.pro_canceling), color = Amber)
            "resuming" -> Text(stringResource(R.string.pro_resuming), color = Amber)
            "waiting_for_user" -> Text(stringResource(R.string.pro_waiting), color = Amber)
        }
        ui.error?.let { Text(it, color = Amber, fontSize = 12.sp) }
        if (paused != null) {
            Text(stringResource(R.string.pro_queue_paused), color = Amber, fontSize = 12.sp)
            GhostButton(stringResource(R.string.pro_review), enabled = !ui.busy) { review = true }
        }
        GhostButton(stringResource(R.string.pro_refresh), enabled = !ui.busy && runningJobId == null) {
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
