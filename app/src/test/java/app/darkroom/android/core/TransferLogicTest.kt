package app.darkroom.android.core

import app.darkroom.android.data.transfer.TransferRegistry
import app.darkroom.android.data.transfer.TransferState
import app.darkroom.android.data.transfer.isStaleReceiving
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransferLogicTest {
    @Test
    fun receivingWithoutProgressBeyondThreeMinutesIsStale() {
        val started = 1_000L
        assertFalse(isStaleReceiving(TransferState.Receiving, started, started + 179_000L))
        assertTrue(isStaleReceiving(TransferState.Receiving, started, started + TransferRegistry.STALE_RECEIVING_MS + 1))
        assertFalse(isStaleReceiving(TransferState.Ingesting, started, started + TransferRegistry.STALE_RECEIVING_MS + 1))
        assertFalse(isStaleReceiving(TransferState.Failed, started, started + TransferRegistry.STALE_RECEIVING_MS + 1))
    }
}
