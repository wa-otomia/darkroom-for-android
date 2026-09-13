package io.github.wa_otomia.darkroom.data.printer

import io.github.wa_otomia.darkroom.core.ProPrinterStatus

/** A snapshot, not a claim of continuous connection. Refreshed explicitly or by the active job. */
data class PrinterUiState(
    val status: ProPrinterStatus? = null,
    val observedAt: Long? = null,
    val busy: Boolean = false,
    val controlPhase: String? = null,
    val error: String? = null,
    val allowResume: Boolean = false,
)
