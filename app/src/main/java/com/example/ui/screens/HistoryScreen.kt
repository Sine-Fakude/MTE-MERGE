package com.example.ui.screens

import android.net.Uri
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CallMerge
import androidx.compose.material.icons.rounded.CallSplit
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DocumentHistoryEntity
import com.example.ui.components.MteEmptyState
import com.example.ui.components.MteTopBar
import com.example.ui.components.RecentHistoryCard
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.CyberMagenta
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.liquidGlass
import com.example.ui.theme.neonGlow
import com.example.ui.viewmodel.HistoryViewModel
import com.example.ui.viewmodel.ThemeMode
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateBack: () -> Unit,
    themeMode: ThemeMode,
    onToggleTheme: () -> Unit,
    viewModel: HistoryViewModel,
    onActionMerge: (Uri) -> Unit = {},
    onActionSplit: (Uri) -> Unit = {},
    onActionLock: (Uri) -> Unit = {},
    onActionUnlock: (Uri) -> Unit = {},
    onActionShareNearby: (File, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val historyList by viewModel.historyList.collectAsState()
    var selectedFilter by remember { mutableStateOf("ALL") }
    var showClearConfirmation by remember { mutableStateOf(false) }

    // Actionable File Hub Bottom Sheet State
    var activeItemForActions by remember { mutableStateOf<DocumentHistoryEntity?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val filteredList = when (selectedFilter) {
        "MERGE" -> historyList.filter { it.operationType == "MERGE" }
        "SPLIT" -> historyList.filter { it.operationType == "SPLIT" }
        "IMAGE_TO_PDF" -> historyList.filter { it.operationType == "IMAGE_TO_PDF" }
        "QR_CODE" -> historyList.filter { it.operationType == "QR_CODE" }
        "LOCK" -> historyList.filter { it.operationType == "LOCK" }
        "UNLOCK" -> historyList.filter { it.operationType == "UNLOCK" }
        else -> historyList
    }

    Scaffold(
        topBar = {
            MteTopBar(
                title = "Document Hub & History",
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
        ) {
            // Filter row & Clear action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    val filters = listOf(
                        "ALL" to "All",
                        "MERGE" to "Merged",
                        "SPLIT" to "Split",
                        "LOCK" to "Locked",
                        "UNLOCK" to "Unlocked",
                        "IMAGE_TO_PDF" to "Images",
                        "QR_CODE" to "QR Codes"
                    )
                    items(filters) { (key, label) ->
                        FilterChip(
                            selected = selectedFilter == key,
                            onClick = { selectedFilter = key },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.testTag("filter_chip_$key")
                        )
                    }
                }

                if (historyList.isNotEmpty()) {
                    IconButton(
                        onClick = { showClearConfirmation = true },
                        modifier = Modifier.testTag("clear_history_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteForever,
                            contentDescription = "Clear All History",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (filteredList.isEmpty()) {
                MteEmptyState(
                    icon = Icons.Rounded.History,
                    title = "No history found",
                    description = if (selectedFilter == "ALL") "Processed documents and QR codes will appear here as actionable items." else "No records matching this filter."
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList, key = { it.id }) { item ->
                        RecentHistoryCard(
                            item = item,
                            onOpen = { activeItemForActions = item },
                            onShare = { viewModel.shareItem(context, item) }
                        )
                    }
                }
            }
        }
    }

    // Liquid Glass Actionable Operations Bottom Sheet
    if (activeItemForActions != null) {
        val item = activeItemForActions!!
        val fileUri = Uri.parse(item.uriString)
        val isPdf = item.operationType != "QR_CODE" && item.fileName.endsWith(".pdf", ignoreCase = true)

        ModalBottomSheet(
            onDismissRequest = { activeItemForActions = null },
            sheetState = sheetState,
            containerColor = Color.Transparent,
            dragHandle = null
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .liquidGlass(shape = RoundedCornerShape(28.dp), surfaceAlpha = 0.92f)
                    .padding(20.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header with document info
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isPdf) Icons.Rounded.Description else Icons.Rounded.QrCode2,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(32.dp)
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.fileName,
                                fontFamily = AmiriFontFamily,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${item.operationType} • ${item.formattedSize}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "Quick Document Actions",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 2.dp, top = 4.dp)
                    )

                    // Actionable buttons tailored to file (unified ElectricCyan & CyberMagenta)
                    if (isPdf) {
                        ActionSheetRow(
                            icon = Icons.Rounded.CallMerge,
                            title = "Merge with other files",
                            subtitle = "Pre-load into Merge tool with other documents",
                            accentColor = ElectricCyan,
                            onClick = {
                                activeItemForActions = null
                                onActionMerge(fileUri)
                            }
                        )

                        ActionSheetRow(
                            icon = Icons.Rounded.CallSplit,
                            title = "Split Document",
                            subtitle = "Extract pages or ranges directly from this PDF",
                            accentColor = CyberMagenta,
                            onClick = {
                                activeItemForActions = null
                                onActionSplit(fileUri)
                            }
                        )

                        ActionSheetRow(
                            icon = Icons.Rounded.Lock,
                            title = "Lock with Password (AES)",
                            subtitle = "Batch protect with cryptographic envelope",
                            accentColor = ElectricCyan,
                            onClick = {
                                activeItemForActions = null
                                onActionLock(fileUri)
                            }
                        )

                        ActionSheetRow(
                            icon = Icons.Rounded.LockOpen,
                            title = "Unlock / Decrypt",
                            subtitle = "Remove security restrictions from this PDF",
                            accentColor = CyberMagenta,
                            onClick = {
                                activeItemForActions = null
                                onActionUnlock(fileUri)
                            }
                        )
                    }

                    val localFile = File(item.filePath)
                    if (localFile.exists()) {
                        ActionSheetRow(
                            icon = Icons.Rounded.WifiTethering,
                            title = "Share to Nearby Device",
                            subtitle = "High-speed offline P2P transfer via Google Nearby",
                            accentColor = ElectricCyan,
                            onClick = {
                                activeItemForActions = null
                                onActionShareNearby(localFile, item.fileName)
                            }
                        )
                    }

                    ActionSheetRow(
                        icon = Icons.Rounded.Share,
                        title = "Share via Android Sheet",
                        subtitle = "Send to WhatsApp, Drive, Gmail, or Bluetooth",
                        accentColor = CyberMagenta,
                        onClick = {
                            activeItemForActions = null
                            viewModel.shareItem(context, item)
                        }
                    )

                    ActionSheetRow(
                        icon = Icons.Rounded.OpenInNew,
                        title = "Open in External Viewer",
                        subtitle = "View with default system viewer",
                        accentColor = CyberMagenta,
                        onClick = {
                            activeItemForActions = null
                            viewModel.openItem(context, item)
                        }
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("Clear All History?") },
            text = { Text("This will remove all document records from history. Cached files will be cleaned up.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAll()
                        showClearConfirmation = false
                    },
                    modifier = Modifier.testTag("confirm_clear_history")
                ) {
                    Text("Clear All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ActionSheetRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(shape = RoundedCornerShape(16.dp), surfaceAlpha = 0.40f)
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Clean icon directly tinted with the neon accent color, no blurry colored background box
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
    }
}
