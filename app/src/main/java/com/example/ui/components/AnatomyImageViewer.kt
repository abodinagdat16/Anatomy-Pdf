package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Schema
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.anatomy.AnatomyImage
import com.example.ui.theme.GoogleBlue
import com.example.ui.theme.GoogleBlueLight
import com.example.ui.theme.GoogleRed

/**
 * Universal Anatomical Image & Vector Schematic Display Component
 * Renders both Web Atlas images (with Wikimedia User-Agent bypass) and rich interactive vector schematics.
 */
@Composable
fun AnatomyImageViewer(
    image: AnatomyImage,
    structureId: String = "",
    modifier: Modifier = Modifier,
    isZoomable: Boolean = false,
    showModeToggle: Boolean = true
) {
    val context = LocalContext.current
    var isSchematicMode by remember { mutableStateOf(false) }

    // Build Coil ImageRequest with User-Agent to avoid 403 Forbidden on Wikimedia/Medical servers
    val imageRequest = remember(image.imageUrl) {
        ImageRequest.Builder(context)
            .data(image.imageUrl)
            .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
            .crossfade(true)
            .build()
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E1F22))
    ) {
        if (!isSchematicMode) {
            SubcomposeAsyncImage(
                model = imageRequest,
                contentDescription = image.title,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = GoogleBlue,
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Loading Atlas...",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                error = {
                    // Fallback to high-yield vector schematic seamlessly when network image fails
                    AnatomyVectorSchematic(
                        structureId = structureId,
                        title = image.title,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            )
        } else {
            AnatomyVectorSchematic(
                structureId = structureId,
                title = image.title,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Top-Right Mode Toggle (Schematic / Atlas Scan)
        if (showModeToggle) {
            Surface(
                color = Color.Black.copy(alpha = 0.75f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { isSchematicMode = !isSchematicMode }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isSchematicMode) Icons.Default.LocalHospital else Icons.Default.Schema,
                        contentDescription = "Toggle Mode",
                        tint = if (isSchematicMode) GoogleBlue else Color(0xFFFFD54F),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isSchematicMode) "Atlas Photo" else "Schematic",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Dedicated Custom Canvas Medical Schematic Diagrams
 * Renders color-coded, labeled anatomical representations (Carotid bifurcation, Sheath cross-section, Triangle, Circle of Willis, etc.)
 */
@Composable
fun AnatomyVectorSchematic(
    structureId: String,
    title: String,
    modifier: Modifier = Modifier
) {
    val cleanId = structureId.lowercase()

    Box(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF13151A), Color(0xFF1C212B))
                )
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            cleanId.contains("carotid_sheath") || cleanId.contains("sheath") -> {
                CarotidSheathSchematic(modifier = Modifier.fillMaxSize())
            }
            cleanId.contains("circle_of_willis") || cleanId.contains("willis") -> {
                CircleOfWillisSchematic(modifier = Modifier.fillMaxSize())
            }
            cleanId.contains("triangle") -> {
                CarotidTriangleSchematic(modifier = Modifier.fillMaxSize())
            }
            cleanId.contains("vagus") || cleanId.contains("nerve") -> {
                VagusNerveSchematic(modifier = Modifier.fillMaxSize())
            }
            cleanId.contains("jugular") || cleanId.contains("vein") -> {
                JugularVeinSchematic(modifier = Modifier.fillMaxSize())
            }
            else -> {
                // Default: Carotid Artery & Bifurcation System
                CarotidArteryBifurcationSchematic(modifier = Modifier.fillMaxSize())
            }
        }

        // Blueprint Watermark Badge
        Surface(
            color = Color(0x99000000),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp)
        ) {
            Text(
                text = "SCHEMATIC BLUEPRINT",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8AB4F8),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun CarotidArteryBifurcationSchematic(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val redArtery = Color(0xFFE53935)
        val deepRed = Color(0xFFB71C1C)
        val lightRed = Color(0xFFFF8A80)

        // 1. Common Carotid Artery (CCA) Stem
        val ccaBottomX = w * 0.45f
        val ccaBottomY = h * 0.88f
        val bifX = w * 0.45f
        val bifY = h * 0.48f

        // Draw CCA Trunk
        drawLine(
            color = redArtery,
            start = Offset(ccaBottomX, ccaBottomY),
            end = Offset(bifX, bifY),
            strokeWidth = 24f,
            cap = StrokeCap.Round
        )

        // Carotid Sinus Bulge at bifurcation
        drawCircle(
            color = deepRed,
            radius = 18f,
            center = Offset(bifX, bifY)
        )

        // 2. Internal Carotid Artery (ICA) - Lateral/Straight branch (0 branches in neck)
        val icaEndX = w * 0.28f
        val icaEndY = h * 0.12f
        val icaPath = Path().apply {
            moveTo(bifX, bifY)
            cubicTo(
                w * 0.35f, h * 0.38f,
                w * 0.30f, h * 0.25f,
                icaEndX, icaEndY
            )
        }
        drawPath(
            path = icaPath,
            color = redArtery,
            style = Stroke(width = 18f, cap = StrokeCap.Round)
        )

        // 3. External Carotid Artery (ECA) - Medial/Anterior branch with 8 branches
        val ecaEndX = w * 0.68f
        val ecaEndY = h * 0.14f
        val ecaPath = Path().apply {
            moveTo(bifX, bifY)
            cubicTo(
                w * 0.55f, h * 0.40f,
                w * 0.62f, h * 0.28f,
                ecaEndX, ecaEndY
            )
        }
        drawPath(
            path = ecaPath,
            color = redArtery,
            style = Stroke(width = 16f, cap = StrokeCap.Round)
        )

        // Superior Thyroid Artery Branch (comes off anterior ECA low)
        drawLine(
            color = lightRed,
            start = Offset(w * 0.52f, h * 0.43f),
            end = Offset(w * 0.72f, h * 0.52f),
            strokeWidth = 8f,
            cap = StrokeCap.Round
        )

        // Lingual Artery Branch
        drawLine(
            color = lightRed,
            start = Offset(w * 0.57f, h * 0.35f),
            end = Offset(w * 0.78f, h * 0.38f),
            strokeWidth = 8f,
            cap = StrokeCap.Round
        )

        // Facial Artery Branch
        drawLine(
            color = lightRed,
            start = Offset(w * 0.60f, h * 0.28f),
            end = Offset(w * 0.82f, h * 0.26f),
            strokeWidth = 8f,
            cap = StrokeCap.Round
        )

        // Occipital Artery Branch (posterior)
        drawLine(
            color = lightRed,
            start = Offset(w * 0.54f, h * 0.32f),
            end = Offset(w * 0.38f, h * 0.26f),
            strokeWidth = 8f,
            cap = StrokeCap.Round
        )

        // Terminal branches of ECA (Maxillary & Superficial Temporal)
        drawLine(
            color = lightRed,
            start = Offset(ecaEndX, ecaEndY),
            end = Offset(w * 0.85f, h * 0.10f), // Maxillary
            strokeWidth = 9f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = lightRed,
            start = Offset(ecaEndX, ecaEndY),
            end = Offset(w * 0.68f, h * 0.04f), // Superficial Temporal
            strokeWidth = 9f,
            cap = StrokeCap.Round
        )
    }

    // Callout labels
    Box(modifier = Modifier.fillMaxSize()) {
        LabelBadge(text = "ICA (No branches)", color = GoogleBlue, modifier = Modifier.align(Alignment.TopStart).padding(start = 12.dp, top = 12.dp))
        LabelBadge(text = "ECA (8 branches)", color = GoogleRed, modifier = Modifier.align(Alignment.TopEnd).padding(end = 12.dp, top = 12.dp))
        LabelBadge(text = "C3/C4 Bifurcation", color = Color(0xFFFFD54F), modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp))
        LabelBadge(text = "Common Carotid (CCA)", color = GoogleRed, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp))
    }
}

