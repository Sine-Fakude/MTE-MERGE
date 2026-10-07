package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.domain.util.P2pRole
import com.example.domain.util.P2pShareManager
import com.example.domain.util.P2pTransferState
import com.example.ui.components.MteTopBar
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.CyberMagenta
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.liquidGlass
import com.example.ui.theme.neonGlow
import com.example.ui.viewmodel.P2pViewModel
import com.example.ui.viewmodel.ThemeMode
import java.io.File
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun P2pTransferScreen(
    onNavigateBack: () -> Unit,
    themeMode: ThemeMode,
    onToggleTheme: () -> Unit,
    viewModel: P2pViewModel,
    initialRole: P2pRole = P2pRole.RECEIVER,
    fileToSend: File? = null,
    onLaunchPreview: (android.net.Uri, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    var hasPermissions by remember { mutableStateOf(false) }

    // Required permissions depending on Android version
    val requiredPermissions = remember {
        val list = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.BLUETOOTH_SCAN)
            list.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            list.add(Manifest.permission.BLUETOOTH_CONNECT)
            list.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            list.add(Manifest.permission.BLUETOOTH_SCAN)
            list.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            list.add(Manifest.permission.BLUETOOTH_CONNECT)
            list.add(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            list.add(Manifest.permission.ACCESS_FINE_LOCATION)
            list.add(Manifest.permission.CHANGE_WIFI_STATE)
        }
        list.toTypedArray()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermissions = results.values.all { it } || results.isEmpty()
        if (hasPermissions) {
            if (initialRole == P2pRole.SENDER && fileToSend != null) {
                viewModel.startSending(context, fileToSend)
            } else if (initialRole == P2pRole.RECEIVER) {
                viewModel.startReceiving(context)
            }
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(requiredPermissions)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.cancel(context)
        }
    }

    Scaffold(
        topBar = {
            MteTopBar(
                title = if (state.role == P2pRole.SENDER) "Sending Document" else "Nearby P2P Receive",
                canNavigateBack = true,
                onNavigateBack = {
                    viewModel.cancel(context)
                    onNavigateBack()
                },
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
            // Header Info Card
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = ElectricCyan.copy(alpha = 0.12f),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Text(
                        text = if (state.role == P2pRole.SENDER) "OFFLINE P2P SENDER" else "OFFLINE P2P DISCOVERY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = ElectricCyan,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Text(
                    text = when (state.state) {
                        P2pTransferState.ADVERTISING -> "Waiting for Nearby Device..."
                        P2pTransferState.DISCOVERING -> "Scanning for Senders..."
                        P2pTransferState.CONNECTING -> "Establishing Secure Link..."
                        P2pTransferState.CONNECTED,
                        P2pTransferState.TRANSFERRING -> if (state.role == P2pRole.SENDER) "Transmitting Document..." else "Receiving Document..."
                        P2pTransferState.COMPLETED -> "Transfer Complete!"
                        P2pTransferState.ERROR -> "Transfer Failed"
                        P2pTransferState.IDLE -> "Preparing P2P Link..."
                    },
                    fontFamily = AmiriFontFamily,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = state.statusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Radar Pulse Graphic Animation Area
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                when (state.state) {
                    P2pTransferState.COMPLETED -> {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .neonGlow(color = ElectricCyan, radius = 16.dp, shape = CircleShape)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(ElectricCyan.copy(alpha = 0.25f), Color(0xFF0B132B))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }

                    P2pTransferState.ERROR -> {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }

                    else -> {
                        LiquidRadarAnimation()
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .neonGlow(
                                    color = if (state.state == P2pTransferState.TRANSFERRING) CyberMagenta else ElectricCyan,
                                    radius = 12.dp,
                                    shape = CircleShape
                                )
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            (if (state.state == P2pTransferState.TRANSFERRING) CyberMagenta else ElectricCyan).copy(alpha = 0.35f),
                                            Color(0xFF0B132B)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (state.state == P2pTransferState.TRANSFERRING) Icons.Rounded.Description else Icons.Rounded.WifiTethering,
                                contentDescription = null,
                                tint = if (state.state == P2pTransferState.TRANSFERRING) CyberMagenta else ElectricCyan,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }
                }
            }

            // Transfer Status Card / Progress Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(shape = RoundedCornerShape(22.dp), surfaceAlpha = 0.55f)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.fileName != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Description,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = state.fileName ?: "Document",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (state.fileSizeBytes > 0) {
                                val sizeMb = state.fileSizeBytes / (1024f * 1024f)
                                Text(
                                    text = String.format("%.2f MB", sizeMb),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (state.state == P2pTransferState.TRANSFERRING) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "High-Speed WiFi Direct",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberMagenta
                            )
                            Text(
                                text = "${(state.progress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = ElectricCyan
                            )
                        }

                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = ElectricCyan,
                            trackColor = Color(0xFF1E293B)
                        )
                    }
                }

                if (state.state == P2pTransferState.COMPLETED && state.completedFile != null) {
                    val file = state.completedFile!!
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                openFileInSystemViewer(context, file)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.OpenInNew,
                                contentDescription = null,
                                tint = Color(0xFF050811),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open", color = Color(0xFF050811), fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                shareFileViaAndroid(context, file)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = null,
                                tint = CyberMagenta,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share", color = CyberMagenta, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Error message display
                if (state.errorMessage != null) {
                    Text(
                        text = state.errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Bottom Actions
            Button(
                onClick = {
                    viewModel.cancel(context)
                    onNavigateBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("p2p_dismiss_btn"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.state == P2pTransferState.COMPLETED) ElectricCyan else Color(0xFF1E293B)
                )
            ) {
                Text(
                    text = if (state.state == P2pTransferState.COMPLETED) "Done" else "Cancel Transfer",
                    fontWeight = FontWeight.Bold,
                    color = if (state.state == P2pTransferState.COMPLETED) Color(0xFF050811) else Color.White
                )
            }
        }
    }
}

