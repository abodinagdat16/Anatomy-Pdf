package com.example.ui.viewer

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.pdf.PdfDocumentItem
import com.example.ui.components.AllImagesDialog
import com.example.ui.components.ContextualSelectionMenu
import com.example.ui.components.ImageZoomDialog
import com.example.ui.drawers.AnatomyDetailDrawer
import com.example.ui.drawers.GeminiChatDrawer
import com.example.ui.theme.GeminiSparkle
import com.example.ui.theme.GoogleBlue
import com.example.ui.theme.GoogleBlueLight
import com.example.ui.theme.GoogleRed
import com.example.viewmodel.AnatomyPdfViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    viewModel: AnatomyPdfViewModel,
    onImportPdfRequest: (() -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var showLectureMenu by remember { mutableStateOf(false) }

    // File picker contract for opening user PDFs
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "Imported Medical PDF"
            viewModel.loadFromUri(uri, fileName)
        }
    }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Column {
                    // Google Apps styled Top Bar
                    TopAppBar(
                        title = {
                            Column(
                                modifier = Modifier
                                    .clickable { showLectureMenu = true }
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = state.currentDocument.title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Switch Lecture",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "${state.currentDocument.topicTag} • Page ${state.currentPageIndex + 1} of ${state.totalPages}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { showLectureMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = "Lectures",
                                    tint = GoogleBlue
                                )
                            }
                        },
                        actions = {
                            // Search in PDF
                            IconButton(onClick = {
                                if (state.isSearchActive) viewModel.clearSearch()
                                else viewModel.setSearchQuery("carotid")
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search text",
                                    tint = if (state.isSearchActive) GoogleBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Open Left Drawer: Gemini AI Assistant
                            IconButton(onClick = { viewModel.openLeftDrawer() }) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Gemini AI",
                                    tint = GeminiSparkle
                                )
                            }

                            // Open Right Drawer: Anatomy Quick Definition & Atlas
                            IconButton(onClick = { viewModel.openRightDrawer() }) {
                                Icon(
                                    imageVector = Icons.Default.LocalHospital,
                                    contentDescription = "Anatomy Quick Lookup",
                                    tint = GoogleRed
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    // Search Query Field if active
                    AnimatedVisibility(visible = state.isSearchActive) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = state.searchQuery,
                                    onValueChange = { viewModel.setSearchQuery(it) },
                                    placeholder = { Text("Search anatomical terms in lecture...", fontSize = 12.sp) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = GoogleBlue,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                if (state.searchMatchCount > 0) {
                                    Surface(
                                        color = GoogleBlueLight,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = "${state.searchMatchCount} found",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GoogleBlue,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                IconButton(onClick = { viewModel.clearSearch() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close search")
                                }
                            }
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    // Lecture Dropdown Menu
                    DropdownMenu(
                        expanded = showLectureMenu,
                        onDismissRequest = { showLectureMenu = false }
                    ) {
                        Text(
                            text = "Anatomy Lecture Library",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = GoogleBlue,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        state.allLectures.forEach { doc ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(doc.title, fontWeight = if (doc.id == state.currentDocument.id) FontWeight.Bold else FontWeight.Normal)
                                        Text(doc.subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                onClick = {
                                    showLectureMenu = false
                                    viewModel.loadDocument(doc)
                                }
                            )
                        }

                        Divider(modifier = Modifier.padding(vertical = 4.dp))

                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = GoogleBlue)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Open PDF from Device / Drive", color = GoogleBlue, fontWeight = FontWeight.SemiBold)
                                }
                            },
                            onClick = {
                                showLectureMenu = false
                                pdfPickerLauncher.launch(arrayOf("application/pdf"))
                            }
                        )
                    }
                }
            },
            bottomBar = {
                // Bottom Page Scrubber & Navigation Bar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        // Quick Anatomical Structure Chips Row
                        val quickAnatomyTerms = listOf(
                            "Common Carotid Artery",
                            "Carotid Sheath",
                            "Carotid Triangle",
                            "External Carotid Artery",
                            "Internal Carotid Artery",
                            "Vagus Nerve",
                            "Circle of Willis"
                        )
                        val chipScrollState = rememberScrollState()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(chipScrollState)
                                .padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Quick Atlas:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            quickAnatomyTerms.forEach { term ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (state.selectedStructure?.name?.contains(term, ignoreCase = true) == true) GoogleBlueLight else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (state.selectedStructure?.name?.contains(term, ignoreCase = true) == true) GoogleBlue else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { viewModel.openAnatomyDefinition(term) }
                                ) {
                                    Text(
                                        text = term,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (state.selectedStructure?.name?.contains(term, ignoreCase = true) == true) GoogleBlue else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // Page navigation buttons & indicator
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = { viewModel.previousPage() },
                                enabled = state.currentPageIndex > 0
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Previous Page",
                                    tint = if (state.currentPageIndex > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                            ) {
                                Text(
                                    text = "Page ${state.currentPageIndex + 1} of ${state.totalPages}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                )
                            }

                            IconButton(
                                onClick = { viewModel.nextPage() },
                                enabled = state.currentPageIndex < state.totalPages - 1
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Next Page",
                                    tint = if (state.currentPageIndex < state.totalPages - 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            // MAIN PDF CANVAS & INTERACTIVE VIEWPORT
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color(0xFFE8EAED)),
                contentAlignment = Alignment.Center
            ) {
                if (state.isLoadingPage) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = GoogleBlue)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Rendering High-Resolution PDF...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    PdfPageCanvas(
                        state = state,
                        onAnatomyClick = { term -> viewModel.onTextSelected(term) },
                        onTextSelection = { selected, context -> viewModel.onTextSelected(selected, context) },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Floating Contextual Action Menu when text is selected / tapped
                AnimatedVisibility(
                    visible = state.isSelectionPopupVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                        .zIndex(20f)
                ) {
                    ContextualSelectionMenu(
                        selectedText = state.selectedText,
                        onQuickDefinition = { term -> viewModel.openAnatomyDefinition(term) },
                        onAskGemini = { term -> viewModel.openGeminiWithContext("Explain high-yield clinical anatomy regarding: $term") },
                        onSearchInDoc = { term -> viewModel.setSearchQuery(term) },
                        onDismiss = { viewModel.dismissSelectionPopup() }
                    )
                }
            }
        }

        // LEFT DRAWER: GEMINI AI ASSISTANT (Gemini App Design)
        AnimatedVisibility(
            visible = state.isLeftDrawerOpen,
            enter = slideInHorizontally(initialOffsetX = { -it }),
            exit = slideOutHorizontally(targetOffsetX = { -it }),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .zIndex(30f)
        ) {
            GeminiChatDrawer(
                messages = state.geminiMessages,
                isThinking = state.isGeminiThinking,
                inputText = state.geminiInputText,
                activeContextText = state.activeGeminiContextText,
                onInputChange = { viewModel.updateGeminiInput(it) },
                onSendMessage = { prompt -> viewModel.sendMessageToGemini(prompt) },
                onClose = { viewModel.closeLeftDrawer() }
            )
        }

        // RIGHT DRAWER: ANATOMY QUICK DEFINITION & ATLAS DEEP-DIVE
        AnimatedVisibility(
            visible = state.isRightDrawerOpen,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .zIndex(30f)
        ) {
            AnatomyDetailDrawer(
                structure = state.selectedStructure,
                isLoading = state.isLoadingStructure,
                hasBackHistory = state.structureHistory.isNotEmpty(),
                onBackHistory = { viewModel.navigateStructureBack() },
                onClose = { viewModel.closeRightDrawer() },
                onBranchClick = { branchId -> viewModel.openAnatomyDefinition(branchId) },
                onImageClick = { img -> viewModel.openZoomImage(img) },
                onSeeAllImages = { viewModel.openAllImagesModal() },
                onAskGemini = { prompt -> viewModel.openGeminiWithContext(prompt) }
            )
        }

        // Image Zoom Modal
        ImageZoomDialog(
            image = state.activeZoomImage,
            onDismiss = { viewModel.closeZoomImage() }
        )

        // All Images Gallery Modal
        if (state.isAllImagesModalOpen && state.selectedStructure != null) {
            AllImagesDialog(
                structureTitle = state.selectedStructure!!.name,
                images = state.selectedStructure!!.images,
                onImageClick = { img ->
                    viewModel.closeAllImagesModal()
                    viewModel.openZoomImage(img)
                },
                onDismiss = { viewModel.closeAllImagesModal() }
            )
        }
    }
}

/**
 * Interactive PDF Page Viewport with Pinch-to-Zoom, Pan, and clickable anatomical terms overlay
 */
@Composable
private fun PdfPageCanvas(
    state: com.example.viewmodel.UiState,
    onAnatomyClick: (String) -> Unit,
    onTextSelection: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val pageScrollState = rememberScrollState()

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 3.5f)
                    val extraWidth = (size.width * (scale - 1)) / 2
                    val extraHeight = (size.height * (scale - 1)) / 2
                    offset = Offset(
                        x = (offset.x + pan.x).coerceIn(-extraWidth, extraWidth),
                        y = (offset.y + pan.y).coerceIn(-extraHeight, extraHeight)
                    )
                }
            },
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(pageScrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // PDF Page Sheet Container
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
                    .shadow(8.dp)
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Rendered PDF Bitmap if available
                    if (state.currentPageBitmap != null) {
                        Image(
                            bitmap = state.currentPageBitmap.asImageBitmap(),
                            contentDescription = "PDF Page ${state.currentPageIndex + 1}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = Color(0xFFE0E0E0))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Interactive Selectable Medical Text Section
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = GoogleBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Interactive Lecture Reader (Tap any term or highlight text below):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoogleBlue
                        )
                    }

                    // Interactive Term Fast Links for Lecture
                    val keyTerms = listOf(
                        "Common Carotid Artery",
                        "Carotid Sheath",
                        "Carotid Triangle",
                        "Internal Carotid Artery",
                        "External Carotid Artery",
                        "Internal Jugular Vein",
                        "Vagus Nerve",
                        "Circle of Willis"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        keyTerms.take(4).forEach { term ->
                            Surface(
                                color = GoogleBlueLight,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onAnatomyClick(term) }
                            ) {
                                Text(
                                    text = "📌 $term",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = GoogleBlue,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Selectable Text Container
                    SelectionContainer {
                        Text(
                            text = state.currentPageText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF202124),
                            lineHeight = 22.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