@Composable
private fun CarotidSheathSchematic(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.5f

        // 1. Carotid Sheath (Teal Fascial Ring)
        drawCircle(
            color = Color(0xFF00897B).copy(alpha = 0.25f),
            radius = minOf(w, h) * 0.38f,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = Color(0xFF00897B),
            radius = minOf(w, h) * 0.38f,
            center = Offset(cx, cy),
            style = Stroke(width = 5f)
        )

        // 2. Common Carotid Artery (MEDIAL - Red Circle)
        val arteryX = cx - (w * 0.16f)
        val arteryY = cy
        drawCircle(
            color = Color(0xFFE53935),
            radius = minOf(w, h) * 0.14f,
            center = Offset(arteryX, arteryY)
        )
        drawCircle(
            color = Color(0xFFFFCDD2),
            radius = minOf(w, h) * 0.08f,
            center = Offset(arteryX, arteryY)
        )

        // 3. Internal Jugular Vein (LATERAL - Blue Oval, larger lumen)
        val veinX = cx + (w * 0.16f)
        val veinY = cy - (h * 0.02f)
        drawOval(
            color = Color(0xFF1976D2),
            topLeft = Offset(veinX - (w * 0.16f), veinY - (h * 0.20f)),
            size = Size(w * 0.32f, h * 0.40f)
        )
        drawOval(
            color = Color(0xFFBBDEFB),
            topLeft = Offset(veinX - (w * 0.10f), veinY - (h * 0.13f)),
            size = Size(w * 0.20f, h * 0.26f)
        )

        // 4. Vagus Nerve CN X (POSTERIOR - Yellow Dot in Groove)
        val vagusX = cx
        val vagusY = cy + (h * 0.18f)
        drawCircle(
            color = Color(0xFFFBC02D),
            radius = minOf(w, h) * 0.06f,
            center = Offset(vagusX, vagusY)
        )

        // 5. Ansa Cervicalis (ANTERIOR - Green Dot)
        val ansaX = cx - (w * 0.04f)
        val ansaY = cy - (h * 0.26f)
        drawCircle(
            color = Color(0xFF43A047),
            radius = minOf(w, h) * 0.045f,
            center = Offset(ansaX, ansaY)
        )

        // 6. Sympathetic Trunk (BEHIND Sheath - Amber Dot)
        val sympX = arteryX - (w * 0.08f)
        val sympY = vagusY + (h * 0.08f)
        drawCircle(
            color = Color(0xFFFF9800),
            radius = minOf(w, h) * 0.04f,
            center = Offset(sympX, sympY)
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LabelBadge(text = "ANTERIOR: Ansa Cervicalis", color = Color(0xFF43A047), modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp))
        LabelBadge(text = "MEDIAL: CCA", color = GoogleRed, modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp))
        LabelBadge(text = "LATERAL: IJV", color = GoogleBlue, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp))
        LabelBadge(text = "POSTERIOR: Vagus (CN X)", color = Color(0xFFFBC02D), modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp))
    }
}