@Composable
private fun LiquidRadarAnimation() {
    val transition = rememberInfiniteTransition(label = "p2p_radar")

    val pulse1 by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse1"
    )

    val pulse2 by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, delayMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse2"
    )

    val pulse3 by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, delayMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse3"
    )

    val rotationAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotationAngle"
    )

    Canvas(modifier = Modifier.size(220.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = size.minDimension / 2f

        // Grid rings
        drawCircle(
            color = ElectricCyan.copy(alpha = 0.12f),
            radius = maxRadius * 0.4f,
            style = Stroke(width = 1.dp.toPx())
        )
        drawCircle(
            color = CyberMagenta.copy(alpha = 0.10f),
            radius = maxRadius * 0.7f,
            style = Stroke(width = 1.dp.toPx())
        )
        drawCircle(
            color = ElectricCyan.copy(alpha = 0.10f),
            radius = maxRadius * 0.95f,
            style = Stroke(width = 1.dp.toPx())
        )

        // Pulsing expanding rings
        drawCircle(
            color = ElectricCyan.copy(alpha = (1f - pulse1) * 0.5f),
            radius = maxRadius * pulse1,
            style = Stroke(width = 2.dp.toPx())
        )
        drawCircle(
            color = CyberMagenta.copy(alpha = (1f - pulse2) * 0.4f),
            radius = maxRadius * pulse2,
            style = Stroke(width = 1.5.dp.toPx())
        )
        drawCircle(
            color = ElectricCyan.copy(alpha = (1f - pulse3) * 0.35f),
            radius = maxRadius * pulse3,
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Sweeping radar beam
        val angleRad = Math.toRadians(rotationAngle.toDouble())
        val endX = center.x + (maxRadius * cos(angleRad)).toFloat()
        val endY = center.y + (maxRadius * sin(angleRad)).toFloat()

        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(ElectricCyan.copy(alpha = 0.8f), CyberMagenta.copy(alpha = 0.1f)),
                start = center,
                end = Offset(endX, endY)
            ),
            start = center,
            end = Offset(endX, endY),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

private fun openFileInSystemViewer(context: android.content.Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {}
}

private fun shareFileViaAndroid(context: android.content.Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share via Nearby P2P"))
    } catch (_: Exception) {}
}
