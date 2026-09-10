package io.github.wa_otomia.darkroom.data.automation

import io.github.wa_otomia.darkroom.core.SKIP_WATERMARK_NO_PRINT
import io.github.wa_otomia.darkroom.core.isConfigured
import io.github.wa_otomia.darkroom.core.planAutomation
import io.github.wa_otomia.darkroom.data.jobs.AiJobs
import io.github.wa_otomia.darkroom.data.printer.PrintQueue
import io.github.wa_otomia.darkroom.data.settings.ActivityLog
import io.github.wa_otomia.darkroom.data.settings.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Automation @Inject constructor(
    private val settings: SettingsRepository,
    private val printQueue: PrintQueue,
    private val aiJobs: AiJobs,
    private val activityLog: ActivityLog,
) {
    fun onIngested(photoId: String, origin: String) {
        val app = settings.readSettings()
        val presetId = settings.readPresets().lastSelectedId
        val aiConfigured = settings.aiConfigured()
        val watermarkConfigured = settings.readWatermark().isConfigured()
        val plan = planAutomation(
            autoEdit = app.autoEdit,
            autoWatermark = app.autoWatermark,
            autoPrint = app.autoPrint,
            presetId = presetId,
            aiConfigured = aiConfigured,
            watermarkConfigured = watermarkConfigured,
            printerBound = app.printerMac.isNotBlank(),
        )
        for (reason in plan.skipped) {
            activityLog.record("automation", "$origin skipped $reason")
        }
        when {
            plan.edit && plan.print -> printQueue.enqueue(
                photoId = photoId,
                source = "original",
                presetId = presetId,
                origin = PrintQueue.ORIGIN_AUTO,
                watermark = plan.watermark,
            )
            plan.edit -> {
                if (app.autoWatermark) {
                    activityLog.record("automation", "$origin skipped $SKIP_WATERMARK_NO_PRINT")
                }
                aiJobs.enqueueEdit(photoId, "original", presetId, "")
            }
            plan.print -> printQueue.enqueue(
                photoId = photoId,
                source = "original",
                presetId = null,
                origin = PrintQueue.ORIGIN_AUTO,
                watermark = plan.watermark,
            )
        }
    }
}
