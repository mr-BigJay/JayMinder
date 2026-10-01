package com.offlinejournal.service.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArvanCloudJsonParsingTest {

    @Test
    fun parseModelsResponse_extractsIds() {
        val json = """
            {"data":[{"id":"gpt-test","owned_by":"org"},{"id":"other-model"}]}
        """.trimIndent()
        val models = ArvanCloudJsonParsing.parseModelsResponse(json)
        assertEquals(2, models.size)
        assertEquals("gpt-test", models[0].id)
    }

    @Test
    fun parseChatCompletionResponse_extractsContent() {
        val json = """
            {"choices":[{"message":{"role":"assistant","content":"سلام دنیا"}}]}
        """.trimIndent()
        assertEquals("سلام دنیا", ArvanCloudJsonParsing.parseChatCompletionResponse(json))
    }

    @Test
    fun parseErrorMessage_supportsOpenAiAndFlatError() {
        assertEquals(
            "bad key",
            ArvanCloudJsonParsing.parseErrorMessage("""{"error":{"message":"bad key"}}""")
        )
        assertEquals(
            "Unauthorized",
            ArvanCloudJsonParsing.parseErrorMessage("""{"error":"Unauthorized","status":401}""")
        )
    }

    @Test
    fun parseBackendCleanupResponse_readsTextField() {
        val json = """{"text":"متن تمیز"}"""
        assertEquals("متن تمیز", ArvanCloudJsonParsing.parseBackendCleanupResponse(json))
    }
}
