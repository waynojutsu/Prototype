package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.VarsityRepository
import com.example.data.unwrapFirestoreError
import com.example.model.AdminNotification
import com.example.model.PostReport
import com.example.model.PresenceStatus
import com.example.model.ReportStatus
import com.example.model.StudentProfile
import com.example.model.UiState
import com.example.model.University
import com.example.model.UserNotification
import com.example.model.UserPresence
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
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

data class ViewedProfileTarget(
    val userId: String,
    val fallbackName: String = "",
    val fallbackUniversity: String = University.UB.code
)

class SessionViewModel(
    private val repository: VarsityRepository,
    val currentUserId: String
) : ViewModel() {

    val myProfileState: StateFlow<UiState<StudentProfile?>> = repository.observeMyProfile(currentUserId)
        .map<StudentProfile?, UiState<StudentProfile?>> { UiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreError(error) ?: throw error
            Log.w(TAG, "Error observing student profile", root)
            emit(UiState.Error(root.localizedMessage ?: "Failed to load student profile"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    private val _directoryCampusFilter = MutableStateFlow<String>("ALL")
    val directoryCampusFilter: StateFlow<String> = _directoryCampusFilter.asStateFlow()

    private val _directoryQuery = MutableStateFlow("")
    val directoryQuery: StateFlow<String> = _directoryQuery.asStateFlow()

    val directoryState: StateFlow<UiState<List<StudentProfile>>> = combine(
        repository.observeStudentDirectory(currentUserId),
        _directoryCampusFilter,
        _directoryQuery
    ) { list, campus, query ->
        val filtered = list.filter { student ->
            val campusOk = campus == "ALL" || student.university == campus
            val q = query.trim()
            val queryOk = q.isEmpty() ||
                student.displayName.contains(q, ignoreCase = true) ||
                student.faculty.contains(q, ignoreCase = true) ||
                student.university.contains(q, ignoreCase = true)
            campusOk && queryOk
        }
        UiState.Success(filtered) as UiState<List<StudentProfile>>
    }
        .catch { error ->
            val root = unwrapFirestoreError(error) ?: throw error
            Log.w(TAG, "Error observing student directory", root)
            emit(UiState.Error(root.localizedMessage ?: "Failed to load student directory"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    private val _viewedProfileTarget = MutableStateFlow<ViewedProfileTarget?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val viewedProfileState: StateFlow<UiState<StudentProfile?>> = _viewedProfileTarget
        .flatMapLatest { target ->
            if (target == null) {
                flowOf(UiState.Success(null))
            } else {
                repository.observeStudentProfile(
                    targetUserId = target.userId,
                    fallbackName = target.fallbackName,
                    fallbackUniversity = target.fallbackUniversity
                )
                    .map<StudentProfile?, UiState<StudentProfile?>> { UiState.Success(it) }
                    .catch { error ->
                        val root = unwrapFirestoreError(error) ?: throw error
                        Log.w(TAG, "Error observing viewed student profile", root)
                        emit(UiState.Error(root.localizedMessage ?: "Failed to load profile"))
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _preferredPresence = MutableStateFlow(PresenceStatus.ONLINE)
    val preferredPresence: StateFlow<PresenceStatus> = _preferredPresence.asStateFlow()

    val presenceMap: StateFlow<Map<String, UserPresence>> = repository.observeAllPresence(currentUserId)
        .catch { error ->
            unwrapFirestoreError(error) ?: throw error
            emit(emptyMap())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyMap()
        )

    private val _fcmToken = MutableStateFlow<String?>(null)
    val fcmToken: StateFlow<String?> = _fcmToken.asStateFlow()

    val notificationsState: StateFlow<UiState<List<UserNotification>>> = repository
        .observeMyNotifications(currentUserId)
        .map<List<UserNotification>, UiState<List<UserNotification>>> { UiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreError(error) ?: throw error
            Log.w(TAG, "Error observing notifications", root)
            emit(UiState.Error(root.localizedMessage ?: "Failed to load notifications"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val unreadNotificationCount: StateFlow<Int> = notificationsState
        .map { state ->
            when (state) {
                is UiState.Success -> state.data.count { !it.read }
                else -> 0
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = 0
        )

    val isAdmin: StateFlow<Boolean> = repository
        .observeIsAdmin(currentUserId)
        .catch { emit(false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = false
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val adminNotificationsState: StateFlow<UiState<List<AdminNotification>>> = isAdmin
        .flatMapLatest { admin ->
            if (!admin) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observeAdminNotifications(currentUserId)
                    .map<List<AdminNotification>, UiState<List<AdminNotification>>> { UiState.Success(it) }
                    .catch { error ->
                        val root = unwrapFirestoreError(error) ?: throw error
                        Log.w(TAG, "Error observing admin notifications", root)
                        emit(UiState.Error(root.localizedMessage ?: "Failed to load admin alerts"))
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Success(emptyList())
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val adminReportsState: StateFlow<UiState<List<PostReport>>> = isAdmin
        .flatMapLatest { admin ->
            if (!admin) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observeAllPostReportsForAdmin(currentUserId)
                    .map<List<PostReport>, UiState<List<PostReport>>> { UiState.Success(it) }
                    .catch { error ->
                        val root = unwrapFirestoreError(error) ?: throw error
                        Log.w(TAG, "Error observing post reports", root)
                        emit(UiState.Error(root.localizedMessage ?: "Failed to load reported posts"))
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Success(emptyList())
        )

    val adminUnreadCount: StateFlow<Int> = adminNotificationsState
        .map { state ->
            when (state) {
                is UiState.Success -> state.data.count { !it.read }
                else -> 0
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = 0
        )

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    fun setDirectoryCampusFilter(campus: String) {
        _directoryCampusFilter.value = campus
    }

    fun setDirectoryQuery(query: String) {
        _directoryQuery.value = query
    }

    fun openStudentProfile(
        userId: String,
        fallbackName: String = "",
        fallbackUniversity: String = University.UB.code
    ) {
        _viewedProfileTarget.value = ViewedProfileTarget(userId, fallbackName, fallbackUniversity)
    }

    fun closeStudentProfile() {
        _viewedProfileTarget.value = null
    }

    fun registerStudent(
        googleEmail: String,
        studentEmail: String,
        displayName: String,
        university: University,
        faculty: String,
        yearOfStudy: Int,
        bio: String
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.registerVerifiedStudent(
                googleEmail = googleEmail,
                studentEmail = studentEmail,
                displayName = displayName,
                university = university,
                faculty = faculty,
                yearOfStudy = yearOfStudy,
                bio = bio
            )
            _isSubmitting.value = false
            result.onSuccess {
                _events.emit("Verified .ac.bw profile active. Welcome to VarsityBW!")
            }.onFailure { err ->
                _events.emit(err.localizedMessage ?: "Verification failed. Check your .ac.bw email.")
            }
        }
    }

    fun updateProfile(
        displayName: String,
        university: University,
        faculty: String,
        yearOfStudy: Int,
        bio: String,
        discoverable: Boolean
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.updateMyProfile(
                displayName = displayName,
                university = university,
                faculty = faculty,
                yearOfStudy = yearOfStudy,
                bio = bio,
                discoverable = discoverable
            )
            _isSubmitting.value = false
            result.onSuccess {
                _events.emit("Campus profile updated")
            }.onFailure { err ->
                _events.emit(err.localizedMessage ?: "Could not update profile")
            }
        }
    }

    fun setManualPresence(status: PresenceStatus) {
        _preferredPresence.value = status
        viewModelScope.launch {
            repository.updateMyPresence(status)
        }
    }

    fun onAppForegrounded() {
        val target = _preferredPresence.value
        viewModelScope.launch {
            repository.updateMyPresence(target)
        }
    }

    fun onAppBackgrounded() {
        val target = if (_preferredPresence.value == PresenceStatus.OFFLINE) {
            PresenceStatus.OFFLINE
        } else {
            PresenceStatus.AWAY
        }
        viewModelScope.launch {
            repository.updateMyPresence(target)
        }
    }

    suspend fun sendPresenceHeartbeat() {
        val target = _preferredPresence.value
        if (target != PresenceStatus.OFFLINE) {
            repository.updateMyPresence(target)
        }
    }

    suspend fun markOfflineBeforeSignOut() {
        repository.updateMyPresence(PresenceStatus.OFFLINE)
    }

    fun syncFcmToken() {
        viewModelScope.launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                if (!token.isNullOrBlank()) {
                    _fcmToken.value = token
                    repository.registerFcmToken(token)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Unable to fetch FCM token: ${e.message}")
            }
        }
    }

    fun markNotificationRead(notifId: String) {
        viewModelScope.launch {
            repository.markNotificationRead(notifId)
        }
    }

    fun markAllNotificationsRead() {
        val current = (notificationsState.value as? UiState.Success)?.data.orEmpty()
        val unreadIds = current.filter { !it.read }.map { it.id }
        if (unreadIds.isEmpty()) return
        viewModelScope.launch {
            repository.markAllNotificationsRead(unreadIds)
        }
    }

    fun markAdminNotificationRead(notifId: String) {
        viewModelScope.launch {
            repository.markAdminNotificationRead(notifId)
        }
    }

    fun updatePostReportStatus(reportId: String, status: ReportStatus) {
        viewModelScope.launch {
            repository.updatePostReportStatus(reportId, status)
                .onSuccess { _events.emit("Report marked as ${status.label}") }
                .onFailure { err -> _events.emit(err.localizedMessage ?: "Could not update report") }
        }
    }

    fun removeFlaggedPost(postId: String, reportId: String?) {
        viewModelScope.launch {
            repository.deletePostAsAdmin(postId, reportId)
                .onSuccess { _events.emit("Flagged post removed") }
                .onFailure { err -> _events.emit(err.localizedMessage ?: "Could not remove post") }
        }
    }

    private companion object {
        const val TAG = "SessionVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
