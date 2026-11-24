package com.orbitterminal.app

import org.junit.Assert.*
import org.junit.Test
import java.util.regex.Pattern

class CommandOutputParserTest {

    @Test
    fun testAntigravityAuthUrlExtraction() {
        val sampleOutput = """
            Please visit the following URL to authenticate:
            https://accounts.google.com/o/oauth2/v2/auth?client_id=12345&redirect_uri=urn:ietf:wg:oauth:2.0:oob
            Enter your token:
        """.trimIndent()

        val urlPattern = Pattern.compile("https://[\\w\\-\\.]+\\.[a-z]{2,}(?:/[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=]*)?")
        val matcher = urlPattern.matcher(sampleOutput)
        assertTrue(matcher.find())
        val url = matcher.group(0)
        assertTrue(url.startsWith("https://accounts.google.com"))
    }

    @Test
    fun testEmailParsingFromAuthStatus() {
        val sampleStatus = "Successfully authenticated as engineer@example.com (Google Cloud)"
        val emailPattern = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
        val matcher = emailPattern.matcher(sampleStatus)
        assertTrue(matcher.find())
        assertEquals("engineer@example.com", matcher.group(0))
    }
}
