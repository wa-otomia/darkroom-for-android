package io.github.wa_otomia.darkroom.ui

import io.github.wa_otomia.darkroom.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JobPhasesTest {
    @Test
    fun knownWireKeysMap() {
        assertEquals(R.string.job_state_queued, printPhaseRes("queued"))
        assertEquals(R.string.phase_print_connecting, printPhaseRes("connecting"))
        assertEquals(R.string.phase_print_uploading, printPhaseRes("uploading"))
        assertEquals(R.string.phase_generate_uploading, generatePhaseRes("uploading"))
        assertEquals(R.string.phase_generate_preparing, generatePhaseRes("preparing"))
        assertEquals(R.string.phase_generate_saving, generatePhaseRes("saving"))
        assertEquals(R.string.job_state_aborted, jobStateRes("aborted"))
        assertEquals(R.string.job_state_no_paper, jobStateRes("no_paper"))
        assertEquals(R.string.transfer_phase_done, transferPhaseRes("done"))
        assertEquals(R.string.job_state_cancelled, jobStateRes("cancel"))
        assertEquals(R.string.job_state_out_of_paper, jobStateRes("paper_out"))
    }

    @Test
    fun unknownKeysAreNull() {
        assertNull(printPhaseRes("not-a-phase"))
        assertNull(generatePhaseRes("mystery"))
        assertNull(jobStateRes("??"))
    }
}
