package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.anatomy.AnatomyImage
import com.example.data.anatomy.AnatomyRepository
import com.example.data.anatomy.AnatomyStructure
import com.example.data.gemini.ChatMessage
import com.example.data.gemini.GeminiService
import com.example.data.pdf.PdfDocumentItem
import com.example.data.pdf.PdfDocumentManager
import com.example.data.pdf.PdfPageData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
    val currentDocument: PdfDocumentItem = PdfDocumentManager.sampleLectures[0],
    val currentPageIndex: Int = 0,
    val totalPages: Int = 3,
    val pages: List<PdfPageData> = emptyList(),
    val currentPageBitmap: Bitmap? = null,
    val currentPageText: String = "",
    val isLoadingDocument: Boolean = false,
    val isLoadingPage: Boolean = false,
    val allLectures: List<PdfDocumentItem> = PdfDocumentManager.sampleLectures,
    
    // Search
    val isSearchActive: Boolean = false,
    val searchQuery: String = "",
    val searchMatchCount: Int = 0,

    // Selection & Popup menu
    val selectedText: String = "",
    val isSelectionPopupVisible: Boolean = false,
    val selectionContextSentence: String = "",

    // Gemini API Key Dialog
    val isApiKeyDialogOpen: Boolean = false,
    val customApiKey: String = "",

    // Right Drawer: Anatomy Quick Definition & Relations Atlas
    val isRightDrawerOpen: Boolean = false,
    val selectedStructure: AnatomyStructure? = null,
    val structureHistory: List<AnatomyStructure> = emptyList(),
    val isLoadingStructure: Boolean = false,
    val activeZoomImage: AnatomyImage? = null,
    val isAllImagesModalOpen: Boolean = false,

    // Left Drawer: Gemini AI Assistant (Gemini App design)
    val isLeftDrawerOpen: Boolean = false,
    val geminiMessages: List<ChatMessage> = emptyList(),
    val isGeminiThinking: Boolean = false,
    val geminiInputText: String = "",
    val activeGeminiContextText: String? = null,

    // Toast / Feedback message
    val snackbarMessage: String? = null
)

class AnatomyPdfViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            PdfDocumentManager.ensurePresetPdfFiles(getApplication())
            val savedKey = GeminiService.getSavedCustomApiKey(getApplication())
            _uiState.update { it.copy(customApiKey = savedKey) }
            loadDocument(PdfDocumentManager.sampleLectures[0])
            initializeGeminiWelcome()
        }
    }

    private fun initializeGeminiWelcome() {
        val welcomeMsg = ChatMessage(
            isUser = false,
            text = "👋 Welcome to your Medical Anatomy Assistant! I'm powered by Gemini.\n\nHighlight or tap any anatomical term in your PDF (like **Common Carotid Artery**, **Carotid Sheath**, **Circle of Willis**) to ask for mnemonics, clinical correlations, relations, or board exam reviews.\n\n💡 You can also configure your own Gemini API key anytime using the key icon.",
            suggestedQuestions = listOf(
                "High-yield mnemonics for Carotid Branches",
                "Clinical relations inside the Carotid Sheath",
                "How to identify the Carotid Triangle borders",
                "Explain Carotid Sinus vs Carotid Body"
            )
        )
        _uiState.update { it.copy(geminiMessages = listOf(welcomeMsg)) }
    }

    fun loadDocument(item: PdfDocumentItem) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    currentDocument = item,
                    currentPageIndex = 0,
                    isLoadingDocument = true,
                    isSelectionPopupVisible = false
                )
            }
            val loadedPages = PdfDocumentManager.loadAllDocumentPages(getApplication(), item)
            val firstPage = loadedPages.firstOrNull()

            _uiState.update {
                it.copy(
                    pages = loadedPages,
                    totalPages = if (loadedPages.isNotEmpty()) loadedPages.size else item.pageCount,
                    currentPageIndex = 0,
                    currentPageBitmap = firstPage?.bitmap,
                    currentPageText = firstPage?.text ?: "",
                    isLoadingDocument = false
                )
            }
        }
    }

    fun onVisiblePageChanged(pageIndex: Int) {
        if (pageIndex in _uiState.value.pages.indices) {
            val page = _uiState.value.pages[pageIndex]
            _uiState.update {
                it.copy(
                    currentPageIndex = pageIndex,
                    currentPageBitmap = page.bitmap,
                    currentPageText = page.text
                )
            }
        }
    }

    fun openApiKeyDialog() {
        _uiState.update { it.copy(isApiKeyDialogOpen = true) }
    }

    fun closeApiKeyDialog() {
        _uiState.update { it.copy(isApiKeyDialogOpen = false) }
    }

    fun onApiKeySaved(key: String) {
        _uiState.update { it.copy(customApiKey = key, isApiKeyDialogOpen = false) }
        showSnackbar("Gemini API key saved successfully!")
    }

    fun loadFromUri(uri: Uri, fileName: String? = null) {
        val docItem = PdfDocumentItem(
            id = "custom_${System.currentTimeMillis()}",
            title = fileName ?: "Imported Medical PDF",
            subtitle = "Custom Document (${uri.lastPathSegment ?: "PDF"})",
            pageCount = 1,
            uri = uri,
            isPreset = false,
            topicTag = "Imported PDF"
        )
        _uiState.update {
            it.copy(
                allLectures = listOf(docItem) + it.allLectures.filter { l -> !l.id.startsWith("custom_") }
            )
        }
        loadDocument(docItem)
        showSnackbar("Opened PDF: ${docItem.title}")
    }

    fun loadPage(pageIndex: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingPage = true, isSelectionPopupVisible = false) }
            val pageCount = _uiState.value.totalPages
            val safeIndex = pageIndex.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
            val bitmap = PdfDocumentManager.renderPage(safeIndex)
            val docId = _uiState.value.currentDocument.id
            val text = PdfDocumentManager.getPageText(docId, safeIndex)

            _uiState.update {
                it.copy(
                    currentPageIndex = safeIndex,
                    currentPageBitmap = bitmap,
                    currentPageText = text,
                    isLoadingPage = false
                )
            }
        }
    }

    fun nextPage() {
        val next = _uiState.value.currentPageIndex + 1
        if (next < _uiState.value.totalPages) {
            loadPage(next)
        }
    }

    fun previousPage() {
        val prev = _uiState.value.currentPageIndex - 1
        if (prev >= 0) {
            loadPage(prev)
        }
    }

    // --- Text Selection & Contextual Popup ---
    fun onTextSelected(selectedText: String, contextSentence: String = "") {
        val clean = selectedText.trim()
        if (clean.isBlank()) {
            _uiState.update { it.copy(isSelectionPopupVisible = false, selectedText = "") }
            return
        }
        _uiState.update {
            it.copy(
                selectedText = clean,
                selectionContextSentence = if (contextSentence.isNotBlank()) contextSentence else clean,
                isSelectionPopupVisible = true
            )
        }
    }

    fun dismissSelectionPopup() {
        _uiState.update { it.copy(isSelectionPopupVisible = false) }
    }

    // --- Right Drawer: Anatomy Quick Definition & Deep-Dive ---
    fun openAnatomyDefinition(term: String) {
        val query = term.trim()
        if (query.isBlank()) return

        dismissSelectionPopup()
        _uiState.update { it.copy(isRightDrawerOpen = true, isLeftDrawerOpen = false, isLoadingStructure = true) }

        viewModelScope.launch {
            // 1. Check built-in database
            val local = AnatomyRepository.findStructure(query)
            if (local != null) {
                // If local structure has few images, asynchronously enhance with real search results
                val currentStructure = if (local.images.size <= 1) {
                    val searchedImages = com.example.data.anatomy.AnatomyImageSearchService.searchAnatomyImages(local.name)
                    if (searchedImages.isNotEmpty()) {
                        val enhanced = local.copy(images = (local.images + searchedImages).distinctBy { it.imageUrl })
                        AnatomyRepository.saveDynamicStructure(enhanced)
                        enhanced
                    } else local
                } else local

                _uiState.update {
                    it.copy(
                        selectedStructure = currentStructure,
                        structureHistory = if (it.selectedStructure != null && it.selectedStructure.id != currentStructure.id) {
                            it.structureHistory + it.selectedStructure
                        } else it.structureHistory,
                        isLoadingStructure = false
                    )
                }
            } else {
                // 2. Try generating dynamic anatomy card via Gemini AI
                var dynamic = GeminiService.generateAnatomyCard(query, getApplication())
                if (dynamic == null) {
                    // 3. Synthesize smart anatomical structure for the exact query
                    dynamic = AnatomyRepository.createDynamicStructureForTerm(query)
                }

                AnatomyRepository.saveDynamicStructure(dynamic)
                _uiState.update {
                    it.copy(
                        selectedStructure = dynamic,
                        structureHistory = if (it.selectedStructure != null && it.selectedStructure.id != dynamic.id) {
                            it.structureHistory + it.selectedStructure
                        } else it.structureHistory,
                        isLoadingStructure = false
                    )
                }
            }
        }
    }

    fun navigateStructureBack() {
        val history = _uiState.value.structureHistory
        if (history.isNotEmpty()) {
            val previous = history.last()
            _uiState.update {
                it.copy(
                    selectedStructure = previous,
                    structureHistory = history.dropLast(1)
                )
            }
        }
    }

    fun closeRightDrawer() {
        _uiState.update { it.copy(isRightDrawerOpen = false) }
    }

    fun openRightDrawer() {
        if (_uiState.value.selectedStructure == null) {
            openAnatomyDefinition("Common Carotid Artery")
        } else {
            _uiState.update { it.copy(isRightDrawerOpen = true, isLeftDrawerOpen = false) }
        }
    }

    fun openZoomImage(image: AnatomyImage) {
        _uiState.update { it.copy(activeZoomImage = image) }
    }

    fun closeZoomImage() {
        _uiState.update { it.copy(activeZoomImage = null) }
    }

    fun openAllImagesModal() {
        _uiState.update { it.copy(isAllImagesModalOpen = true) }
    }

    fun closeAllImagesModal() {
        _uiState.update { it.copy(isAllImagesModalOpen = false) }
    }

    // --- Left Drawer: Gemini AI Assistant ---
    fun openGeminiWithContext(promptPrefix: String? = null) {
        val context = _uiState.value.selectedText.ifBlank {
            _uiState.value.selectionContextSentence.ifBlank {
                "Topic: ${_uiState.value.currentDocument.title}"
            }
        }
        dismissSelectionPopup()
        _uiState.update {
            it.copy(
                isLeftDrawerOpen = true,
                isRightDrawerOpen = false,
                activeGeminiContextText = context
            )
        }

        if (!promptPrefix.isNullOrBlank()) {
            sendMessageToGemini(promptPrefix)
        }
    }

    fun closeLeftDrawer() {
        _uiState.update { it.copy(isLeftDrawerOpen = false) }
    }

    fun openLeftDrawer() {
        _uiState.update { it.copy(isLeftDrawerOpen = true, isRightDrawerOpen = false) }
    }

    fun updateGeminiInput(text: String) {
        _uiState.update { it.copy(geminiInputText = text) }
    }

    fun sendMessageToGemini(customPrompt: String? = null) {
        val textToSend = (customPrompt ?: _uiState.value.geminiInputText).trim()
        if (textToSend.isBlank()) return

        val userMessage = ChatMessage(
            isUser = true,
            text = textToSend,
            contextText = _uiState.value.activeGeminiContextText
        )

        val updatedList = _uiState.value.geminiMessages + userMessage
        _uiState.update {
            it.copy(
                geminiMessages = updatedList,
                geminiInputText = "",
                isGeminiThinking = true
            )
        }

        viewModelScope.launch {
            val responseText = GeminiService.chatWithGemini(
                messages = updatedList,
                medicalContext = _uiState.value.activeGeminiContextText,
                context = getApplication()
            )

            val aiMessage = ChatMessage(
                isUser = false,
                text = responseText,
                contextText = _uiState.value.activeGeminiContextText
            )

            _uiState.update {
                it.copy(
                    geminiMessages = it.geminiMessages + aiMessage,
                    isGeminiThinking = false
                )
            }
        }
    }

    fun toggleSearchBar() {
        val newState = !_uiState.value.isSearchActive
        _uiState.update {
            it.copy(
                isSearchActive = newState,
                searchQuery = if (!newState) "" else it.searchQuery,
                searchMatchCount = if (!newState) 0 else it.searchMatchCount
            )
        }
    }

    fun openSearchBar() {
        _uiState.update { it.copy(isSearchActive = true) }
    }

    fun setSearchQuery(query: String) {
        val clean = query.trim()
        val matches = if (clean.isNotBlank()) {
            val totalWords = _uiState.value.pages.sumOf { p ->
                p.words.count { w -> w.text.contains(clean, ignoreCase = true) }
            }
            if (totalWords > 0) totalWords else {
                _uiState.value.pages.sumOf { p ->
                    val count = p.text.split(clean, ignoreCase = true).size - 1
                    count.coerceAtLeast(0)
                }
            }
        } else 0

        _uiState.update {
            it.copy(
                searchQuery = query,
                searchMatchCount = matches,
                isSearchActive = true // Keep search bar open even if user backspaces to empty!
            )
        }
    }

    fun clearSearchText() {
        _uiState.update { it.copy(searchQuery = "", searchMatchCount = 0) }
    }

    fun closeSearchBar() {
        _uiState.update { it.copy(searchQuery = "", isSearchActive = false, searchMatchCount = 0) }
    }

    fun clearSearch() {
        closeSearchBar()
    }

    fun showSnackbar(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        PdfDocumentManager.closeCurrentRenderer()
    }
}
