package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.MarkChatUnread
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AdminNotification
import com.example.model.NotificationType
import com.example.model.PostReport
import com.example.model.ReportReason
import com.example.model.ReportStatus
import com.example.model.UiState
import com.example.model.UserNotification
import com.example.notifications.VarsityNotificationHelper
import com.example.ui.components.StateErrorCard
import com.example.ui.components.StateLoadingBox
import com.example.ui.components.UniversityBadge
import com.example.ui.components.VarsityEmptyState
import com.example.ui.components.formatTimestampShort
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CoralCrimson
import com.example.ui.theme.CrispSnow
import com.example.ui.theme.DeepCyanContainer
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPula
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.IceCyanText
import com.example.ui.theme.MidnightSlate
import com.example.ui.theme.MintPulaText
import com.example.ui.theme.WarmAmberText

@Composable
fun NotificationsSheetContent(
    notificationsState: UiState<List<UserNotification>>,
    unreadCount: Int,
    fcmToken: String?,
    onMarkAllRead: () -> Unit,
    onNotificationClick: (UserNotification) -> Unit,
    onPermissionGranted: () -> Unit,
    isAdmin: Boolean = false,
    initialAdminTab: Boolean = false,
    adminNotificationsState: UiState<List<AdminNotification>> = UiState.Success(emptyList()),
    adminReportsState: UiState<List<PostReport>> = UiState.Success(emptyList()),
    adminUnreadCount: Int = 0,
    onAdminNotificationClick: (AdminNotification) -> Unit = {},
    onUpdateReportStatus: (String, ReportStatus) -> Unit = { _, _ -> },
    onRemoveFlaggedPost: (String, String?) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(VarsityNotificationHelper.hasNotificationPermission(context))
    }
    var selectedSection by remember(isAdmin, initialAdminTab) {
        mutableIntStateOf(if (isAdmin && initialAdminTab) 1 else 0)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted || VarsityNotificationHelper.hasNotificationPermission(context)
        if (hasPermission) {
            VarsityNotificationHelper.ensureChannels(context)
            onPermissionGranted()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp)
            .testTag("notifications_sheet")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Notifications",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = CrispSnow
                )
                Text(
                    text = if (unreadCount > 0) "$unreadCount unread alert${if (unreadCount == 1) "" else "s"}" else "All caught up",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (unreadCount > 0) {
                TextButton(
                    onClick = onMarkAllRead,
                    modifier = Modifier.testTag("mark_all_notifications_read_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DoneAll,
                        contentDescription = "Mark all read",
                        tint = ElectricCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Mark all read", color = ElectricCyan)
                }
            }
        }

        // Push Notification Permission & FCM Device Status Banner
        Surface(
            color = if (hasPermission) CarbonSurface else DeepCyanContainer.copy(alpha = 0.65f),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(
                0.8.dp,
                if (hasPermission) HairlineBorder else ElectricCyan.copy(alpha = 0.5f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (hasPermission) EmeraldContainer else ElectricCyan.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsActive,
                        contentDescription = "Push alerts status",
                        tint = if (hasPermission) EmeraldPula else ElectricCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (hasPermission) "Real-time Push Alerts Enabled" else "Turn On Push Notifications",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (hasPermission) MintPulaText else CrispSnow
                    )
                    Text(
                        text = if (hasPermission) {
                            if (!fcmToken.isNullOrBlank()) {
                                "FCM device registered • Replies & DM alerts active"
                            } else {
                                "Registering FCM token for real-time alerts..."
                            }
                        } else {
                            "Get notified right away on new DMs, group chats, and post replies."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCyan,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 40.dp)
                            .testTag("enable_push_notifications_button")
                    ) {
                        Text("Allow", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (isAdmin) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedSection == 0,
                    onClick = { selectedSection = 0 },
                    shape = RoundedCornerShape(50),
                    label = {
                        Text(if (unreadCount > 0) "Inbox ($unreadCount)" else "Inbox")
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DeepCyanContainer,
                        selectedLabelColor = IceCyanText,
                        containerColor = MidnightSlate
                    )
                )
                FilterChip(
                    selected = selectedSection == 1,
                    onClick = { selectedSection = 1 },
                    shape = RoundedCornerShape(50),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Shield,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = {
                        Text(if (adminUnreadCount > 0) "Admin Alerts ($adminUnreadCount)" else "Admin Alerts")
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF3A1023),
                        selectedLabelColor = CoralCrimson,
                        containerColor = MidnightSlate
                    ),
                    modifier = Modifier.testTag("admin_alerts_tab")
                )
            }
        }

        HorizontalDivider(color = HairlineBorder, thickness = 0.8.dp)

        if (isAdmin && selectedSection == 1) {
            AdminModerationAlertsList(
                adminNotificationsState = adminNotificationsState,
                adminReportsState = adminReportsState,
                onAdminNotificationClick = onAdminNotificationClick,
                onUpdateReportStatus = onUpdateReportStatus,
                onRemoveFlaggedPost = onRemoveFlaggedPost
            )
        } else {
            when (notificationsState) {
                is UiState.Loading -> StateLoadingBox("Loading alerts...", Modifier.height(220.dp))
                is UiState.Error -> StateErrorCard(notificationsState.message)
                is UiState.Success -> {
                    val items = notificationsState.data
                    if (items.isEmpty()) {
                        VarsityEmptyState(
                            icon = Icons.Outlined.NotificationsNone,
                            title = "No notifications yet",
                            subtitle = "When classmates reply to your feed posts or message you in chat, alerts appear here."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(360.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(items, key = { it.id }) { notif ->
                                NotificationItemRow(
                                    notif = notif,
                                    onClick = { onNotificationClick(notif) }
                                )
                                HorizontalDivider(color = HairlineBorder, thickness = 0.6.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminModerationAlertsList(
    adminNotificationsState: UiState<List<AdminNotification>>,
    adminReportsState: UiState<List<PostReport>>,
    onAdminNotificationClick: (AdminNotification) -> Unit,
    onUpdateReportStatus: (String, ReportStatus) -> Unit,
    onRemoveFlaggedPost: (String, String?) -> Unit
) {
    val reportsById = remember(adminReportsState) {
        (adminReportsState as? UiState.Success)?.data.orEmpty().associateBy { it.id }
    }
    when (adminNotificationsState) {
        is UiState.Loading -> StateLoadingBox("Loading moderation alerts...", Modifier.height(220.dp))
        is UiState.Error -> StateErrorCard(adminNotificationsState.message)
        is UiState.Success -> {
            val alerts = adminNotificationsState.data
            if (alerts.isEmpty()) {
                VarsityEmptyState(
                    icon = Icons.Outlined.Shield,
                    title = "Moderation queue clear",
                    subtitle = "Flagged posts reported by students appear here for administrator action."
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(alerts, key = { it.id }) { alert ->
                        val linkedReport = reportsById[alert.reportId]
                        val reportStatus = ReportStatus.fromRaw(linkedReport?.status)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (!alert.read) CoralCrimson.copy(alpha = 0.08f) else Color.Transparent)
                                .clickable { onAdminNotificationClick(alert) }
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Flag,
                                    contentDescription = "Moderation report",
                                    tint = CoralCrimson,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = alert.title.ifBlank { ReportReason.fromRaw(alert.reason).label },
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = CrispSnow,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = reportStatus.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when (reportStatus) {
                                        ReportStatus.PENDING -> WarmAmberText
                                        ReportStatus.REVIEWED -> MintPulaText
                                        ReportStatus.DISMISSED -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                            Text(
                                text = alert.body,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (reportStatus == ReportStatus.PENDING) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { onUpdateReportStatus(alert.reportId, ReportStatus.DISMISSED) },
                                        shape = RoundedCornerShape(50),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text("Dismiss", style = MaterialTheme.typography.labelMedium)
                                    }
                                    Button(
                                        onClick = { onRemoveFlaggedPost(alert.postId, alert.reportId) },
                                        shape = RoundedCornerShape(50),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = CoralCrimson,
                                            contentColor = Color.White
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.DeleteOutline,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text("Remove Post", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = HairlineBorder, thickness = 0.6.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationItemRow(
    notif: UserNotification,
    onClick: () -> Unit
) {
    val type = NotificationType.fromRaw(notif.notifType)
    val icon = when (type) {
        NotificationType.CHAT_MESSAGE -> Icons.Outlined.MarkChatUnread
        NotificationType.POST_REPLY -> Icons.Outlined.ChatBubbleOutline
        NotificationType.ADMIN_REPORT -> Icons.Outlined.Flag
    }
    val iconTint = when (type) {
        NotificationType.CHAT_MESSAGE -> ElectricCyan
        NotificationType.POST_REPLY -> CoralCrimson
        NotificationType.ADMIN_REPORT -> WarmAmberText
    }
    val iconBg = when (type) {
        NotificationType.CHAT_MESSAGE -> DeepCyanContainer
        NotificationType.POST_REPLY -> Color(0xFF3A1023)
        NotificationType.ADMIN_REPORT -> Color(0xFF332208)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (!notif.read) ElectricCyan.copy(alpha = 0.06f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp)
            .testTag("notification_item_${notif.id}"),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = type.name,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = notif.senderName.ifBlank { "Student" },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = CrispSnow,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (notif.senderUniversity.isNotBlank()) {
                    UniversityBadge(notif.senderUniversity)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = formatTimestampShort(notif.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = notif.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (!notif.read) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = IceCyanText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = notif.body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (!notif.read) {
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(ElectricCyan)
            )
        }
    }
}