@Composable
private fun CircleOfWillisSchematic(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.48f

        val red = Color(0xFFE53935)
        val darkRed = Color(0xFFC62828)

        // Anterior Cerebral Arteries (ACA) & ACom
        val acaLeft = Offset(cx - (w * 0.16f), cy - (h * 0.24f))
        val acaRight = Offset(cx + (w * 0.16f), cy - (h * 0.24f))
        val icaLeft = Offset(cx - (w * 0.24f), cy - (h * 0.04f))
        val icaRight = Offset(cx + (w * 0.24f), cy - (h * 0.04f))
        val pcaLeft = Offset(cx - (w * 0.16f), cy + (h * 0.16f))
        val pcaRight = Offset(cx + (w * 0.16f), cy + (h * 0.16f))
        val basilarTop = Offset(cx, cy + (h * 0.24f))
        val basilarBottom = Offset(cx, cy + (h * 0.42f))

        // ACom
        drawLine(color = red, start = acaLeft, end = acaRight, strokeWidth = 10f, cap = StrokeCap.Round)
        // ACAs
        drawLine(color = red, start = acaLeft, end = icaLeft, strokeWidth = 14f, cap = StrokeCap.Round)
        drawLine(color = red, start = acaRight, end = icaRight, strokeWidth = 14f, cap = StrokeCap.Round)
        // PComs
        drawLine(color = red, start = icaLeft, end = pcaLeft, strokeWidth = 10f, cap = StrokeCap.Round)
        drawLine(color = red, start = icaRight, end = pcaRight, strokeWidth = 10f, cap = StrokeCap.Round)
        // PCAs
        drawLine(color = red, start = pcaLeft, end = basilarTop, strokeWidth = 14f, cap = StrokeCap.Round)
        drawLine(color = red, start = pcaRight, end = basilarTop, strokeWidth = 14f, cap = StrokeCap.Round)
        // Basilar Artery
        drawLine(color = darkRed, start = basilarTop, end = basilarBottom, strokeWidth = 20f, cap = StrokeCap.Round)

        // Middle Cerebral Arteries (MCA) branching outward
        drawLine(color = red, start = icaLeft, end = Offset(cx - (w * 0.42f), cy - (h * 0.04f)), strokeWidth = 16f, cap = StrokeCap.Round)
        drawLine(color = red, start = icaRight, end = Offset(cx + (w * 0.42f), cy - (h * 0.04f)), strokeWidth = 16f, cap = StrokeCap.Round)

        // Aneurysm warning dots on ACom & PCom
        drawCircle(color = Color(0xFFFFD54F), radius = 10f, center = Offset(cx, cy - (h * 0.24f)))
        drawCircle(color = Color(0xFFFFD54F), radius = 9f, center = Offset(cx - (w * 0.20f), cy + (h * 0.06f)))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LabelBadge(text = "ACom (Berry Aneurysm)", color = Color(0xFFFFD54F), modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp))
        LabelBadge(text = "MCA", color = GoogleRed, modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp))
        LabelBadge(text = "MCA", color = GoogleRed, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp))
        LabelBadge(text = "Basilar Artery", color = GoogleRed, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp))
    }
}

