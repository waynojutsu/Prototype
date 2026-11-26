package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.PresenceStatus
import com.example.model.University
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CoralCrimson
import com.example.ui.theme.CrispSnow
import com.example.ui.theme.DeepCyanContainer
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElevatedCarbon
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPula
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.HighestCarbon
import com.example.ui.theme.IceCyanText
import com.example.ui.theme.KalahariAmber
import com.example.ui.theme.MidnightSlate
import com.example.ui.theme.MintPulaText
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.WarmAmberText
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Clean dark social container styled after X / Instagram / Facebook cards with crisp hairline borders.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    accentColor: Color = HairlineBorder,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val borderColor = if (accentColor == ElectricCyan || accentColor == HairlineBorder) {
        HairlineBorder
    } else {
        accentColor.copy(alpha = 0.35f)
    }
    Column(
        modifier = modifier
            .clip(shape)
            .background(CarbonSurface)
            .border(width = 0.8.dp, color = borderColor, shape = shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        content = content
    )
}

/**
 * Clean elevated social surface without fake 3D drop shadows.
 */
@Composable
fun SoftNeuCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(shape)
            .background(CarbonSurface)
            .border(width = 0.8.dp, color = HairlineBorder, shape = shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        content = content
    )
}

/**
 * Clean social pill badge (used for verified tags, price pills, and post labels).
 */
@Composable
fun NeubrutalBadge(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(50),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = contentColor,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

/**
 * Clean circular social icon badge replacing heavy 3D clay pods.
 */
@Composable
fun ClayIconPod(
    icon: ImageVector,
    contentDescription: String,
    containerColor: Color,
    iconTint: Color,
    size: Dp = 42.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(containerColor)
            .border(0.8.dp, HairlineBorder, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(size * 0.50f)
        )
    }
}

@Composable
fun VerifiedStudentIcon(
    modifier: Modifier = Modifier,
    size: Dp = 15.dp
) {
    Icon(
        imageVector = Icons.Filled.Verified,
        contentDescription = "Verified .ac.bw student",
        tint = ElectricCyan,
        modifier = modifier.size(size)
    )
}

@Composable
fun UniversityBadge(
    universityCode: String,
    modifier: Modifier = Modifier
) {
    val (bg, fg) = when (universityCode.uppercase()) {
        "UB" -> DeepCyanContainer to IceCyanText
        "BIUST" -> EmeraldContainer to MintPulaText
        "BAC" -> AmberContainer to WarmAmberText
        "BOTHO" -> Color(0xFF281A42) to Color(0xFFD8B4FE)
        "BUAN" -> Color(0xFF133626) to Color(0xFF86EFAC)
        else -> ElevatedCarbon to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        modifier = modifier,
        color = bg,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = if (universityCode == "ALL") "ALL BW" else universityCode.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = fg,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun CampusFilterStrip(
    selectedCampus: String,
    onSelectCampus: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        "ALL" to "All",
        University.UB.code to "UB",
        University.BIUST.code to "BIUST",
        University.BAC.code to "BAC",
        University.BOTHO.code to "Botho",
        University.BUAN.code to "BUAN"
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { (code, label) ->
            val selected = selectedCampus == code
            FilterChip(
                selected = selected,
                onClick = { onSelectCampus(code) },
                shape = RoundedCornerShape(50),
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CrispSnow,
                    selectedLabelColor = ObsidianVoid,
                    containerColor = CarbonSurface,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    borderColor = HairlineBorder,
                    selectedBorderColor = CrispSnow
                ),
                modifier = Modifier.testTag("campus_filter_$code")
            )
        }
    }
}

@Composable
fun PresenceStatusDot(
    status: PresenceStatus,
    size: Dp = 10.dp,
    borderWidth: Dp = 1.5.dp,
    modifier: Modifier = Modifier
) {
    val dotColor = when (status) {
        PresenceStatus.ONLINE -> EmeraldPula
        PresenceStatus.AWAY -> KalahariAmber
        PresenceStatus.OFFLINE -> Color(0xFF536471)
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(dotColor)
            .border(borderWidth, ObsidianVoid, CircleShape)
            .semantics { contentDescription = "Status: ${status.label}" }
    )
}

@Composable
fun PresenceStatusPill(
    status: PresenceStatus,
    modifier: Modifier = Modifier
) {
    val (bg, fg) = when (status) {
        PresenceStatus.ONLINE -> EmeraldContainer to MintPulaText
        PresenceStatus.AWAY -> AmberContainer to WarmAmberText
        PresenceStatus.OFFLINE -> ElevatedCarbon to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(50),
        modifier = modifier.testTag("presence_pill_${status.raw}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            PresenceStatusDot(status = status, size = 8.dp, borderWidth = 1.dp)
            Text(
                text = status.label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = fg,
                maxLines = 1
            )
        }
    }
}

