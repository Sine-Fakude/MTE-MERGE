package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.rounded.CallSplit
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.SplitType
import com.example.ui.components.MteEmptyState
import com.example.ui.components.MteResultDialog
import com.example.ui.components.MteTopBar
import com.example.ui.viewmodel.SplitViewModel
import com.example.ui.viewmodel.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitScreen(
    onNavigateBack: () -> Unit,
    themeMode: ThemeMode,
    onToggleTheme: () -> Unit,
    viewModel: SplitViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.selectPdf(context, it) }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            MteTopBar(
                title = "Split PDF",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                themeMode = themeMode,
                onToggleTheme = onToggleTheme
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (uiState.selectedPdf != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.split(context) },
                            enabled = !uiState.isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("perform_split_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(uiState.statusMessage.ifBlank { "Splitting..." })
                            } else {
                                Icon(imageVector = Icons.Rounded.CallSplit, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when (uiState.splitConfig.splitType) {
                                        SplitType.ALL_PAGES -> "Split All Pages (${uiState.selectedPdf?.pageCount ?: 0})"
                                        SplitType.EXTRACT_SELECTED -> "Extract Selected (${uiState.splitConfig.selectedPages.size})"
                                        SplitType.PAGE_RANGE -> "Split by Range"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
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
                contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Picker Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("split_picker_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Extract Pages or Split by Range",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Select a PDF file to preview pages, extract select ranges, or split into individual documents.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                                    modifier = Modifier.testTag("select_split_pdf_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Rounded.FileOpen, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (uiState.selectedPdf == null) "Choose PDF" else "Change PDF")
                                }

                                if (uiState.selectedPdf != null) {
                                    OutlinedButton(
                                        onClick = { viewModel.reset() },
                                        modifier = Modifier.testTag("reset_split_button"),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Clear")
                                    }
                                }
                            }
                        }
                    }
                }

                if (uiState.selectedPdf != null) {
                    val pdf = uiState.selectedPdf!!

                    // Document Details Card
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Description,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = pdf.fileName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Total ${pdf.pageCount} pages • ${pdf.formattedSize}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Split Mode Switcher Tabs
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Split Method",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                val tabs = listOf(
                                    SplitType.ALL_PAGES to "All Pages",
                                    SplitType.PAGE_RANGE to "By Range",
                                    SplitType.EXTRACT_SELECTED to "Select Pages"
                                )
                                val selectedIndex = tabs.indexOfFirst { it.first == uiState.splitConfig.splitType }

                                SecondaryTabRow(
                                    selectedTabIndex = selectedIndex.coerceAtLeast(0),
                                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                                ) {
                                    tabs.forEachIndexed { index, (type, label) ->
                                        Tab(
                                            selected = selectedIndex == index,
                                            onClick = { viewModel.setSplitType(type) },
                                            text = {
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = if (selectedIndex == index) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            modifier = Modifier.testTag("split_tab_$index")
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                when (uiState.splitConfig.splitType) {
                                    SplitType.ALL_PAGES -> {
                                        Text(
                                            text = "Will split into ${pdf.pageCount} individual PDF documents (one per page).",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    SplitType.PAGE_RANGE -> {
                                        Column {
                                            Text(
                                                text = "Enter page range expression (e.g. 1-3, 5, 7-${pdf.pageCount}):",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            OutlinedTextField(
                                                value = uiState.splitConfig.rangeExpression,
                                                onValueChange = { viewModel.setRangeExpression(it) },
                                                placeholder = { Text("e.g. 1-2, 4-${minOf(pdf.pageCount, 6)}") },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("split_range_input"),
                                                singleLine = true
                                            )

                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                FilterChip(
                                                    selected = uiState.splitConfig.rangeExpression == "1-${minOf(pdf.pageCount, 3)}",
                                                    onClick = { viewModel.setRangeExpression("1-${minOf(pdf.pageCount, 3)}") },
                                                    label = { Text("First ${minOf(pdf.pageCount, 3)} pages") }
                                                )
                                                if (pdf.pageCount > 1) {
                                                    FilterChip(
                                                        selected = uiState.splitConfig.rangeExpression == "${pdf.pageCount}",
                                                        onClick = { viewModel.setRangeExpression("${pdf.pageCount}") },
                                                        label = { Text("Last page") }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    SplitType.EXTRACT_SELECTED -> {
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "${uiState.splitConfig.selectedPages.size} of ${pdf.pageCount} pages selected",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )

                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    OutlinedButton(
                                                        onClick = { viewModel.selectAllPages() },
                                                        modifier = Modifier.testTag("split_select_all_btn")
                                                    ) {
                                                        Text("All", style = MaterialTheme.typography.labelSmall)
                                                    }
                                                    OutlinedButton(
                                                        onClick = { viewModel.clearPageSelection() },
                                                        modifier = Modifier.testTag("split_clear_sel_btn")
                                                    ) {
                                                        Text("Clear", style = MaterialTheme.typography.labelSmall)
                                                    }
                                                }
                                            }
                                            Text(
                                                text = "Tap any page thumbnail below to toggle it for extraction.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Page Thumbnails Strip
                    item {
                        Column {
                            Text(
                                text = "Page Visual Preview",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                            )

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                items((1..pdf.pageCount).toList()) { pageNumber ->
                                    val isSelected = uiState.splitConfig.selectedPages.contains(pageNumber)
                                    val thumb = uiState.pageThumbnails[pageNumber]

                                    PageThumbnailCard(
                                        pageNumber = pageNumber,
                                        thumbnail = thumb,
                                        isSelected = isSelected,
                                        isSelectable = uiState.splitConfig.splitType == SplitType.EXTRACT_SELECTED,
                                        onClick = {
                                            if (uiState.splitConfig.splitType == SplitType.EXTRACT_SELECTED) {
                                                viewModel.togglePageSelection(pageNumber)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Output Base Name
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Output Files Prefix",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = uiState.splitConfig.outputBaseName,
                                    onValueChange = { viewModel.setOutputBaseName(it) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("split_prefix_input"),
                                    singleLine = true
                                )
                            }
                        }
                    }
                } else {
                    item {
                        MteEmptyState(
                            icon = Icons.Rounded.Description,
                            title = "No PDF selected to split",
                            description = "Choose a PDF file to begin extracting or splitting pages."
                        )
                    }
                }
            }
        }
    }

    // Success Result Dialog
    uiState.result?.let { result ->
        MteResultDialog(
            result = result,
            onDismiss = { viewModel.dismissResult() }
        )
    }
}

@Composable
private fun PageThumbnailCard(
    pageNumber: Int,
    thumbnail: Bitmap?,
    isSelected: Boolean,
    isSelectable: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(110.dp)
            .clickable(enabled = isSelectable, onClick = onClick)
            .border(
                width = if (isSelected && isSelectable) 2.5.dp else 1.dp,
                color = if (isSelected && isSelectable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            )
            .testTag("page_thumb_${pageNumber}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnail != null) {
                    Image(
                        bitmap = thumbnail.asImageBitmap(),
                        contentDescription = "Page $pageNumber",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Loading...", style = MaterialTheme.typography.labelSmall)
                    }
                }

                if (isSelectable) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(22.dp)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Page $pageNumber",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected && isSelectable) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected && isSelectable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
