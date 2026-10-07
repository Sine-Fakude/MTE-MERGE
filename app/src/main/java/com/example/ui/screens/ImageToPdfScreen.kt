package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.domain.model.CompressionLevel
import com.example.domain.model.ImageItem
import com.example.domain.model.PageMargin
import com.example.domain.model.PageOrientation
import com.example.domain.model.PageSizeFormat
import com.example.domain.util.ShareManager
import com.example.ui.components.MteEmptyState
import com.example.ui.components.MteResultDialog
import com.example.ui.components.MteTopBar
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.viewmodel.ImageToPdfViewModel
import com.example.ui.viewmodel.ThemeMode
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageToPdfScreen(
    onNavigateBack: () -> Unit,
    themeMode: ThemeMode,
    onToggleTheme: () -> Unit,
    viewModel: ImageToPdfViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var passwordVisible by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.addImages(context, uris)
        }
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
                title = "Image to PDF",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                themeMode = themeMode,
                onToggleTheme = onToggleTheme
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (uiState.selectedImages.isNotEmpty()) {
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
                            onClick = { viewModel.convert(context) },
                            enabled = !uiState.isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("perform_image_to_pdf_button"),
                            shape = RoundedCornerShape(27.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(uiState.statusMessage.ifBlank { "Converting..." }, style = MaterialTheme.typography.labelLarge)
                            } else {
                                Icon(imageVector = Icons.Rounded.PictureAsPdf, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (uiState.config.isEncrypted) "Convert & Encrypt (${uiState.selectedImages.size} Photos)" else "Convert ${uiState.selectedImages.size} Photos to PDF",
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
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header Picker Card (Spans full width)
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("image_picker_card"),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Convert Photos to Compact PDF",
                                fontFamily = AmiriFontFamily,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Select gallery photos, downsample resolution, reorder in the staging grid, and password protect with AES encryption.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    modifier = Modifier.testTag("pick_images_button"),
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    Icon(imageVector = Icons.Rounded.AddPhotoAlternate, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add Images")
                                }

                                if (uiState.selectedImages.isNotEmpty()) {
                                    OutlinedButton(
                                        onClick = { viewModel.clearAll() },
                                        modifier = Modifier.testTag("clear_images_button"),
                                        shape = RoundedCornerShape(24.dp)
                                    ) {
                                        Icon(imageVector = Icons.Rounded.DeleteSweep, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Clear")
                                    }
                                }
                            }
                        }
                    }
                }

                if (uiState.selectedImages.isNotEmpty()) {
                    // Smart PDF Compression Slider Card (Full Width)
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.Speed,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Smart PDF Compression",
                                            fontFamily = AmiriFontFamily,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = uiState.config.compression.title,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = uiState.config.compression.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Compose Slider with 3 states (Fast/Web=0, Medium=1, High Quality=2)
                                Slider(
                                    value = uiState.compressionSliderValue,
                                    onValueChange = { viewModel.setCompressionSlider(it) },
                                    valueRange = 0f..2f,
                                    steps = 1,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("compression_slider")
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Fast (720p)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Medium (1080p)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    Text("Original (100%)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    // Page Layout & Security Card (Full Width)
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Layout & Security Options",
                                    fontFamily = AmiriFontFamily,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Orientation Chips
                                Text("Orientation", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    PageOrientation.values().forEach { orient ->
                                        FilterChip(
                                            selected = uiState.config.orientation == orient,
                                            onClick = { viewModel.setOrientation(orient) },
                                            label = { Text(orient.label, style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Page Format Chips
                                Text("Page Format", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    PageSizeFormat.values().forEach { format ->
                                        FilterChip(
                                            selected = uiState.config.pageSize == format,
                                            onClick = { viewModel.setPageSize(format) },
                                            label = { Text(format.label, style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Output File Name
                                OutlinedTextField(
                                    value = uiState.config.outputFileName,
                                    onValueChange = { viewModel.setOutputFileName(it) },
                                    label = { Text("Output PDF Name") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("image_pdf_output_name_input"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    trailingIcon = { Text(".pdf", modifier = Modifier.padding(end = 12.dp)) }
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Document Security (AES Password Protection)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.Lock,
                                            contentDescription = null,
                                            tint = if (uiState.config.isEncrypted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "Encrypt PDF with Password",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Standard AES-128 / AES-256 security",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Switch(
                                        checked = uiState.config.isEncrypted,
                                        onCheckedChange = { viewModel.setEncrypted(it) },
                                        modifier = Modifier.testTag("image_pdf_encrypt_toggle")
                                    )
                                }

                                AnimatedVisibility(visible = uiState.config.isEncrypted) {
                                    Column(modifier = Modifier.padding(top = 10.dp)) {
                                        OutlinedTextField(
                                            value = uiState.config.password,
                                            onValueChange = { viewModel.setPassword(it) },
                                            label = { Text("PDF Protection Password") },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("image_pdf_password_input"),
                                            singleLine = true,
                                            shape = RoundedCornerShape(14.dp),
                                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                            trailingIcon = {
                                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                                    Icon(
                                                        imageVector = if (passwordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                                        contentDescription = "Toggle password visibility"
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Staging Grid Header
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Visual Staging Grid (${uiState.selectedImages.size} photos)",
                                fontFamily = AmiriFontFamily,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Tap rotate, swap, or 'X'",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Staging Grid Items (2 Columns)
                    itemsIndexed(uiState.selectedImages, key = { _, item -> item.id }) { index, item ->
                        GridImageStagingCard(
                            item = item,
                            index = index,
                            totalItems = uiState.selectedImages.size,
                            onRotate = { viewModel.rotateImage(item) },
                            onMovePrev = { viewModel.moveUp(index) },
                            onMoveNext = { viewModel.moveDown(index) },
                            onRemove = { viewModel.removeImage(item) }
                        )
                    }
                } else {
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                        MteEmptyState(
                            icon = Icons.Rounded.AddPhotoAlternate,
                            title = "No images selected",
                            description = "Tap 'Add Images' above to choose gallery photos to convert."
                        )
                    }
                }
            }
        }
    }

    // Success Result Dialog (Subtle Glassmorphic)
    uiState.result?.let { result ->
        MteResultDialog(
            result = result,
            onDismiss = { viewModel.dismissResult() }
        )
    }
}

@Composable
private fun GridImageStagingCard(
    item: ImageItem,
    index: Int,
    totalItems: Int,
    onRotate: () -> Unit,
    onMovePrev: () -> Unit,
    onMoveNext: () -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                shape = RoundedCornerShape(18.dp)
            )
            .testTag("image_staging_card_${index}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Photo Preview filling top area
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(item.uri)
                    .crossfade(true)
                    .build(),
                contentDescription = "Image preview",
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(item.rotationDegrees.toFloat())
                    .clip(RoundedCornerShape(18.dp)),
                contentScale = ContentScale.Crop
            )

            // Dark gradient overlay for contrast on badges
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Color.Black.copy(alpha = 0.25f)
                    )
            )

            // Index badge top-left
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .size(26.dp)
                    .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                    .align(Alignment.TopStart),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Remove 'X' button top-right (as required)
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = CircleShape,
                modifier = Modifier
                    .padding(6.dp)
                    .align(Alignment.TopEnd)
                    .clickable(onClick = onRemove)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Remove photo",
                    tint = Color.White,
                    modifier = Modifier.padding(4.dp).size(16.dp)
                )
            }

            // Bottom control strip: Rotate + Move arrows
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.70f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (index > 0) Color.White.copy(alpha = 0.25f) else Color.Transparent,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.clickable(enabled = index > 0, onClick = onMovePrev)
                ) {
                    Text(
                        text = "◀",
                        fontSize = 11.sp,
                        color = if (index > 0) Color.White else Color.Transparent,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                IconButton(
                    onClick = onRotate,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RotateRight,
                        contentDescription = "Rotate 90",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Surface(
                    color = if (index < totalItems - 1) Color.White.copy(alpha = 0.25f) else Color.Transparent,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.clickable(enabled = index < totalItems - 1, onClick = onMoveNext)
                ) {
                    Text(
                        text = "▶",
                        fontSize = 11.sp,
                        color = if (index < totalItems - 1) Color.White else Color.Transparent,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
