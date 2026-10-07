package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.domain.util.P2pRole
import com.example.ui.navigation.Screen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ImageToPdfScreen
import com.example.ui.screens.LockPdfScreen
import com.example.ui.screens.MergeScreen
import com.example.ui.screens.NfcKeyEmulatorScreen
import com.example.ui.screens.P2pTransferScreen
import com.example.ui.screens.PdfPreviewScreen
import com.example.ui.screens.QrGeneratorScreen
import com.example.ui.screens.SplitScreen
import com.example.ui.screens.UnlockPdfScreen
import com.example.ui.theme.MteMergeTheme
import com.example.ui.viewmodel.HistoryViewModel
import com.example.ui.viewmodel.ImageToPdfViewModel
import com.example.ui.viewmodel.LockPdfViewModel
import com.example.ui.viewmodel.MergeViewModel
import com.example.ui.viewmodel.P2pViewModel
import com.example.ui.viewmodel.PdfPreviewViewModel
import com.example.ui.viewmodel.QrViewModel
import com.example.ui.viewmodel.SplitViewModel
import com.example.ui.viewmodel.ThemeViewModel
import com.example.ui.viewmodel.UnlockPdfViewModel
import android.net.Uri
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeViewModel: ThemeViewModel = viewModel()
            val themeMode by themeViewModel.themeMode.collectAsState()

            MteMergeTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MteMergeAppContent(
                        themeViewModel = themeViewModel
                    )
                }
            }
        }
    }
}

@Composable
fun MteMergeAppContent(
    themeViewModel: ThemeViewModel
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val themeMode by themeViewModel.themeMode.collectAsState()

    val historyViewModel: HistoryViewModel = viewModel()
    val mergeViewModel: MergeViewModel = viewModel()
    val splitViewModel: SplitViewModel = viewModel()
    val imageToPdfViewModel: ImageToPdfViewModel = viewModel()
    val qrViewModel: QrViewModel = viewModel()
    val lockPdfViewModel: LockPdfViewModel = viewModel()
    val unlockPdfViewModel: UnlockPdfViewModel = viewModel()
    val p2pViewModel: P2pViewModel = viewModel()
    val pdfPreviewViewModel: PdfPreviewViewModel = viewModel()

    var p2pFileToSend by remember { mutableStateOf<File?>(null) }
    var p2pInitialRole by remember { mutableStateOf(P2pRole.RECEIVER) }
    var previewUri by remember { mutableStateOf<Uri?>(null) }
    var previewFileName by remember { mutableStateOf("Document.pdf") }

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                themeMode = themeMode,
                onToggleTheme = { themeViewModel.toggleTheme() },
                onNavigate = { route ->
                    if (route == Screen.P2pShare.route) {
                        p2pFileToSend = null
                        p2pInitialRole = P2pRole.RECEIVER
                    }
                    navController.navigate(route)
                },
                historyViewModel = historyViewModel
            )
        }

        composable(Screen.Merge.route) {
            MergeScreen(
                onNavigateBack = { navController.popBackStack() },
                themeMode = themeMode,
                onToggleTheme = { themeViewModel.toggleTheme() },
                viewModel = mergeViewModel,
                onPreviewPdf = { uri, fileName ->
                    previewUri = uri
                    previewFileName = fileName
                    navController.navigate(Screen.PdfPreview.route)
                }
            )
        }

        composable(Screen.Split.route) {
            SplitScreen(
                onNavigateBack = { navController.popBackStack() },
                themeMode = themeMode,
                onToggleTheme = { themeViewModel.toggleTheme() },
                viewModel = splitViewModel
            )
        }

        composable(Screen.ImageToPdf.route) {
            ImageToPdfScreen(
                onNavigateBack = { navController.popBackStack() },
                themeMode = themeMode,
                onToggleTheme = { themeViewModel.toggleTheme() },
                viewModel = imageToPdfViewModel
            )
        }

        composable(Screen.QrGenerator.route) {
            QrGeneratorScreen(
                onNavigateBack = { navController.popBackStack() },
                themeMode = themeMode,
                onToggleTheme = { themeViewModel.toggleTheme() },
                viewModel = qrViewModel
            )
        }

        composable(Screen.LockPdf.route) {
            LockPdfScreen(
                onNavigateBack = { navController.popBackStack() },
                themeMode = themeMode,
                onToggleTheme = { themeViewModel.toggleTheme() },
                viewModel = lockPdfViewModel
            )
        }

        composable(Screen.UnlockPdf.route) {
            UnlockPdfScreen(
                onNavigateBack = { navController.popBackStack() },
                themeMode = themeMode,
                onToggleTheme = { themeViewModel.toggleTheme() },
                viewModel = unlockPdfViewModel
            )
        }

        composable(Screen.History.route) {
            HistoryScreen(
                onNavigateBack = { navController.popBackStack() },
                themeMode = themeMode,
                onToggleTheme = { themeViewModel.toggleTheme() },
                viewModel = historyViewModel,
                onActionMerge = { uri ->
                    mergeViewModel.prefillWithUri(context, uri)
                    navController.navigate(Screen.Merge.route)
                },
                onActionSplit = { uri ->
                    splitViewModel.selectPdf(context, uri)
                    navController.navigate(Screen.Split.route)
                },
                onActionLock = { uri ->
                    lockPdfViewModel.addPdfFromUri(context, uri)
                    navController.navigate(Screen.LockPdf.route)
                },
                onActionUnlock = { uri ->
                    unlockPdfViewModel.addPdfFromUri(context, uri)
                    navController.navigate(Screen.UnlockPdf.route)
                },
                onActionShareNearby = { file, fileName ->
                    p2pFileToSend = file
                    p2pInitialRole = P2pRole.SENDER
                    navController.navigate(Screen.P2pShare.route)
                },
                onActionPreview = { uri, fileName ->
                    previewUri = uri
                    previewFileName = fileName
                    navController.navigate(Screen.PdfPreview.route)
                }
            )
        }

        composable(Screen.P2pShare.route) {
            P2pTransferScreen(
                onNavigateBack = { navController.popBackStack() },
                themeMode = themeMode,
                onToggleTheme = { themeViewModel.toggleTheme() },
                viewModel = p2pViewModel,
                initialRole = p2pInitialRole,
                fileToSend = p2pFileToSend
            )
        }

        composable(Screen.NfcKeyEmulator.route) {
            NfcKeyEmulatorScreen(
                onNavigateBack = { navController.popBackStack() },
                themeMode = themeMode,
                onToggleTheme = { themeViewModel.toggleTheme() }
            )
        }

        composable(Screen.PdfPreview.route) {
            val uri = previewUri
            if (uri != null) {
                PdfPreviewScreen(
                    uri = uri,
                    fileName = previewFileName,
                    viewModel = pdfPreviewViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onSelectPageRange = { selectedUri ->
                        splitViewModel.selectPdf(context, selectedUri)
                        navController.navigate(Screen.Split.route)
                    },
                    onConfirmKeep = {
                        navController.popBackStack()
                    }
                )
            } else {
                HomeScreen(
                    themeMode = themeMode,
                    onToggleTheme = { themeViewModel.toggleTheme() },
                    onNavigate = { route -> navController.navigate(route) },
                    historyViewModel = historyViewModel
                )
            }
        }
    }
}
