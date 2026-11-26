package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DynamicFeed
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.authStateFlow
import com.example.auth.signOutFromVarsityBw
import com.example.data.VarsityRepository
import com.example.model.NotificationType
import com.example.model.PresenceStatus
import com.example.model.StudentProfile
import com.example.model.UiState
import com.example.notifications.VarsityNotificationHelper
import com.example.ui.components.StateErrorCard
import com.example.ui.components.StateLoadingBox
import com.example.ui.components.UniversityBadge
import com.example.ui.components.VarsityAvatar
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChatListScreen
import com.example.ui.screens.ChatRoomScreen
import com.example.ui.screens.EventsScreen
import com.example.ui.screens.FeedScreen
import com.example.ui.screens.GlobalSearchOverlay
import com.example.ui.screens.MarketplaceScreen
import com.example.ui.screens.NotificationsSheetContent
import com.example.ui.screens.ProfileAndDirectorySheetContent
import com.example.ui.screens.StudentRegistrationScreen
import com.example.ui.screens.StudyGroupsScreen
import com.example.ui.screens.UserProfileScreen
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CoralCrimson
import com.example.ui.theme.CrispSnow
import com.example.ui.theme.DeepCyanContainer
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.VarsityBwTheme
import com.example.ui.viewmodel.ChatViewModel
import com.example.ui.viewmodel.FeedViewModel
import com.example.ui.viewmodel.MarketAndEventsViewModel
import com.example.ui.viewmodel.SessionViewModel
import com.example.ui.viewmodel.StudyGroupsViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive

data class PendingNotifNavigation(
    val type: NotificationType,
    val targetId: String,
    val notifId: String
)

class MainActivity : ComponentActivity() {
    private val pendingNotificationNav = MutableStateFlow<PendingNotifNavigation?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        VarsityNotificationHelper.ensureChannels(this)
        pendingNotificationNav.value = extractPendingNotification(intent)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            val pendingNav by pendingNotificationNav.collectAsStateWithLifecycle()
            VarsityBwTheme {
                AppNavigation(
                    pendingNotif = pendingNav,
                    onConsumedPendingNotif = { pendingNotificationNav.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractPendingNotification(intent)?.let {
            pendingNotificationNav.value = it
        }
    }

    private fun extractPendingNotification(intent: Intent?): PendingNotifNavigation? {
        val targetId = intent?.getStringExtra(VarsityNotificationHelper.EXTRA_TARGET_ID).orEmpty()
        if (targetId.isBlank()) return null
        val type = NotificationType.fromRaw(
            intent?.getStringExtra(VarsityNotificationHelper.EXTRA_NOTIF_TYPE)
        )
        val notifId = intent?.getStringExtra(VarsityNotificationHelper.EXTRA_NOTIF_ID).orEmpty()
        return PendingNotifNavigation(
            type = type,
            targetId = targetId,
            notifId = notifId
        )
    }
}

private enum class VarsityTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    FEED("feed", "Feed", Icons.Filled.DynamicFeed, Icons.Outlined.DynamicFeed),
    MARKET("market", "Market", Icons.Filled.Storefront, Icons.Outlined.Storefront),
    STUDY("study", "Study", Icons.Filled.Groups, Icons.Outlined.Groups),
    EVENTS("events", "Events", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth)
}

@Composable
fun AppNavigation(
    auth: FirebaseAuth = Firebase.auth,
    pendingNotif: PendingNotifNavigation? = null,
    onConsumedPendingNotif: () -> Unit = {}
) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            AuthenticatedVarsityFlow(
                currentUserId = user.uid,
                googleEmail = user.email.orEmpty(),
                googleDisplayName = user.displayName.orEmpty(),
                pendingNotif = pendingNotif,
                onConsumedPendingNotif = onConsumedPendingNotif
            )
        }
    }
}

