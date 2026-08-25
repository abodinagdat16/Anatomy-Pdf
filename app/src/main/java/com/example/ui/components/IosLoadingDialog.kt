package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.roundToInt

/**
 * An iOS-styled frosted glass HUD loading dialog with Cupertino activity indicator.
 */
@Composable
fun IosLoadingDialog(
    title: String = "Loading PDF",
    message: String? = null,
    onDismissRequest: (() -> Unit)? = null
) {
    Dialog(
        onDismissRequest = { onDismissRequest?.invoke() },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // iOS Frosted Glass Rounded Box
            Box(
                modifier = Modifier
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(22.dp),
                        ambientColor = Color.Black.copy(alpha = 0.5f),
                        spotColor = Color.Black.copy(alpha = 0.7f)
                    )
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xDF1C1C1E)) // iOS Dark Elevated HUD Glass
                    .border(
                        width = 0.75.dp,
                        color = Color.White.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(22.dp)
                    )
                    .padding(horizontal = 28.dp, vertical = 24.dp)
                    .widthIn(min = 140.dp, max = 220.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // iOS Cupertino Activity Indicator
                    IosCupertinoActivityIndicator(
                        modifier = Modifier.size(36.dp),
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Title
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        letterSpacing = (-0.2).sp
                    )

                    if (!message.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = message,
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Inline iOS Frosted Glass HUD Widget (non-modal)
 */
@Composable
fun IosLoadingHUD(
    message: String = "Loading...",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color.Black.copy(alpha = 0.3f),
                spotColor = Color.Black.copy(alpha = 0.5f)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xDF1C1C1E))
            .border(
                width = 0.75.dp,
                color = Color.White.copy(alpha = 0.18f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 24.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            IosCupertinoActivityIndicator(
                modifier = Modifier.size(32.dp),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Authentic iOS-style 12-blade rotating activity indicator
 */
@Composable
fun IosCupertinoActivityIndicator(
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    val transition = rememberInfiniteTransition(label = "ios_spinner")
    val step by transition.animateFloat(
        initialValue = 0f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ios_spinner_step"
    )

    val currentStep = step.toInt() % 12

    Canvas(modifier = modifier) {
        val count = 12
        val width = size.width
        val height = size.height
        val centerX = width / 2f
        val centerY = height / 2f

        val bladeWidth = width * 0.085f
        val bladeHeight = height * 0.28f
        val cornerRadius = bladeWidth / 2f

        for (i in 0 until count) {
            val angle = i * (360f / count)
            // Calculate iOS opacity decay across the 12 blades
            val distance = (i - currentStep + count) % count
            val alpha = (1f - (distance / count.toFloat()) * 0.75f).coerceIn(0.2f, 1f)

            rotate(degrees = angle, pivot = Offset(centerX, centerY)) {
                drawRoundRect(
                    color = color.copy(alpha = alpha),
                    topLeft = Offset(centerX - bladeWidth / 2f, centerY - height * 0.46f),
                    size = Size(bladeWidth, bladeHeight),
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                )
            }
        }
    }
}
