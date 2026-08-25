package com.example.ui.viewer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Translate
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.gemini.GeminiService
import com.example.data.pdf.ExtractedPdfWord
import com.example.data.pdf.PdfPageData
import com.example.data.translate.GoogleTranslateHelper
import com.example.ui.components.AllImagesDialog
import com.example.ui.components.ContextualSelectionMenu
import com.example.ui.components.GeminiApiKeyDialog
import com.example.ui.components.ImageZoomDialog
import com.example.ui.components.IosLoadingDialog
import com.example.ui.components.IosLoadingHUD
import com.example.ui.components.IosCupertinoActivityIndicator
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
                                viewModel.toggleSearchBar()
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
                                    placeholder = { Text("Search text or anatomical terms in PDF...", fontSize = 12.sp) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = GoogleBlue
                                        )
                                    },
                                    trailingIcon = {
                                        if (state.searchQuery.isNotEmpty()) {
                                            IconButton(
                                                onClick = { viewModel.clearSearchText() },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Clear,
                                                    contentDescription = "Clear search text",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = GoogleBlue,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    singleLine = true
                                )
                                if (state.searchMatchCount > 0) {
                                    Spacer(modifier = Modifier.width(8.dp))
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
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(onClick = { viewModel.closeSearchBar() }) {
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
                    IosLoadingHUD(
                        message = "Loading PDF Pages...",
                        modifier = Modifier.align(Alignment.Center)
                    )
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
                                searchQuery = state.searchQuery,
                                onAnatomyClick = { term -> viewModel.openAnatomyDefinition(term) },
                                onAskGemini = { term ->
                                    viewModel.openGeminiWithContext("Explain high-yield anatomical relations, course, and USMLE facts regarding: $term")
                                },
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
                        onTranslate = { text -> GoogleTranslateHelper.translateText(context, text) },
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
 * Individual Page Item with Direct On-PDF Text Selection & Double-Tap (Powered by PDFBox)
 */
@Composable
private fun PdfPageListItem(
    page: PdfPageData,
    searchQuery: String = "",
    onAnatomyClick: (String) -> Unit,
    onAskGemini: (String) -> Unit,
    onTextSelected: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isTranslateAvailable = remember(context) { GoogleTranslateHelper.isGoogleTranslateAvailable(context) }
    var pageBitmap by remember(page.pageIndex, page.bitmap) { mutableStateOf(page.bitmap) }
    var selectedWordIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var activeSelectionText by remember { mutableStateOf("") }
    var menuAnchorNormX by remember { mutableStateOf(0.5f) }
    var menuAnchorNormY by remember { mutableStateOf(0.5f) }
    var dragStartIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(page.pageIndex, page.bitmap) {
        if (pageBitmap == null) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val bmp = com.example.data.pdf.PdfDocumentManager.renderPage(page.pageIndex)
                if (bmp != null) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        pageBitmap = bmp
                    }
                }
            }
        }
    }

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Page Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
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
                    text = "Tap or double-tap words directly on PDF",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Interactive PDF Page Canvas with direct in-PDF text selection
            val currentBmp = pageBitmap ?: page.bitmap
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White)
            ) {
                val boxWidthPx = constraints.maxWidth.toFloat()
                val aspectRatio = if (currentBmp != null && currentBmp.width > 0) {
                    currentBmp.width.toFloat() / currentBmp.height.toFloat()
                } else {
                    595f / 842f
                }
                val boxHeightPx = boxWidthPx / aspectRatio

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(aspectRatio)
                ) {
                    // 1. Rendered High-Resolution PDF Page Image or iOS Spinner
                    if (currentBmp != null) {
                        Image(
                            bitmap = currentBmp.asImageBitmap(),
                            contentDescription = "PDF Page ${page.pageIndex + 1}",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            IosCupertinoActivityIndicator(
                                modifier = Modifier.size(28.dp),
                                color = GoogleBlue
                            )
                        }
                    }

                    // 2. Direct Selection Highlight Canvas overlay directly on top of PDF
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(page.words) {
                                detectTapGestures(
                                    onTap = { offset ->
                                        val normX = offset.x / size.width
                                        val normY = offset.y / size.height
                                        val clickedWordIdx = page.words.indexOfFirst { w ->
                                            normX >= (w.normLeft - 0.015f) && normX <= (w.normRight + 0.015f) &&
                                                    normY >= (w.normTop - 0.015f) && normY <= (w.normBottom + 0.015f)
                                        }

                                        if (clickedWordIdx != -1) {
                                            val w = page.words[clickedWordIdx]
                                            selectedWordIndices = setOf(clickedWordIdx)
                                            activeSelectionText = w.text
                                            menuAnchorNormX = (w.normLeft + w.normRight) / 2f
                                            menuAnchorNormY = w.normTop
                                            onTextSelected(w.text, page.text)
                                        } else {
                                            selectedWordIndices = emptySet()
                                            activeSelectionText = ""
                                        }
                                    },
                                    onDoubleTap = { offset ->
                                        val normX = offset.x / size.width
                                        val normY = offset.y / size.height
                                        val clickedWordIdx = page.words.indexOfFirst { w ->
                                            normX >= (w.normLeft - 0.025f) && normX <= (w.normRight + 0.025f) &&
                                                    normY >= (w.normTop - 0.025f) && normY <= (w.normBottom + 0.025f)
                                        }

                                        if (clickedWordIdx != -1) {
                                            val w = page.words[clickedWordIdx]
                                            val start = maxOf(0, clickedWordIdx - 1)
                                            val end = minOf(page.words.size - 1, clickedWordIdx + 1)
                                            val combinedPhrase = (start..end).joinToString(" ") { page.words[it].text }

                                            val isCompoundAnatomy = combinedPhrase.contains("Artery", true) ||
                                                    combinedPhrase.contains("Nerve", true) ||
                                                    combinedPhrase.contains("Vein", true) ||
                                                    combinedPhrase.contains("Triangle", true) ||
                                                    combinedPhrase.contains("Carotid", true) ||
                                                    combinedPhrase.contains("Sheath", true)

                                            val selected = if (isCompoundAnatomy) {
                                                selectedWordIndices = (start..end).toSet()
                                                menuAnchorNormX = (page.words[start].normLeft + page.words[end].normRight) / 2f
                                                menuAnchorNormY = page.words[start].normTop
                                                combinedPhrase
                                            } else {
                                                selectedWordIndices = setOf(clickedWordIdx)
                                                menuAnchorNormX = (w.normLeft + w.normRight) / 2f
                                                menuAnchorNormY = w.normTop
                                                w.text
                                            }

                                            activeSelectionText = selected
                                            onTextSelected(selected, page.text)
                                        }
                                    }
                                )
                            }
                            .pointerInput(page.words) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        val normX = offset.x / size.width
                                        val normY = offset.y / size.height
                                        val foundIdx = page.words.indexOfFirst { w ->
                                            normX >= (w.normLeft - 0.02f) && normX <= (w.normRight + 0.02f) &&
                                                    normY >= (w.normTop - 0.02f) && normY <= (w.normBottom + 0.02f)
                                        }
                                        dragStartIndex = if (foundIdx != -1) foundIdx else null
                                        if (foundIdx != -1) {
                                            selectedWordIndices = setOf(foundIdx)
                                        }
                                    },
                                    onDrag = { change, _ ->
                                        val startIdx = dragStartIndex
                                        if (startIdx != null) {
                                            val normX = change.position.x / size.width
                                            val normY = change.position.y / size.height
                                            val currentIdx = page.words.indexOfFirst { w ->
                                                normX >= (w.normLeft - 0.03f) && normX <= (w.normRight + 0.03f) &&
                                                        normY >= (w.normTop - 0.03f) && normY <= (w.normBottom + 0.03f)
                                            }
                                            if (currentIdx != -1) {
                                                val minIdx = minOf(startIdx, currentIdx)
                                                val maxIdx = maxOf(startIdx, currentIdx)
                                                selectedWordIndices = (minIdx..maxIdx).toSet()
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        if (selectedWordIndices.isNotEmpty()) {
                                            val sorted = selectedWordIndices.sorted()
                                            val text = sorted.joinToString(" ") { page.words[it].text }
                                            activeSelectionText = text
                                            val first = page.words[sorted.first()]
                                            val last = page.words[sorted.last()]
                                            menuAnchorNormX = ((first.normLeft + last.normRight) / 2f).coerceIn(0.1f, 0.9f)
                                            menuAnchorNormY = first.normTop
                                            onTextSelected(text, page.text)
                                        }
                                        dragStartIndex = null
                                    },
                                    onDragCancel = {
                                        dragStartIndex = null
                                    }
                                )
                            }
                    ) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height

                        // 1. Draw Search Matches directly on PDF Canvas (Bright yellow/amber highlight)
                        val trimmedQuery = searchQuery.trim()
                        if (trimmedQuery.isNotBlank()) {
                            page.words.forEach { word ->
                                if (word.text.contains(trimmedQuery, ignoreCase = true)) {
                                    val left = word.normLeft * canvasWidth
                                    val top = word.normTop * canvasHeight
                                    val right = word.normRight * canvasWidth
                                    val bottom = word.normBottom * canvasHeight
                                    val width = (right - left).coerceAtLeast(10f)
                                    val height = (bottom - top).coerceAtLeast(12f)

                                    // Bright translucent yellow highlight
                                    drawRoundRect(
                                        color = Color(0x99FFEB3B),
                                        topLeft = Offset(left - 2f, top - 1f),
                                        size = Size(width + 4f, height + 2f),
                                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                                    )
                                    // Deep amber crisp border
                                    drawRoundRect(
                                        color = Color(0xFFF57F17),
                                        topLeft = Offset(left - 2f, top - 1f),
                                        size = Size(width + 4f, height + 2f),
                                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                                        style = Stroke(width = 1.2.dp.toPx())
                                    )
                                }
                            }
                        }

                        // 2. Draw user selection highlights directly over selected words on the PDF
                        selectedWordIndices.forEach { idx ->
                            val word = page.words.getOrNull(idx) ?: return@forEach
                            val left = word.normLeft * canvasWidth
                            val top = word.normTop * canvasHeight
                            val right = word.normRight * canvasWidth
                            val bottom = word.normBottom * canvasHeight
                            val width = (right - left).coerceAtLeast(10f)
                            val height = (bottom - top).coerceAtLeast(12f)

                            drawRoundRect(
                                color = Color(0x551A73E8),
                                topLeft = Offset(left, top),
                                size = Size(width, height),
                                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                            )
                            drawRoundRect(
                                color = Color(0xFF1A73E8),
                                topLeft = Offset(left, top),
                                size = Size(width, height),
                                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                        }
                    }

                    // 3. Floating In-PDF Action Toolbar anchored directly above the selected text
                    if (selectedWordIndices.isNotEmpty() && activeSelectionText.isNotBlank()) {
                        val density = LocalDensity.current
                        val menuX = (menuAnchorNormX * boxWidthPx).coerceIn(110f, boxWidthPx - 110f)
                        val menuY = ((menuAnchorNormY * boxHeightPx) - with(density) { 48.dp.toPx() }).coerceAtLeast(6f)

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = Color(0xFF202124),
                                shadowElevation = 8.dp,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .offset {
                                        IntOffset(
                                            (menuX - with(density) { 100.dp.toPx() }).toInt(),
                                            menuY.toInt()
                                        )
                                    }
                                    .zIndex(15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Ask Gemini Button
                                    Surface(
                                        color = Color(0xFF333537),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.clickable {
                                            onAskGemini(activeSelectionText)
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = "Ask Gemini",
                                                tint = Color(0xFF8AB4F8),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "Gemini",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }

                                    // Anatomy Lookup Button
                                    Surface(
                                        color = Color(0xFF333537),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.clickable {
                                            onAnatomyClick(activeSelectionText)
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LocalHospital,
                                                contentDescription = "Anatomy Lookup",
                                                tint = Color(0xFF81C995),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "Anatomy",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }

                                    // Google Translate Button (Shown if Google Translate is available)
                                    if (isTranslateAvailable) {
                                        Surface(
                                            color = Color(0xFF333537),
                                            shape = RoundedCornerShape(16.dp),
                                            modifier = Modifier.clickable {
                                                GoogleTranslateHelper.translateText(context, activeSelectionText)
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Translate,
                                                    contentDescription = "Translate with Google Translate",
                                                    tint = Color(0xFF8AB4F8),
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = "Translate",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }

                                    // Copy Button
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Selected Text", activeSelectionText)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "Copied: \"$activeSelectionText\"", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }

                                    // Clear Button
                                    IconButton(
                                        onClick = {
                                            selectedWordIndices = emptySet()
                                            activeSelectionText = ""
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear",
                                            tint = Color.LightGray,
                                            modifier = Modifier.size(14.dp)
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
