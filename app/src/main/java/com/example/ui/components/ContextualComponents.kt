package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.anatomy.AnatomyImage
import com.example.ui.theme.GeminiSparkle
import com.example.ui.theme.GoogleBlue
import com.example.ui.theme.GoogleBlueLight
import com.example.ui.theme.GoogleRed

import androidx.compose.material.icons.filled.Translate
import androidx.compose.ui.platform.LocalContext
import com.example.data.translate.GoogleTranslateHelper

/**
 * Contextual Floating Action Menu that appears when a medical student selects or taps text in the lecture
 */
@Composable
fun ContextualSelectionMenu(
    selectedText: String,
    onQuickDefinition: (String) -> Unit,
    onAskGemini: (String) -> Unit,
    onSearchInDoc: (String) -> Unit,
    onTranslate: ((String) -> Unit)? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val isTranslateAvailable = remember { GoogleTranslateHelper.isGoogleTranslateAvailable(context) }

    Surface(
        modifier = modifier
            .shadow(12.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp)),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .width(if (isTranslateAvailable) 330.dp else 280.dp)
        ) {
            // Header showing selected term
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "\"$selectedText\"",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close popup",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Action Items
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 1. Quick Anatomy Definition (Right Drawer)
                SelectionActionButton(
                    icon = Icons.Default.LocalHospital,
                    label = "Quick Def",
                    tint = GoogleRed,
                    backgroundTint = Color(0xFFFCE8E6),
                    onClick = { onQuickDefinition(selectedText) }
                )

                // 2. Ask Gemini AI (Left Drawer)
                SelectionActionButton(
                    icon = Icons.Default.AutoAwesome,
                    label = "Ask Gemini",
                    tint = GeminiSparkle,
                    backgroundTint = Color(0xFFF3E8FF),
                    onClick = { onAskGemini(selectedText) }
                )

                // 3. Google Translate (Only shown if Google Translate app or handler is available)
                if (isTranslateAvailable) {
                    SelectionActionButton(
                        icon = Icons.Default.Translate,
                        label = "Translate",
                        tint = GoogleBlue,
                        backgroundTint = GoogleBlueLight,
                        onClick = {
                            if (onTranslate != null) {
                                onTranslate(selectedText)
                            } else {
                                GoogleTranslateHelper.translateText(context, selectedText)
                            }
                            onDismiss()
                        }
                    )
                }

                // 4. Copy Text
                SelectionActionButton(
                    icon = Icons.Default.ContentCopy,
                    label = "Copy",
                    tint = GoogleBlue,
                    backgroundTint = GoogleBlueLight,
                    onClick = {
                        clipboardManager.setText(AnnotatedString(selectedText))
                        onDismiss()
                    }
                )

                // 5. Search in Document
                SelectionActionButton(
                    icon = Icons.Default.Search,
                    label = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    backgroundTint = MaterialTheme.colorScheme.surfaceVariant,
                    onClick = { onSearchInDoc(selectedText) }
                )
            }
        }
    }
}

@Composable
private fun SelectionActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    backgroundTint: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(backgroundTint),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Fullscreen Interactive Image Zoom Dialog with pinch-to-zoom and anatomical description
 */
@Composable
fun ImageZoomDialog(
    image: AnatomyImage?,
    onDismiss: () -> Unit
) {
    if (image == null) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
        ) {
            // Close Button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(44.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close image",
                    tint = Color.White
                )
            }

            // Image and title
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1F22)),
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .weight(1f, fill = false)
                        .shadow(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(380.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AnatomyImageViewer(
                            image = image,
                            structureId = image.title,
                            modifier = Modifier.fillMaxSize(),
                            showModeToggle = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title & Description
                Surface(
                    color = Color(0xFF1E1E1E),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(0.95f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ZoomIn,
                                contentDescription = null,
                                tint = GoogleBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = image.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = image.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFB0BEC5)
                        )
                    }
                }
            }
        }
    }
}
