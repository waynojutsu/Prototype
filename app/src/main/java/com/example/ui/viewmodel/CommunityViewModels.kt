package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.VarsityRepository
import com.example.data.unwrapFirestoreError
import com.example.model.CampusEvent
import com.example.model.CampusPost
import com.example.model.ChatChannel
import com.example.model.ChatMessage
import com.example.model.EventCategory
import com.example.model.ListingCondition
import com.example.model.MarketCategory
import com.example.model.MarketListing
import com.example.model.PostCategory
import com.example.model.PostComment
import com.example.model.ReportReason
import com.example.model.StudentProfile
import com.example.model.StudyGroup
import com.example.model.StudyJoinRequest
import com.example.model.UiState
import com.example.model.University
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FeedViewModel(
    private val repository: VarsityRepository,
    val currentUserId: String
) : ViewModel() {

    private val _campusFilter = MutableStateFlow("ALL")
    val campusFilter: StateFlow<String> = _campusFilter.asStateFlow()

    private val _categoryFilter = MutableStateFlow<PostCategory?>(null)
    val categoryFilter: StateFlow<PostCategory?> = _categoryFilter.asStateFlow()

    private val _selectedPostIdForComments = MutableStateFlow<String?>(null)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _refreshTrigger = MutableStateFlow(0)

    private val _feedback = MutableSharedFlow<String>()
    val feedback: SharedFlow<String> = _feedback.asSharedFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val livePostsFlow = _refreshTrigger.flatMapLatest {
        repository.observePosts(currentUserId)
    }

    val allPosts: StateFlow<List<CampusPost>> = livePostsFlow
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val postsState: StateFlow<UiState<List<CampusPost>>> = combine(
        livePostsFlow,
        _campusFilter,
        _categoryFilter
    ) { posts, campus, category ->
        val filtered = posts.filter { post ->
            (campus == "ALL" || post.authorUniversity == campus) &&
                (category == null || post.category == category.name)
        }
        UiState.Success(filtered) as UiState<List<CampusPost>>
    }
        .catch { error ->
            val root = unwrapFirestoreError(error) ?: throw error
            Log.w("FeedVM", "Error observing posts", root)
            emit(UiState.Error(root.localizedMessage ?: "Failed to load campus feed"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    @OptIn(ExperimentalCoroutinesApi::class)
    val commentsState: StateFlow<UiState<List<PostComment>>> = _selectedPostIdForComments
        .flatMapLatest { postId ->
            if (postId.isNullOrBlank()) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observeComments(postId, currentUserId)
                    .map<List<PostComment>, UiState<List<PostComment>>> { UiState.Success(it) }
                    .catch { error ->
                        val root = unwrapFirestoreError(error) ?: throw error
                        Log.w("FeedVM", "Error observing comments", root)
                        emit(UiState.Error(root.localizedMessage ?: "Failed to load replies"))
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Success(emptyList()))

    val reportedPostIds: StateFlow<Set<String>> = repository.observeMyReportedPostIds(currentUserId)
        .catch { emit(emptySet()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    fun setCampusFilter(campus: String) {
        _campusFilter.value = campus
    }

    fun setCategoryFilter(category: PostCategory?) {
        _categoryFilter.value = category
    }

    fun selectPostForComments(postId: String?) {
        _selectedPostIdForComments.value = postId
    }

    fun refreshFeed() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val result = repository.fetchLatestPosts()
                result.onSuccess {
                    _refreshTrigger.value += 1
                }.onFailure { err ->
                    _feedback.emit(err.localizedMessage ?: "Could not refresh feed")
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun createPost(profile: StudentProfile, category: PostCategory, title: String, body: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.createPost(profile, category, title, body)
                .onSuccess {
                    onSuccess()
                    _feedback.emit("Posted to VarsityBW feed")
                }
                .onFailure { _feedback.emit(it.localizedMessage ?: "Could not publish post") }
        }
    }

    fun toggleLike(post: CampusPost) {
        viewModelScope.launch {
            val liked = post.likedByUids.contains(currentUserId)
            repository.togglePostLike(post.id, liked)
                .onFailure { _feedback.emit(it.localizedMessage ?: "Could not update like") }
        }
    }

    fun addComment(post: CampusPost, profile: StudentProfile, body: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.addComment(post, profile, body)
                .onSuccess { onSuccess() }
                .onFailure { _feedback.emit(it.localizedMessage ?: "Could not post reply") }
        }
    }

    fun reportPost(
        post: CampusPost,
        profile: StudentProfile,
        reason: ReportReason,
        details: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.reportPost(post, profile, reason, details)
                .onSuccess {
                    onSuccess()
                    _feedback.emit("Post reported. Administrators have been notified.")
                }
                .onFailure { err ->
                    _feedback.emit(err.localizedMessage ?: "Could not submit report")
                }
        }
    }
}

class StudyGroupsViewModel(
    private val repository: VarsityRepository,
    val currentUserId: String
) : ViewModel() {

    private val _campusFilter = MutableStateFlow("ALL")
    val campusFilter: StateFlow<String> = _campusFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _managedGroupId = MutableStateFlow<String?>(null)

    private val _feedback = MutableSharedFlow<String>()
    val feedback: SharedFlow<String> = _feedback.asSharedFlow()

    val studyGroupsState: StateFlow<UiState<List<StudyGroup>>> = combine(
        repository.observeStudyGroups(currentUserId),
        _campusFilter,
        _searchQuery
    ) { groups, campus, query ->
        val q = query.trim()
        val filtered = groups.filter { g ->
            val campusOk = campus == "ALL" || g.university == campus
            val queryOk = q.isEmpty() ||
                g.courseCode.contains(q, ignoreCase = true) ||
                g.studyTopic.contains(q, ignoreCase = true) ||
                g.description.contains(q, ignoreCase = true)
            campusOk && queryOk
        }
        UiState.Success(filtered) as UiState<List<StudyGroup>>
    }
        .catch { error ->
            val root = unwrapFirestoreError(error) ?: throw error
            Log.w("StudyGroupsVM", "Error observing study groups", root)
            emit(UiState.Error(root.localizedMessage ?: "Failed to load study groups"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    @OptIn(ExperimentalCoroutinesApi::class)
    val joinRequestsState: StateFlow<UiState<List<StudyJoinRequest>>> = _managedGroupId
        .flatMapLatest { groupId ->
            if (groupId.isNullOrBlank()) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observeJoinRequestsForCreator(groupId, currentUserId)
                    .map<List<StudyJoinRequest>, UiState<List<StudyJoinRequest>>> { UiState.Success(it) }
                    .catch { error ->
                        val root = unwrapFirestoreError(error) ?: throw error
                        Log.w("StudyGroupsVM", "Error observing join requests", root)
                        emit(UiState.Error(root.localizedMessage ?: "Failed to load join requests"))
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Success(emptyList()))

    fun setCampusFilter(campus: String) {
        _campusFilter.value = campus
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openManageRequests(groupId: String?) {
        _managedGroupId.value = groupId
    }

    fun createStudyGroup(
        profile: StudentProfile,
        university: University,
        courseCode: String,
        studyTopic: String,
        description: String,
        meetingSchedule: String,
        maxMembers: Int,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.createStudyGroup(
                profile = profile,
                university = university,
                courseCode = courseCode,
                studyTopic = studyTopic,
                description = description,
                meetingSchedule = meetingSchedule,
                maxMembers = maxMembers
            )
                .onSuccess {
                    onSuccess()
                    _feedback.emit("Study group created with a linked group chat!")
                }
                .onFailure { _feedback.emit(it.localizedMessage ?: "Could not create study group") }
        }
    }

    fun requestToJoin(group: StudyGroup, profile: StudentProfile, note: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.requestToJoinStudyGroup(group, profile, note)
                .onSuccess {
                    onSuccess()
                    _feedback.emit("Join request sent to ${group.creatorName}")
                }
                .onFailure { _feedback.emit(it.localizedMessage ?: "Could not send join request") }
        }
    }

    fun cancelRequest(groupId: String) {
        viewModelScope.launch {
            repository.cancelJoinRequest(groupId)
                .onSuccess { _feedback.emit("Join request withdrawn") }
                .onFailure { _feedback.emit(it.localizedMessage ?: "Could not withdraw request") }
        }
    }

    fun respondToRequest(group: StudyGroup, request: StudyJoinRequest, approve: Boolean) {
        viewModelScope.launch {
            repository.respondToJoinRequest(group, request, approve)
                .onSuccess {
                    val verb = if (approve) "accepted into" else "declined for"
                    _feedback.emit("${request.requesterName} $verb ${group.courseCode}")
                }
                .onFailure { _feedback.emit(it.localizedMessage ?: "Could not update join request") }
        }
    }

    fun leaveGroup(group: StudyGroup) {
        viewModelScope.launch {
            repository.leaveStudyGroup(group)
                .onSuccess { _feedback.emit("Left ${group.courseCode} study group") }
                .onFailure { _feedback.emit(it.localizedMessage ?: "Could not leave group") }
        }
    }
}

class ChatViewModel(
    private val repository: VarsityRepository,
    val currentUserId: String
) : ViewModel() {

    private val _activeChannelId = MutableStateFlow<String?>(null)
    val activeChannelId: StateFlow<String?> = _activeChannelId.asStateFlow()

    private val _feedback = MutableSharedFlow<String>()
    val feedback: SharedFlow<String> = _feedback.asSharedFlow()

    val publicChannelsState: StateFlow<UiState<List<ChatChannel>>> = repository.observePublicChannels(currentUserId)
        .map<List<ChatChannel>, UiState<List<ChatChannel>>> { UiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreError(error) ?: throw error
            Log.w("ChatVM", "Error observing public channels", root)
            emit(UiState.Error(root.localizedMessage ?: "Failed to load campus channels"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val myMemberChannelsState: StateFlow<UiState<List<ChatChannel>>> = repository.observeMyMemberChannels(currentUserId)
        .map<List<ChatChannel>, UiState<List<ChatChannel>>> { list ->
            UiState.Success(list.filter { it.visibility == "direct" })
        }
        .catch { error ->
            val root = unwrapFirestoreError(error) ?: throw error
            Log.w("ChatVM", "Error observing direct channels", root)
            emit(UiState.Error(root.localizedMessage ?: "Failed to load direct chats"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeChannelState: StateFlow<ChatChannel?> = _activeChannelId
        .flatMapLatest { channelId ->
            if (channelId.isNullOrBlank()) flowOf(null)
            else repository.observeChannel(channelId, currentUserId).catch { emit(null) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeMessagesState: StateFlow<UiState<List<ChatMessage>>> = activeChannelState
        .flatMapLatest { channel ->
            if (channel == null) {
                flowOf(UiState.Loading)
            } else {
                repository.observeMessages(channel, currentUserId)
                    .map<List<ChatMessage>, UiState<List<ChatMessage>>> { UiState.Success(it) }
                    .catch { error ->
                        val root = unwrapFirestoreError(error) ?: throw error
                        Log.w("ChatVM", "Error observing messages", root)
                        emit(UiState.Error(root.localizedMessage ?: "Failed to load messages"))
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    fun openChannel(channelId: String?) {
        _activeChannelId.value = channelId
    }

    fun createPublicChannel(
        profile: StudentProfile,
        name: String,
        description: String,
        universityCode: String,
        courseCode: String,
        onOpened: (String) -> Unit
    ) {
        viewModelScope.launch {
            repository.createPublicChannel(profile, name, description, universityCode, courseCode)
                .onSuccess { channelId ->
                    _activeChannelId.value = channelId
                    onOpened(channelId)
                }
                .onFailure { _feedback.emit(it.localizedMessage ?: "Could not create channel") }
        }
    }

    fun openDirectChat(myProfile: StudentProfile, otherProfile: StudentProfile, onOpened: (String) -> Unit) {
        viewModelScope.launch {
            repository.openOrCreateDirectChannel(myProfile, otherProfile)
                .onSuccess { channelId ->
                    _activeChannelId.value = channelId
                    onOpened(channelId)
                }
                .onFailure { _feedback.emit(it.localizedMessage ?: "Could not open chat") }
        }
    }

    fun sendMessage(channel: ChatChannel, profile: StudentProfile, text: String, onSent: () -> Unit) {
        viewModelScope.launch {
            repository.sendMessage(channel, profile, text)
                .onSuccess { onSent() }
                .onFailure { _feedback.emit(it.localizedMessage ?: "Failed to send message") }
        }
    }
}

class MarketAndEventsViewModel(
    private val repository: VarsityRepository,
    val currentUserId: String
) : ViewModel() {

    private val _marketCampusFilter = MutableStateFlow("ALL")
    val marketCampusFilter: StateFlow<String> = _marketCampusFilter.asStateFlow()

    private val _marketCategoryFilter = MutableStateFlow<MarketCategory?>(null)
    val marketCategoryFilter: StateFlow<MarketCategory?> = _marketCategoryFilter.asStateFlow()

    private val _eventCampusFilter = MutableStateFlow("ALL")
    val eventCampusFilter: StateFlow<String> = _eventCampusFilter.asStateFlow()

    private val _eventCategoryFilter = MutableStateFlow<EventCategory?>(null)
    val eventCategoryFilter: StateFlow<EventCategory?> = _eventCategoryFilter.asStateFlow()

    private val _selectedDateIso = MutableStateFlow<String?>(null)
    val selectedDateIso: StateFlow<String?> = _selectedDateIso.asStateFlow()

    private val _feedback = MutableSharedFlow<String>()
    val feedback: SharedFlow<String> = _feedback.asSharedFlow()

    val allListings: StateFlow<List<MarketListing>> = repository.observeAvailableListings(currentUserId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allEvents: StateFlow<List<CampusEvent>> = repository.observeEvents(currentUserId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val listingsState: StateFlow<UiState<List<MarketListing>>> = combine(
        repository.observeAvailableListings(currentUserId),
        _marketCampusFilter,
        _marketCategoryFilter
    ) { list, campus, category ->
        val filtered = list.filter { item ->
            (campus == "ALL" || item.sellerUniversity == campus) &&
                (category == null || item.category == category.name)
        }
        UiState.Success(filtered) as UiState<List<MarketListing>>
    }
        .catch { error ->
            val root = unwrapFirestoreError(error) ?: throw error
            Log.w("MarketEventsVM", "Error observing listings", root)
            emit(UiState.Error(root.localizedMessage ?: "Failed to load marketplace"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val myListingsState: StateFlow<UiState<List<MarketListing>>> = repository.observeMyListings(currentUserId)
        .map<List<MarketListing>, UiState<List<MarketListing>>> { UiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreError(error) ?: throw error
            emit(UiState.Error(root.localizedMessage ?: "Failed to load your listings"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val eventsState: StateFlow<UiState<List<CampusEvent>>> = combine(
        repository.observeEvents(currentUserId),
        _eventCampusFilter,
        _eventCategoryFilter
    ) { list, campus, category ->
        val filtered = list.filter { event ->
            (campus == "ALL" || event.university == campus) &&
                (category == null || event.category == category.name)
        }
        UiState.Success(filtered) as UiState<List<CampusEvent>>
    }
        .catch { error ->
            val root = unwrapFirestoreError(error) ?: throw error
            Log.w("MarketEventsVM", "Error observing events", root)
            emit(UiState.Error(root.localizedMessage ?: "Failed to load campus events"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    fun setMarketCampusFilter(campus: String) {
        _marketCampusFilter.value = campus
    }

    fun setMarketCategoryFilter(category: MarketCategory?) {
        _marketCategoryFilter.value = category
    }

    fun setEventCampusFilter(campus: String) {
        _eventCampusFilter.value = campus
    }

    fun setEventCategoryFilter(category: EventCategory?) {
        _eventCategoryFilter.value = category
    }

    fun setSelectedDateIso(dateIso: String?) {
        _selectedDateIso.value = dateIso
    }

    fun createListing(
        profile: StudentProfile,
        title: String,
        description: String,
        category: MarketCategory,
        priceBwp: Long,
        condition: ListingCondition,
        pickupSpot: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.createListing(profile, title, description, category, priceBwp, condition, pickupSpot)
                .onSuccess {
                    onSuccess()
                    _feedback.emit("Listing live on VarsityBW Market")
                }
                .onFailure { _feedback.emit(it.localizedMessage ?: "Could not post listing") }
        }
    }

    fun markSold(listingId: String) {
        viewModelScope.launch {
            repository.markListingSold(listingId)
                .onSuccess { _feedback.emit("Marked as sold") }
                .onFailure { _feedback.emit(it.localizedMessage ?: "Could not mark sold") }
        }
    }

    fun createEvent(
        profile: StudentProfile,
        university: University,
        title: String,
        description: String,
        venue: String,
        eventDateLabel: String,
        eventDateIso: String,
        category: EventCategory,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.createEvent(profile, university, title, description, venue, eventDateLabel, eventDateIso, category)
                .onSuccess {
                    onSuccess()
                    _feedback.emit("Campus event added to calendar")
                }
                .onFailure { _feedback.emit(it.localizedMessage ?: "Could not publish event") }
        }
    }

    fun toggleRsvp(event: CampusEvent) {
        viewModelScope.launch {
            val going = event.attendeeUids.contains(currentUserId)
            repository.toggleEventRsvp(event.id, going)
                .onFailure { _feedback.emit(it.localizedMessage ?: "Could not update RSVP") }
        }
    }
}
