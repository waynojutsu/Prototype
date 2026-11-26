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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.ChannelType
import com.example.model.ChatChannel
import com.example.model.ChatMessage
import com.example.model.PresenceStatus
import com.example.model.StudentProfile
import com.example.model.UiState
import com.example.model.University
import com.example.model.UserPresence
import com.example.ui.components.MyPresenceSelectorChip
import com.example.ui.components.PresenceStatusDot
import com.example.ui.components.PresenceStatusPill
import com.example.ui.components.StateErrorCard
import com.example.ui.components.StateLoadingBox
import com.example.ui.components.UniversityBadge
import com.example.ui.components.VarsityAvatar
import com.example.ui.components.VarsityEmptyState
import com.example.ui.components.formatTimestampShort
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.DeepCyanContainer
import com.example.ui.theme.DeepCyanNavy
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElevatedCarbon
import com.example.ui.theme.EmeraldPula
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.IceCyanText
import com.example.ui.theme.KalahariAmber
import com.example.ui.theme.ObsidianVoid
import com.example.ui.viewmodel.ChatViewModel

@Composable
fun ChatListScreen(
    viewModel: ChatViewModel,
    myProfile: StudentProfile,
    presenceMap: Map<String, UserPresence> = emptyMap(),
    myPresenceStatus: PresenceStatus = PresenceStatus.ONLINE,
    onSelectMyPresence: (PresenceStatus) -> Unit = {},
    onOpenRoom: (String) -> Unit,
    onOpenDirectory: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val publicState by viewModel.publicChannelsState.collectAsStateWithLifecycle()
    val directState by viewModel.myMemberChannelsState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showCreateChannel by remember { mutableStateOf(false) }

    if (onBack != null) {
        BackHandler(onBack = onBack)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianVoid)
            .then(
                if (onBack != null) Modifier.statusBarsPadding().navigationBarsPadding()
                else Modifier
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (onBack != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, HairlineBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("chat_list_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to main tabs",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "Campus Chat & DMs",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        MyPresenceSelectorChip(
                            status = myPresenceStatus,
                            onSelectStatus = onSelectMyPresence
                        )
                    }
                }
            }

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
                    text = { Text("Campus Lounges") },
                    modifier = Modifier.testTag("chat_tab_lounges")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("DMs & Group Chats") },
                    modifier = Modifier.testTag("chat_tab_direct")
                )
            }

            if (selectedTab == 0) {
                when (val state = publicState) {
                    is UiState.Loading -> StateLoadingBox("Loading campus lounges...")
                    is UiState.Error -> StateErrorCard(state.message)
                    is UiState.Success -> {
                        if (state.data.isEmpty()) {
                            VarsityEmptyState(
                                icon = Icons.Outlined.Forum,
                                title = "Open a Campus Lounge",
                                subtitle = "Create a public chat room for UB, BIUST, BAC, Botho, BUAN, or any course code.",
                                actionLabel = "New Lounge",
                                onAction = { showCreateChannel = true }
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(state.data, key = { it.id }) { channel ->
                                    ChannelListCard(
                                        channel = channel,
                                        myUserId = myProfile.userId,
                                        presenceMap = presenceMap,
                                        onClick = {
                                            viewModel.openChannel(channel.id)
                                            onOpenRoom(channel.id)
                                        }
                                    )
                                }
                                item { Spacer(Modifier.height(72.dp)) }
                            }
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    OutlinedButton(
                        onClick = onOpenDirectory,
                        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .testTag("find_student_dm_button")
                    ) {
                        Icon(Icons.Outlined.PersonSearch, contentDescription = "Find classmate", tint = ElectricCyan)
                        Spacer(Modifier.width(8.dp))
                        Text("Find a Botswana Student to Message", color = ElectricCyan)
                    }

                    when (val state = directState) {
                        is UiState.Loading -> StateLoadingBox("Loading private chats...")
                        is UiState.Error -> StateErrorCard(state.message)
                        is UiState.Success -> {
                            if (state.data.isEmpty()) {
                                VarsityEmptyState(
                                    icon = Icons.Outlined.Lock,
                                    title = "No private chats yet",
                                    subtitle = "Message a classmate from the student directory, tap Message Seller in Market, or get accepted into a Study Group.",
                                    actionLabel = "Browse Students",
                                    onAction = onOpenDirectory
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(state.data, key = { it.id }) { channel ->
                                        ChannelListCard(
                                            channel = channel,
                                            myUserId = myProfile.userId,
                                            presenceMap = presenceMap,
                                            onClick = {
                                                viewModel.openChannel(channel.id)
                                                onOpenRoom(channel.id)
                                            }
                                        )
                                    }
                                    item { Spacer(Modifier.height(72.dp)) }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (selectedTab == 0) {
            FloatingActionButton(
                onClick = { showCreateChannel = true },
                containerColor = ElectricCyan,
                contentColor = DeepCyanNavy,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .testTag("create_channel_fab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Create public lounge")
            }
        }
    }

    if (showCreateChannel) {
        CreateChannelDialog(
            defaultUniversity = myProfile.university,
            onDismiss = { showCreateChannel = false },
            onSubmit = { name, desc, uniCode, courseCode ->
                viewModel.createPublicChannel(myProfile, name, desc, uniCode, courseCode) { channelId ->
                    showCreateChannel = false
                    onOpenRoom(channelId)
                }
            }
        )
    }
}

@Composable
private fun ChannelListCard(
    channel: ChatChannel,
    myUserId: String,
    presenceMap: Map<String, UserPresence>,
    onClick: () -> Unit
) {
    val type = ChannelType.fromRaw(channel.channelType)
    val directPeerUid = if (type == ChannelType.DIRECT) {
        channel.memberUids.firstOrNull { it != myUserId }
    } else null
    val peerPresence = directPeerUid?.let {
        presenceMap[it]?.effectiveStatus() ?: PresenceStatus.OFFLINE
    }
    val onlineCount = when (type) {
        ChannelType.DIRECT -> 0
        ChannelType.STUDY_GROUP -> channel.memberUids.count { uid ->
            presenceMap[uid]?.effectiveStatus() == PresenceStatus.ONLINE
        }
        ChannelType.CAMPUS_LOUNGE -> presenceMap.values.count {
            it.effectiveStatus() == PresenceStatus.ONLINE
        }
    }

    Surface(
        color = CarbonSurface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("channel_card_${channel.id}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (directPeerUid != null && peerPresence != null) {
                VarsityAvatar(
                    name = channel.name,
                    universityCode = channel.university,
                    size = 42.dp,
                    presenceStatus = peerPresence
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    UniversityBadge(channel.university)
                    if (channel.courseCode.isNotBlank()) {
                        Surface(color = DeepCyanContainer, shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = channel.courseCode,
                                style = MaterialTheme.typography.labelSmall,
                                color = IceCyanText,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                    if (peerPresence != null) {
                        PresenceStatusPill(status = peerPresence)
                    } else if (onlineCount > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            PresenceStatusDot(status = PresenceStatus.ONLINE, size = 8.dp, borderWidth = 1.dp)
                            Text(
                                text = "$onlineCount online",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldPula
                            )
                        }
                    } else {
                        Text(
                            text = type.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = formatTimestampShort(channel.updatedAt ?: channel.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

            if (channel.lastMessageText.isNotBlank()) {
                Text(
                    text = "${channel.lastMessageSender}: ${channel.lastMessageText}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else if (channel.description.isNotBlank()) {
                Text(
                    text = channel.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            }
        }
    }
}

@Composable
fun ChatRoomScreen(
    viewModel: ChatViewModel,
    myProfile: StudentProfile,
    presenceMap: Map<String, UserPresence> = emptyMap(),
    myPresenceStatus: PresenceStatus = PresenceStatus.ONLINE,
    onSelectMyPresence: (PresenceStatus) -> Unit = {},
    onBack: () -> Unit,
    onOpenStudentProfile: (userId: String, name: String, university: String) -> Unit = { _, _, _ -> }
) {
    val channel by viewModel.activeChannelState.collectAsStateWithLifecycle()
    val messagesState by viewModel.activeMessagesState.collectAsStateWithLifecycle()
    var messageInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    BackHandler(onBack = onBack)

    val messageList = (messagesState as? UiState.Success)?.data.orEmpty()
    LaunchedEffect(messageList.size) {
        if (messageList.isNotEmpty()) {
            listState.animateScrollToItem(messageList.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, HairlineBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("chat_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to chats",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                val directPeerUid = channel?.let { ch ->
                    if (ChannelType.fromRaw(ch.channelType) == ChannelType.DIRECT) {
                        ch.memberUids.firstOrNull { it != myProfile.userId }
                    } else null
                }
                val directPeerStatus = directPeerUid?.let {
                    presenceMap[it]?.effectiveStatus() ?: PresenceStatus.OFFLINE
                }
                if (directPeerUid != null && channel != null && directPeerStatus != null) {
                    VarsityAvatar(
                        name = channel!!.name,
                        universityCode = channel!!.university,
                        size = 38.dp,
                        presenceStatus = directPeerStatus,
                        modifier = Modifier
                            .clip(RoundedCornerShape(19.dp))
                            .clickable {
                                val ch = channel!!
                                onOpenStudentProfile(directPeerUid, ch.name, ch.university)
                            }
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (directPeerUid != null && channel != null) {
                                Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        val ch = channel!!
                                        onOpenStudentProfile(directPeerUid, ch.name, ch.university)
                                    }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                    .testTag("chat_room_peer_header")
                            } else Modifier
                        )
                ) {
                    Text(
                        text = channel?.name ?: "Loading room...",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    channel?.let { ch ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            UniversityBadge(ch.university)
                            if (ch.courseCode.isNotBlank()) {
                                Text(
                                    text = ch.courseCode,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ElectricCyan
                                )
                            }
                            if (directPeerStatus != null) {
                                PresenceStatusPill(status = directPeerStatus)
                            } else {
                                val roomOnlineCount = if (ChannelType.fromRaw(ch.channelType) == ChannelType.STUDY_GROUP) {
                                    ch.memberUids.count { uid ->
                                        presenceMap[uid]?.effectiveStatus() == PresenceStatus.ONLINE
                                    }
                                } else {
                                    presenceMap.values.count {
                                        it.effectiveStatus() == PresenceStatus.ONLINE
                                    }
                                }
                                if (roomOnlineCount > 0) {
                                    PresenceStatusDot(status = PresenceStatus.ONLINE, size = 8.dp, borderWidth = 1.dp)
                                    Text(
                                        text = "$roomOnlineCount online",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldPula
                                    )
                                } else {
                                    Text(
                                        text = ChannelType.fromRaw(ch.channelType).label,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                MyPresenceSelectorChip(
                    status = myPresenceStatus,
                    onSelectStatus = onSelectMyPresence
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (val state = messagesState) {
                is UiState.Loading -> StateLoadingBox("Connecting to live chat...")
                is UiState.Error -> StateErrorCard(state.message)
                is UiState.Success -> {
                    if (state.data.isEmpty()) {
                        VarsityEmptyState(
                            icon = Icons.Outlined.Forum,
                            title = "Dumela!",
                            subtitle = "Break the ice and send the first message in this room."
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(state.data, key = { it.id }) { msg ->
                                val isMine = msg.senderId == viewModel.currentUserId
                                val senderStatus = if (isMine) {
                                    myPresenceStatus
                                } else {
                                    presenceMap[msg.senderId]?.effectiveStatus() ?: PresenceStatus.OFFLINE
                                }
                                ChatMessageBubble(
                                    message = msg,
                                    isMine = isMine,
                                    senderStatus = senderStatus,
                                    onSenderClick = {
                                        onOpenStudentProfile(
                                            msg.senderId,
                                            msg.senderName,
                                            msg.senderUniversity
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        val quickPrompts = remember(channel?.channelType) {
            when (ChannelType.fromRaw(channel?.channelType)) {
                ChannelType.STUDY_GROUP -> listOf(
                    "When is our next session?",
                    "Sharing past paper notes",
                    "Can someone explain this topic?",
                    "I'm at the library now"
                )
                ChannelType.DIRECT -> listOf(
                    "Dumela! Is this still available?",
                    "Can we meet at Student Centre?",
                    "What's your best price?",
                    "On my way!"
                )
                ChannelType.CAMPUS_LOUNGE -> listOf(
                    "Dumela! Anyone around campus?",
                    "Library seat update?",
                    "Any upcoming club events?",
                    "Good luck with tests!"
                )
            }
        }

        HorizontalDivider(color = HairlineBorder)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickPrompts.forEach { prompt ->
                Surface(
                    color = DeepCyanContainer.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { messageInput = prompt }
                ) {
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.labelMedium,
                        color = IceCyanText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ObsidianVoid)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = messageInput,
                onValueChange = { if (it.length <= 1000) messageInput = it },
                placeholder = { Text("Message...") },
                maxLines = 4,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = HairlineBorder,
                    focusedContainerColor = CarbonSurface,
                    unfocusedContainerColor = CarbonSurface
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_message_input")
            )
            IconButton(
                onClick = {
                    val active = channel ?: return@IconButton
                    val trimmed = messageInput.trim()
                    if (trimmed.isNotEmpty()) {
                        viewModel.sendMessage(active, myProfile, trimmed) {
                            messageInput = ""
                        }
                    }
                },
                enabled = channel != null && messageInput.trim().isNotEmpty(),
                modifier = Modifier
                    .size(48.dp)
                    .testTag("send_chat_message_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send message",
                    tint = ElectricCyan
                )
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: ChatMessage,
    isMine: Boolean,
    senderStatus: PresenceStatus,
    onSenderClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .padding(
                    bottom = 4.dp,
                    start = if (isMine) 0.dp else 2.dp,
                    end = if (isMine) 2.dp else 0.dp
                )
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onSenderClick)
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .testTag("chat_sender_${message.id}")
        ) {
            if (!isMine) {
                VarsityAvatar(
                    name = message.senderName,
                    universityCode = message.senderUniversity,
                    size = 24.dp,
                    presenceStatus = senderStatus
                )
            } else {
                PresenceStatusDot(
                    status = senderStatus,
                    size = 8.dp,
                    borderWidth = 1.dp
                )
            }
            Text(
                text = if (isMine) "You" else message.senderName,
                style = MaterialTheme.typography.labelLarge,
                color = if (isMine) ElectricCyan else MaterialTheme.colorScheme.onSurface
            )
            UniversityBadge(message.senderUniversity)
        }

        Surface(
            color = if (isMine) ElectricCyan else ElevatedCarbon,
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isMine) 20.dp else 6.dp,
                bottomEnd = if (isMine) 6.dp else 20.dp
            ),
            modifier = Modifier.widthIn(max = 310.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isMine) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = formatTimestampShort(message.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isMine) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.78f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
private fun CreateChannelDialog(
    defaultUniversity: String,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var universityCode by remember { mutableStateOf(defaultUniversity) }
    var courseCode by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("New Campus Lounge", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = universityCode == "ALL",
                        onClick = { universityCode = "ALL" },
                        label = { Text("ALL BW") }
                    )
                    University.entries.forEach { uni ->
                        FilterChip(
                            selected = universityCode == uni.code,
                            onClick = { universityCode = uni.code },
                            label = { Text(uni.code) }
                        )
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 80) name = it },
                    label = { Text("Room Name") },
                    placeholder = { Text("e.g. UB Block 247 Tech Lounge") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("channel_name_input")
                )
                OutlinedTextField(
                    value = courseCode,
                    onValueChange = { if (it.length <= 16) courseCode = it.uppercase() },
                    label = { Text("Course Code (Optional)") },
                    placeholder = { Text("e.g. CSI142") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 240) description = it },
                    label = { Text("Topic / Description") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(name, description, universityCode, courseCode) },
                enabled = name.trim().length >= 2,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricCyan,
                    contentColor = DeepCyanNavy
                ),
                modifier = Modifier.testTag("submit_channel_button")
            ) {
                Text("Create Room")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
