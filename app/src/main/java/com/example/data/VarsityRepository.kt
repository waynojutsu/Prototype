package com.example.data

import android.content.Context
import com.example.R
import com.example.model.AdminNotification
import com.example.model.CampusEvent
import com.example.model.CampusPost
import com.example.model.ChannelType
import com.example.model.ChatChannel
import com.example.model.ChatMessage
import com.example.model.EventCategory
import com.example.model.JoinRequestStatus
import com.example.model.ListingCondition
import com.example.model.MarketCategory
import com.example.model.MarketListing
import com.example.model.NotificationType
import com.example.model.PostCategory
import com.example.model.PostComment
import com.example.model.PostReport
import com.example.model.PresenceStatus
import com.example.model.ReportReason
import com.example.model.ReportStatus
import com.example.model.StudentEmailValidator
import com.example.model.StudentProfile
import com.example.model.StudyGroup
import com.example.model.StudyGroupStatus
import com.example.model.StudyJoinRequest
import com.example.model.University
import com.example.model.UserNotification
import com.example.model.UserPresence
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest

class VarsityRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing VarsityBW.")
    }

    private fun safeTs(t: Timestamp?): Timestamp = t ?: Timestamp(0, 0)

    // --- STUDENT REGISTRATION (.ac.bw) & DIRECTORY ---

    fun observeMyProfile(userId: String): Flow<StudentProfile?> = flow {
        require(userId.isNotBlank())
        val path = "studentProfiles/$userId"
        emitAll(
            db.collection("studentProfiles").document(userId)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(StudentProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val root = unwrapFirestoreError(error)
                    if (root == null) throw error
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(root, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeStudentProfile(
        targetUserId: String,
        fallbackName: String = "",
        fallbackUniversity: String = University.UB.code
    ): Flow<StudentProfile?> = flow {
        require(targetUserId.isNotBlank())
        val path = "studentProfiles/$targetUserId"
        emitAll(
            db.collection("studentProfiles").document(targetUserId)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(StudentProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else if (fallbackName.isNotBlank()) {
                        StudentProfile(
                            userId = targetUserId,
                            displayName = fallbackName,
                            university = fallbackUniversity
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val root = unwrapFirestoreError(error)
                    if (root == null) throw error
                    if (fallbackName.isNotBlank()) {
                        emit(
                            StudentProfile(
                                userId = targetUserId,
                                displayName = fallbackName,
                                university = fallbackUniversity,
                                faculty = "Private student profile",
                                bio = "This student has restricted full profile visibility."
                            )
                        )
                    } else {
                        throw error
                    }
                }
        )
    }

    fun observeStudentDirectory(currentUserId: String): Flow<List<StudentProfile>> = flow {
        val path = "studentProfiles"
        emitAll(
            db.collection("studentProfiles")
                .whereEqualTo("discoverable", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(StudentProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.displayName.lowercase() }
                }
                .catch { error ->
                    val root = unwrapFirestoreError(error)
                    if (root == null) throw error
                    if (auth.currentUser?.uid == currentUserId) {
                        handleFirestoreError(root, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun registerVerifiedStudent(
        googleEmail: String,
        studentEmail: String,
        displayName: String,
        university: University,
        faculty: String,
        yearOfStudy: Int,
        bio: String
    ): Result<Unit> {
        val uidPreview = auth.currentUser?.uid ?: "unauthenticated"
        val path = "studentProfiles/$uidPreview"
        return try {
            val uid = requireUserId()
            val normalizedStudentEmail = StudentEmailValidator.normalize(studentEmail)
            require(StudentEmailValidator.isValidAcBwEmail(normalizedStudentEmail)) {
                "Please enter a valid Botswana university email ending in .ac.bw"
            }
            val domain = StudentEmailValidator.extractDomain(normalizedStudentEmail)
            val cleanName = displayName.trim().take(50)
            require(cleanName.length >= 2) { "Display name must be at least 2 characters." }
            val cleanFaculty = faculty.trim().take(60)
            require(cleanFaculty.length >= 2) { "Faculty or programme must be at least 2 characters." }
            val safeYear = yearOfStudy.coerceIn(1, 6).toLong()
            val cleanBio = bio.trim().take(200)
            val cleanGoogleEmail = googleEmail.trim().ifBlank { normalizedStudentEmail }.take(120)

            val userRef = db.collection("users").document(uid)
            val profileRef = db.collection("studentProfiles").document(uid)

            val batch = db.batch()
            batch.set(
                userRef,
                mapOf(
                    "userId" to uid,
                    "googleEmail" to cleanGoogleEmail,
                    "studentEmail" to normalizedStudentEmail,
                    "verifiedDomain" to domain,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                profileRef,
                mapOf(
                    "userId" to uid,
                    "displayName" to cleanName,
                    "university" to university.code,
                    "faculty" to cleanFaculty,
                    "yearOfStudy" to safeYear,
                    "bio" to cleanBio,
                    "verifiedDomain" to domain,
                    "acBwVerified" to true,
                    "discoverable" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateMyProfile(
        displayName: String,
        university: University,
        faculty: String,
        yearOfStudy: Int,
        bio: String,
        discoverable: Boolean
    ): Result<Unit> {
        val uidPreview = auth.currentUser?.uid ?: "unauthenticated"
        val path = "studentProfiles/$uidPreview"
        return try {
            val uid = requireUserId()
            val cleanName = displayName.trim().take(50)
            require(cleanName.length >= 2) { "Display name must be at least 2 characters." }
            val cleanFaculty = faculty.trim().take(60)
            require(cleanFaculty.length >= 2) { "Faculty must be at least 2 characters." }
            val cleanBio = bio.trim().take(200)
            val safeYear = yearOfStudy.coerceIn(1, 6).toLong()

            db.collection("studentProfiles").document(uid).update(
                mapOf(
                    "displayName" to cleanName,
                    "university" to university.code,
                    "faculty" to cleanFaculty,
                    "yearOfStudy" to safeYear,
                    "bio" to cleanBio,
                    "discoverable" to discoverable,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- REAL-TIME USER PRESENCE (ONLINE / AWAY / OFFLINE) ---

    fun observeAllPresence(currentUserId: String): Flow<Map<String, UserPresence>> = flow {
        val path = "userPresence"
        emitAll(
            db.collection("userPresence")
                .whereEqualTo("visibility", "public")
                .snapshots()
                .map { snap ->
                    snap.toObjects(UserPresence::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .associateBy { it.userId.ifBlank { it.id } }
                }
                .catch { error ->
                    val root = unwrapFirestoreError(error)
                    if (root == null) throw error
                    if (auth.currentUser?.uid == currentUserId) {
                        handleFirestoreError(root, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun updateMyPresence(status: PresenceStatus): Result<Unit> {
        val uidPreview = auth.currentUser?.uid ?: "unauthenticated"
        val path = "userPresence/$uidPreview"
        return try {
            val uid = requireUserId()
            db.collection("userPresence").document(uid).set(
                mapOf(
                    "userId" to uid,
                    "status" to status.raw,
                    "visibility" to "public",
                    "lastActiveAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- FCM DEVICE TOKENS & REAL-TIME INBOX NOTIFICATIONS ---

    private fun hashTokenId(token: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(token.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }.take(64)
    }

    suspend fun registerFcmToken(token: String): Result<Unit> {
        val cleanToken = token.trim().take(512)
        if (cleanToken.length < 10) return Result.failure(IllegalArgumentException("Invalid FCM token"))
        val uidPreview = auth.currentUser?.uid ?: "unauthenticated"
        val tokenId = hashTokenId(cleanToken)
        val path = "users/$uidPreview/fcmTokens/$tokenId"
        return try {
            val uid = requireUserId()
            db.collection("users").document(uid)
                .collection("fcmTokens").document(tokenId)
                .set(
                    mapOf(
                        "userId" to uid,
                        "token" to cleanToken,
                        "platform" to "android",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeMyNotifications(userId: String): Flow<List<UserNotification>> = flow {
        require(userId.isNotBlank())
        val path = "users/$userId/notifications"
        emitAll(
            db.collection("users").document(userId).collection("notifications")
                .snapshots()
                .map { snap ->
                    snap.toObjects(UserNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { safeTs(it.createdAt) }
                }
                .catch { error ->
                    val root = unwrapFirestoreError(error)
                    if (root == null) throw error
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(root, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun markNotificationRead(notifId: String): Result<Unit> {
        val uidPreview = auth.currentUser?.uid ?: "unauthenticated"
        val path = "users/$uidPreview/notifications/$notifId"
        return try {
            val uid = requireUserId()
            require(notifId.isNotBlank())
            db.collection("users").document(uid)
                .collection("notifications").document(notifId)
                .update(mapOf("read" to true))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun markAllNotificationsRead(notifIds: List<String>): Result<Unit> {
        if (notifIds.isEmpty()) return Result.success(Unit)
        val uidPreview = auth.currentUser?.uid ?: "unauthenticated"
        val path = "users/$uidPreview/notifications"
        return try {
            val uid = requireUserId()
            val batch = db.batch()
            notifIds.distinct().take(100).forEach { id ->
                val ref = db.collection("users").document(uid).collection("notifications").document(id)
                batch.update(ref, mapOf("read" to true))
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- CAMPUS FEED & COMMENTS ---

    fun observePosts(currentUserId: String): Flow<List<CampusPost>> = flow {
        val path = "posts"
        emitAll(
            db.collection("posts")
                .whereEqualTo("visibility", "public")
                .snapshots()
                .map { snap ->
                    snap.toObjects(CampusPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { safeTs(it.createdAt) }
                }
                .catch { error ->
                    val root = unwrapFirestoreError(error)
                    if (root == null) throw error
                    if (auth.currentUser?.uid == currentUserId) {
                        handleFirestoreError(root, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun fetchLatestPosts(): Result<List<CampusPost>> {
        val path = "posts"
        return try {
            requireUserId()
            val snap = db.collection("posts")
                .whereEqualTo("visibility", "public")
                .get(Source.SERVER)
                .await()
            val posts = snap.toObjects(CampusPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedByDescending { safeTs(it.createdAt) }
            Result.success(posts)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun createPost(
        profile: StudentProfile,
        category: PostCategory,
        title: String,
        body: String
    ): Result<Unit> {
        var path = "posts"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim().take(120)
            val cleanBody = body.trim().take(1000)
            require(cleanTitle.length >= 2) { "Post headline must be at least 2 characters." }
            require(cleanBody.isNotEmpty()) { "Post content cannot be empty." }

            val docRef = db.collection("posts").document()
            path = docRef.path
            docRef.set(
                mapOf(
                    "authorId" to uid,
                    "authorName" to profile.displayName.take(50).ifBlank { "Student" },
                    "authorUniversity" to profile.university,
                    "category" to category.name,
                    "title" to cleanTitle,
                    "body" to cleanBody,
                    "visibility" to "public",
                    "likedByUids" to emptyList<String>(),
                    "commentCount" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun togglePostLike(postId: String, currentlyLiked: Boolean): Result<Unit> {
        val path = "posts/$postId"
        return try {
            val uid = requireUserId()
            require(postId.isNotBlank())
            val updateValue = if (currentlyLiked) FieldValue.arrayRemove(uid) else FieldValue.arrayUnion(uid)
            db.collection("posts").document(postId).update(
                mapOf(
                    "likedByUids" to updateValue,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeComments(postId: String, currentUserId: String): Flow<List<PostComment>> = flow {
        val path = "posts/$postId/comments"
        emitAll(
            db.collection("posts").document(postId).collection("comments")
                .whereEqualTo("visibility", "public")
                .snapshots()
                .map { snap ->
                    snap.toObjects(PostComment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { safeTs(it.createdAt) }
                }
                .catch { error ->
                    val root = unwrapFirestoreError(error)
                    if (root == null) throw error
                    if (auth.currentUser?.uid == currentUserId) {
                        handleFirestoreError(root, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addComment(post: CampusPost, profile: StudentProfile, body: String): Result<Unit> {
        val postId = post.id
        val path = "posts/$postId/comments"
        return try {
            val uid = requireUserId()
            val cleanBody = body.trim().take(500)
            require(postId.isNotBlank())
            require(cleanBody.isNotEmpty()) { "Reply cannot be empty." }

            val postRef = db.collection("posts").document(postId)
            val commentRef = postRef.collection("comments").document()
            val senderName = profile.displayName.take(50).ifBlank { "Student" }
            val batch = db.batch()
            batch.set(
                commentRef,
                mapOf(
                    "postId" to postId,
                    "authorId" to uid,
                    "authorName" to senderName,
                    "authorUniversity" to profile.university,
                    "body" to cleanBody,
                    "visibility" to "public",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                postRef,
                mapOf(
                    "commentCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (post.authorId.isNotBlank() && post.authorId != uid) {
                val notifRef = db.collection("users").document(post.authorId)
                    .collection("notifications").document()
                batch.set(
                    notifRef,
                    mapOf(
                        "recipientId" to post.authorId,
                        "senderId" to uid,
                        "senderName" to senderName,
                        "senderUniversity" to profile.university,
                        "notifType" to NotificationType.POST_REPLY.name,
                        "title" to "$senderName replied to your post".take(100),
                        "body" to cleanBody.take(240),
                        "targetId" to postId,
                        "read" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- POST REPORTING & ADMIN MODERATION ---

    fun observeMyReportedPostIds(currentUserId: String): Flow<Set<String>> = flow {
        val path = "postReports"
        emitAll(
            db.collection("postReports")
                .whereEqualTo("reporterId", currentUserId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(PostReport::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .mapNotNull { it.postId.takeIf(String::isNotBlank) }
                        .toSet()
                }
                .catch { error ->
                    val root = unwrapFirestoreError(error)
                    if (root == null) throw error
                    if (auth.currentUser?.uid == currentUserId) {
                        handleFirestoreError(root, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun reportPost(
        post: CampusPost,
        reporterProfile: StudentProfile,
        reason: ReportReason,
        details: String
    ): Result<Unit> {
        var path = "postReports"
        return try {
            val uid = requireUserId()
            require(post.id.isNotBlank()) { "Invalid post reference." }
            require(post.authorId.isNotBlank() && post.authorId != uid) {
                "You cannot report your own post."
            }

            val existing = db.collection("postReports")
                .whereEqualTo("reporterId", uid)
                .get()
                .await()
            if (existing.documents.any { it.getString("postId") == post.id }) {
                return Result.failure(IllegalStateException("You have already reported this post."))
            }

            val reportId = "${post.id}_${uid}".take(128)
            val notifId = "report_${reportId}".take(128)
            val reportRef = db.collection("postReports").document(reportId)
            val adminNotifRef = db.collection("adminNotifications").document(notifId)
            path = reportRef.path

            val cleanAuthorName = post.authorName.trim().take(50).ifBlank { "Student" }
            val cleanPostTitle = post.title.trim().take(120)
            val cleanPostExcerpt = post.body.trim().take(240)
            val cleanReporterName = reporterProfile.displayName.trim().take(50).ifBlank { "Student" }
            val cleanDetails = details.trim().take(400)

            val notifTitle = "Post reported: ${reason.label}".take(120)
            val notifBody = buildString {
                append("$cleanReporterName (${reporterProfile.university}) flagged ${cleanAuthorName}'s post")
                if (cleanPostTitle.isNotBlank()) {
                    append(" \"$cleanPostTitle\"")
                }
                if (cleanDetails.isNotBlank()) {
                    append(": $cleanDetails")
                }
            }.take(300)

            val batch = db.batch()
            batch.set(
                reportRef,
                mapOf(
                    "postId" to post.id,
                    "postAuthorId" to post.authorId,
                    "postAuthorName" to cleanAuthorName,
                    "postTitle" to cleanPostTitle,
                    "postExcerpt" to cleanPostExcerpt,
                    "reporterId" to uid,
                    "reporterName" to cleanReporterName,
                    "reporterUniversity" to reporterProfile.university,
                    "reason" to reason.name,
                    "details" to cleanDetails,
                    "status" to ReportStatus.PENDING.raw,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                adminNotifRef,
                mapOf(
                    "reportId" to reportId,
                    "postId" to post.id,
                    "postAuthorId" to post.authorId,
                    "postTitle" to cleanPostTitle,
                    "reporterId" to uid,
                    "reporterName" to cleanReporterName,
                    "reporterUniversity" to reporterProfile.university,
                    "reason" to reason.name,
                    "details" to cleanDetails,
                    "title" to notifTitle,
                    "body" to notifBody,
                    "read" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeIsAdmin(currentUserId: String): Flow<Boolean> = flow {
        require(currentUserId.isNotBlank())
        emitAll(
            db.collection("admins").document(currentUserId)
                .snapshots()
                .map { it.exists() }
                .catch { error ->
                    unwrapFirestoreError(error) ?: throw error
                    emit(false)
                }
        )
    }

    fun observeAdminNotifications(currentUserId: String): Flow<List<AdminNotification>> = flow {
        val path = "adminNotifications"
        emitAll(
            db.collection("adminNotifications")
                .snapshots()
                .map { snap ->
                    snap.toObjects(AdminNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { safeTs(it.createdAt) }
                }
                .catch { error ->
                    val root = unwrapFirestoreError(error)
                    if (root == null) throw error
                    if (auth.currentUser?.uid == currentUserId) {
                        handleFirestoreError(root, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeAllPostReportsForAdmin(currentUserId: String): Flow<List<PostReport>> = flow {
        val path = "postReports"
        emitAll(
            db.collection("postReports")
                .snapshots()
                .map { snap ->
                    snap.toObjects(PostReport::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { safeTs(it.createdAt) }
                }
                .catch { error ->
                    val root = unwrapFirestoreError(error)
                    if (root == null) throw error
                    if (auth.currentUser?.uid == currentUserId) {
                        handleFirestoreError(root, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun markAdminNotificationRead(notifId: String): Result<Unit> {
        val path = "adminNotifications/$notifId"
        return try {
            requireUserId()
            require(notifId.isNotBlank())
            db.collection("adminNotifications").document(notifId)
                .update(mapOf("read" to true))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updatePostReportStatus(reportId: String, status: ReportStatus): Result<Unit> {
        val path = "postReports/$reportId"
        return try {
            requireUserId()
            require(reportId.isNotBlank())
            db.collection("postReports").document(reportId)
                .update(
                    mapOf(
                        "status" to status.raw,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deletePostAsAdmin(postId: String, reportId: String? = null): Result<Unit> {
        val path = "posts/$postId"
        return try {
            requireUserId()
            require(postId.isNotBlank())
            val batch = db.batch()
            batch.delete(db.collection("posts").document(postId))
            if (!reportId.isNullOrBlank()) {
                batch.update(
                    db.collection("postReports").document(reportId),
                    mapOf(
                        "status" to ReportStatus.REVIEWED.raw,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- STUDY GROUPS & JOIN REQUESTS ---

    fun observeStudyGroups(currentUserId: String): Flow<List<StudyGroup>> = flow {
        val path = "studyGroups"
        emitAll(
            db.collection("studyGroups")
                .whereEqualTo("visibility", "public")
                .snapshots()
                .map { snap ->
                    snap.toObjects(StudyGroup::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { safeTs(it.updatedAt ?: it.createdAt) }
                }
                .catch { error ->
                    val root = unwrapFirestoreError(error)
                    if (root == null) throw error
                    if (auth.currentUser?.uid == currentUserId) {
                        handleFirestoreError(root, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createStudyGroup(
        profile: StudentProfile,
        university: University,
        courseCode: String,
        studyTopic: String,
        description: String,
        meetingSchedule: String,
        maxMembers: Int
    ): Result<String> {
        var path = "studyGroups"
        return try {
            val uid = requireUserId()
            val cleanCode = courseCode.trim().uppercase().take(16)
            val cleanTopic = studyTopic.trim().take(100)
            val cleanDesc = description.trim().take(600)
            val cleanSchedule = meetingSchedule.trim().take(100)
            val safeMax = maxMembers.coerceIn(2, 30).toLong()

            require(cleanCode.length >= 2) { "Course code must be at least 2 characters (e.g., CSI241)." }
            require(cleanTopic.length >= 3) { "Study topic must be at least 3 characters." }
            require(cleanDesc.isNotEmpty()) { "Please describe your study group goals." }

            val groupRef = db.collection("studyGroups").document()
            val channelRef = db.collection("channels").document(groupRef.id)
            path = groupRef.path

            val batch = db.batch()
            batch.set(
                groupRef,
                mapOf(
                    "creatorId" to uid,
                    "creatorName" to profile.displayName.take(50).ifBlank { "Student" },
                    "creatorUniversity" to profile.university,
                    "university" to university.code,
                    "courseCode" to cleanCode,
                    "studyTopic" to cleanTopic,
                    "description" to cleanDesc,
                    "meetingSchedule" to cleanSchedule,
                    "maxMembers" to safeMax,
                    "memberUids" to listOf(uid),
                    "pendingRequestUids" to emptyList<String>(),
                    "visibility" to "public",
                    "groupStatus" to StudyGroupStatus.OPEN.raw,
                    "linkedChannelId" to channelRef.id,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                channelRef,
                mapOf(
                    "name" to "$cleanCode • $cleanTopic".take(80),
                    "description" to "${university.code} study group chat".take(240),
                    "university" to university.code,
                    "courseCode" to cleanCode,
                    "channelType" to ChannelType.STUDY_GROUP.name,
                    "visibility" to "direct",
                    "creatorId" to uid,
                    "memberUids" to listOf(uid),
                    "lastMessageText" to "Study group room created",
                    "lastMessageSender" to profile.displayName.take(60),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(groupRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun requestToJoinStudyGroup(
        group: StudyGroup,
        profile: StudentProfile,
        note: String
    ): Result<Unit> {
        val path = "studyGroups/${group.id}/joinRequests"
        return try {
            val uid = requireUserId()
            require(group.id.isNotBlank())
            require(!group.memberUids.contains(uid)) { "You are already a member of this study group." }
            require(!group.pendingRequestUids.contains(uid)) { "You already have a pending request." }
            require(group.groupStatus == StudyGroupStatus.OPEN.raw) { "This study group is not accepting new requests." }

            val groupRef = db.collection("studyGroups").document(group.id)
            val requestRef = groupRef.collection("joinRequests").document(uid)
            val cleanNote = note.trim().take(200)

            val batch = db.batch()
            batch.set(
                requestRef,
                mapOf(
                    "groupId" to group.id,
                    "groupCreatorId" to group.creatorId,
                    "requesterId" to uid,
                    "requesterName" to profile.displayName.take(50).ifBlank { "Student" },
                    "requesterUniversity" to profile.university,
                    "requesterFaculty" to profile.faculty.take(60).ifBlank { "Student" },
                    "note" to cleanNote,
                    "requestStatus" to JoinRequestStatus.PENDING.raw,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                groupRef,
                mapOf(
                    "pendingRequestUids" to FieldValue.arrayUnion(uid),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun cancelJoinRequest(groupId: String): Result<Unit> {
        val uidPreview = auth.currentUser?.uid ?: "unauthenticated"
        val path = "studyGroups/$groupId/joinRequests/$uidPreview"
        return try {
            val uid = requireUserId()
            val groupRef = db.collection("studyGroups").document(groupId)
            val requestRef = groupRef.collection("joinRequests").document(uid)

            val batch = db.batch()
            batch.delete(requestRef)
            batch.update(
                groupRef,
                mapOf(
                    "pendingRequestUids" to FieldValue.arrayRemove(uid),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observeJoinRequestsForCreator(groupId: String, currentUserId: String): Flow<List<StudyJoinRequest>> = flow {
        val path = "studyGroups/$groupId/joinRequests"
        emitAll(
            db.collection("studyGroups").document(groupId).collection("joinRequests")
                .whereEqualTo("groupCreatorId", currentUserId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(StudyJoinRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { safeTs(it.createdAt) }
                }
                .catch { error ->
                    val root = unwrapFirestoreError(error)
                    if (root == null) throw error
                    if (auth.currentUser?.uid == currentUserId) {
                        handleFirestoreError(root, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun respondToJoinRequest(
        group: StudyGroup,
        request: StudyJoinRequest,
        approve: Boolean
    ): Result<Unit> {
        val path = "studyGroups/${group.id}/joinRequests/${request.requesterId}"
        return try {
            val uid = requireUserId()
            check(uid == group.creatorId) { "Only the study group creator can approve or decline requests." }
            val groupRef = db.collection("studyGroups").document(group.id)
            val requestRef = groupRef.collection("joinRequests").document(request.requesterId)
            val newStatus = if (approve) JoinRequestStatus.ACCEPTED.raw else JoinRequestStatus.DECLINED.raw

            val batch = db.batch()
            batch.update(
                requestRef,
                mapOf(
                    "requestStatus" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (approve) {
                val projectedCount = (group.memberUids + request.requesterId).distinct().size
                val nextGroupStatus = if (projectedCount >= group.maxMembers) {
                    StudyGroupStatus.FULL.raw
                } else {
                    group.groupStatus
                }
                batch.update(
                    groupRef,
                    mapOf(
                        "memberUids" to FieldValue.arrayUnion(request.requesterId),
                        "pendingRequestUids" to FieldValue.arrayRemove(request.requesterId),
                        "groupStatus" to nextGroupStatus,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                if (group.linkedChannelId.isNotBlank()) {
                    val channelRef = db.collection("channels").document(group.linkedChannelId)
                    batch.update(
                        channelRef,
                        mapOf(
                            "memberUids" to FieldValue.arrayUnion(request.requesterId),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            } else {
                batch.update(
                    groupRef,
                    mapOf(
                        "pendingRequestUids" to FieldValue.arrayRemove(request.requesterId),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun leaveStudyGroup(group: StudyGroup): Result<Unit> {
        val path = "studyGroups/${group.id}"
        return try {
            val uid = requireUserId()
            require(uid != group.creatorId) { "Group creators cannot leave their own study group." }
            val batch = db.batch()
            batch.update(
                db.collection("studyGroups").document(group.id),
                mapOf(
                    "memberUids" to FieldValue.arrayRemove(uid),
                    "groupStatus" to StudyGroupStatus.OPEN.raw,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (group.linkedChannelId.isNotBlank()) {
                batch.update(
                    db.collection("channels").document(group.linkedChannelId),
                    mapOf(
                        "memberUids" to FieldValue.arrayRemove(uid),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- CHAT CHANNELS & REALTIME MESSAGES ---

    fun observePublicChannels(currentUserId: String): Flow<List<ChatChannel>> = flow {
        val path = "channels"
        emitAll(
            db.collection("channels")
                .whereEqualTo("visibility", "public")
                .snapshots()
                .map { snap ->
                    snap.toObjects(ChatChannel::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { safeTs(it.updatedAt ?: it.createdAt) }
                }
                .catch { error ->
                    val root = unwrapFirestoreError(error)
                    if (root == null) throw error
                    if (auth.currentUser?.uid == currentUserId) {
                        handleFirestoreError(root, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeMyMemberChannels(currentUserId: String): Flow<List<ChatChannel>> = flow {
        val path = "channels"
        emitAll(
            db.collection("channels")
                .whereArrayContains("memberUids", currentUserId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ChatChannel::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { safeTs(it.updatedAt ?: it.createdAt) }
                }
                .catch { error ->
                    val root = unwrapFirestoreError(error)
                    if (root == null) throw error
                    if (auth.currentUser?.uid == currentUserId) {
                        handleFirestoreError(root, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeChannel(channelId: String, currentUserId: String): Flow<ChatChannel?> = flow {
        val path = "channels/$channelId"
        emitAll(
            db.collection("channels").document(channelId)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(ChatChannel::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    val root = unwrapFirestoreError(error)
                    if (root == null) throw error
                    if (auth.currentUser?.uid == currentUserId) {
                        handleFirestoreError(root, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createPublicChannel(
        profile: StudentProfile,
        name: String,
        description: String,
        universityCode: String,
        courseCode: String
    ): Result<String> {
        var path = "channels"
        return try {
            val uid = requireUserId()
            val cleanName = name.trim().take(80)
            require(cleanName.length >= 2) { "Channel name must be at least 2 characters." }
            val cleanDesc = description.trim().take(240)
            val cleanCourse = courseCode.trim().uppercase().take(16)
            val docRef = db.collection("channels").document()
            path = docRef.path
            docRef.set(
                mapOf(
                    "name" to cleanName,
                    "description" to cleanDesc,
                    "university" to universityCode,
                    "courseCode" to cleanCourse,
                    "channelType" to ChannelType.CAMPUS_LOUNGE.name,
                    "visibility" to "public",
                    "creatorId" to uid,
                    "memberUids" to listOf(uid),
                    "lastMessageText" to "Channel opened for BW students",
                    "lastMessageSender" to profile.displayName.take(60),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun openOrCreateDirectChannel(
        myProfile: StudentProfile,
        otherProfile: StudentProfile
    ): Result<String> {
        val uid = requireUserId()
        require(otherProfile.userId.isNotBlank() && otherProfile.userId != uid)
        val pair = listOf(uid, otherProfile.userId).sorted()
        val channelId = "dm_${pair[0]}_${pair[1]}".take(128)
        val path = "channels/$channelId"
        return try {
            val channelRef = db.collection("channels").document(channelId)
            val myChannels = db.collection("channels")
                .whereArrayContains("memberUids", uid)
                .get()
                .await()
            val alreadyExists = myChannels.documents.any { it.id == channelId }
            if (!alreadyExists) {
                channelRef.set(
                    mapOf(
                        "name" to "${myProfile.displayName} & ${otherProfile.displayName}".take(80),
                        "description" to "Direct chat (${myProfile.university} / ${otherProfile.university})".take(240),
                        "university" to "ALL",
                        "courseCode" to "",
                        "channelType" to ChannelType.DIRECT.name,
                        "visibility" to "direct",
                        "creatorId" to uid,
                        "memberUids" to pair,
                        "lastMessageText" to "Say dumela to start chatting",
                        "lastMessageSender" to myProfile.displayName.take(60),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(channelId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeMessages(channel: ChatChannel, currentUserId: String): Flow<List<ChatMessage>> = flow {
        val path = "channels/${channel.id}/messages"
        val baseCollection = db.collection("channels").document(channel.id).collection("messages")
        val query = if (channel.visibility == "public") {
            baseCollection.whereEqualTo("visibility", "public")
        } else {
            baseCollection.whereArrayContains("memberUids", currentUserId)
        }
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { safeTs(it.createdAt) }
                }
                .catch { error ->
                    val root = unwrapFirestoreError(error)
                    if (root == null) throw error
                    if (auth.currentUser?.uid == currentUserId) {
                        handleFirestoreError(root, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun sendMessage(
        channel: ChatChannel,
        profile: StudentProfile,
        text: String
    ): Result<Unit> {
        val path = "channels/${channel.id}/messages"
        return try {
            val uid = requireUserId()
            val cleanText = text.trim().take(1000)
            require(cleanText.isNotEmpty()) { "Message cannot be empty." }

            val channelRef = db.collection("channels").document(channel.id)
            val msgRef = channelRef.collection("messages").document()
            val senderName = profile.displayName.take(50).ifBlank { "Student" }
            val messageMembers = if (channel.visibility == "public") {
                emptyList()
            } else {
                (channel.memberUids + uid).distinct().take(50)
            }

            val batch = db.batch()
            batch.set(
                msgRef,
                mapOf(
                    "channelId" to channel.id,
                    "senderId" to uid,
                    "senderName" to senderName,
                    "senderUniversity" to profile.university,
                    "text" to cleanText,
                    "visibility" to channel.visibility,
                    "memberUids" to messageMembers,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                channelRef,
                mapOf(
                    "lastMessageText" to cleanText.take(200),
                    "lastMessageSender" to profile.displayName.take(60),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            val recipientUids = (channel.memberUids + channel.creatorId)
                .filter { it.isNotBlank() && it != uid }
                .distinct()
                .take(15)
            val notifTitle = if (channel.channelType == ChannelType.DIRECT.name) {
                "$senderName sent you a message"
            } else {
                "$senderName in ${channel.name}"
            }.take(100)
            recipientUids.forEach { recipientUid ->
                val notifRef = db.collection("users").document(recipientUid)
                    .collection("notifications").document()
                batch.set(
                    notifRef,
                    mapOf(
                        "recipientId" to recipientUid,
                        "senderId" to uid,
                        "senderName" to senderName,
                        "senderUniversity" to profile.university,
                        "notifType" to NotificationType.CHAT_MESSAGE.name,
                        "title" to notifTitle,
                        "body" to cleanText.take(240),
                        "targetId" to channel.id,
                        "read" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // --- STUDENT MARKETPLACE (BWP) ---

    fun observeAvailableListings(currentUserId: String): Flow<List<MarketListing>> = flow {
        val path = "listings"
        emitAll(
            db.collection("listings")
                .whereEqualTo("listingStatus", "available")
                .snapshots()
                .map { snap ->
                    snap.toObjects(MarketListing::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { safeTs(it.createdAt) }
                }
                .catch { error ->
                    val root = unwrapFirestoreError(error)
                    if (root == null) throw error
                    if (auth.currentUser?.uid == currentUserId) {
                        handleFirestoreError(root, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeMyListings(currentUserId: String): Flow<List<MarketListing>> = flow {
        val path = "listings"
        emitAll(
            db.collection("listings")
                .whereEqualTo("sellerId", currentUserId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(MarketListing::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { safeTs(it.createdAt) }
                }
                .catch { error ->
                    val root = unwrapFirestoreError(error)
                    if (root == null) throw error
                    if (auth.currentUser?.uid == currentUserId) {
                        handleFirestoreError(root, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createListing(
        profile: StudentProfile,
        title: String,
        description: String,
        category: MarketCategory,
        priceBwp: Long,
        condition: ListingCondition,
        pickupSpot: String
    ): Result<Unit> {
        var path = "listings"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim().take(100)
            val cleanDesc = description.trim().take(600)
            val cleanPickup = pickupSpot.trim().take(100)
            require(cleanTitle.length >= 2) { "Title must be at least 2 characters." }
            require(cleanDesc.isNotEmpty()) { "Description cannot be empty." }
            require(priceBwp in 0L..500000L) { "Price must be between P0 and P500,000." }
            require(cleanPickup.isNotEmpty()) { "Campus pickup location is required." }

            val docRef = db.collection("listings").document()
            path = docRef.path
            docRef.set(
                mapOf(
                    "sellerId" to uid,
                    "sellerName" to profile.displayName.take(50).ifBlank { "Student" },
                    "sellerUniversity" to profile.university,
                    "title" to cleanTitle,
                    "description" to cleanDesc,
                    "category" to category.name,
                    "priceBwp" to priceBwp,
                    "condition" to condition.name,
                    "pickupSpot" to cleanPickup,
                    "listingStatus" to "available",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun markListingSold(listingId: String): Result<Unit> {
        val path = "listings/$listingId"
        return try {
            requireUserId()
            db.collection("listings").document(listingId).update(
                mapOf(
                    "listingStatus" to "sold",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- CAMPUS EVENTS & CLUBS ---

    fun observeEvents(currentUserId: String): Flow<List<CampusEvent>> = flow {
        val path = "events"
        emitAll(
            db.collection("events")
                .whereEqualTo("visibility", "public")
                .snapshots()
                .map { snap ->
                    snap.toObjects(CampusEvent::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(
                            compareBy<CampusEvent> { it.eventDateIso.ifBlank { "9999-99-99" } }
                                .thenByDescending { safeTs(it.createdAt) }
                        )
                }
                .catch { error ->
                    val root = unwrapFirestoreError(error)
                    if (root == null) throw error
                    if (auth.currentUser?.uid == currentUserId) {
                        handleFirestoreError(root, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createEvent(
        profile: StudentProfile,
        university: University,
        title: String,
        description: String,
        venue: String,
        eventDateLabel: String,
        eventDateIso: String,
        category: EventCategory
    ): Result<Unit> {
        var path = "events"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim().take(100)
            val cleanDesc = description.trim().take(600)
            val cleanVenue = venue.trim().take(100)
            val cleanDate = eventDateLabel.trim().take(50)
            val cleanIso = eventDateIso.trim().take(20)
            require(cleanTitle.length >= 2) { "Event title must be at least 2 characters." }
            require(cleanDesc.isNotEmpty()) { "Event details cannot be empty." }
            require(cleanVenue.length >= 2) { "Campus venue is required." }
            require(cleanDate.length >= 2) { "Date and time label is required." }

            val docRef = db.collection("events").document()
            path = docRef.path
            docRef.set(
                mapOf(
                    "organizerId" to uid,
                    "organizerName" to profile.displayName.take(50).ifBlank { "Student" },
                    "university" to university.code,
                    "title" to cleanTitle,
                    "description" to cleanDesc,
                    "venue" to cleanVenue,
                    "eventDateLabel" to cleanDate,
                    "eventDateIso" to cleanIso,
                    "category" to category.name,
                    "visibility" to "public",
                    "attendeeUids" to listOf(uid),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleEventRsvp(eventId: String, currentlyGoing: Boolean): Result<Unit> {
        val path = "events/$eventId"
        return try {
            val uid = requireUserId()
            val updateValue = if (currentlyGoing) FieldValue.arrayRemove(uid) else FieldValue.arrayUnion(uid)
            db.collection("events").document(eventId).update(
                mapOf(
                    "attendeeUids" to updateValue,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }
}
