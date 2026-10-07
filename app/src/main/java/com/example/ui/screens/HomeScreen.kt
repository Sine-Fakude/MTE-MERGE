package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CallMerge
import androidx.compose.material.icons.rounded.CallSplit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.MteTopBar
import com.example.ui.components.RecentHistoryCard
import com.example.ui.navigation.Screen
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.CyberMagenta
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.liquidGlass
import com.example.ui.theme.neonGlow
import com.example.ui.viewmodel.HistoryViewModel
import com.example.ui.viewmodel.ThemeMode

@Composable
fun HomeScreen(
    themeMode: ThemeMode,
    onToggleTheme: () -> Unit,
    onNavigate: (String) -> Unit,
    historyViewModel: HistoryViewModel
) {
    val context = LocalContext.current
    val recentHistory by historyViewModel.recentList.collectAsState()

    Scaffold(
        topBar = {
            MteTopBar(
                title = "MTE Merge",
                canNavigateBack = false,
                themeMode = themeMode,
                onToggleTheme = onToggleTheme,
                onNavigateToHistory = { onNavigate(Screen.History.route) }
            )
        },
        floatingActionButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Receive File FAB (Starts Nearby P2P Discovery)
                Box(
                    modifier = Modifier
                        .neonGlow(color = CyberMagenta, radius = 8.dp, shape = CircleShape)
                        .liquidGlass(shape = CircleShape, surfaceAlpha = 0.85f)
                        .clickable { onNavigate(Screen.P2pShare.route) }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .testTag("home_fab_receive_p2p")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WifiTethering,
                            contentDescription = "Receive File",
                            tint = CyberMagenta,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Receive File",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Smooth, unified Brush.horizontalGradient transitioning from Electric Cyan to Cyber Magenta
                Box(
                    modifier = Modifier
                        .neonGlow(color = ElectricCyan, radius = 10.dp, shape = CircleShape)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(ElectricCyan, CyberMagenta)
                            )
                        )
                        .clickable { onNavigate(Screen.Merge.route) }
                        .padding(horizontal = 22.dp, vertical = 12.dp)
                        .testTag("home_fab_merge")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CallMerge,
                            contentDescription = "Merge PDFs",
                            tint = Color(0xFF050811),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Merge PDFs",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF050811)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Hero Banner with Liquid Glass & Clean Dual-Tone Accents
                item {
                    LiquidHeroBanner()
                }

                // Grid Section Header
                item {
                    Text(
                        text = "Document & QR Utilities",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }

                // Row 1: Merge PDFs (Electric Cyan) & Split PDF (Cyber Magenta)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LiquidGlassTile(
                            modifier = Modifier.weight(1f),
                            title = "Merge PDFs",
                            subtitle = "Combine & reorder files",
                            badge = "Multi-file",
                            icon = Icons.Rounded.CallMerge,
                            accentColor = ElectricCyan,
                            onClick = { onNavigate(Screen.Merge.route) },
                            testTag = "home_card_merge"
                        )

                        LiquidGlassTile(
                            modifier = Modifier.weight(1f),
                            title = "Split PDF",
                            subtitle = "Extract pages & ranges",
                            badge = "High Speed",
                            icon = Icons.Rounded.CallSplit,
                            accentColor = CyberMagenta,
                            onClick = { onNavigate(Screen.Split.route) },
                            testTag = "home_card_split"
                        )
                    }
                }

                // Row 2: Image to PDF (Electric Cyan) & QR Studio (Cyber Magenta)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LiquidGlassTile(
                            modifier = Modifier.weight(1f),
                            title = "Image to PDF",
                            subtitle = "Smart downsampling & convert",
                            badge = "Compact",
                            icon = Icons.Rounded.PictureAsPdf,
                            accentColor = ElectricCyan,
                            onClick = { onNavigate(Screen.ImageToPdf.route) },
                            testTag = "home_card_image_pdf"
                        )

                        LiquidGlassTile(
                            modifier = Modifier.weight(1f),
                            title = "QR Studio",
                            subtitle = "Custom colors & center logo",
                            badge = "Universal",
                            icon = Icons.Rounded.QrCode2,
                            accentColor = CyberMagenta,
                            onClick = { onNavigate(Screen.QrGenerator.route) },
                            testTag = "home_card_qr"
                        )
                    }
                }

                // Row 3: Parallel Batch Lock (Electric Cyan) & Batch Unlock (Cyber Magenta)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LiquidGlassTile(
                            modifier = Modifier.weight(1f),
                            title = "Batch Lock",
                            subtitle = "Parallel AES-128 encryption",
                            badge = "Concurrent",
                            icon = Icons.Rounded.Lock,
                            accentColor = ElectricCyan,
                            onClick = { onNavigate(Screen.LockPdf.route) },
                            testTag = "home_card_lock_pdf"
                        )

                        LiquidGlassTile(
                            modifier = Modifier.weight(1f),
                            title = "Batch Unlock",
                            subtitle = "Remove security restrictions",
                            badge = "Fast Decrypt",
                            icon = Icons.Rounded.LockOpen,
                            accentColor = CyberMagenta,
                            onClick = { onNavigate(Screen.UnlockPdf.route) },
                            testTag = "home_card_unlock_pdf"
                        )
                    }
                }

                // Recent Documents Section
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, top = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Operations",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (recentHistory.isNotEmpty()) {
                            TextButton(
                                onClick = { onNavigate(Screen.History.route) },
                                modifier = Modifier.testTag("home_view_all_history")
                            ) {
                                Text(
                                    text = "View All",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ElectricCyan
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = null,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                if (recentHistory.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlass(shape = RoundedCornerShape(20.dp), surfaceAlpha = 0.45f)
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "No recent operations yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Merged documents, split files, and QR codes will appear here",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                } else {
                    items(recentHistory, key = { it.id }) { item ->
                        RecentHistoryCard(
                            item = item,
                            onOpen = { onNavigate(Screen.History.route) },
                            onShare = { historyViewModel.shareItem(context, item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiquidHeroBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(shape = RoundedCornerShape(26.dp), surfaceAlpha = 0.55f, borderAlpha = 0.35f)
            .padding(22.dp)
            .testTag("home_hero_banner")
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Simple translucent capsule badge (no glowing or harsh background)
                Surface(
                    color = ElectricCyan.copy(alpha = 0.12f),
                    shape = CircleShape
                ) {
                    Text(
                        text = "100% FREE UTILITY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricCyan,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    color = CyberMagenta.copy(alpha = 0.12f),
                    shape = CircleShape
                ) {
                    Text(
                        text = "SCRATCH MEMORY OPTIMIZED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberMagenta,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "MTE Merge Suite",
                fontFamily = AmiriFontFamily,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "High-performance parallel PDF merge, split, image conversion, batch AES protection, and custom branded QR studio.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LiquidGlassTile(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    badge: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = modifier
            .height(144.dp)
            .liquidGlass(shape = RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sleek minimalist icon: directly tinted with the neon accent color, NO blurry colored box behind it
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(28.dp)
                )

                // Refined capsule badge: simple translucent capsule with colored text, NO glowing box
                Surface(
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.10f)
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
