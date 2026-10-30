package io.github.wa_otomia.darkroom.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ForegroundServicePauseTest {
    @Test
    fun mergeKeepsFirstPauseTimeAndOrsListening() {
        val first = mergeFgsPause(null, now = 1_000L, ftpWasListening = false)
        assertEquals(FgsPause(pausedAt = 1_000L, ftpWasListening = false), first)

        val repeated = mergeFgsPause(first, now = 2_000L, ftpWasListening = false)
        assertEquals(first, repeated)

        val ftpTimedOutLater = mergeFgsPause(repeated, now = 3_000L, ftpWasListening = true)
        assertEquals(FgsPause(pausedAt = 1_000L, ftpWasListening = true), ftpTimedOutLater)

        val jobsWriteFalse = mergeFgsPause(ftpTimedOutLater, now = 4_000L, ftpWasListening = false)
        assertEquals(ftpTimedOutLater, jobsWriteFalse)

        assertEquals(
            FgsPause(pausedAt = 50L, ftpWasListening = true),
            mergeFgsPause(null, now = 50L, ftpWasListening = true),
        )
    }

    @Test
    fun timeoutWhileFtpListeningStopsThenRecordsAndNotifies() {
        val plan = fgsTimeoutPlan(FgsService.Ftp, ftpListening = true)
        assertEquals(
            listOf(
                FgsStopStep.StopListening,
                FgsStopStep.RemoveForeground,
                FgsStopStep.StopSelf,
                FgsStopStep.RecordPause,
                FgsStopStep.NotifyFtpPaused,
            ),
            plan.steps,
        )
        assertTrue(plan.ftpWasListening)
        assertTrue(plan.steps.indexOf(FgsStopStep.StopSelf) < plan.steps.indexOf(FgsStopStep.RecordPause))
    }

    @Test
    fun timeoutWhileFtpNotListeningDoesNotNotify() {
        val plan = fgsTimeoutPlan(FgsService.Ftp, ftpListening = false)
        assertEquals(
            listOf(
                FgsStopStep.StopListening,
                FgsStopStep.RemoveForeground,
                FgsStopStep.StopSelf,
                FgsStopStep.RecordPause,
            ),
            plan.steps,
        )
        assertFalse(plan.ftpWasListening)
        assertFalse(plan.steps.contains(FgsStopStep.NotifyFtpPaused))
    }

    @Test
    fun timeoutOfKeepAliveAndJobsDoesNotTouchFtpListening() {
        val expected = listOf(
            FgsStopStep.RemoveForeground,
            FgsStopStep.StopSelf,
            FgsStopStep.RecordPause,
        )
        for (service in listOf(FgsService.KeepAlive, FgsService.Jobs)) {
            val ignoredListening = fgsTimeoutPlan(service, ftpListening = true)
            val notListening = fgsTimeoutPlan(service)
            assertEquals(service.name, expected, ignoredListening.steps)
            assertEquals(service.name, expected, notListening.steps)
            assertFalse(service.name, ignoredListening.ftpWasListening)
            assertFalse(service.name, notListening.ftpWasListening)
        }
    }

    @Test
    fun rejectedPlanRecordsPauseAndNotifiesOnlyFtp() {
        val ftpOutside = fgsRejectedPlan(FgsService.Ftp, inService = false)
        assertEquals(
            listOf(FgsStopStep.RecordPause, FgsStopStep.NotifyFtpPaused),
            ftpOutside.steps,
        )
        assertTrue(ftpOutside.ftpWasListening)

        val ftpInside = fgsRejectedPlan(FgsService.Ftp, inService = true)
        assertEquals(
            listOf(FgsStopStep.RecordPause, FgsStopStep.NotifyFtpPaused, FgsStopStep.StopSelf),
            ftpInside.steps,
        )
        assertTrue(ftpInside.ftpWasListening)

        for (service in listOf(FgsService.KeepAlive, FgsService.Jobs)) {
            val outside = fgsRejectedPlan(service, inService = false)
            assertEquals(service.name, listOf(FgsStopStep.RecordPause), outside.steps)
            assertFalse(service.name, outside.ftpWasListening)

            val inside = fgsRejectedPlan(service, inService = true)
            assertEquals(
                service.name,
                listOf(FgsStopStep.RecordPause, FgsStopStep.StopSelf),
                inside.steps,
            )
            assertFalse(service.name, inside.ftpWasListening)
        }
    }

    @Test
    fun pausedBlocksAutomaticStartsAndAllowsVisibleOrUserStarts() {
        val whilePaused = mapOf(
            FgsStartOrigin.ProcessAutoStart to false,
            FgsStartOrigin.StickyRestart to false,
            FgsStartOrigin.JobsBusy to false,
            FgsStartOrigin.VisibleResume to true,
            FgsStartOrigin.UserAction to true,
        )
        for ((origin, allowed) in whilePaused) {
            assertEquals(origin.name, allowed, fgsStartAllowed(origin, paused = true))
        }
        for (origin in FgsStartOrigin.entries) {
            assertTrue(origin.name, fgsStartAllowed(origin, paused = false))
        }
    }

    @Test
    fun belowApi31DoesNotEvaluateStartNotAllowedPredicate() {
        assertEquals(31, FGS_START_NOT_ALLOWED_SDK)
        for (sdk in intArrayOf(29, 30)) {
            val rejected = isFgsStartNotAllowed(sdk) {
                fail("API $sdk evaluated the start-not-allowed predicate")
                true
            }
            assertFalse(rejected)
        }
    }

    @Test
    fun api31EvaluatesStartNotAllowedPredicate() {
        var evaluated = false
        assertFalse(
            isFgsStartNotAllowed(FGS_START_NOT_ALLOWED_SDK) {
                evaluated = true
                false
            },
        )
        assertTrue(evaluated)
        assertTrue(isFgsStartNotAllowed(31) { true })
        assertFalse(isFgsStartNotAllowed(35) { false })
    }

    @Test
    fun autoStartSkipChecksLocalNetworkBeforeNotifications() {
        assertEquals(
            FtpAutoStartSkip.LocalNetworkPermission,
            ftpAutoStartSkip(localNetworkGranted = false, notificationsGranted = false),
        )
        assertEquals(
            FtpAutoStartSkip.LocalNetworkPermission,
            ftpAutoStartSkip(localNetworkGranted = false, notificationsGranted = true),
        )
        assertEquals(
            FtpAutoStartSkip.NotificationPermission,
            ftpAutoStartSkip(localNetworkGranted = true, notificationsGranted = false),
        )
        assertNull(ftpAutoStartSkip(localNetworkGranted = true, notificationsGranted = true))
    }

    @Test
    fun resumeWithPermissionsRestartsListeningFtpWithoutCancellingNotice() {
        val plan = fgsResumePlan(
            pause = FgsPause(pausedAt = 1L, ftpWasListening = true),
            keepAliveEnabled = false,
            ftpSkip = null,
        )
        assertTrue(plan.clearPause)
        assertTrue(plan.startFtp)
        assertNull(plan.ftpSkip)
        assertFalse(plan.cancelFtpNotice)
        assertFalse(plan.startKeepAlive)
        assertTrue(plan.reevaluateJobs)
    }

    @Test
    fun resumeSkipsFtpAndCancelsNoticeWhenPermissionMissing() {
        val pause = FgsPause(pausedAt = 1L, ftpWasListening = true)
        for (skip in listOf(FtpAutoStartSkip.LocalNetworkPermission, FtpAutoStartSkip.NotificationPermission)) {
            val plan = fgsResumePlan(pause, keepAliveEnabled = false, ftpSkip = skip)
            assertEquals(skip.name, skip, plan.ftpSkip)
            assertFalse(skip.name, plan.startFtp)
            assertTrue(skip.name, plan.cancelFtpNotice)
            assertTrue(skip.name, plan.clearPause)
            assertTrue(skip.name, plan.reevaluateJobs)
            assertFalse(skip.name, plan.startKeepAlive)
        }
    }

    @Test
    fun resumeDoesNotStartFtpThatWasNotListening() {
        val plan = fgsResumePlan(
            pause = FgsPause(pausedAt = 1L, ftpWasListening = false),
            keepAliveEnabled = false,
            ftpSkip = FtpAutoStartSkip.LocalNetworkPermission,
        )
        assertTrue(plan.clearPause)
        assertFalse(plan.startFtp)
        assertNull(plan.ftpSkip)
        assertTrue(plan.cancelFtpNotice)
        assertFalse(plan.startKeepAlive)
        assertTrue(plan.reevaluateJobs)
    }

    @Test
    fun resumeStartsKeepAliveOnlyWhenSwitchIsOn() {
        val pause = FgsPause(pausedAt = 8L, ftpWasListening = false)
        val enabled = fgsResumePlan(pause, keepAliveEnabled = true, ftpSkip = null)
        assertTrue(enabled.startKeepAlive)
        assertFalse(enabled.startFtp)
        assertTrue(enabled.clearPause)

        val disabled = fgsResumePlan(pause, keepAliveEnabled = false, ftpSkip = null)
        assertFalse(disabled.startKeepAlive)
    }

    @Test
    fun resumeDoesNothingWithoutPause() {
        val plan = fgsResumePlan(
            pause = null,
            keepAliveEnabled = true,
            ftpSkip = FtpAutoStartSkip.NotificationPermission,
        )
        assertFalse(plan.clearPause)
        assertFalse(plan.startFtp)
        assertNull(plan.ftpSkip)
        assertFalse(plan.cancelFtpNotice)
        assertFalse(plan.startKeepAlive)
        assertFalse(plan.reevaluateJobs)
    }
}
