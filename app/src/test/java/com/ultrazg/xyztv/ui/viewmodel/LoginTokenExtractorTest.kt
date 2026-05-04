package com.ultrazg.xyztv.ui.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class LoginTokenExtractorTest {
    @Test
    fun extractsCamelCaseTokensFromLoginBody() {
        val tokens = extractAuthTokens(
            rawBody = """{"data":{"type":"USER","accessToken":"access-token","refreshToken":"refresh-token"}}""",
            headers = emptyMap()
        )

        assertNotNull(tokens)
        assertEquals("access-token", tokens?.first)
        assertEquals("refresh-token", tokens?.second)
    }

    @Test
    fun extractsJikeTokenKeysFromNestedLoginBody() {
        val tokens = extractAuthTokens(
            rawBody = """{"data":{"auth":{"x-jike-access-token":"access-token","x-jike-refresh-token":"refresh-token"}}}""",
            headers = emptyMap()
        )

        assertNotNull(tokens)
        assertEquals("access-token", tokens?.first)
        assertEquals("refresh-token", tokens?.second)
    }

    @Test
    fun extractsTokensFromSetCookieHeaders() {
        val tokens = extractAuthTokens(
            rawBody = """{"data":{"type":"USER"}}""",
            headers = mapOf(
                "Set-Cookie" to listOf(
                    "x-jike-access-token=access-token; Path=/; HttpOnly",
                    "x-jike-refresh-token=refresh-token; Path=/; HttpOnly"
                )
            )
        )

        assertNotNull(tokens)
        assertEquals("access-token", tokens?.first)
        assertEquals("refresh-token", tokens?.second)
    }
}
