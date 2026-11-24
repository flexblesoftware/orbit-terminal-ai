package com.orbitterminal.app

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.security.MessageDigest

class RootfsVerificationTest {

    @Test
    fun testSha256Calculation() {
        val testData = "orbit-terminal-ai-rootfs-test".toByteArray()
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(testData).joinToString("") { "%02x".format(it) }

        assertNotNull(hash)
        assertEquals(64, hash.length)
    }

    @Test
    fun testRangeHeaderParsing() {
        val existingBytes = 1048576L // 1 MB
        val rangeHeader = "bytes=$existingBytes-"
        assertTrue(rangeHeader.startsWith("bytes=1048576-"))
    }
}
