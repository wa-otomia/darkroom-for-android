package app.darkroom.android.core

import app.darkroom.android.data.catalog.CatalogRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class UndoCountdownTest {
    @Test
    fun fullWhenNothingElapsed() {
        assertEquals(1f, undoRemainingFraction(0, CatalogRepository.UNDO_GRACE_MS), 0f)
        assertEquals(1f, undoRemainingFraction(-10, 5_000L), 0f)
    }

    @Test
    fun halfAtMidpoint() {
        assertEquals(0.5f, undoRemainingFraction(7_500L, CatalogRepository.UNDO_GRACE_MS), 0.0001f)
    }

    @Test
    fun zeroWhenWindowCloses() {
        assertEquals(0f, undoRemainingFraction(CatalogRepository.UNDO_GRACE_MS, CatalogRepository.UNDO_GRACE_MS), 0f)
        assertEquals(0f, undoRemainingFraction(20_000L, CatalogRepository.UNDO_GRACE_MS), 0f)
    }

    @Test
    fun zeroWhenTotalIsNotPositive() {
        assertEquals(0f, undoRemainingFraction(0, 0), 0f)
        assertEquals(0f, undoRemainingFraction(100, -1), 0f)
    }
}
