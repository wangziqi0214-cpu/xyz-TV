@file:OptIn(
    androidx.tv.material3.ExperimentalTvMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package com.ultrazg.xyztv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.ultrazg.xyztv.data.AppLogger
import com.ultrazg.xyztv.data.EmbeddedBackendManager
import com.ultrazg.xyztv.ui.viewmodel.LoginViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToWebLogin: () -> Unit,
    onNavigateToPairLogin: () -> Unit
) {
    val viewModel: LoginViewModel = viewModel()
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 32.dp, horizontal = 28.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier
                    .width(620.dp)
                    .fillMaxHeight()
                    .verticalScroll(scrollState)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Xiaoyuzhou TV",
                    color = Color(0xFF020303),
                    fontSize = 42.sp,
                    style = MaterialTheme.typography.headlineLarge
                )

                Text(
                    text = "SMS auth now runs directly so request failures can be shown without crashing the app.",
                    color = Color.LightGray,
                    fontSize = 18.sp
                )

                Surface(
                    onClick = onNavigateToWebLogin,
                    shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.small),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        focusedContainerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewOnFocus()
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Open Official Web Login",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 18.sp
                        )
                    }
                }

                Text(
                    text = "Recommended: the official web page handles captcha and SMS validation itself. The manual SMS flow below is only a fallback for debugging.",
                    color = Color(0xFFB0BEC5),
                    fontSize = 14.sp
                )

                Surface(
                    onClick = onNavigateToPairLogin,
                    shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.small),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewOnFocus()
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Open Legacy QR Pairing",
                            color = Color(0xFF020303),
                            fontSize = 18.sp
                        )
                    }
                }

                Text(
                    text = "The diagnostic panel is pinned on the right so you can keep watching logs while moving through the form.",
                    color = Color(0xFF3A3D42),
                    fontSize = 14.sp
                )

                Text(
                    text = "Action Status: ${viewModel.status}",
                    color = Color(0xFFFFCC80),
                    fontSize = 16.sp
                )

                TvInputField(
                    label = "Phone Number",
                    value = viewModel.phone,
                    onValueChange = viewModel::onPhoneChange,
                    placeholder = "Enter 11 digits"
                )

                Surface(
                    onClick = { viewModel.sendCode() },
                    shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.small),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    focusedContainerColor = MaterialTheme.colorScheme.primary
                ),
                enabled = !viewModel.isCodeSent,
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewOnFocus()
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when {
                                viewModel.isLoading -> "Sending..."
                                viewModel.isCodeSent -> "Code Sent"
                                else -> "Send SMS Code"
                            },
                            color = if (viewModel.isCodeSent) Color(0xFF3A3D42) else MaterialTheme.colorScheme.onPrimary,
                            fontSize = 18.sp
                        )
                    }
                }

                if (viewModel.isCodeSent) {
                    TvInputField(
                        label = "Verification Code",
                        value = viewModel.code,
                        onValueChange = viewModel::onCodeChange,
                        placeholder = "Enter 4 to 6 digits"
                    )
                }

                Surface(
                    onClick = { viewModel.login(onLoginSuccess) },
                    shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.small),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    focusedContainerColor = MaterialTheme.colorScheme.primary
                ),
                enabled = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewOnFocus()
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (viewModel.isLoading) "Working..." else "Login",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 18.sp
                        )
                    }
                }

                viewModel.error?.let { message ->
                    Text(
                        text = message,
                        color = Color.Red,
                        fontSize = 15.sp
                    )
                }
            }

            StatusPanel(
                modifier = Modifier
                    .width(500.dp)
                    .fillMaxHeight()
            )
        }
    }
}

@Composable
fun StatusPanel(modifier: Modifier = Modifier) {
    val healthy = EmbeddedBackendManager.isHealthy
    val status = EmbeddedBackendManager.statusMessage
    val baseUrl = EmbeddedBackendManager.baseUrl
    val lastError = EmbeddedBackendManager.lastError
    val diagnosticLogs = AppLogger.diagnosticLines
    val logScrollState = rememberScrollState()

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = if (healthy) "Backend Status: Healthy" else "Backend Status: Not Ready",
            color = if (healthy) Color(0xFF81C784) else Color(0xFFFFB74D),
            fontSize = 16.sp
        )
        Text(
            text = "Base URL: $baseUrl",
            color = Color(0xFF90CAF9),
            fontSize = 14.sp
        )
        Text(
            text = status,
            color = Color.LightGray,
            fontSize = 14.sp
        )
        lastError?.let {
            Text(
                text = "Last Error: $it",
                color = Color.Red,
                fontSize = 14.sp
            )
        }
        if (diagnosticLogs.isNotEmpty()) {
            Text(
                text = "Diagnostic Logs:",
                color = Color(0xFFFFCC80),
                fontSize = 14.sp
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(logScrollState),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (line in diagnosticLogs.takeLast(8)) {
                    Text(
                        text = line,
                        color = Color(0xFFB0BEC5),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    val inputFocusRequester = remember { FocusRequester() }
    val fieldModifier = Modifier
        .fillMaxWidth()
        .bringIntoViewOnFocus()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            color = Color(0xFF3A3D42),
            fontSize = 14.sp
        )

        Box(
            modifier = fieldModifier
                .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .onFocusChanged { state ->
                    if (state.hasFocus) {
                        inputFocusRequester.requestFocus()
                    }
                }
                .focusable()
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = TextStyle(color = Color(0xFF020303), fontSize = 18.sp),
                cursorBrush = SolidColor(Color(0xFF020303)),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(inputFocusRequester)
            ) { innerTextField ->
                if (value.isEmpty()) {
                    Text(text = placeholder, color = Color(0xFF3A3D42), fontSize = 18.sp)
                }
                innerTextField()
            }
        }
    }
}

@Composable
private fun Modifier.bringIntoViewOnFocus(): Modifier {
    val requester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    return this
        .bringIntoViewRequester(requester)
        .onFocusChanged { state ->
            if (state.hasFocus) {
                scope.launch {
                    requester.bringIntoView()
                }
            }
        }
}