@Composable
private fun CarotidTriangleSchematic(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Triangle vertices:
        // Top: Digastric muscle
        // Bottom: Omohyoid
        // Posterior: Sternocleidomastoid (SCM)
        val pDigastric = Offset(w * 0.50f, h * 0.15f)
        val pOmohyoid = Offset(w * 0.78f, h * 0.82f)
        val pScm = Offset(w * 0.18f, h * 0.65f)

        val triPath = Path().apply {
            moveTo(pDigastric.x, pDigastric.y)
            lineTo(pOmohyoid.x, pOmohyoid.y)
            lineTo(pScm.x, pScm.y)
            close()
        }

        // Fill triangle space
        drawPath(path = triPath, color = Color(0xFFE65100).copy(alpha = 0.20f))
        drawPath(path = triPath, color = Color(0xFFE65100), style = Stroke(width = 4f))

        // Neurovascular bundle inside triangle
        drawLine(
            color = Color(0xFFE53935),
            start = Offset(w * 0.40f, h * 0.75f),
            end = Offset(w * 0.48f, h * 0.25f),
            strokeWidth = 16f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFF1976D2),
            start = Offset(w * 0.30f, h * 0.70f),
            end = Offset(w * 0.36f, h * 0.28f),
            strokeWidth = 18f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFFFBC02D),
            start = Offset(w * 0.35f, h * 0.72f),
            end = Offset(w * 0.42f, h * 0.26f),
            strokeWidth = 6f,
            cap = StrokeCap.Round
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LabelBadge(text = "Digastric Posterior Belly", color = Color(0xFF8E24AA), modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp))
        LabelBadge(text = "SCM Anterior Border", color = Color(0xFF8E24AA), modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp))
        LabelBadge(text = "Omohyoid Superior Belly", color = Color(0xFF8E24AA), modifier = Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 8.dp))
    }
}

