package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DynamicFeed
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.CampusPost
import com.example.model.PostCategory
import com.example.model.PostComment
import com.example.model.PresenceStatus
import com.example.model.ReportReason
import com.example.model.StudentProfile
import com.example.model.UiState
import com.example.model.UserPresence
import com.example.ui.components.CampusFilterStrip
import com.example.ui.components.StateErrorCard
import com.example.ui.components.StateLoadingBox
import com.example.ui.components.UniversityBadge
import com.example.ui.components.VarsityAvatar
import com.example.ui.components.VarsityEmptyState
import com.example.ui.components.VerifiedStudentIcon
import com.example.ui.components.formatTimestampShort
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CoralCrimson
import com.example.ui.theme.CrispSnow
import com.example.ui.theme.DeepCyanContainer
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElevatedCarbon
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPula
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.IceCyanText
import com.example.ui.theme.KalahariAmber
import com.example.ui.theme.MidnightSlate
import com.example.ui.theme.MintPulaText
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.WarmAmberText
import com.example.ui.viewmodel.FeedViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    myProfile: StudentProfile,
    presenceMap: Map<String, UserPresence> = emptyMap(),
    onlinePeers: List<StudentProfile> = emptyList(),
    initialOpenPostId: String? = null,
    onConsumedInitialPost: () -> Unit = {},
    onOpenStudyGroups: () -> Unit = {},
    onOpenChat: () -> Unit = {},
    onOpenMarket: () -> Unit = {},
    onOpenEvents: () -> Unit = {},
    onOpenStudentProfile: (userId: String, name: String, university: String) -> Unit = { _, _, _ -> },
    onMessageAuthor: ((userId: String, name: String, university: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val postsState by viewModel.postsState.collectAsStateWithLifecycle()
    val allPosts by viewModel.allPosts.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val campusFilter by viewModel.campusFilter.collectAsStateWithLifecycle()
    val categoryFilter by viewModel.categoryFilter.collectAsStateWithLifecycle()
    val commentsState by viewModel.commentsState.collectAsStateWithLifecycle()
    val reportedPostIds by viewModel.reportedPostIds.collectAsStateWithLifecycle()
    val pullToRefreshState = rememberPullToRefreshState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var activePostForComments by remember { mutableStateOf<CampusPost?>(null) }
    var postToReport by remember { mutableStateOf<CampusPost?>(null) }

    LaunchedEffect(initialOpenPostId, allPosts) {
        if (!initialOpenPostId.isNullOrBlank()) {
            val found = allPosts.firstOrNull { it.id == initialOpenPostId }
            if (found != null) {
                activePostForComments = found
                viewModel.selectPostForComments(found.id)
                onConsumedInitialPost()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianVoid)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CampusFilterStrip(
                selectedCampus = campusFilter,
                onSelectCampus = viewModel::setCampusFilter
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = categoryFilter == null,
                    onClick = { viewModel.setCategoryFilter(null) },
                    shape = RoundedCornerShape(50),
                    label = { Text("For You") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DeepCyanContainer,
                        selectedLabelColor = IceCyanText,
                        containerColor = MidnightSlate
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = categoryFilter == null,
                        borderColor = HairlineBorder,
                        selectedBorderColor = ElectricCyan
                    )
                )
                PostCategory.entries.forEach { cat ->
                    val selected = categoryFilter == cat
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.setCategoryFilter(if (selected) null else cat) },
                        shape = RoundedCornerShape(50),
                        label = { Text(cat.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DeepCyanContainer,
                            selectedLabelColor = IceCyanText,
                            containerColor = MidnightSlate
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = HairlineBorder,
                            selectedBorderColor = ElectricCyan
                        )
                    )
                }
            }

            HorizontalDivider(color = HairlineBorder, thickness = 0.8.dp)

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = viewModel::refreshFeed,
                state = pullToRefreshState,
                indicator = {
                    PullToRefreshDefaults.Indicator(
                        state = pullToRefreshState,
                        isRefreshing = isRefreshing,
                        containerColor = CarbonSurface,
                        color = ElectricCyan,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("feed_pull_to_refresh")
            ) {
                when (val state = postsState) {
                    is UiState.Loading -> StateLoadingBox("Loading campus feed...")
                    is UiState.Error -> Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        StateErrorCard(state.message)
                    }
                    is UiState.Success -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("feed_posts_list"),
                            contentPadding = PaddingValues(bottom = 96.dp)
                        ) {
                            item(key = "social_stories_and_composer") {
                                SocialStoriesAndComposerHeader(
                                    myProfile = myProfile,
                                    presenceMap = presenceMap,
                                    onlinePeers = onlinePeers,
                                    onCreatePost = { showCreateDialog = true },
                                    onOpenStudyGroups = onOpenStudyGroups,
                                    onOpenChat = onOpenChat,
                                    onOpenMarket = onOpenMarket,
                                    onOpenEvents = onOpenEvents,
                                    onOpenStudentProfile = onOpenStudentProfile
                                )
                            }

                            if (state.data.isEmpty()) {
                                item(key = "feed_empty") {
                                    VarsityEmptyState(
                                        icon = Icons.Outlined.DynamicFeed,
                                        title = "Start the campus timeline",
                                        subtitle = "Share notes, announcements, room tips, or questions with Botswana students.",
                                        actionLabel = "Post Now",
                                        onAction = { showCreateDialog = true }
                                    )
                                }
                            } else {
                                items(state.data, key = { it.id }) { post ->
                                    val authorPresence = presenceMap[post.authorId]?.effectiveStatus()
                                        ?.takeIf { it != PresenceStatus.OFFLINE }
                                    val isOwnPost = post.authorId == viewModel.currentUserId
                                    val isReported = post.id in reportedPostIds
                                    SocialTimelinePostRow(
                                        post = post,
                                        currentUserId = viewModel.currentUserId,
                                        authorPresence = authorPresence,
                                        isReported = isReported,
                                        onAuthorClick = {
                                            onOpenStudentProfile(
                                                post.authorId,
                                                post.authorName,
                                                post.authorUniversity
                                            )
                                        },
                                        onLikeClick = { viewModel.toggleLike(post) },
                                        onCommentClick = {
                                            activePostForComments = post
                                            viewModel.selectPostForComments(post.id)
                                        },
                                        onMessageClick = if (
                                            onMessageAuthor != null &&
                                            post.authorId.isNotBlank() &&
                                            !isOwnPost
                                        ) {
                                            {
                                                onMessageAuthor(
                                                    post.authorId,
                                                    post.authorName,
                                                    post.authorUniversity
                                                )
                                            }
                                        } else null,
                                        onReportClick = if (
                                            post.authorId.isNotBlank() &&
                                            !isOwnPost
                                        ) {
                                            { postToReport = post }
                                        } else null
                                    )
                                    HorizontalDivider(color = HairlineBorder, thickness = 0.8.dp)
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showCreateDialog = true },
            containerColor = ElectricCyan,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("create_post_fab")
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Create campus post")
        }
    }

    if (showCreateDialog) {
        CreatePostDialog(
            onDismiss = { showCreateDialog = false },
            onSubmit = { category, title, body ->
                viewModel.createPost(myProfile, category, title, body) {
                    showCreateDialog = false
                }
            }
        )
    }

    val selectedPost = activePostForComments
    if (selectedPost != null) {
        ModalBottomSheet(
            onDismissRequest = {
                activePostForComments = null
                viewModel.selectPostForComments(null)
            },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            PostCommentsSheetContent(
                post = selectedPost,
                commentsState = commentsState,
                onOpenStudentProfile = { uid, name, uni ->
                    activePostForComments = null
                    viewModel.selectPostForComments(null)
                    onOpenStudentProfile(uid, name, uni)
                },
                onSendComment = { text, onClear ->
                    viewModel.addComment(selectedPost, myProfile, text, onClear)
                }
            )
        }
    }

    val reportTarget = postToReport
    if (reportTarget != null) {
        ReportPostDialog(
            post = reportTarget,
            alreadyReported = reportTarget.id in reportedPostIds,
            onDismiss = { postToReport = null },
            onSubmit = { reason, details ->
                viewModel.reportPost(
                    post = reportTarget,
                    profile = myProfile,
                    reason = reason,
                    details = details
                ) {
                    postToReport = null
                }
            }
        )
    }
}

/**
 * Instagram-style Stories / Campus Spaces horizontal rail paired with a Facebook-style "What's on your mind?" composer bar.
 */
@Composable
private fun SocialStoriesAndComposerHeader(
    myProfile: StudentProfile,
    presenceMap: Map<String, UserPresence>,
    onlinePeers: List<StudentProfile>,
    onCreatePost: () -> Unit,
    onOpenStudyGroups: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenMarket: () -> Unit,
    onOpenEvents: () -> Unit,
    onOpenStudentProfile: (userId: String, name: String, university: String) -> Unit
) {
    val firstName = myProfile.displayName.trim()
        .split(Regex("\\s+"))
        .firstOrNull()
        ?.ifBlank { "Student" } ?: "Student"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ObsidianVoid)
            .testTag("campus_bento_grid")
    ) {
        // 1. Instagram-style Stories & Campus Spaces strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Your Story / Quick Post circle
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .width(64.dp)
                    .clickable(onClick = onCreatePost)
            ) {
                Box(modifier = Modifier.size(58.dp)) {
                    VarsityAvatar(
                        name = myProfile.displayName,
                        universityCode = myProfile.university,
                        size = 58.dp,
                        showStoryRing = false
                    )
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(ElectricCyan)
                            .border(2.dp, ObsidianVoid, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "New post",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
                Text(
                    text = "Your post",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            StorySpaceItem(
                label = "Lounges",
                icon = Icons.Outlined.Forum,
                ringColors = listOf(Color(0xFF1D9BF0), Color(0xFF00BA7C)),
                onClick = onOpenChat,
                modifier = Modifier.testTag("bento_chat_tile")
            )
            StorySpaceItem(
                label = "Study",
                icon = Icons.Outlined.Groups,
                ringColors = listOf(Color(0xFFF58529), Color(0xFFDD2A7B)),
                onClick = onOpenStudyGroups,
                modifier = Modifier.testTag("bento_study_tile")
            )
            StorySpaceItem(
                label = "Market",
                icon = Icons.Outlined.AutoStories,
                ringColors = listOf(Color(0xFFF59E0B), Color(0xFFF91880)),
                onClick = onOpenMarket,
                modifier = Modifier.testTag("bento_market_tile")
            )
            StorySpaceItem(
                label = "Events",
                icon = Icons.Outlined.CalendarMonth,
                ringColors = listOf(Color(0xFF00BA7C), Color(0xFF1D9BF0)),
                onClick = onOpenEvents,
                modifier = Modifier.testTag("bento_calendar_tile")
            )

            // Active classmates currently online / away
            onlinePeers.take(6).forEach { peer ->
                val status = presenceMap[peer.userId]?.effectiveStatus() ?: PresenceStatus.OFFLINE
                val peerFirstName = peer.displayName.trim().split(Regex("\\s+")).firstOrNull().orEmpty()
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .width(64.dp)
                        .clickable {
                            onOpenStudentProfile(peer.userId, peer.displayName, peer.university)
                        }
                ) {
                    VarsityAvatar(
                        name = peer.displayName,
                        universityCode = peer.university,
                        size = 58.dp,
                        presenceStatus = status,
                        showStoryRing = status == PresenceStatus.ONLINE
                    )
                    Text(
                        text = peerFirstName.ifBlank { peer.university },
                        style = MaterialTheme.typography.labelSmall,
                        color = CrispSnow,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        HorizontalDivider(color = HairlineBorder, thickness = 0.8.dp)

        // 2. Facebook-style "What's on your mind?" quick composer bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MidnightSlate)
                .clickable(onClick = onCreatePost)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("feed_quick_composer"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            VarsityAvatar(
                name = myProfile.displayName,
                universityCode = myProfile.university,
                size = 38.dp
            )
            Surface(
                color = CarbonSurface,
                shape = RoundedCornerShape(50),
                border = BorderStroke(0.8.dp, HairlineBorder),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "What's happening at ${myProfile.university}, $firstName?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
            Icon(
                imageVector = Icons.Outlined.EditNote,
                contentDescription = "Write post",
                tint = ElectricCyan,
                modifier = Modifier.size(24.dp)
            )
        }

        HorizontalDivider(color = HairlineBorder, thickness = 4.dp)
    }
}

@Composable
private fun StorySpaceItem(
    label: String,
    icon: ImageVector,
    ringColors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .width(64.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .border(2.dp, Brush.linearGradient(ringColors), CircleShape)
                .padding(4.dp)
                .clip(CircleShape)
                .background(CarbonSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = CrispSnow,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * X / Threads / Facebook style hairline-separated timeline post row.
 */
@Composable
private fun SocialTimelinePostRow(
    post: CampusPost,
    currentUserId: String,
    authorPresence: PresenceStatus?,
    isReported: Boolean,
    onAuthorClick: () -> Unit,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onMessageClick: (() -> Unit)?,
    onReportClick: (() -> Unit)?
) {
    val category = PostCategory.fromRaw(post.category)
    val liked = post.likedByUids.contains(currentUserId)
    val (catBg, catFg) = when (category) {
        PostCategory.ANNOUNCEMENT -> Color(0xFF332208) to WarmAmberText
        PostCategory.OPPORTUNITY -> EmeraldContainer to MintPulaText
        PostCategory.QUESTION -> DeepCyanContainer to IceCyanText
        PostCategory.CAMPUS_LIFE -> ElevatedCarbon to MaterialTheme.colorScheme.onSurfaceVariant
    }
    var menuExpanded by remember(post.id) { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ObsidianVoid)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("feed_post_card_${post.id}"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        VarsityAvatar(
            name = post.authorName,
            universityCode = post.authorUniversity,
            size = 42.dp,
            presenceStatus = authorPresence,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onAuthorClick)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Author line: Name + Verified Blue Tick + Uni + • Timestamp + Category Pill + More Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onAuthorClick)
                        .testTag("post_author_${post.id}")
                ) {
                    Text(
                        text = post.authorName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = CrispSnow,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    VerifiedStudentIcon(size = 14.dp)
                    UniversityBadge(post.authorUniversity)
                    Text(
                        text = "· ${formatTimestampShort(post.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Spacer(Modifier.weight(1f))
                Surface(
                    color = catBg,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = category.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = catFg,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
                if (onReportClick != null) {
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier
                                .size(36.dp)
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .testTag("post_more_button_${post.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MoreHoriz,
                                contentDescription = "More post options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            containerColor = CarbonSurface
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (isReported) "Reported to admins" else "Report post",
                                        color = if (isReported) MaterialTheme.colorScheme.onSurfaceVariant else CoralCrimson,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Flag,
                                        contentDescription = null,
                                        tint = if (isReported) MaterialTheme.colorScheme.onSurfaceVariant else CoralCrimson
                                    )
                                },
                                enabled = !isReported,
                                onClick = {
                                    menuExpanded = false
                                    onReportClick()
                                },
                                modifier = Modifier.testTag("report_post_action_${post.id}")
                            )
                        }
                    }
                }
            }

            if (isReported) {
                Surface(
                    color = Color(0xFF3A1023),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Flag,
                            contentDescription = null,
                            tint = CoralCrimson,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Reported for admin review",
                            style = MaterialTheme.typography.labelSmall,
                            color = CoralCrimson
                        )
                    }
                }
            }

            if (post.title.isNotBlank()) {
                Text(
                    text = post.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = CrispSnow
                )
            }

            Text(
                text = post.body,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFE7E9EA)
            )

            // Instagram / X engagement row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 40.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onLikeClick)
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                        .testTag("like_post_${post.id}")
                ) {
                    Icon(
                        imageVector = if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like post",
                        tint = if (liked) CoralCrimson else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp)
                    )
                    Text(
                        text = "${post.likedByUids.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (liked) CoralCrimson else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 40.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onCommentClick)
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                        .testTag("comment_post_${post.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "View replies",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp)
                    )
                    Text(
                        text = if (post.commentCount > 0) "${post.commentCount}" else "Reply",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (onMessageClick != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 40.dp)
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onMessageClick)
                            .padding(horizontal = 6.dp, vertical = 6.dp)
                            .testTag("message_author_${post.id}")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Send,
                            contentDescription = "Direct message ${post.authorName}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "DM",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreatePostDialog(
    onDismiss: () -> Unit,
    onSubmit: (PostCategory, String, String) -> Unit
) {
    var category by remember { mutableStateOf(PostCategory.CAMPUS_LIFE) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Create Post", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PostCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 120) title = it },
                    label = { Text("Headline") },
                    placeholder = { Text("e.g. UB Library extended hours this week") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("post_title_input")
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { if (it.length <= 1000) body = it },
                    label = { Text("What's happening?") },
                    placeholder = { Text("Share helpful details with fellow students...") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("post_body_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(category, title, body) },
                enabled = title.trim().length >= 2 && body.trim().isNotEmpty(),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricCyan,
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("submit_post_button")
            ) {
                Text("Post", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun PostCommentsSheetContent(
    post: CampusPost,
    commentsState: UiState<List<PostComment>>,
    onOpenStudentProfile: (userId: String, name: String, university: String) -> Unit,
    onSendComment: (String, () -> Unit) -> Unit
) {
    var replyText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = post.title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = CrispSnow
        )

        when (commentsState) {
            is UiState.Loading -> StateLoadingBox("Loading replies...", Modifier.height(140.dp))
            is UiState.Error -> StateErrorCard(commentsState.message)
            is UiState.Success -> {
                if (commentsState.data.isEmpty()) {
                    Text(
                        text = "No replies yet. Be the first to respond!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(commentsState.data, key = { it.id }) { comment ->
                            Surface(
                                color = CarbonSurface,
                                shape = MaterialTheme.shapes.small,
                                border = BorderStroke(0.8.dp, HairlineBorder)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    onOpenStudentProfile(
                                                        comment.authorId,
                                                        comment.authorName,
                                                        comment.authorUniversity
                                                    )
                                                }
                                                .padding(vertical = 2.dp)
                                                .testTag("comment_author_${comment.id}")
                                        ) {
                                            Text(
                                                text = comment.authorName,
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = CrispSnow
                                            )
                                            VerifiedStudentIcon(size = 13.dp)
                                            UniversityBadge(comment.authorUniversity)
                                        }
                                        Spacer(Modifier.weight(1f))
                                        Text(
                                            text = formatTimestampShort(comment.createdAt),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = comment.body,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFFE7E9EA)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = replyText,
                onValueChange = { if (it.length <= 500) replyText = it },
                placeholder = { Text("Post your reply...") },
                singleLine = true,
                shape = RoundedCornerShape(50),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = HairlineBorder,
                    focusedContainerColor = CarbonSurface,
                    unfocusedContainerColor = CarbonSurface
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("comment_input")
            )
            IconButton(
                onClick = {
                    val trimmed = replyText.trim()
                    if (trimmed.isNotEmpty()) {
                        onSendComment(trimmed) { replyText = "" }
                    }
                },
                enabled = replyText.trim().isNotEmpty(),
                modifier = Modifier
                    .size(48.dp)
                    .testTag("send_comment_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send reply",
                    tint = ElectricCyan
                )
            }
        }
    }
}

@Composable
private fun ReportPostDialog(
    post: CampusPost,
    alreadyReported: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (ReportReason, String) -> Unit
) {
    var selectedReason by remember { mutableStateOf(ReportReason.INAPPROPRIATE_CONTENT) }
    var details by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Flag,
                contentDescription = null,
                tint = CoralCrimson
            )
        },
        title = {
            Text(
                text = "Report Post",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = CrispSnow
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = CarbonSurface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(0.8.dp, HairlineBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "${post.authorName} • ${post.authorUniversity}",
                            style = MaterialTheme.typography.labelMedium,
                            color = IceCyanText
                        )
                        if (post.title.isNotBlank()) {
                            Text(
                                text = post.title,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = CrispSnow,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = post.body,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Text(
                    text = "Why are you reporting this post? Administrators will be notified immediately.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ReportReason.entries.forEach { reason ->
                        val selected = selectedReason == reason
                        Surface(
                            color = if (selected) Color(0xFF3A1023) else CarbonSurface,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.dp,
                                if (selected) CoralCrimson else HairlineBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedReason = reason }
                                .testTag("report_reason_${reason.name}")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = reason.label,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (selected) CrispSnow else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = reason.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = details,
                    onValueChange = { if (it.length <= 400) details = it },
                    label = { Text("Additional details (optional)") },
                    placeholder = { Text("Add context for VarsityBW moderators...") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("report_details_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(selectedReason, details) },
                enabled = !alreadyReported,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CoralCrimson,
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("submit_report_button")
            ) {
                Text(
                    text = if (alreadyReported) "Already Reported" else "Submit Report",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

