package com.example.ui.components

import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.widget.TextView
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.linkify.LinkifyPlugin

/**
 * High-performance Markdown rendering component using Markwon library.
 * Supports bold, italics, headings, lists, tables, strikethrough, links, code blocks, and blockquotes.
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    fontSize: TextUnit = 14.sp,
    lineSpacingExtra: Float = 6f,
    lineSpacingMultiplier: Float = 1.15f
) {
    val context = LocalContext.current
    val textColorArgb = color.toArgb()
    val linkColorArgb = MaterialTheme.colorScheme.primary.toArgb()

    val markwon = remember(context) {
        Markwon.builder(context)
            .usePlugin(TablePlugin.create(context))
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(LinkifyPlugin.create())
            .build()
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextView(ctx).apply {
                setTextColor(textColorArgb)
                setLinkTextColor(linkColorArgb)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize.value)
                setLineSpacing(lineSpacingExtra, lineSpacingMultiplier)
                movementMethod = LinkMovementMethod.getInstance()
                isClickable = false
                isLongClickable = false
            }
        },
        update = { textView ->
            textView.setTextColor(textColorArgb)
            textView.setLinkTextColor(linkColorArgb)
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize.value)
            markwon.setMarkdown(textView, markdown)
        }
    )
}
