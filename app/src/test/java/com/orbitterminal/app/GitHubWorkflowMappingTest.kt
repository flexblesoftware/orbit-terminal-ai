package com.orbitterminal.app

import com.orbitterminal.app.data.github.WorkflowRun
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class GitHubWorkflowMappingTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testWorkflowRunDeserialization() {
        val sampleJson = """
            {
                "id": 123456789,
                "status": "completed",
                "conclusion": "success",
                "html_url": "https://github.com/flexblesoftware/orbit-terminal-ai/actions/runs/123456789",
                "created_at": "2026-11-24T08:00:00Z"
            }
        """.trimIndent()

        val run = json.decodeFromString<WorkflowRun>(sampleJson)
        assertEquals(123456789L, run.id)
        assertEquals("completed", run.status)
        assertEquals("success", run.conclusion)
    }
}
