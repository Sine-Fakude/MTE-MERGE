package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.domain.model.ContactQrData
import com.example.domain.model.QrType
import com.example.domain.model.WifiQrData
import com.example.ui.components.MteTopBar
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.viewmodel.QrViewModel
import com.example.ui.viewmodel.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrGeneratorScreen(
    onNavigateBack: () -> Unit,
    themeMode: ThemeMode,
    onToggleTheme: () -> Unit,
    viewModel: QrViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCustomizer by remember { mutableStateOf(false) }

    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        viewModel.setLogoUri(uri)
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
                title = "QR Code Generator",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                themeMode = themeMode,
                onToggleTheme = onToggleTheme
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.shareQrCode(context) },
                        enabled = uiState.generatedBitmap != null,
                        modifier = Modifier
                            .weight(1.3f)
                            .height(52.dp)
                            .testTag("share_qr_button"),
                        shape = RoundedCornerShape(26.dp), // Large pill-shaped button
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Rounded.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Sheet", fontWeight = FontWeight.Bold)
                    }

                    FilledTonalButton(
                        onClick = { viewModel.saveQrToDevice(context) },
                        enabled = uiState.generatedBitmap != null,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("save_qr_button"),
                        shape = RoundedCornerShape(26.dp)
                    ) {
                        Icon(imageVector = Icons.Rounded.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save")
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
                contentPadding = PaddingValues(top = 10.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Live QR Code Preview Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("qr_preview_card"),
                        shape = RoundedCornerShape(26.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(240.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(uiState.config.backgroundColor))
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (uiState.isGenerating) {
                                    CircularProgressIndicator()
                                } else if (uiState.generatedBitmap != null) {
                                    Image(
                                        bitmap = uiState.generatedBitmap!!.asImageBitmap(),
                                        contentDescription = "Generated QR Code",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("QR Content", uiState.config.content))
                                        Toast.makeText(context, "Copied content to clipboard", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.testTag("copy_qr_content_btn")
                                ) {
                                    Icon(imageVector = Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Copy Text", style = MaterialTheme.typography.labelMedium)
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                OutlinedButton(
                                    onClick = { showCustomizer = !showCustomizer },
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.testTag("toggle_customizer_btn")
                                ) {
                                    Icon(imageVector = Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (showCustomizer) "Hide Styles" else "Customize", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }

                // QR Styling & Customization (Collapsible)
                if (showCustomizer) {
                    item {
                        QrStyleCustomizerCard(
                            foregroundColor = uiState.config.foregroundColor,
                            backgroundColor = uiState.config.backgroundColor,
                            ecLevel = uiState.config.errorCorrectionLevel,
                            withCenterBadge = uiState.config.withCenterBadge,
                            logoUri = uiState.config.logoUri,
                            onPickLogo = {
                                logoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onClearLogo = { viewModel.setLogoUri(null) },
                            onColorsChange = { fg, bg -> viewModel.setColors(fg, bg) },
                            onEcChange = { viewModel.setErrorCorrection(it) },
                            onToggleBadge = { viewModel.toggleCenterBadge(it) }
                        )
                    }
                }

                // QR Content Type Tabs
                item {
                    Column {
                        Text(
                            text = "Content Type",
                            fontFamily = AmiriFontFamily,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                        )

                        val types = QrType.values().toList()
                        ScrollableTabRow(
                            selectedTabIndex = types.indexOf(uiState.config.qrType).coerceAtLeast(0),
                            edgePadding = 0.dp,
                            modifier = Modifier.clip(RoundedCornerShape(14.dp))
                        ) {
                            types.forEachIndexed { _, type ->
                                Tab(
                                    selected = uiState.config.qrType == type,
                                    onClick = { viewModel.setQrType(type) },
                                    text = {
                                        Text(
                                            text = type.label,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (uiState.config.qrType == type) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    modifier = Modifier.testTag("qr_type_tab_${type.name}")
                                )
                            }
                        }
                    }
                }

                // Dynamic Input Form Card
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Enter ${uiState.config.qrType.label} Data",
                                fontFamily = AmiriFontFamily,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            when (uiState.config.qrType) {
                                QrType.TEXT -> {
                                    OutlinedTextField(
                                        value = uiState.rawTextInput,
                                        onValueChange = { viewModel.updateRawText(it) },
                                        label = { Text("Text content") },
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp)
                                            .testTag("qr_text_input"),
                                        maxLines = 5
                                    )
                                }

                                QrType.URL -> {
                                    OutlinedTextField(
                                        value = uiState.rawTextInput,
                                        onValueChange = { viewModel.updateRawText(it) },
                                        label = { Text("Website URL (e.g. https://example.com)") },
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("qr_url_input"),
                                        singleLine = true
                                    )
                                }

                                QrType.WIFI -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        OutlinedTextField(
                                            value = uiState.wifiData.ssid,
                                            onValueChange = { viewModel.updateWifi(uiState.wifiData.copy(ssid = it)) },
                                            label = { Text("Network Name (SSID)") },
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = uiState.wifiData.password,
                                            onValueChange = { viewModel.updateWifi(uiState.wifiData.copy(password = it)) },
                                            label = { Text("Password") },
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            listOf("WPA", "WEP", "nopass").forEach { sec ->
                                                FilterChip(
                                                    selected = uiState.wifiData.security == sec,
                                                    onClick = { viewModel.updateWifi(uiState.wifiData.copy(security = sec)) },
                                                    label = { Text(if (sec == "nopass") "Open" else sec) }
                                                )
                                            }
                                        }
                                    }
                                }

                                QrType.VCARD -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        OutlinedTextField(
                                            value = uiState.contactData.name,
                                            onValueChange = { viewModel.updateContact(uiState.contactData.copy(name = it)) },
                                            label = { Text("Full Name") },
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = uiState.contactData.phone,
                                            onValueChange = { viewModel.updateContact(uiState.contactData.copy(phone = it)) },
                                            label = { Text("Phone Number") },
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = uiState.contactData.email,
                                            onValueChange = { viewModel.updateContact(uiState.contactData.copy(email = it)) },
                                            label = { Text("Email Address") },
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = uiState.contactData.org,
                                            onValueChange = { viewModel.updateContact(uiState.contactData.copy(org = it)) },
                                            label = { Text("Organization / Company") },
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )
                                    }
                                }

                                QrType.EMAIL -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        OutlinedTextField(
                                            value = uiState.emailTo,
                                            onValueChange = { viewModel.updateEmail(it, uiState.emailSubject, uiState.emailBody) },
                                            label = { Text("Recipient Email") },
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = uiState.emailSubject,
                                            onValueChange = { viewModel.updateEmail(uiState.emailTo, it, uiState.emailBody) },
                                            label = { Text("Subject") },
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = uiState.emailBody,
                                            onValueChange = { viewModel.updateEmail(uiState.emailTo, uiState.emailSubject, it) },
                                            label = { Text("Message Body") },
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.fillMaxWidth(),
                                            maxLines = 3
                                        )
                                    }
                                }

                                QrType.PHONE -> {
                                    OutlinedTextField(
                                        value = uiState.phoneNumber,
                                        onValueChange = { viewModel.updatePhone(it) },
                                        label = { Text("Phone Number (with country code)") },
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                }

                                QrType.SMS -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        OutlinedTextField(
                                            value = uiState.smsNumber,
                                            onValueChange = { viewModel.updateSms(it, uiState.smsBody) },
                                            label = { Text("Phone Number") },
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = uiState.smsBody,
                                            onValueChange = { viewModel.updateSms(uiState.smsNumber, it) },
                                            label = { Text("SMS Message Text") },
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.fillMaxWidth(),
                                            maxLines = 3
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QrStyleCustomizerCard(
    foregroundColor: Int,
    backgroundColor: Int,
    ecLevel: String,
    withCenterBadge: Boolean,
    logoUri: Uri?,
    onPickLogo: () -> Unit,
    onClearLogo: () -> Unit,
    onColorsChange: (Int, Int) -> Unit,
    onEcChange: (String) -> Unit,
    onToggleBadge: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val fgPalette = listOf(
        AndroidColor.parseColor("#0F172A") to "Slate",
        AndroidColor.parseColor("#1E40AF") to "Blue",
        AndroidColor.parseColor("#047857") to "Emerald",
        AndroidColor.parseColor("#B91C1C") to "Crimson",
        AndroidColor.parseColor("#6D28D9") to "Violet",
        AndroidColor.parseColor("#B45309") to "Amber"
    )

    val bgPalette = listOf(
        AndroidColor.WHITE to "White",
        AndroidColor.parseColor("#F8FAFC") to "Slate Light",
        AndroidColor.parseColor("#E2E8F0") to "Soft Gray"
    )

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Styling & Custom Branding",
                fontFamily = AmiriFontFamily,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Custom Center Logo Upload Section
            Text("Center Brand Logo Overlay", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(6.dp))

            if (logoUri != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(logoUri).build(),
                            contentDescription = "Logo Preview",
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Custom logo attached", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }

                    IconButton(onClick = onClearLogo) {
                        Icon(imageVector = Icons.Rounded.Close, contentDescription = "Remove logo", tint = MaterialTheme.colorScheme.error)
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onPickLogo,
                    modifier = Modifier.fillMaxWidth().testTag("upload_qr_logo_btn"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Upload Center Logo (Max 25% area)")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Foreground color swatches
            Text("Foreground Color", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(fgPalette) { (colorInt, name) ->
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(colorInt))
                            .border(
                                width = if (foregroundColor == colorInt) 3.dp else 1.dp,
                                color = if (foregroundColor == colorInt) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { onColorsChange(colorInt, backgroundColor) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (foregroundColor == colorInt) {
                            Icon(imageVector = Icons.Rounded.Check, contentDescription = name, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Background color swatches
            Text("Background Color", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(bgPalette) { (colorInt, name) ->
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(colorInt))
                            .border(
                                width = if (backgroundColor == colorInt) 3.dp else 1.dp,
                                color = if (backgroundColor == colorInt) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            )
                            .clickable { onColorsChange(foregroundColor, colorInt) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (backgroundColor == colorInt) {
                            Icon(imageVector = Icons.Rounded.Check, contentDescription = name, tint = Color.DarkGray, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Center Badge Toggle (if no custom logo)
            if (logoUri == null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Center 'MTE' Emblem", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text("Embeds branded badge", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = withCenterBadge,
                        onCheckedChange = onToggleBadge
                    )
                }
            }
        }
    }
}
