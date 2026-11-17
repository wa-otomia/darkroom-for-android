package io.github.wa_otomia.darkroom.data.imaging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.Executor

class ErrorTrappingExecutorTest {
    private val direct = Executor { it.run() }

    @Test
    fun commandRunsOnDelegate() {
        var delegated = 0
        var ran = false
        var trapped: Throwable? = null
        val executor = ErrorTrappingExecutor({ delegated++; it.run() }) { trapped = it }
        executor.execute { ran = true }
        assertEquals(1, delegated)
        assertTrue(ran)
        assertNull(trapped)
    }

    @Test
    fun errorsAndExceptionsAreTrappedNotRethrown() {
        val thrown = listOf(OutOfMemoryError("oom"), UnsatisfiedLinkError("lib"), IllegalStateException("x"))
        for (t in thrown) {
            var trapped: Throwable? = null
            ErrorTrappingExecutor(direct) { trapped = it }.execute { throw t }
            assertSame(t, trapped)
        }
    }

    @Test
    fun failingHandlerDoesNotEscape() {
        ErrorTrappingExecutor(direct) { error("handler") }.execute { throw OutOfMemoryError() }
    }
}
