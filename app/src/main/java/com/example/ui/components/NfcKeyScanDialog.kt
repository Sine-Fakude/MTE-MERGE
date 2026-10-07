package com.example.ui.components

import android.app.Activity
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Contactless
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.util.NfcKeyManager
import com.example.ui.theme.CyberMagenta
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.liquidGlass
import com.example.ui.theme.neonGlow

@Composable
fun NfcKeyScanDialog(
    onDismissRequest: () -> Unit,
    onKeyExtracted: (derivedKey: String, rawUid: String) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var detectedUid by remember { mutableStateOf<String?>(null) }
    var derivedAesKey by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isScanning by remember { mutableStateOf(true) }

    // Start reader mode via NfcAdapter without triggering system sounds/dialogs
    DisposableEffect(activity) {
        if (activity != null && NfcKeyManager.isNfcSupported(activity)) {
            NfcKeyManager.startReaderMode(
                activity = activity,
                onTagDetected = { uid, key ->
                    detectedUid = uid
                    derivedAesKey = key
                    isScanning = false
                    errorMessage = null
                },
                onError = { err ->
                    errorMessage = err
                }
            )
        } else {
            errorMessage = "NFC hardware is not available on this device. You can test with the simulator below."
        }

        onDispose {
            if (activity != null) {
                NfcKeyManager.stopReaderMode(activity)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .liquidGlass(shape = RoundedCornerShape(28.dp), surfaceAlpha = 0.92f)
                .padding(22.dp)
                .testTag("nfc_scan_dialog")
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = ElectricCyan.copy(alpha = 0.12f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Nfc,
                                    contentDescription = null,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "NFC Hardware Key",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Physical AES-256 Security",
                                style = MaterialTheme.typography.bodySmall,
                                color = ElectricCyan
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Pulsing Radar / Contactless Animation
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isScanning) {
                        NfcRadarPulseAnimation()
                    }

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .neonGlow(
                                color = if (detectedUid != null) CyberMagenta else ElectricCyan,
                                radius = 10.dp,
                                shape = CircleShape
                            )
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        (if (detectedUid != null) CyberMagenta else ElectricCyan).copy(alpha = 0.35f),
                                        Color(0xFF0B132B)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (detectedUid != null) Icons.Rounded.Key else Icons.Rounded.Contactless,
                            contentDescription = null,
                            tint = if (detectedUid != null) CyberMagenta else ElectricCyan,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                if (detectedUid == null) {
                    Text(
                        text = "Hold an NFC ring, transit card, or tag against the back of your device",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    if (errorMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    // Simulation button for testing without physical NFC tags
                    OutlinedButton(
                        onClick = {
                            val (simUid, simKey) = NfcKeyManager.generateSimulatedTag()
                            detectedUid = simUid
                            derivedAesKey = simKey
                            isScanning = false
                            errorMessage = null
                        },
                        modifier = Modifier.testTag("nfc_simulate_btn"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Security,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Simulate NFC Tag (Test Mode)",
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricCyan
                        )
                    }
                } else {
                    // Tag Scanned Success Card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(shape = RoundedCornerShape(18.dp), surfaceAlpha = 0.5f)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "HARDWARE UID",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = ElectricCyan
                            )
                            Surface(
                                shape = CircleShape,
                                color = ElectricCyan.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = detectedUid ?: "",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricCyan,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "DERIVED 256-BIT AES KEY (SHA-256)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = CyberMagenta
                        )

                        Text(
                            text = derivedAesKey ?: "",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Apply Button
                    Button(
                        onClick = {
                            derivedAesKey?.let { key ->
                                onKeyExtracted(key, detectedUid ?: "")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("nfc_apply_key_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .neonGlow(color = CyberMagenta, radius = 6.dp, shape = RoundedCornerShape(16.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(ElectricCyan, CyberMagenta)
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Use Derived 256-Bit AES Key",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF050811)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NfcRadarPulseAnimation() {
    val transition = rememberInfiniteTransition(label = "nfc_radar")
    val pulse1 by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse1"
    )
    val pulse2 by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, delayMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse2"
    )

    Canvas(modifier = Modifier.size(140.dp)) {
        val maxRadius = size.minDimension / 2f

        // Outer ring 1
        drawCircle(
            color = ElectricCyan.copy(alpha = (1f - pulse1) * 0.45f),
            radius = maxRadius * pulse1,
            style = Stroke(width = 2.dp.toPx())
        )

        // Outer ring 2
        drawCircle(
            color = CyberMagenta.copy(alpha = (1f - pulse2) * 0.40f),
            radius = maxRadius * pulse2,
            style = Stroke(width = 1.5.dp.toPx())
        )
    }
}
