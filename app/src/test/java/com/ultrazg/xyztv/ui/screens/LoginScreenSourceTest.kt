package com.ultrazg.xyztv.ui.screens

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LoginScreenSourceTest {
    @Test
    fun tvInputFieldIsFocusableAndNumeric() {
        val source = File("src/main/java/com/ultrazg/xyztv/ui/screens/LoginScreen.kt").readText()

        assertTrue(source.contains(".focusable()"))
        assertTrue(source.contains("KeyboardType.Number"))
    }

    @Test
    fun tvInputFieldTransfersTvFocusToTextField() {
        val source = File("src/main/java/com/ultrazg/xyztv/ui/screens/LoginScreen.kt").readText()

        assertTrue(source.contains("FocusRequester()"))
        assertTrue(source.contains(".focusRequester("))
        assertTrue(source.contains(".requestFocus()"))
    }
}
