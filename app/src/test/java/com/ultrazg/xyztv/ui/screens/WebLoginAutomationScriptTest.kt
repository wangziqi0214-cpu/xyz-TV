package com.ultrazg.xyztv.ui.screens

import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class WebLoginAutomationScriptTest {
    @Test
    fun loginAutomationScriptHandlesOfficialRadioLabelQrTab() {
        val script = loginAutomationScript()

        assertTrue(script.contains("input[type=\"radio\"]"))
        assertTrue(script.contains("labelTarget"))
        assertTrue(script.contains("dispatchEvent(new Event('change'"))
        assertTrue(script.contains("QRCode"))
    }

    @Test
    fun loginAutomationScriptDoesNotPermanentlySuppressQrTabRetries() {
        val script = loginAutomationScript()

        assertFalse(script.contains("xyzQrOpened === '1'"))
    }

    @Test
    fun loginAutomationScriptDoesNotAutoFocusPhoneInput() {
        val tickBody = loginAutomationScript().substringAfter("window.__xyzLoginBridgeTick = function()")

        assertFalse(tickBody.contains("focusPhoneInput()"))
    }

    @Test
    fun loginAutomationScriptDoesNotAutoClickQrSwitcher() {
        val tickBody = loginAutomationScript().substringAfter("window.__xyzLoginBridgeTick = function()")

        assertFalse(tickBody.contains("clickSwitcher()"))
    }

    @Test
    fun loginAutomationScriptKeepsOfficialLoginTabsVisible() {
        val script = loginAutomationScript()

        assertTrue(script.contains("xyzLoginTopSpacer"))
    }

    @Test
    fun loginAutomationScriptDoesNotZoomOfficialPage() {
        val script = loginAutomationScript()

        assertFalse(script.contains("document.body.style.zoom = '1.08'"))
    }

    @Test
    fun loginAutomationScriptSupportsManualPhoneLoginFocus() {
        val script = loginAutomationScript()

        assertTrue(script.contains("openPhoneTab"))
        assertTrue(script.contains("focusPhoneInput"))
        assertTrue(script.contains("toggleAgreement"))
    }

}
