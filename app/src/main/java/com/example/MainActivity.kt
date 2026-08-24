package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewer.PdfViewerScreen
import com.example.viewmodel.AnatomyPdfViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: AnatomyPdfViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle incoming PDF from other apps (e.g. Google Drive, WhatsApp, Files)
        handleIncomingPdfIntent(intent)

        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PdfViewerScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingPdfIntent(intent)
    }

    private fun handleIncomingPdfIntent(intent: Intent?) {
        if (intent == null) return

        val action = intent.action
        val type = intent.type

        if (Intent.ACTION_VIEW == action && intent.data != null) {
            val uri: Uri = intent.data ?: return
            val fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "Opened PDF"
            viewModel.loadFromUri(uri, fileName)
        } else if (Intent.ACTION_SEND == action && type != null) {
            if ("application/pdf" == type || type.startsWith("application/")) {
                val uri: Uri? = intent.getParcelableExtra(Intent.EXTRA_STREAM)
                if (uri != null) {
                    val fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "Shared PDF"
                    viewModel.loadFromUri(uri, fileName)
                }
            }
        }
    }
}
