package io.github.wa_otomia.darkroom.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.wa_otomia.darkroom.R
import io.github.wa_otomia.darkroom.core.formatByteProgress
import io.github.wa_otomia.darkroom.data.progress.ProgressUi
import io.github.wa_otomia.darkroom.data.progress.failedProgressUi
import io.github.wa_otomia.darkroom.data.progress.indeterminateUi
import io.github.wa_otomia.darkroom.data.progress.toUi
import io.github.wa_otomia.darkroom.data.transfer.IncomingTransfer
import io.github.wa_otomia.darkroom.data.transfer.TransferState

/**
 * Wire keys from [io.github.wa_otomia.darkroom.data.progress.JobProgress]. The percent
 * math stays in the progress engine; only the label is localised.
 */
private val GENERATE_PHASES: Map<String, Int> = mapOf(
    "preparing" to R.string.phase_generate_preparing,
    "saving" to R.string.phase_generate_saving,
    "uploading" to R.string.phase_generate_uploading,
    "processing" to R.string.phase_generate_processing,
    "downloading" to R.string.phase_generate_downloading,
    "done" to R.string.phase_generate_done,
    "error" to R.string.phase_generate_error,
)

private val PRINT_PHASES: Map<String, Int> = mapOf(
    "queued" to R.string.job_state_queued,
    "editing" to R.string.phase_print_editing,
    "preparing" to R.string.phase_print_preparing,
    "connecting" to R.string.phase_print_connecting,
    "sending" to R.string.phase_print_sending,
    "uploading" to R.string.phase_print_uploading,
    "printing" to R.string.phase_print_printing,
    "done" to R.string.phase_print_done,
    "error" to R.string.phase_print_error,
)

private val TRANSFER_PHASES: Map<String, Int> = mapOf(
    "reading" to R.string.phase_import_reading,
    "receiving" to R.string.transfer_phase_receiving,
    "transcoding" to R.string.transfer_phase_transcoding,
    "ingesting" to R.string.transfer_phase_ingesting,
    "done" to R.string.transfer_phase_done,
    "failed" to R.string.transfer_phase_failed,
)

private val JOB_STATES: Map<String, Int> = mapOf(
    "downloading" to R.string.job_state_downloading,
    "printing" to R.string.job_state_printing,
    "queued" to R.string.job_state_queued,
    "waiting" to R.string.job_state_waiting,
    "pending" to R.string.job_state_pending,
    "no_paper" to R.string.job_state_no_paper,
    "out_of_paper" to R.string.job_state_out_of_paper,
    "paper_empty" to R.string.job_state_paper_empty,
    "paper_out" to R.string.job_state_out_of_paper,
    "lack_paper" to R.string.job_state_no_paper,
    "load_paper" to R.string.job_state_no_paper,
    "done" to R.string.job_state_done,
    "success" to R.string.job_state_success,
    "finished" to R.string.job_state_finished,
    "error" to R.string.job_state_error,
    "failed" to R.string.job_state_failed,
    "cancelled" to R.string.job_state_cancelled,
    "cancel" to R.string.job_state_cancelled,
    "aborted" to R.string.job_state_aborted,
)

fun generatePhaseRes(phase: String): Int? = GENERATE_PHASES[phase]

fun printPhaseRes(phase: String): Int? = PRINT_PHASES[phase]

fun jobStateRes(state: String): Int? = JOB_STATES[state]

fun transferPhaseRes(phase: String): Int? = TRANSFER_PHASES[phase.lowercase()]

@Composable
fun localizedGeneratePhase(phase: String): String =
    stringResourceOrRaw(GENERATE_PHASES, phase)

@Composable
fun localizedJobState(jobState: String): String =
    stringResourceOrRaw(JOB_STATES, jobState)

/** Printer job label: [jobState] wins while the phase is `printing`. */
@Composable
fun localizedPrintPhase(phase: String, jobState: String? = null): String {
    if (phase == "printing" && !jobState.isNullOrBlank()) {
        return localizedJobState(jobState)
    }
    return stringResourceOrRaw(PRINT_PHASES, phase)
}

@Composable
fun localizedTransferPhase(phase: String): String =
    stringResourceOrRaw(TRANSFER_PHASES, phase.lowercase())

/** Room / enum print-job fields land as different types; compare on this key. */
fun jobFieldKey(value: Any?): String = value?.toString()?.lowercase()?.substringAfterLast('.').orEmpty()

@Composable
fun transferProgressUi(transfer: IncomingTransfer): ProgressUi {
    val phase = localizedTransferPhase(transfer.progress?.phase ?: transfer.phase)
    val bytes = localizedByteProgress(transfer.bytes, transfer.total).ifEmpty { null }
    val snap = transfer.progress
    return when {
        snap != null -> snap.toUi(phase, bytes)
        transfer.state == TransferState.Failed -> failedProgressUi(phase, bytes)
        else -> indeterminateUi(phase, bytes)
    }
}

@Composable
fun localizedByteProgress(loaded: Long?, total: Long?): String = formatByteProgress(
    loaded,
    total,
    megabyteLabel = stringResource(R.string.progress_unit_mb),
    kilobyteLabel = stringResource(R.string.progress_unit_kb),
    byteLabel = stringResource(R.string.progress_unit_b),
)

@Composable
private fun stringResourceOrRaw(map: Map<String, Int>, key: String): String {
    val res = map[key] ?: map[key.lowercase()] ?: return key
    return stringResource(res)
}