@Composable
fun MyPresenceSelectorChip(
    status: PresenceStatus,
    onSelectStatus: (PresenceStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val (bg, fg) = when (status) {
        PresenceStatus.ONLINE -> EmeraldContainer to MintPulaText
        PresenceStatus.AWAY -> AmberContainer to WarmAmberText
        PresenceStatus.OFFLINE -> ElevatedCarbon to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(modifier = modifier) {
        Surface(
            color = bg,
            shape = RoundedCornerShape(50),
            border = BorderStroke(0.8.dp, fg.copy(alpha = 0.3f)),
            modifier = Modifier
                .defaultMinSize(minHeight = 40.dp)
                .clip(RoundedCornerShape(50))
                .clickable { expanded = true }
                .testTag("my_presence_selector")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PresenceStatusDot(status = status, size = 9.dp, borderWidth = 1.dp)
                Text(
                    text = status.label,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = fg,
                    maxLines = 1
                )
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = "Change chat availability status",
                    tint = fg,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = CarbonSurface
        ) {
            PresenceStatus.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PresenceStatusDot(status = option, size = 10.dp, borderWidth = 1.dp)
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (option == status) ElectricCyan else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelectStatus(option)
                    },
                    modifier = Modifier.testTag("select_presence_${option.raw}")
                )
            }
        }
    }
}

@Composable
fun VarsityAvatar(
    name: String,
    universityCode: String,
    size: Dp = 40.dp,
    presenceStatus: PresenceStatus? = null,
    showStoryRing: Boolean = false,
    modifier: Modifier = Modifier
) {
    val initials = name.trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "BW" }

    val avatarBg = when (universityCode.uppercase()) {
        "UB" -> Color(0xFF132C40)
        "BIUST" -> Color(0xFF12332A)
        "BAC" -> Color(0xFF332610)
        "BOTHO" -> Color(0xFF281B3D)
        "BUAN" -> Color(0xFF163522)
        else -> ElevatedCarbon
    }

    val ringBrush = if (showStoryRing) {
        Brush.linearGradient(
            listOf(
                Color(0xFFF58529),
                Color(0xFFDD2A7B),
                Color(0xFF1D9BF0)
            )
        )
    } else {
        Brush.linearGradient(listOf(HairlineBorder, HairlineBorder))
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(avatarBg)
                .border(if (showStoryRing) 2.dp else 1.dp, ringBrush, CircleShape)
                .padding(if (showStoryRing) 2.dp else 0.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = CrispSnow,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
        if (presenceStatus != null) {
            val dotSize = (size * 0.28f).coerceIn(8.dp, 14.dp)
            PresenceStatusDot(
                status = presenceStatus,
                size = dotSize,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}

@Composable
fun StateLoadingBox(
    label: String = "Loading...",
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "skeleton_shimmer")
    val shimmerOffset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1150, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_x"
    )
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            CarbonSurface,
            HighestCarbon,
            CarbonSurface
        ),
        start = Offset(shimmerOffset - 300f, 0f),
        end = Offset(shimmerOffset, 220f)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        repeat(3) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MidnightSlate)
                    .border(0.8.dp, HairlineBorder, RoundedCornerShape(14.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(shimmerBrush)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.45f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(shimmerBrush)
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(shimmerBrush)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(shimmerBrush)
                )
            }
        }
    }
}

@Composable
fun StateErrorCard(
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(0.8.dp, CoralCrimson.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = "Error",
                tint = CoralCrimson
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
fun VarsityEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MidnightSlate)
                .border(0.8.dp, HairlineBorder, RoundedCornerShape(16.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ClayIconPod(
                icon = icon,
                contentDescription = title,
                containerColor = ElevatedCarbon,
                iconTint = CrispSnow,
                size = 52.dp
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    Text(actionLabel, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

fun formatTimestampShort(timestamp: Timestamp?): String {
    if (timestamp == null) return "Just now"
    val nowMs = System.currentTimeMillis()
    val diffSec = ((nowMs - timestamp.toDate().time) / 1000L).coerceAtLeast(0L)
    return when {
        diffSec < 60L -> "now"
        diffSec < 3600L -> "${diffSec / 60L}m"
        diffSec < 86400L -> "${diffSec / 3600L}h"
        else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(timestamp.toDate().time))
    }
}
