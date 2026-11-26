package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.PresenceStatus
import com.example.model.StudentProfile
import com.example.model.UiState
import com.example.model.University
import com.example.ui.components.CampusFilterStrip
import com.example.ui.components.ClayIconPod
import com.example.ui.components.GlassCard
import com.example.ui.components.NeubrutalBadge
import com.example.ui.components.PresenceStatusPill
import com.example.ui.components.SoftNeuCard
import com.example.ui.components.StateErrorCard
import com.example.ui.components.StateLoadingBox
import com.example.ui.components.UniversityBadge
import com.example.ui.components.VarsityAvatar
import com.example.ui.components.VarsityEmptyState
import com.example.ui.components.VerifiedStudentIcon
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CoralCrimson
import com.example.ui.theme.DeepCyanContainer
import com.example.ui.theme.DeepCyanNavy
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPula
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.IceCyanText
import com.example.ui.theme.KalahariAmber
import com.example.ui.theme.MintPulaText
import com.example.ui.theme.ObsidianVoid
import com.example.ui.viewmodel.SessionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    sessionViewModel: SessionViewModel,
    myProfile: StudentProfile,
    targetUserId: String,
    onBack: () -> Unit,
    onMessageStudent: (StudentProfile) -> Unit,
    onOpenDirectory: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOwnProfile = targetUserId == myProfile.userId
    val viewedState by sessionViewModel.viewedProfileState.collectAsStateWithLifecycle()
    val presenceMap by sessionViewModel.presenceMap.collectAsStateWithLifecycle()
    val preferredPresence by sessionViewModel.preferredPresence.collectAsStateWithLifecycle()
    val isSubmitting by sessionViewModel.isSubmitting.collectAsStateWithLifecycle()
    val targetPresence = if (isOwnProfile) {
        preferredPresence
    } else {
        presenceMap[targetUserId]?.effectiveStatus() ?: PresenceStatus.OFFLINE
    }
    var isEditing by remember(targetUserId) { mutableStateOf(false) }

    BackHandler {
        if (isEditing) isEditing = false else onBack()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianVoid)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("user_profile_screen")
    ) {
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            ),
            navigationIcon = {
                IconButton(
                    onClick = {
                        if (isEditing) isEditing = false else onBack()
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("profile_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            title = {
                Text(
                    text = when {
                        isEditing -> "Edit My Profile"
                        isOwnProfile -> "My Campus Profile"
                        else -> "Student Profile"
                    },
                    style = MaterialTheme.typography.titleLarge
                )
            },
            actions = {
                if (isOwnProfile) {
                    IconButton(
                        onClick = { isEditing = !isEditing },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("toggle_edit_profile_button")
                    ) {
                        Icon(
                            imageVector = if (isEditing) Icons.Outlined.Close else Icons.Outlined.Edit,
                            contentDescription = if (isEditing) "Cancel editing" else "Edit profile",
                            tint = ElectricCyan
                        )
                    }
                }
            }
        )
        HorizontalDivider(color = HairlineBorder)

        val resolvedState: UiState<StudentProfile?> = if (isOwnProfile) {
            UiState.Success(myProfile)
        } else {
            viewedState
        }

        when (resolvedState) {
            is UiState.Loading -> StateLoadingBox("Loading student profile...")
            is UiState.Error -> StateErrorCard(resolvedState.message)
            is UiState.Success -> {
                val profile = resolvedState.data
                if (profile == null) {
                    VarsityEmptyState(
                        icon = Icons.Outlined.Person,
                        title = "Profile unavailable",
                        subtitle = "This student profile could not be loaded right now.",
                        actionLabel = "Go Back",
                        onAction = onBack
                    )
                } else if (isOwnProfile && isEditing) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(
                            modifier = Modifier
                                .widthIn(max = 680.dp)
                                .fillMaxWidth()
                        ) {
                            MyProfileEditor(
                                profile = profile,
                                isSubmitting = isSubmitting,
                                onSave = { name, uni, faculty, year, bio, discoverable ->
                                    sessionViewModel.updateProfile(name, uni, faculty, year, bio, discoverable)
                                    isEditing = false
                                },
                                onSignOut = onSignOut
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        ProfileShowcaseContent(
                            profile = profile,
                            isOwnProfile = isOwnProfile,
                            presenceStatus = targetPresence,
                            onEditProfile = { isEditing = true },
                            onOpenDirectory = onOpenDirectory,
                            onMessageStudent = { onMessageStudent(profile) },
                            onSignOut = onSignOut,
                            modifier = Modifier
                                .widthIn(max = 680.dp)
                                .fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileShowcaseContent(
    profile: StudentProfile,
    isOwnProfile: Boolean,
    presenceStatus: PresenceStatus,
    onEditProfile: () -> Unit,
    onOpenDirectory: () -> Unit,
    onMessageStudent: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uni = University.fromCode(profile.university)

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // X / Facebook style cover banner + overlapping avatar
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                DeepCyanContainer,
                                Color(0xFF101824),
                                CarbonSurface
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 64.dp, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .background(ObsidianVoid, CircleShape)
                            .padding(4.dp)
                    ) {
                        VarsityAvatar(
                            name = profile.displayName,
                            universityCode = profile.university,
                            size = 76.dp,
                            presenceStatus = presenceStatus
                        )
                    }

                    if (isOwnProfile) {
                        OutlinedButton(
                            onClick = onEditProfile,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, HairlineBorder),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 40.dp)
                                .testTag("edit_my_profile_button")
                        ) {
                            Icon(
                                Icons.Outlined.Edit,
                                contentDescription = "Edit profile",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Edit profile",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        Button(
                            onClick = onMessageStudent,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricCyan,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 40.dp)
                                .testTag("profile_message_student_button")
                        ) {
                            Icon(
                                Icons.Outlined.Chat,
                                contentDescription = "Message student",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Message", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Name + Verified Blue Checkmark + University Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = profile.displayName.ifBlank { "VarsityBW Student" },
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (profile.acBwVerified || profile.verifiedDomain.endsWith(".ac.bw")) {
                        VerifiedStudentIcon(size = 18.dp)
                    }
                    UniversityBadge(profile.university)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (profile.verifiedDomain.isNotBlank()) {
                        Text(
                            text = "@${profile.verifiedDomain}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    PresenceStatusPill(status = presenceStatus)
                }

                Text(
                    text = profile.bio.ifBlank { "No bio added yet. Say dumela in chat!" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        HorizontalDivider(color = HairlineBorder, thickness = 0.8.dp)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                color = CarbonSurface,
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(0.8.dp, HairlineBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ClayIconPod(
                        icon = Icons.Outlined.School,
                        contentDescription = "University",
                        containerColor = DeepCyanContainer,
                        iconTint = ElectricCyan,
                        size = 40.dp
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "University",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = uni.fullName,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${uni.code} · ${uni.city}",
                            style = MaterialTheme.typography.bodySmall,
                            color = IceCyanText
                        )
                    }
                }
            }

            Surface(
                color = CarbonSurface,
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(0.8.dp, HairlineBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ClayIconPod(
                        icon = Icons.Outlined.MenuBook,
                        contentDescription = "Course and year",
                        containerColor = AmberContainer,
                        iconTint = KalahariAmber,
                        size = 40.dp
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Course & Year of Study",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = profile.faculty.ifBlank { "Course not specified" },
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Year ${profile.yearOfStudy.coerceIn(1L, 6L)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = KalahariAmber
                        )
                    }
                }
            }

            if (isOwnProfile) {
                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                    onClick = onOpenDirectory,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, HairlineBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("profile_open_directory_button")
                ) {
                    Icon(Icons.Outlined.PeopleOutline, contentDescription = "Browse students", tint = ElectricCyan, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Browse Botswana Student Directory", color = ElectricCyan)
                }

                OutlinedButton(
                    onClick = onSignOut,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, CoralCrimson.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("profile_sign_out_button")
                ) {
                    Icon(Icons.Outlined.Logout, contentDescription = "Sign out", tint = CoralCrimson, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Sign Out", color = CoralCrimson)
                }
            }
        }
    }
}

@Composable
fun ProfileAndDirectorySheetContent(
    sessionViewModel: SessionViewModel,
    myProfile: StudentProfile,
    initialTab: Int = 0,
    onViewProfile: (StudentProfile) -> Unit = {},
    onMessageStudent: (StudentProfile) -> Unit,
    onSignOut: () -> Unit
) {
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }
    val directoryState by sessionViewModel.directoryState.collectAsStateWithLifecycle()
    val presenceMap by sessionViewModel.presenceMap.collectAsStateWithLifecycle()
    val directoryCampus by sessionViewModel.directoryCampusFilter.collectAsStateWithLifecycle()
    val directoryQuery by sessionViewModel.directoryQuery.collectAsStateWithLifecycle()
    val isSubmitting by sessionViewModel.isSubmitting.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = ElectricCyan,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = ElectricCyan
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Student Directory") },
                modifier = Modifier.testTag("tab_student_directory")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("My Profile") },
                modifier = Modifier.testTag("tab_my_profile")
            )
        }

        if (selectedTab == 0) {
            OutlinedTextField(
                value = directoryQuery,
                onValueChange = sessionViewModel::setDirectoryQuery,
                placeholder = { Text("Search name, course, or campus...") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = "Search directory") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = HairlineBorder,
                    focusedContainerColor = CarbonSurface,
                    unfocusedContainerColor = CarbonSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )
            CampusFilterStrip(
                selectedCampus = directoryCampus,
                onSelectCampus = sessionViewModel::setDirectoryCampusFilter
            )

            when (val state = directoryState) {
                is UiState.Loading -> StateLoadingBox("Loading Botswana students...", Modifier.height(260.dp))
                is UiState.Error -> StateErrorCard(state.message)
                is UiState.Success -> {
                    val peers = state.data.filter { it.userId != myProfile.userId }
                    if (peers.isEmpty()) {
                        VarsityEmptyState(
                            icon = Icons.Outlined.PeopleOutline,
                            title = "No classmates match",
                            subtitle = "Adjust the campus filter or invite friends with .ac.bw emails to join VarsityBW."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(360.dp),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(peers, key = { it.userId }) { peer ->
                                Surface(
                                    color = CarbonSurface,
                                    shape = MaterialTheme.shapes.medium,
                                    border = BorderStroke(1.dp, HairlineBorder),
                                    modifier = Modifier.clickable { onViewProfile(peer) }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        val peerStatus = presenceMap[peer.userId]?.effectiveStatus()
                                            ?: PresenceStatus.OFFLINE
                                        VarsityAvatar(
                                            name = peer.displayName,
                                            universityCode = peer.university,
                                            size = 42.dp,
                                            presenceStatus = peerStatus
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = peer.displayName,
                                                    style = MaterialTheme.typography.titleSmall
                                                )
                                                UniversityBadge(peer.university)
                                                PresenceStatusPill(status = peerStatus)
                                            }
                                            Text(
                                                text = "${peer.faculty} • Yr ${peer.yearOfStudy}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Button(
                                            onClick = { onMessageStudent(peer) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = ElectricCyan,
                                                contentColor = DeepCyanNavy
                                            ),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                            modifier = Modifier.testTag("dm_student_${peer.userId}")
                                        ) {
                                            Icon(Icons.Outlined.Chat, contentDescription = "Message", modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Chat")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            MyProfileEditor(
                profile = myProfile,
                isSubmitting = isSubmitting,
                onSave = { name, uni, faculty, year, bio, discoverable ->
                    sessionViewModel.updateProfile(name, uni, faculty, year, bio, discoverable)
                },
                onSignOut = onSignOut,
                modifier = Modifier.height(440.dp)
            )
        }
    }
}

@Composable
private fun MyProfileEditor(
    profile: StudentProfile,
    isSubmitting: Boolean,
    onSave: (String, University, String, Int, String, Boolean) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var displayName by remember(profile) { mutableStateOf(profile.displayName) }
    var university by remember(profile) { mutableStateOf(University.fromCode(profile.university)) }
    var faculty by remember(profile) { mutableStateOf(profile.faculty) }
    var yearOfStudy by remember(profile) { mutableIntStateOf(profile.yearOfStudy.toInt().coerceIn(1, 5)) }
    var bio by remember(profile) { mutableStateOf(profile.bio) }
    var discoverable by remember(profile) { mutableStateOf(profile.discoverable) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            color = EmeraldContainer.copy(alpha = 0.55f),
            shape = MaterialTheme.shapes.small,
            border = BorderStroke(1.dp, EmeraldPula.copy(alpha = 0.55f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Filled.Verified, contentDescription = "Verified", tint = EmeraldPula)
                Column {
                    Text(
                        text = "Verified Botswana Student",
                        style = MaterialTheme.typography.titleSmall,
                        color = MintPulaText
                    )
                    Text(
                        text = "Domain: ${profile.verifiedDomain}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MintPulaText
                    )
                }
            }
        }

        OutlinedTextField(
            value = displayName,
            onValueChange = { if (it.length <= 50) displayName = it },
            label = { Text("Display Name") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_name_input")
        )

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            University.entries.forEach { uni ->
                FilterChip(
                    selected = university == uni,
                    onClick = { university = uni },
                    label = { Text(uni.code) }
                )
            }
        }

        OutlinedTextField(
            value = faculty,
            onValueChange = { if (it.length <= 60) faculty = it },
            label = { Text("Course / Faculty / Programme") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_course_input")
        )

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (1..5).forEach { yr ->
                FilterChip(
                    selected = yearOfStudy == yr,
                    onClick = { yearOfStudy = yr },
                    label = { Text("Yr $yr") }
                )
            }
        }

        OutlinedTextField(
            value = bio,
            onValueChange = { if (it.length <= 200) bio = it },
            label = { Text("Short Campus Bio") },
            maxLines = 3,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_bio_input")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Visible in Student Directory", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = discoverable, onCheckedChange = { discoverable = it })
        }

        Button(
            onClick = { onSave(displayName, university, faculty, yearOfStudy, bio, discoverable) },
            enabled = !isSubmitting && displayName.trim().length >= 2 && faculty.trim().length >= 2,
            colors = ButtonDefaults.buttonColors(
                containerColor = ElectricCyan,
                contentColor = DeepCyanNavy
            ),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .testTag("save_profile_button")
        ) {
            Text(if (isSubmitting) "Saving..." else "Save Profile")
        }

        HorizontalDivider(color = HairlineBorder)

        OutlinedButton(
            onClick = onSignOut,
            border = BorderStroke(1.dp, CoralCrimson.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .testTag("sign_out_button")
        ) {
            Icon(Icons.Outlined.Logout, contentDescription = "Sign out", tint = CoralCrimson, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Sign Out", color = CoralCrimson)
        }
    }
}
