package com.orbitterminal.app

import com.orbitterminal.app.data.provider.model.ChatMessage
import com.orbitterminal.app.data.provider.model.PROVIDERS
import com.orbitterminal.app.data.provider.model.getProvider
import org.junit.Assert.*
import org.junit.Test

class ProviderRequestTest {

    @Test
    fun testProviderRegistration() {
        assertEquals(4, PROVIDERS.size)
        assertNotNull(getProvider("gemini"))
        assertNotNull(getProvider("groq"))
        assertNotNull(getProvider("openrouter"))
        assertNotNull(getProvider("openai"))
    }

    @Test
    fun testChatMessageCreation() {
        val userMsg = ChatMessage(role = "user", content = "Hello Orbit AI")
        assertEquals("user", userMsg.role)
        assertEquals("Hello Orbit AI", userMsg.content)
        assertNull(userMsg.toolCallId)
    }

    @Test
    fun testGeminiModelList() {
        val gemini = getProvider("gemini")
        assertNotNull(gemini)
        assertTrue(gemini!!.models.any { it.id == "gemini-2.0-flash" })
        assertTrue(gemini.models.first { it.id == "gemini-2.0-flash" }.supportsTools)
    }
}
