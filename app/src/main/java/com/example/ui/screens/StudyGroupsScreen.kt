package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.JoinRequestStatus
import com.example.model.StudentProfile
import com.example.model.StudyGroup
import com.example.model.StudyGroupStatus
import com.example.model.StudyJoinRequest
import com.example.model.UiState
import com.example.model.University
import com.example.ui.components.CampusFilterStrip
import com.example.ui.components.StateErrorCard
import com.example.ui.components.StateLoadingBox
import com.example.ui.components.UniversityBadge
import com.example.ui.components.VarsityAvatar
import com.example.ui.components.VarsityEmptyState
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CoralCrimson
import com.example.ui.theme.DeepAmberInk
import com.example.ui.theme.DeepCyanContainer
import com.example.ui.theme.DeepCyanNavy
import com.example.ui.theme.DeepEmeraldInk
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElevatedCarbon
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPula
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.IceCyanText
import com.example.ui.theme.KalahariAmber
import com.example.ui.theme.MintPulaText
import com.example.ui.theme.WarmAmberText
import com.example.ui.viewmodel.StudyGroupsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyGroupsScreen(
    viewModel: StudyGroupsViewModel,
    myProfile: StudentProfile,
    onOpenGroupChat: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val groupsState by viewModel.studyGroupsState.collectAsStateWithLifecycle()
    val campusFilter by viewModel.campusFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val joinRequestsState by viewModel.joinRequestsState.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var requestTargetGroup by remember { mutableStateOf<StudyGroup?>(null) }
    var managedGroup by remember { mutableStateOf<StudyGroup?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::setSearchQuery,
                placeholder = { Text("Search course code (e.g. CSI241) or study topic") },
                leadingIcon = {
                    Icon(Icons.Outlined.Search, contentDescription = "Search study groups")
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = HairlineBorder,
                    focusedContainerColor = CarbonSurface,
                    unfocusedContainerColor = CarbonSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("study_group_search_input")
            )

            CampusFilterStrip(
                selectedCampus = campusFilter,
                onSelectCampus = viewModel::setCampusFilter
            )

            when (val state = groupsState) {
                is UiState.Loading -> StateLoadingBox("Loading study groups...")
                is UiState.Error -> StateErrorCard(state.message)
                is UiState.Success -> {
                    if (state.data.isEmpty()) {
                        VarsityEmptyState(
                            icon = Icons.Outlined.Groups,
                            title = "No study groups found",
                            subtitle = "Create a study post for your university, course code, and topic so classmates can request to join.",
                            actionLabel = "Post Study Group",
                            onAction = { showCreateDialog = true }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = 96.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.data, key = { it.id }) { group ->
                                StudyGroupCard(
                                    group = group,
                                    currentUserId = viewModel.currentUserId,
                                    onRequestJoin = { requestTargetGroup = group },
                                    onCancelRequest = { viewModel.cancelRequest(group.id) },
                                    onManageRequests = {
                                        managedGroup = group
                                        viewModel.openManageRequests(group.id)
                                    },
                                    onOpenChat = {
                                        if (group.linkedChannelId.isNotBlank()) {
                                            onOpenGroupChat(group.linkedChannelId)
                                        }
                                    },
                                    onLeaveGroup = { viewModel.leaveGroup(group) }
                                )
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showCreateDialog = true },
            containerColor = ElectricCyan,
            contentColor = DeepCyanNavy,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("create_study_group_fab")
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Create study group post")
        }
    }

    if (showCreateDialog) {
        CreateStudyGroupDialog(
            defaultUniversity = University.fromCode(myProfile.university),
            onDismiss = { showCreateDialog = false },
            onSubmit = { uni, code, topic, desc, schedule, maxMembers ->
                viewModel.createStudyGroup(
                    profile = myProfile,
                    university = uni,
                    courseCode = code,
                    studyTopic = topic,
                    description = desc,
                    meetingSchedule = schedule,
                    maxMembers = maxMembers
                ) {
                    showCreateDialog = false
                }
            }
        )
    }

    val targetGroup = requestTargetGroup
    if (targetGroup != null) {
        JoinRequestDialog(
            group = targetGroup,
            onDismiss = { requestTargetGroup = null },
            onSubmit = { note ->
                viewModel.requestToJoin(targetGroup, myProfile, note) {
                    requestTargetGroup = null
                }
            }
        )
    }

    val currentManaged = managedGroup
    if (currentManaged != null) {
        val latestGroup = (groupsState as? UiState.Success)?.data
            ?.firstOrNull { it.id == currentManaged.id } ?: currentManaged
        ModalBottomSheet(
            onDismissRequest = {
                managedGroup = null
                viewModel.openManageRequests(null)
            },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            ManageJoinRequestsSheet(
                group = latestGroup,
                requestsState = joinRequestsState,
                onApprove = { req -> viewModel.respondToRequest(latestGroup, req, approve = true) },
                onDecline = { req -> viewModel.respondToRequest(latestGroup, req, approve = false) }
            )
        }
    }
}

@Composable
private fun StudyGroupCard(
    group: StudyGroup,
    currentUserId: String,
    onRequestJoin: () -> Unit,
    onCancelRequest: () -> Unit,
    onManageRequests: () -> Unit,
    onOpenChat: () -> Unit,
    onLeaveGroup: () -> Unit
) {
    val isCreator = group.creatorId == currentUserId
    val isMember = group.memberUids.contains(currentUserId)
    val isPending = group.pendingRequestUids.contains(currentUserId)
    val status = StudyGroupStatus.fromRaw(group.groupStatus)
    val pendingCount = group.pendingRequestUids.size

    Surface(
        color = CarbonSurface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(
            1.dp,
            if (isCreator && pendingCount > 0) KalahariAmber.copy(alpha = 0.6f) else HairlineBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("study_group_card_${group.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = DeepCyanContainer,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = group.courseCode,
                        style = MaterialTheme.typography.labelMedium,
                        color = IceCyanText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                UniversityBadge(group.university)
                Spacer(Modifier.weight(1f))
                Surface(
                    color = when (status) {
                        StudyGroupStatus.OPEN -> EmeraldContainer
                        StudyGroupStatus.FULL -> AmberContainer
                        StudyGroupStatus.CLOSED -> ElevatedCarbon
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${group.memberUids.size}/${group.maxMembers} • ${status.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = when (status) {
                            StudyGroupStatus.OPEN -> MintPulaText
                            StudyGroupStatus.FULL -> WarmAmberText
                            StudyGroupStatus.CLOSED -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = group.studyTopic,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = group.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (group.meetingSchedule.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = "Schedule",
                        tint = KalahariAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = group.meetingSchedule,
                        style = MaterialTheme.typography.bodySmall,
                        color = WarmAmberText
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VarsityAvatar(name = group.creatorName, universityCode = group.creatorUniversity, size = 26.dp)
                Text(
                    text = "Hosted by ${group.creatorName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(color = HairlineBorder)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when {
                    isCreator -> {
                        Button(
                            onClick = onManageRequests,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (pendingCount > 0) KalahariAmber else ElevatedCarbon,
                                contentColor = if (pendingCount > 0) DeepAmberInk else MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("manage_requests_${group.id}")
                        ) {
                            Text(if (pendingCount > 0) "Requests ($pendingCount)" else "Requests")
                        }
                        Button(
                            onClick = onOpenChat,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricCyan,
                                contentColor = androidx.compose.ui.graphics.Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_group_chat_${group.id}")
                        ) {
                            Icon(Icons.Outlined.Chat, contentDescription = "Group chat", modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Group Chat")
                        }
                    }
                    isMember -> {
                        Button(
                            onClick = onOpenChat,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricCyan,
                                contentColor = androidx.compose.ui.graphics.Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_group_chat_${group.id}")
                        ) {
                            Icon(Icons.Outlined.Chat, contentDescription = "Group chat", modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Open Group Chat")
                        }
                        TextButton(
                            onClick = onLeaveGroup,
                            modifier = Modifier.testTag("leave_group_${group.id}")
                        ) {
                            Text("Leave", color = CoralCrimson)
                        }
                    }
                    isPending -> {
                        OutlinedButton(
                            onClick = onCancelRequest,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, KalahariAmber),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("withdraw_request_${group.id}")
                        ) {
                            Text("Request Pending · Withdraw", color = WarmAmberText)
                        }
                    }
                    else -> {
                        Button(
                            onClick = onRequestJoin,
                            enabled = status == StudyGroupStatus.OPEN,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricCyan,
                                contentColor = androidx.compose.ui.graphics.Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("request_join_button_${group.id}")
                        ) {
                            Icon(Icons.Filled.GroupAdd, contentDescription = "Request to join", modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (status == StudyGroupStatus.OPEN) "Request to Join Group" else "Group ${status.label}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateStudyGroupDialog(
    defaultUniversity: University,
    onDismiss: () -> Unit,
    onSubmit: (University, String, String, String, String, Int) -> Unit
) {
    var university by remember { mutableStateOf(defaultUniversity) }
    var courseCode by remember { mutableStateOf("") }
    var studyTopic by remember { mutableStateOf("") }
    var schedule by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var maxMembers by remember { mutableIntStateOf(6) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Create Study Group Post", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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
                    value = courseCode,
                    onValueChange = { if (it.length <= 16) courseCode = it.uppercase() },
                    label = { Text("Course Code") },
                    placeholder = { Text("e.g. CSI241, MAT111, ACC202") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("study_course_code_input")
                )
                OutlinedTextField(
                    value = studyTopic,
                    onValueChange = { if (it.length <= 100) studyTopic = it },
                    label = { Text("Study Topic") },
                    placeholder = { Text("e.g. Data Structures & Past Exam Papers") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("study_topic_input")
                )
                OutlinedTextField(
                    value = schedule,
                    onValueChange = { if (it.length <= 100) schedule = it },
                    label = { Text("Schedule & Spot (Optional)") },
                    placeholder = { Text("e.g. Wed 17:30 • UB Library 2nd Floor") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 600) description = it },
                    label = { Text("Group Goals & Details") },
                    placeholder = { Text("What are you preparing for and who should join?") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("study_description_input")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Max Students: $maxMembers", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { if (maxMembers > 2) maxMembers-- },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) { Text("-") }
                        OutlinedButton(
                            onClick = { if (maxMembers < 30) maxMembers++ },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) { Text("+") }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(university, courseCode, studyTopic, description, schedule, maxMembers)
                },
                enabled = courseCode.trim().length >= 2 &&
                    studyTopic.trim().length >= 3 &&
                    description.trim().isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricCyan,
                    contentColor = DeepCyanNavy
                ),
                modifier = Modifier.testTag("submit_study_group_button")
            ) {
                Text("Post Group")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun JoinRequestDialog(
    group: StudyGroup,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "Join ${group.courseCode}",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Send a join request to ${group.creatorName} for \"${group.studyTopic}\".",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { if (it.length <= 200) note = it },
                    label = { Text("Intro Note (Optional)") },
                    placeholder = { Text("e.g. Also taking ${group.courseCode}, ready to revise!") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("join_request_note_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(note) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricCyan,
                    contentColor = DeepCyanNavy
                ),
                modifier = Modifier.testTag("send_join_request_button")
            ) {
                Text("Send Request")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ManageJoinRequestsSheet(
    group: StudyGroup,
    requestsState: UiState<List<StudyJoinRequest>>,
    onApprove: (StudyJoinRequest) -> Unit,
    onDecline: (StudyJoinRequest) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "${group.courseCode} • Join Requests",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "${group.memberUids.size}/${group.maxMembers} seats filled. Accepted students join the private group chat automatically.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        when (requestsState) {
            is UiState.Loading -> StateLoadingBox("Loading requests...", Modifier.height(140.dp))
            is UiState.Error -> StateErrorCard(requestsState.message)
            is UiState.Success -> {
                val pending = requestsState.data.filter {
                    it.requestStatus == JoinRequestStatus.PENDING.raw
                }
                val handled = requestsState.data.filter {
                    it.requestStatus != JoinRequestStatus.PENDING.raw
                }

                if (requestsState.data.isEmpty()) {
                    Text(
                        text = "No students have requested to join yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 20.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(pending + handled, key = { it.requesterId }) { req ->
                            val reqStatus = JoinRequestStatus.fromRaw(req.requestStatus)
                            Surface(
                                color = CarbonSurface,
                                shape = MaterialTheme.shapes.medium,
                                border = BorderStroke(1.dp, HairlineBorder)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        VarsityAvatar(
                                            name = req.requesterName,
                                            universityCode = req.requesterUniversity,
                                            size = 36.dp
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = req.requesterName,
                                                    style = MaterialTheme.typography.titleSmall
                                                )
                                                UniversityBadge(req.requesterUniversity)
                                            }
                                            Text(
                                                text = req.requesterFaculty,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    if (req.note.isNotBlank()) {
                                        Text(
                                            text = "\"${req.note}\"",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    if (reqStatus == JoinRequestStatus.PENDING) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = { onDecline(req) },
                                                border = BorderStroke(1.dp, CoralCrimson.copy(alpha = 0.6f)),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("decline_request_${req.requesterId}")
                                            ) {
                                                Icon(Icons.Filled.Close, contentDescription = "Decline", tint = CoralCrimson, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Decline", color = CoralCrimson)
                                            }
                                            Button(
                                                onClick = { onApprove(req) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = EmeraldPula,
                                                    contentColor = DeepEmeraldInk
                                                ),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("approve_request_${req.requesterId}")
                                            ) {
                                                Icon(Icons.Filled.Check, contentDescription = "Approve", modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Approve")
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = "Status: ${reqStatus.label}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (reqStatus == JoinRequestStatus.ACCEPTED) EmeraldPula else CoralCrimson
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