@Composable
private fun VagusNerveSchematic(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val yellowNerve = Color(0xFFFBC02D)
        val redArtery = Color(0xFFE53935)

        // Main Vagus Nerve Trunk
        val vagusPath = Path().apply {
            moveTo(w * 0.5f, h * 0.10f)
            cubicTo(
                w * 0.48f, h * 0.35f,
                w * 0.52f, h * 0.60f,
                w * 0.50f, h * 0.90f
            )
        }
        drawPath(path = vagusPath, color = yellowNerve, style = Stroke(width = 12f, cap = StrokeCap.Round))

        // Superior Laryngeal Nerve branching
        val slnPath = Path().apply {
            moveTo(w * 0.49f, h * 0.30f)
            cubicTo(w * 0.62f, h * 0.34f, w * 0.72f, h * 0.38f, w * 0.82f, h * 0.42f)
        }
        drawPath(path = slnPath, color = Color(0xFFFFD54F), style = Stroke(width = 7f, cap = StrokeCap.Round))

        // Recurrent Laryngeal Nerve looping under great vessel
        drawCircle(color = redArtery, radius = 22f, center = Offset(w * 0.50f, h * 0.75f)) // Subclavian / Aortic Arch

        val rlnPath = Path().apply {
            moveTo(w * 0.50f, h * 0.78f)
            cubicTo(w * 0.56f, h * 0.84f, w * 0.65f, h * 0.75f, w * 0.65f, h * 0.55f)
        }
        drawPath(path = rlnPath, color = yellowNerve, style = Stroke(width = 8f, cap = StrokeCap.Round))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LabelBadge(text = "Jugular Foramen (CN X)", color = Color(0xFFFBC02D), modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp))
        LabelBadge(text = "Superior Laryngeal Nerve", color = Color(0xFFFFD54F), modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp))
        LabelBadge(text = "Recurrent Laryngeal (Loops under Arch/Subclavian)", color = Color(0xFFFBC02D), modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp))
    }
}

@Composable
private fun JugularVeinSchematic(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val blue = Color(0xFF1976D2)
        val lightBlue = Color(0xFF64B5F6)

        // IJV Trunk
        drawLine(
            color = blue,
            start = Offset(w * 0.5f, h * 0.12f),
            end = Offset(w * 0.5f, h * 0.82f),
            strokeWidth = 28f,
            cap = StrokeCap.Round
        )

        // Superior bulb
        drawCircle(color = blue, radius = 20f, center = Offset(w * 0.5f, h * 0.14f))

        // Tributaries (Common facial, lingual, thyroid)
        drawLine(color = lightBlue, start = Offset(w * 0.5f, h * 0.38f), end = Offset(w * 0.80f, h * 0.32f), strokeWidth = 10f, cap = StrokeCap.Round)
        drawLine(color = lightBlue, start = Offset(w * 0.5f, h * 0.48f), end = Offset(w * 0.78f, h * 0.46f), strokeWidth = 9f, cap = StrokeCap.Round)
        drawLine(color = lightBlue, start = Offset(w * 0.5f, h * 0.58f), end = Offset(w * 0.75f, h * 0.60f), strokeWidth = 9f, cap = StrokeCap.Round)

        // Subclavian Vein Confluence at bottom
        drawLine(color = blue, start = Offset(w * 0.20f, h * 0.84f), end = Offset(w * 0.50f, h * 0.82f), strokeWidth = 22f, cap = StrokeCap.Round)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LabelBadge(text = "Superior Bulb (Sigmoid Sinus)", color = GoogleBlue, modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp))
        LabelBadge(text = "Facial & Thyroid Tributaries", color = GoogleBlue, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp))
        LabelBadge(text = "Venous Angle (Pirogoff's Angle)", color = GoogleBlue, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp))
    }
}

@Composable
private fun LabelBadge(text: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        color = Color(0xDD000000),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.6f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}
