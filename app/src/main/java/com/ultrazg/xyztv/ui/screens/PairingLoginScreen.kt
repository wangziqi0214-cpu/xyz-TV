package com.ultrazg.xyztv.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.ultrazg.xyztv.data.PairingServer

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PairingLoginScreen(
    onBack: () -> Unit,
    onPaired: () -> Unit
) {
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(Unit) {
        PairingServer.start(onPaired)
        PairingServer.serverUrl?.let { qrBitmap = generateQrBitmap(it) }
    }

    DisposableEffect(Unit) {
        onDispose { PairingServer.stop() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Legacy QR Pairing",
                color = Color(0xFF020303),
                fontSize = 34.sp,
                style = MaterialTheme.typography.headlineLarge
            )

            Text(
                text = "This page does not use the embedded xyz backend.",
                color = Color(0xFFFFB74D),
                fontSize = 18.sp
            )

            Text(
                text = "It only works when your phone browser can directly reach the TV over the same LAN.",
                color = Color.LightGray,
                fontSize = 16.sp
            )

            qrBitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Pairing QR code",
                    modifier = Modifier.size(280.dp)
                )
            }

            PairingServer.serverUrl?.let {
                Text(
                    text = it,
                    color = Color(0xFF90CAF9),
                    fontSize = 20.sp
                )
            }

            if (PairingServer.allIps.size > 1) {
                Text(
                    text = "Other addresses: ${PairingServer.allIps.drop(1).joinToString(", ") { "http://$it:8888" }}",
                    color = Color(0xFF78909C),
                    fontSize = 14.sp
                )
            }

            Text(
                text = PairingServer.statusMessage,
                color = Color(0xFF3A3D42),
                fontSize = 16.sp
            )

            PairingServer.lastReceivedTokenPrefix?.let {
                Text(
                    text = "Last token prefix: $it..",
                    color = Color(0xFF81C784),
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                onClick = onBack,
                shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.small),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "Back",
                    color = Color(0xFF020303),
                    fontSize = 18.sp,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 14.dp)
                )
            }
        }
    }
}

private fun generateQrBitmap(content: String, size: Int = 800): Bitmap {
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
    for (x in 0 until size) {
        for (y in 0 until size) {
            bitmap.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        }
    }
    return bitmap
}