@Composable
private fun AuthenticatedVarsityFlow(
    currentUserId: String,
    googleEmail: String,
    googleDisplayName: String,
    pendingNotif: PendingNotifNavigation?,
    onConsumedPendingNotif: () -> Unit
) {
    val sessionViewModel: SessionViewModel = viewModel(
        key = "session_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                SessionViewModel(VarsityRepository(db), currentUserId)
            }
        }
    )

    val profileState by sessionViewModel.myProfileState.collectAsStateWithLifecycle()
    val isSubmitting by sessionViewModel.isSubmitting.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(sessionViewModel) {
        sessionViewModel.events.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    when (val state = profileState) {
        is UiState.Loading -> {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ObsidianVoid)
            ) {
                StateLoadingBox("Checking student verification...")
            }
        }
        is UiState.Error -> {
            Scaffold(
                containerColor = ObsidianVoid,
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { padding ->
                Column(modifier = Modifier.padding(padding)) {
                    StateErrorCard(state.message)
                }
            }
        }
        is UiState.Success -> {
            val profile = state.data
            if (profile == null) {
                Scaffold(
                    containerColor = ObsidianVoid,
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { padding ->
                    Column(modifier = Modifier.padding(padding)) {
                        StudentRegistrationScreen(
                            googleEmail = googleEmail,
                            googleDisplayName = googleDisplayName,
                            isSubmitting = isSubmitting,
                            onRegister = { studentEmail, name, uni, faculty, year, bio ->
                                sessionViewModel.registerStudent(
                                    googleEmail = googleEmail,
                                    studentEmail = studentEmail,
                                    displayName = name,
                                    university = uni,
                                    faculty = faculty,
                                    yearOfStudy = year,
                                    bio = bio
                                )
                            }
                        )
                    }
                }
            } else {
                VarsityMainShell(
                    currentUserId = currentUserId,
                    myProfile = profile,
                    sessionViewModel = sessionViewModel,
                    snackbarHostState = snackbarHostState,
                    pendingNotif = pendingNotif,
                    onConsumedPendingNotif = onConsumedPendingNotif
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VarsityMainShell(
    currentUserId: String,
    myProfile: StudentProfile,
    sessionViewModel: SessionViewModel,
    snackbarHostState: SnackbarHostState,
    pendingNotif: PendingNotifNavigation?,
    onConsumedPendingNotif: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val feedVm: FeedViewModel = viewModel(
        key = "feed_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                FeedViewModel(VarsityRepository(db), currentUserId)
            }
        }
    )
    val studyVm: StudyGroupsViewModel = viewModel(
        key = "study_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                StudyGroupsViewModel(VarsityRepository(db), currentUserId)
            }
        }
    )
    val chatVm: ChatViewModel = viewModel(
        key = "chat_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                ChatViewModel(VarsityRepository(db), currentUserId)
            }
        }
    )
    val marketEventsVm: MarketAndEventsViewModel = viewModel(
        key = "market_events_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                MarketAndEventsViewModel(VarsityRepository(db), currentUserId)
            }
        }
    )

    LaunchedEffect(feedVm) { feedVm.feedback.collect { snackbarHostState.showSnackbar(it) } }
    LaunchedEffect(studyVm) { studyVm.feedback.collect { snackbarHostState.showSnackbar(it) } }
    LaunchedEffect(chatVm) { chatVm.feedback.collect { snackbarHostState.showSnackbar(it) } }
    LaunchedEffect(marketEventsVm) { marketEventsVm.feedback.collect { snackbarHostState.showSnackbar(it) } }

    LaunchedEffect(currentUserId) {
        sessionViewModel.syncFcmToken()
    }

    val allPosts by feedVm.allPosts.collectAsStateWithLifecycle()
    val allListings by marketEventsVm.allListings.collectAsStateWithLifecycle()
    val allEvents by marketEventsVm.allEvents.collectAsStateWithLifecycle()
    val presenceMap by sessionViewModel.presenceMap.collectAsStateWithLifecycle()
    val myPresenceStatus by sessionViewModel.preferredPresence.collectAsStateWithLifecycle()
    val directoryState by sessionViewModel.directoryState.collectAsStateWithLifecycle()
    val notificationsState by sessionViewModel.notificationsState.collectAsStateWithLifecycle()
    val unreadNotificationCount by sessionViewModel.unreadNotificationCount.collectAsStateWithLifecycle()
    val isAdmin by sessionViewModel.isAdmin.collectAsStateWithLifecycle()
    val adminNotificationsState by sessionViewModel.adminNotificationsState.collectAsStateWithLifecycle()
    val adminReportsState by sessionViewModel.adminReportsState.collectAsStateWithLifecycle()
    val adminUnreadCount by sessionViewModel.adminUnreadCount.collectAsStateWithLifecycle()
    val totalUnreadNotifications = unreadNotificationCount + if (isAdmin) adminUnreadCount else 0
    val fcmToken by sessionViewModel.fcmToken.collectAsStateWithLifecycle()

    val onlinePeers = remember(directoryState, presenceMap, currentUserId) {
        val peers = (directoryState as? UiState.Success)?.data.orEmpty()
        peers.filter { peer ->
            peer.userId != currentUserId &&
                (presenceMap[peer.userId]?.effectiveStatus() ?: PresenceStatus.OFFLINE) != PresenceStatus.OFFLINE
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    var isForeground by remember { mutableStateOf(true) }
    var presenceClockTick by remember { mutableLongStateOf(0L) }

    DisposableEffect(lifecycleOwner, currentUserId) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    isForeground = true
                    sessionViewModel.onAppForegrounded()
                }
                Lifecycle.Event.ON_STOP -> {
                    isForeground = false
                    sessionViewModel.onAppBackgrounded()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            isForeground = true
            sessionViewModel.onAppForegrounded()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(currentUserId, isForeground) {
        while (isActive && isForeground) {
            delay(45_000L)
            presenceClockTick++
            sessionViewModel.sendPresenceHeartbeat()
        }
    }

    val handleSignOut: () -> Unit = {
        signOutFromVarsityBw(
            credentialManager = credentialManager,
            scope = scope,
            onBeforeSignOut = { sessionViewModel.markOfflineBeforeSignOut() }
        )
    }

    var selectedTab by rememberSaveable { mutableStateOf(VarsityTab.FEED) }
    var isGlobalSearchOpen by rememberSaveable { mutableStateOf(false) }
    var globalSearchQuery by rememberSaveable { mutableStateOf("") }
    var showChatHub by rememberSaveable { mutableStateOf(false) }
    var activeRoomId by rememberSaveable { mutableStateOf<String?>(null) }
    var targetPostIdToOpen by rememberSaveable { mutableStateOf<String?>(null) }
    var showNotificationsSheet by remember { mutableStateOf(false) }
    var openAdminAlertsTab by remember { mutableStateOf(false) }
    var profileSheetTab by remember { mutableIntStateOf(0) }
    var showProfileSheet by remember { mutableStateOf(false) }
    var viewedProfileUserId by rememberSaveable { mutableStateOf<String?>(null) }
    var viewedProfileFallbackName by rememberSaveable { mutableStateOf("") }
    var viewedProfileFallbackUni by rememberSaveable { mutableStateOf(myProfile.university) }

    // Dispatch local OS notifications in real time when new chat messages, post replies, or admin report alerts arrive
    val sessionStartMs = remember(currentUserId) { System.currentTimeMillis() }
    val notifiedIds = remember(currentUserId) { mutableSetOf<String>() }
    LaunchedEffect(notificationsState, activeRoomId, currentUserId) {
        val items = (notificationsState as? UiState.Success)?.data.orEmpty()
        for (notif in items) {
            if (!notif.read && notifiedIds.add(notif.id)) {
                val type = NotificationType.fromRaw(notif.notifType)
                val createdMs = notif.createdAt?.toDate()?.time ?: System.currentTimeMillis()
                val isRecentArrival = createdMs >= sessionStartMs - 15_000L
                val isCurrentlyViewingRoom = type == NotificationType.CHAT_MESSAGE &&
                    notif.targetId == activeRoomId
                if (isRecentArrival && !isCurrentlyViewingRoom) {
                    VarsityNotificationHelper.showSocialNotification(
                        context = context,
                        notifId = notif.id,
                        type = type,
                        title = notif.title,
                        body = notif.body,
                        targetId = notif.targetId
                    )
                }
            }
        }
    }

    LaunchedEffect(isAdmin, adminNotificationsState, currentUserId) {
        if (!isAdmin) return@LaunchedEffect
        val alerts = (adminNotificationsState as? UiState.Success)?.data.orEmpty()
        for (alert in alerts) {
            if (!alert.read && notifiedIds.add("admin_${alert.id}")) {
                val createdMs = alert.createdAt?.toDate()?.time ?: System.currentTimeMillis()
                if (createdMs >= sessionStartMs - 15_000L) {
                    VarsityNotificationHelper.showSocialNotification(
                        context = context,
                        notifId = alert.id,
                        type = NotificationType.ADMIN_REPORT,
                        title = alert.title,
                        body = alert.body,
                        targetId = alert.postId
                    )
                }
            }
        }
    }

    // Handle deep-linking when tapping an Android notification
    LaunchedEffect(pendingNotif) {
        val nav = pendingNotif ?: return@LaunchedEffect
        if (nav.notifId.isNotBlank()) {
            if (nav.type == NotificationType.ADMIN_REPORT) {
                sessionViewModel.markAdminNotificationRead(nav.notifId)
            } else {
                sessionViewModel.markNotificationRead(nav.notifId)
            }
        }
        isGlobalSearchOpen = false
        globalSearchQuery = ""
        viewedProfileUserId = null
        showProfileSheet = false
        showNotificationsSheet = false
        when (nav.type) {
            NotificationType.CHAT_MESSAGE -> {
                showChatHub = false
                activeRoomId = nav.targetId
            }
            NotificationType.POST_REPLY -> {
                activeRoomId = null
                showChatHub = false
                feedVm.setCampusFilter("ALL")
                feedVm.setCategoryFilter(null)
                selectedTab = VarsityTab.FEED
                targetPostIdToOpen = nav.targetId
            }
            NotificationType.ADMIN_REPORT -> {
                activeRoomId = null
                showChatHub = false
                selectedTab = VarsityTab.FEED
                openAdminAlertsTab = true
                showNotificationsSheet = true
            }
        }
        onConsumedPendingNotif()
    }

    val openStudentProfile: (String, String, String) -> Unit = { userId, fallbackName, fallbackUni ->
        if (userId.isNotBlank()) {
            viewedProfileUserId = userId
            viewedProfileFallbackName = fallbackName
            viewedProfileFallbackUni = fallbackUni
            if (userId != myProfile.userId) {
                sessionViewModel.openStudentProfile(userId, fallbackName, fallbackUni)
            }
        }
    }

    val searchFocusRequester = remember { FocusRequester() }
    LaunchedEffect(isGlobalSearchOpen) {
        if (isGlobalSearchOpen) {
            searchFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(viewedProfileUserId, viewedProfileFallbackName, viewedProfileFallbackUni) {
        val uid = viewedProfileUserId
        if (uid != null && uid != myProfile.userId) {
            sessionViewModel.openStudentProfile(uid, viewedProfileFallbackName, viewedProfileFallbackUni)
        }
    }

    LaunchedEffect(activeRoomId) {
        chatVm.openChannel(activeRoomId)
    }

    val activeProfileTargetId = viewedProfileUserId
    if (activeProfileTargetId != null) {
        UserProfileScreen(
            sessionViewModel = sessionViewModel,
            myProfile = myProfile,
            targetUserId = activeProfileTargetId,
            onBack = {
                viewedProfileUserId = null
                sessionViewModel.closeStudentProfile()
            },
            onMessageStudent = { peer ->
                viewedProfileUserId = null
                sessionViewModel.closeStudentProfile()
                chatVm.openDirectChat(myProfile, peer) { channelId ->
                    activeRoomId = channelId
                }
            },
            onOpenDirectory = {
                viewedProfileUserId = null
                sessionViewModel.closeStudentProfile()
                profileSheetTab = 0
                showProfileSheet = true
            },
            onSignOut = {
                viewedProfileUserId = null
                sessionViewModel.closeStudentProfile()
                handleSignOut()
            }
        )
        return
    }

    // Read clock tick so stale timestamps decay cleanly even without new snapshot writes
    @Suppress("UNUSED_EXPRESSION")
    presenceClockTick

    if (activeRoomId != null) {
        ChatRoomScreen(
            viewModel = chatVm,
            myProfile = myProfile,
            presenceMap = presenceMap,
            myPresenceStatus = myPresenceStatus,
            onSelectMyPresence = sessionViewModel::setManualPresence,
            onBack = { activeRoomId = null },
            onOpenStudentProfile = openStudentProfile
        )
        return
    }

    if (showChatHub) {
        ChatListScreen(
            viewModel = chatVm,
            myProfile = myProfile,
            presenceMap = presenceMap,
            myPresenceStatus = myPresenceStatus,
            onSelectMyPresence = sessionViewModel::setManualPresence,
            onOpenRoom = { channelId ->
                activeRoomId = channelId
            },
            onOpenDirectory = {
                profileSheetTab = 0
                showProfileSheet = true
            },
            onBack = { showChatHub = false }
        )
        if (showProfileSheet) {
            ModalBottomSheet(
                onDismissRequest = { showProfileSheet = false },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                ProfileAndDirectorySheetContent(
                    sessionViewModel = sessionViewModel,
                    myProfile = myProfile,
                    initialTab = profileSheetTab,
                    onViewProfile = { peer ->
                        showProfileSheet = false
                        openStudentProfile(peer.userId, peer.displayName, peer.university)
                    },
                    onMessageStudent = { peer ->
                        showProfileSheet = false
                        chatVm.openDirectChat(myProfile, peer) { channelId ->
                            activeRoomId = channelId
                        }
                    },
                    onSignOut = {
                        showProfileSheet = false
                        handleSignOut()
                    }
                )
            }
        }
        return
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid)
    ) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = ObsidianVoid,
            contentWindowInsets = WindowInsets.navigationBars,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Column {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = ObsidianVoid,
                            titleContentColor = CrispSnow
                        ),
                        navigationIcon = {
                            if (isGlobalSearchOpen) {
                                IconButton(
                                    onClick = {
                                        isGlobalSearchOpen = false
                                        globalSearchQuery = ""
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("global_search_close")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Close global search",
                                        tint = CrispSnow
                                    )
                                }
                            }
                        },
                        title = {
                            if (isGlobalSearchOpen) {
                                TextField(
                                    value = globalSearchQuery,
                                    onValueChange = { globalSearchQuery = it },
                                    placeholder = {
                                        Text(
                                            text = "Search posts, market, events...",
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedTextColor = CrispSnow,
                                        unfocusedTextColor = CrispSnow,
                                        cursorColor = ElectricCyan
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(searchFocusRequester)
                                        .testTag("global_search_input")
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "VarsityBW",
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = CrispSnow
                                    )
                                    UniversityBadge(myProfile.university)
                                }
                            }
                        },
                        actions = {
                            if (isGlobalSearchOpen) {
                                if (globalSearchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { globalSearchQuery = "" },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("global_search_clear")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Clear,
                                            contentDescription = "Clear search query",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                Surface(
                                    color = CarbonSurface,
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(0.8.dp, HairlineBorder),
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 38.dp)
                                        .clip(RoundedCornerShape(50))
                                        .clickable { isGlobalSearchOpen = true }
                                        .testTag("global_search_bar")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Search,
                                            contentDescription = "Search posts, market items, or events",
                                            tint = CrispSnow,
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Text(
                                            text = "Search",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        openAdminAlertsTab = false
                                        showNotificationsSheet = true
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("open_notifications_button")
                                ) {
                                    BadgedBox(
                                        badge = {
                                            if (totalUnreadNotifications > 0) {
                                                Badge(
                                                    containerColor = CoralCrimson,
                                                    contentColor = Color.White
                                                ) {
                                                    Text(
                                                        text = if (totalUnreadNotifications > 9) "9+" else "$totalUnreadNotifications"
                                                    )
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Notifications,
                                            contentDescription = "Notifications",
                                            tint = CrispSnow
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { showChatHub = true },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("open_chat_hub_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Forum,
                                        contentDescription = "Open campus chat and DMs",
                                        tint = CrispSnow
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        openStudentProfile(
                                            myProfile.userId,
                                            myProfile.displayName,
                                            myProfile.university
                                        )
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("open_profile_button")
                                ) {
                                    VarsityAvatar(
                                        name = myProfile.displayName,
                                        universityCode = myProfile.university,
                                        size = 32.dp,
                                        presenceStatus = myPresenceStatus
                                    )
                                }
                            }
                        }
                    )
                    HorizontalDivider(color = HairlineBorder, thickness = 0.8.dp)
                }
            },
            bottomBar = {
                if (!useRail) {
                    Column {
                        HorizontalDivider(color = HairlineBorder, thickness = 0.8.dp)
                        NavigationBar(
                            containerColor = ObsidianVoid,
                            contentColor = CrispSnow,
                            tonalElevation = 0.dp,
                            windowInsets = WindowInsets.navigationBars,
                            modifier = Modifier.testTag("varsity_bottom_nav")
                        ) {
                            VarsityTab.entries.forEach { tab ->
                                val selected = !isGlobalSearchOpen && selectedTab == tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        isGlobalSearchOpen = false
                                        globalSearchQuery = ""
                                        selectedTab = tab
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = CrispSnow,
                                        selectedTextColor = CrispSnow,
                                        indicatorColor = DeepCyanContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.testTag("nav_tab_${tab.route}")
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = ObsidianVoid
                    ) {
                        VarsityTab.entries.forEach { tab ->
                            val selected = !isGlobalSearchOpen && selectedTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    isGlobalSearchOpen = false
                                    globalSearchQuery = ""
                                    selectedTab = tab
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = CrispSnow,
                                    selectedTextColor = CrispSnow,
                                    indicatorColor = DeepCyanContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("rail_tab_${tab.route}")
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 720.dp)
                            .fillMaxSize()
                    ) {
                        if (isGlobalSearchOpen) {
                            GlobalSearchOverlay(
                                query = globalSearchQuery,
                                posts = allPosts,
                                listings = allListings,
                                events = allEvents,
                                currentUserId = currentUserId,
                                onSuggestionClick = { suggestion ->
                                    globalSearchQuery = suggestion
                                },
                                onDismiss = {
                                    isGlobalSearchOpen = false
                                    globalSearchQuery = ""
                                },
                                onOpenPost = {
                                    feedVm.setCampusFilter("ALL")
                                    feedVm.setCategoryFilter(null)
                                    isGlobalSearchOpen = false
                                    globalSearchQuery = ""
                                    selectedTab = VarsityTab.FEED
                                },
                                onOpenMarketItem = {
                                    marketEventsVm.setMarketCampusFilter("ALL")
                                    marketEventsVm.setMarketCategoryFilter(null)
                                    isGlobalSearchOpen = false
                                    globalSearchQuery = ""
                                    selectedTab = VarsityTab.MARKET
                                },
                                onContactSeller = { listing ->
                                    isGlobalSearchOpen = false
                                    globalSearchQuery = ""
                                    val sellerProfile = StudentProfile(
                                        userId = listing.sellerId,
                                        displayName = listing.sellerName,
                                        university = listing.sellerUniversity
                                    )
                                    chatVm.openDirectChat(myProfile, sellerProfile) { channelId ->
                                        activeRoomId = channelId
                                    }
                                },
                                onOpenEvent = { event ->
                                    marketEventsVm.setEventCampusFilter("ALL")
                                    marketEventsVm.setEventCategoryFilter(null)
                                    marketEventsVm.setSelectedDateIso(
                                        event.eventDateIso.takeIf { it.isNotBlank() }
                                    )
                                    isGlobalSearchOpen = false
                                    globalSearchQuery = ""
                                    selectedTab = VarsityTab.EVENTS
                                }
                            )
                        } else {
                            when (selectedTab) {
                                VarsityTab.FEED -> FeedScreen(
                                    viewModel = feedVm,
                                    myProfile = myProfile,
                                    presenceMap = presenceMap,
                                    onlinePeers = onlinePeers,
                                    initialOpenPostId = targetPostIdToOpen,
                                    onConsumedInitialPost = { targetPostIdToOpen = null },
                                    onOpenStudyGroups = { selectedTab = VarsityTab.STUDY },
                                    onOpenChat = { showChatHub = true },
                                    onOpenMarket = { selectedTab = VarsityTab.MARKET },
                                    onOpenEvents = { selectedTab = VarsityTab.EVENTS },
                                    onOpenStudentProfile = openStudentProfile,
                                    onMessageAuthor = { uid, name, uni ->
                                        val peer = StudentProfile(
                                            userId = uid,
                                            displayName = name,
                                            university = uni
                                        )
                                        chatVm.openDirectChat(myProfile, peer) { channelId ->
                                            activeRoomId = channelId
                                        }
                                    }
                                )
                                VarsityTab.MARKET -> MarketplaceScreen(
                                    viewModel = marketEventsVm,
                                    myProfile = myProfile,
                                    onMessageSeller = { sellerId, sellerName, sellerUni ->
                                        val sellerProfile = StudentProfile(
                                            userId = sellerId,
                                            displayName = sellerName,
                                            university = sellerUni
                                        )
                                        chatVm.openDirectChat(myProfile, sellerProfile) { channelId ->
                                            activeRoomId = channelId
                                        }
                                    }
                                )
                                VarsityTab.STUDY -> StudyGroupsScreen(
                                    viewModel = studyVm,
                                    myProfile = myProfile,
                                    onOpenGroupChat = { channelId ->
                                        activeRoomId = channelId
                                    }
                                )
                                VarsityTab.EVENTS -> EventsScreen(
                                    viewModel = marketEventsVm,
                                    myProfile = myProfile
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNotificationsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showNotificationsSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            NotificationsSheetContent(
                notificationsState = notificationsState,
                unreadCount = unreadNotificationCount,
                fcmToken = fcmToken,
                onMarkAllRead = sessionViewModel::markAllNotificationsRead,
                onPermissionGranted = sessionViewModel::syncFcmToken,
                isAdmin = isAdmin,
                initialAdminTab = openAdminAlertsTab,
                adminNotificationsState = adminNotificationsState,
                adminReportsState = adminReportsState,
                adminUnreadCount = adminUnreadCount,
                onAdminNotificationClick = { alert ->
                    sessionViewModel.markAdminNotificationRead(alert.id)
                    if (alert.postId.isNotBlank()) {
                        showNotificationsSheet = false
                        feedVm.setCampusFilter("ALL")
                        feedVm.setCategoryFilter(null)
                        selectedTab = VarsityTab.FEED
                        targetPostIdToOpen = alert.postId
                    }
                },
                onUpdateReportStatus = sessionViewModel::updatePostReportStatus,
                onRemoveFlaggedPost = sessionViewModel::removeFlaggedPost,
                onNotificationClick = { notif ->
                    sessionViewModel.markNotificationRead(notif.id)
                    showNotificationsSheet = false
                    when (NotificationType.fromRaw(notif.notifType)) {
                        NotificationType.CHAT_MESSAGE -> {
                            if (notif.targetId.isNotBlank()) {
                                activeRoomId = notif.targetId
                            }
                        }
                        NotificationType.POST_REPLY -> {
                            feedVm.setCampusFilter("ALL")
                            feedVm.setCategoryFilter(null)
                            selectedTab = VarsityTab.FEED
                            if (notif.targetId.isNotBlank()) {
                                targetPostIdToOpen = notif.targetId
                            }
                        }
                        NotificationType.ADMIN_REPORT -> {
                            feedVm.setCampusFilter("ALL")
                            feedVm.setCategoryFilter(null)
                            selectedTab = VarsityTab.FEED
                            if (notif.targetId.isNotBlank()) {
                                targetPostIdToOpen = notif.targetId
                            }
                        }
                    }
                }
            )
        }
    }

    if (showProfileSheet) {
        ModalBottomSheet(
            onDismissRequest = { showProfileSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            ProfileAndDirectorySheetContent(
                sessionViewModel = sessionViewModel,
                myProfile = myProfile,
                initialTab = profileSheetTab,
                onViewProfile = { peer ->
                    showProfileSheet = false
                    openStudentProfile(peer.userId, peer.displayName, peer.university)
                },
                onMessageStudent = { peer ->
                    showProfileSheet = false
                    chatVm.openDirectChat(myProfile, peer) { channelId ->
                        activeRoomId = channelId
                    }
                },
                onSignOut = {
                    showProfileSheet = false
                    handleSignOut()
                }
            )
        }
    }
}
