package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Contactless
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.nfc.KeyEmulatorService
import com.example.domain.util.NfcKeyManager
import com.example.ui.components.MteTopBar
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.CyberMagenta
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.liquidGlass
import com.example.ui.theme.neonGlow
import com.example.ui.viewmodel.ThemeMode

@Composable
fun NfcKeyEmulatorScreen(
    onNavigateBack: () -> Unit,
    themeMode: ThemeMode,
    onToggleTheme: () -> Unit
) {
    val context = LocalContext.current
    var deviceUuid by remember { mutableStateOf(KeyEmulatorService.getOrCreateDeviceKeyUuid(context)) }
    val isNfcSupported = remember { NfcKeyManager.isNfcSupported(context) }
    val isNfcEnabled = remember { NfcKeyManager.isNfcEnabled(context) }

    Scaffold(
        topBar = {
            MteTopBar(
                title = "NFC Key Emulator",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                themeMode = themeMode,
                onToggleTheme = onToggleTheme
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = CyberMagenta.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "HOST-BASED CARD EMULATION (HCE)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = CyberMagenta,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Text(
                    text = "Secondary Phone Key Mode",
                    fontFamily = AmiriFontFamily,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Hold this phone against the primary device to authorize batch encryption or decryption.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Pulsing Concentric Dual-Tone Neon Radar Waves
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                HcePulseAnimation()

                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .neonGlow(color = CyberMagenta, radius = 16.dp, shape = CircleShape)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    CyberMagenta.copy(alpha = 0.40f),
                                    ElectricCyan.copy(alpha = 0.20f),
                                    Color(0xFF0B132B)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Smartphone,
                        contentDescription = null,
                        tint = CyberMagenta,
                        modifier = Modifier.size(42.dp)
                    )
                }
            }

            // Key Emulation Details Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(shape = RoundedCornerShape(24.dp), surfaceAlpha = 0.60f)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (isNfcEnabled) "HCE Smart Key Active" else "NFC Disabled in System",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isNfcEnabled) ElectricCyan else MaterialTheme.colorScheme.error
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = CyberMagenta.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "AID: ${KeyEmulatorService.AID_STRING.take(6)}…",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberMagenta,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Text(
                    text = "DEVICE KEY FINGERPRINT (UUID)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = deviceUuid,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = ElectricCyan,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }

                Text(
                    text = "• Emulates ISO-DEP smart card standard over ISO 7816-4\n• Primary phone transceives SELECT AID APDU command\n• Digests payload with SHA-256 for 256-bit AES key derivation",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Bottom Actions
            Button(
                onClick = onNavigateBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_close_key_emulator"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricCyan
                )
            ) {
                Text(
                    text = "Done / Exit Key Mode",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF050811)
                )
            }
        }
    }
}

@Composable
private fun HcePulseAnimation() {
    val transition = rememberInfiniteTransition(label = "hce_pulse")

    val pulse1 by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse1"
    )

    val pulse2 by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, delayMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse2"
    )

    val pulse3 by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, delayMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse3"
    )

    Canvas(modifier = Modifier.size(240.dp)) {
        val maxRadius = size.minDimension / 2f

        // Ripple 1 (Cyber Magenta)
        drawCircle(
            color = CyberMagenta.copy(alpha = (1f - pulse1) * 0.50f),
            radius = maxRadius * pulse1,
            style = Stroke(width = 2.dp.toPx())
        )

        // Ripple 2 (Electric Cyan)
        drawCircle(
            color = ElectricCyan.copy(alpha = (1f - pulse2) * 0.45f),
            radius = maxRadius * pulse2,
            style = Stroke(width = 1.8.dp.toPx())
        )

        // Ripple 3 (Cyber Magenta)
        drawCircle(
            color = CyberMagenta.copy(alpha = (1f - pulse3) * 0.35f),
            radius = maxRadius * pulse3,
            style = Stroke(width = 1.5.dp.toPx())
        )
    }
}
