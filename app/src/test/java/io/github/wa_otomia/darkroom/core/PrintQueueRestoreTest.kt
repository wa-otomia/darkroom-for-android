package io.github.wa_otomia.darkroom.core

import org.junit.Assert.assertEquals
import org.junit.Test

class PrintQueueRestoreTest {
    @Test
    fun requeuesWhenPhaseIsMissingOrBeforeSend() {
        assertEquals(PrintRestoreAction.Requeue, restoreRunningPrintJob(null))
        assertEquals(PrintRestoreAction.Requeue, restoreRunningPrintJob(""))
        assertEquals(PrintRestoreAction.Requeue, restoreRunningPrintJob("  "))
        assertEquals(PrintRestoreAction.Requeue, restoreRunningPrintJob("editing"))
        assertEquals(PrintRestoreAction.Requeue, restoreRunningPrintJob("preparing"))
        assertEquals(PrintRestoreAction.Requeue, restoreRunningPrintJob("connecting"))
        assertEquals(PrintRestoreAction.Requeue, restoreRunningPrintJob("Preparing"))
    }

    @Test
    fun failsWhenPhaseHasReachedThePrinter() {
        assertEquals(PrintRestoreAction.FailInterrupted, restoreRunningPrintJob("sending"))
        assertEquals(PrintRestoreAction.FailInterrupted, restoreRunningPrintJob("uploading"))
        assertEquals(PrintRestoreAction.FailInterrupted, restoreRunningPrintJob("printing"))
        assertEquals(PrintRestoreAction.FailInterrupted, restoreRunningPrintJob("done"))
        assertEquals(PrintRestoreAction.FailInterrupted, restoreRunningPrintJob("error"))
        assertEquals(PrintRestoreAction.FailInterrupted, restoreRunningPrintJob(" PRINTING "))
    }

    @Test
    fun cancelPathsPersistCancelledWithNoError() {
        val fromCancel = mapPrintWorkerFailure(cancellation = true, cancelRequested = false, message = "Job was cancelled")
        assertEquals("cancelled", fromCancel.state)
        assertEquals(null, fromCancel.error)
        val fromFlag = mapPrintWorkerFailure(cancellation = false, cancelRequested = true, message = "printer rejected")
        assertEquals("cancelled", fromFlag.state)
        assertEquals(null, fromFlag.error)
        val fromGuard = mapPrintWorkerFailure(cancellation = false, cancelRequested = false, message = "已取消")
        assertEquals("cancelled", fromGuard.state)
        assertEquals(null, fromGuard.error)
    }

    @Test
    fun otherExceptionsPersistFailedWithMessage() {
        val failed = mapPrintWorkerFailure(cancellation = false, cancelRequested = false, message = "打印机未绑")
        assertEquals("failed", failed.state)
        assertEquals("打印机未绑", failed.error)
    }

    @Test
    fun cancelFlagWinsOverLaterBluetoothError() {
        val raced = mapPrintWorkerFailure(
            cancellation = false,
            cancelRequested = true,
            message = "无法连接蓝牙设备",
        )
        assertEquals("cancelled", raced.state)
        assertEquals(null, raced.error)
    }

    @Test
    fun cancelFlagsArePerJob() {
        val flags = PrintJobCancelFlags()
        flags.request("job-a")
        assertEquals(true, flags.isRequested("job-a"))
        assertEquals(false, flags.isRequested("job-b"))
        flags.clear("job-b")
        assertEquals(true, flags.isRequested("job-a"))
        flags.clear("job-a")
        assertEquals(false, flags.isRequested("job-a"))
        flags.request("job-b")
        assertEquals(true, flags.isRequested("job-b"))
        assertEquals(false, flags.isRequested("job-a"))
    }

    @Test
    fun cancelledPrintMessagesAreRecognized() {
        assertEquals(true, isCancelledPrintMessage(null))
        assertEquals(true, isCancelledPrintMessage(""))
        assertEquals(true, isCancelledPrintMessage("已取消"))
        assertEquals(true, isCancelledPrintMessage("cancelled"))
        assertEquals(false, isCancelledPrintMessage("无法连接蓝牙设备"))
    }
    @Test
    fun remoteCancellationRequiresConfirmationNotAnIntent() {
        assertEquals("failed", mapPrintWorkerFailure(false, true, "socket error", remoteJobCreated = true).state)
        assertEquals("failed", mapPrintWorkerFailure(true, true, "worker stopped", remoteJobCreated = true).state)
        assertEquals("cancelled", mapPrintWorkerFailure(false, true, null, remoteJobCreated = true, cancelConfirmed = true).state)
    }
    @Test
    fun recoveryAndUncertainPhasesAreNeverAutoReprinted() {
        for (phase in listOf("waiting_for_user", "resuming", "canceling", "outcome_unknown")) {
            assertEquals(PrintRestoreAction.FailInterrupted, restoreRunningPrintJob(phase))
        }
        assertEquals(PrintRestoreAction.FailInterrupted, restoreRunningPrintJob(null, remoteJobCreated = true))
    }
}
