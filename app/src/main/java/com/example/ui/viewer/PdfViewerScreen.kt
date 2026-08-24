package com.example.ui.viewer

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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.gemini.GeminiService
import com.example.data.pdf.PdfPageData
import com.example.ui.components.AllImagesDialog
import com.example.ui.components.ContextualSelectionMenu
import com.example.ui.components.GeminiApiKeyDialog
import com.example.ui.components.ImageZoomDialog
import com.example.ui.drawers.AnatomyDetailDrawer
import com.example.ui.drawers.GeminiChatDrawer
import com.example.ui.theme.GeminiSparkle
import com.example.ui.theme.GoogleBlue
import com.example.ui.theme.GoogleBlueLight
import com.example.ui.theme.GoogleGreen
import com.example.ui.theme.GoogleRed
import com.example.viewmodel.AnatomyPdfViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    viewModel: AnatomyPdfViewModel,
    onImportPdfRequest: (() -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

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

    // Track visible page index as user scrolls continuous Google Drive style list
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { index ->
                viewModel.onVisiblePageChanged(index)
            }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Column {
                    // Google Workspace Styled Medical TopAppBar
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
                                    text = "${state.currentDocument.topicTag} • ${state.totalPages} Pages (Continuous List)",
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
                            // Gemini API Key Settings
                            IconButton(onClick = { viewModel.openApiKeyDialog() }) {
                                val hasKey = GeminiService.getActiveApiKey(context).isNotBlank()
                                Box {
                                    Icon(
                                        imageVector = Icons.Default.Key,
                                        contentDescription = "Gemini API Key Settings",
                                        tint = if (hasKey) GoogleBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (hasKey) {
                                        Surface(
                                            shape = CircleShape,
                                            color = GoogleGreen,
                                            modifier = Modifier
                                                .size(6.dp)
                                                .align(Alignment.TopEnd)
                                        ) {}
                                    }
                                }
                            }

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
                                        Text(
                                            text = doc.title,
                                            fontWeight = if (doc.id == state.currentDocument.id) FontWeight.Bold else FontWeight.Normal
                                        )
                                        Text(
                                            text = doc.subtitle,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
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

                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Key, contentDescription = null, tint = GeminiSparkle)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Gemini API Key Settings", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                                }
                            },
                            onClick = {
                                showLectureMenu = false
                                viewModel.openApiKeyDialog()
                            }
                        )
                    }
                }
            }
        ) { paddingValues ->
            // MAIN CONTINUOUS PDF LIST VIEW (Like Google Drive PDF Viewer)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color(0xFFE8EAED)),
                contentAlignment = Alignment.TopCenter
            ) {
                if (state.isLoadingDocument) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = GoogleBlue)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Loading Continuous PDF Pages...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp, start = 12.dp, end = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        itemsIndexed(
                            items = if (state.pages.isNotEmpty()) state.pages else listOf(
                                PdfPageData(
                                    pageIndex = 0,
                                    totalPages = 1,
                                    bitmap = state.currentPageBitmap,
                                    text = state.currentPageText
                                )
                            ),
                            key = { index, page -> "${state.currentDocument.id}_page_$index" }
                        ) { pageIndex, pageData ->
                            PdfPageListItem(
                                page = pageData,
                                onAnatomyClick = { term -> viewModel.openAnatomyDefinition(term) },
                                onTextSelected = { selected, contextText ->
                                    viewModel.onTextSelected(selected, contextText)
                                },
                                modifier = Modifier.widthIn(max = 680.dp)
                            )
                        }
                    }

                    // Google Drive Style Floating Page Indicator Badge
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xDD202124),
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Page ${state.currentPageIndex + 1} / ${state.totalPages}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Floating Contextual Action Menu when text is selected / tapped
                AnimatedVisibility(
                    visible = state.isSelectionPopupVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 54.dp)
                        .zIndex(20f)
                ) {
                    ContextualSelectionMenu(
                        selectedText = state.selectedText,
                        onQuickDefinition = { term -> viewModel.openAnatomyDefinition(term) },
                        onAskGemini = { term ->
                            viewModel.openGeminiWithContext("Explain high-yield anatomical relations, course, and USMLE facts regarding: $term")
                        },
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
                onClose = { viewModel.closeLeftDrawer() },
                onOpenApiKeySettings = { viewModel.openApiKeyDialog() }
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

        // Gemini API Key Settings Dialog
        GeminiApiKeyDialog(
            isOpen = state.isApiKeyDialogOpen,
            onDismiss = { viewModel.closeApiKeyDialog() },
            onKeySaved = { key -> viewModel.onApiKeySaved(key) }
        )
    }
}

/**
 * Individual Page Item in the Google Drive continuous PDF scroll list
 */
@Composable
private fun PdfPageListItem(
    page: PdfPageData,
    onAnatomyClick: (String) -> Unit,
    onTextSelected: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Page Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Page ${page.pageIndex + 1} of ${page.totalPages}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Text(
                    text = "Double tap / select text to inspect",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Rendered PDF Page Bitmap
            if (page.bitmap != null) {
                Image(
                    bitmap = page.bitmap.asImageBitmap(),
                    contentDescription = "PDF Page ${page.pageIndex + 1}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.height(14.dp))
                Divider(color = Color(0xFFEEEEEE))
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Key Terms Tag Cloud for quick 1-tap anatomy lookup
            if (page.keyTerms.isNotEmpty()) {
                val scrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "High-Yield:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoogleBlue
                    )
                    page.keyTerms.forEach { term ->
                        Surface(
                            color = GoogleBlueLight,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onAnatomyClick(term) }
                        ) {
                            Text(
                                text = "⚡ $term",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = GoogleBlue,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Selectable Medical Text Section with flawless text selection
            SelectionContainer {
                Text(
                    text = page.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF202124),
                    lineHeight = 22.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
