package com.orbita.personalagent

import com.orbita.personalagent.ai.AiRequest
import com.orbita.personalagent.ai.ConnectionCheckResult
import com.orbita.personalagent.ai.DemoAiProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoAiProviderTest {

    private val provider = DemoAiProvider()

    @Test
    fun checkConnectionReturnsOfflineReady() = runTest {
        val result = provider.checkConnection()
        assertTrue(result is ConnectionCheckResult.Ready)
        assertTrue((result as ConnectionCheckResult.Ready).isDemoMode)
    }

    @Test
    fun parsesTaskAndAlarmAndSmsTools() = runTest {
        val taskResp = provider.planAndRespond(
            AiRequest(1L, "Create a task to buy coffee", emptyList(), emptyList(), true)
        )
        assertEquals("create_task", taskResp.proposedToolCalls.first().toolId)

        val alarmResp = provider.planAndRespond(
            AiRequest(1L, "Set alarm for 6:45 am", emptyList(), emptyList(), true)
        )
        assertEquals("open_clock_alarm", alarmResp.proposedToolCalls.first().toolId)
        assertEquals("6", alarmResp.proposedToolCalls.first().arguments["hour"])
        assertEquals("45", alarmResp.proposedToolCalls.first().arguments["minute"])

        val smsResp = provider.planAndRespond(
            AiRequest(1L, "Prepare SMS to +15551234567 saying On my way", emptyList(), emptyList(), true)
        )
        assertEquals("prepare_sms", smsResp.proposedToolCalls.first().toolId)
    }

    @Test
    fun proposesMemoryCandidateOnlyWhenSuggestionsEnabled() = runTest {
        val enabledResp = provider.planAndRespond(
            AiRequest(1L, "Remember that I prefer dark navy UI", emptyList(), emptyList(), true)
        )
        assertEquals(1, enabledResp.memoryCandidates.size)
        assertTrue(enabledResp.proposedToolCalls.isEmpty())

        val disabledResp = provider.planAndRespond(
            AiRequest(1L, "Remember that I prefer dark navy UI", emptyList(), emptyList(), false)
        )
        assertTrue(disabledResp.memoryCandidates.isEmpty())
    }
}
