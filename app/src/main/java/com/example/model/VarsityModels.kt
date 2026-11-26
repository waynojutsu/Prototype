package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class University(
    val code: String,
    val fullName: String,
    val city: String,
    val sampleDomain: String
) {
    UB("UB", "University of Botswana", "Gaborone", "ub.ac.bw"),
    BIUST("BIUST", "Botswana Int'l Univ. of Science & Tech", "Palapye", "biust.ac.bw"),
    BAC("BAC", "Botswana Accountancy College", "Gaborone / Francistown", "bac.ac.bw"),
    BOTHO("BOTHO", "Botho University", "Gaborone / Francistown / Maun", "bothouniversity.ac.bw"),
    BUAN("BUAN", "Botswana Univ. of Agriculture & Natural Resources", "Sebele", "buan.ac.bw"),
    OTHER("OTHER", "Other Botswana Institution", "Botswana", "campus.ac.bw");

    companion object {
        fun fromCode(raw: String?): University =
            entries.firstOrNull { it.code.equals(raw, ignoreCase = true) } ?: OTHER

        fun detectFromDomain(domain: String): University {
            val lower = domain.lowercase()
            return when {
                lower.endsWith("ub.ac.bw") -> UB
                lower.endsWith("biust.ac.bw") -> BIUST
                lower.endsWith("bac.ac.bw") -> BAC
                lower.contains("botho") && lower.endsWith(".ac.bw") -> BOTHO
                lower.endsWith("buan.ac.bw") -> BUAN
                else -> OTHER
            }
        }
    }
}

object StudentEmailValidator {
    private val acBwEmailRegex = Regex("^[A-Za-z0-9._%+\\-]+@([A-Za-z0-9\\-]+\\.)+ac\\.bw$")

    fun normalize(email: String): String = email.trim().lowercase()

    fun isValidAcBwEmail(email: String): Boolean {
        val cleaned = normalize(email)
        return cleaned.length in 8..120 && acBwEmailRegex.matches(cleaned)
    }

    fun extractDomain(email: String): String {
        val cleaned = normalize(email)
        val atIdx = cleaned.indexOf('@')
        return if (atIdx in 1 until cleaned.length - 1) cleaned.substring(atIdx + 1) else ""
    }
}

enum class PostCategory(val label: String) {
    ANNOUNCEMENT("Announcement"),
    QUESTION("Question"),
    CAMPUS_LIFE("Campus Life"),
    OPPORTUNITY("Opportunity");

    companion object {
        fun fromRaw(raw: String?): PostCategory =
            entries.firstOrNull { it.name == raw } ?: CAMPUS_LIFE
    }
}

enum class StudyGroupStatus(val raw: String, val label: String) {
    OPEN("open", "Open"),
    FULL("full", "Full"),
    CLOSED("closed", "Closed");

    companion object {
        fun fromRaw(raw: String?): StudyGroupStatus =
            entries.firstOrNull { it.raw == raw } ?: OPEN
    }
}

enum class JoinRequestStatus(val raw: String, val label: String) {
    PENDING("pending", "Pending"),
    ACCEPTED("accepted", "Accepted"),
    DECLINED("declined", "Declined");

    companion object {
        fun fromRaw(raw: String?): JoinRequestStatus =
            entries.firstOrNull { it.raw == raw } ?: PENDING
    }
}

enum class ChannelType(val label: String) {
    CAMPUS_LOUNGE("Campus Lounge"),
    STUDY_GROUP("Study Group"),
    DIRECT("Direct Message");

    companion object {
        fun fromRaw(raw: String?): ChannelType =
            entries.firstOrNull { it.name == raw } ?: CAMPUS_LOUNGE
    }
}

enum class MarketCategory(val label: String) {
    TEXTBOOKS("Textbooks"),
    TECH("Electronics"),
    HOUSING("Housing"),
    SERVICES("Services");

    companion object {
        fun fromRaw(raw: String?): MarketCategory =
            entries.firstOrNull { it.name == raw } ?: TEXTBOOKS
    }
}

enum class ListingCondition(val label: String) {
    NEW("Brand New"),
    LIKE_NEW("Like New"),
    GOOD("Good"),
    FAIR("Fair");

    companion object {
        fun fromRaw(raw: String?): ListingCondition =
            entries.firstOrNull { it.name == raw } ?: GOOD
    }
}

enum class EventCategory(val label: String) {
    ACADEMIC("Academic"),
    CLUBS("Clubs"),
    SPORTS("Sports"),
    SOCIAL("Social");

    companion object {
        fun fromRaw(raw: String?): EventCategory =
            entries.firstOrNull { it.name == raw } ?: CLUBS
    }
}

enum class PresenceStatus(val raw: String, val label: String) {
    ONLINE("online", "Online"),
    AWAY("away", "Away"),
    OFFLINE("offline", "Offline");

    companion object {
        fun fromRaw(raw: String?): PresenceStatus =
            entries.firstOrNull { it.raw.equals(raw, ignoreCase = true) } ?: OFFLINE
    }
}

enum class NotificationType(val label: String) {
    CHAT_MESSAGE("Message"),
    POST_REPLY("Post Reply"),
    ADMIN_REPORT("Moderation Alert");

    companion object {
        fun fromRaw(raw: String?): NotificationType =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: CHAT_MESSAGE
    }
}

enum class ReportReason(val label: String, val description: String) {
    SPAM("Spam or Scam", "Suspicious links, pyramid schemes, or repetitive promo"),
    HARASSMENT("Harassment or Bullying", "Targeted abuse, threats, or personal attacks"),
    HATE_SPEECH("Hate Speech", "Discriminatory or abusive language toward groups"),
    MISINFORMATION("Misleading Info", "Fake campus notices, forged exam dates, or rumours"),
    INAPPROPRIATE_CONTENT("Inappropriate Content", "NSFW, graphic, or unsafe campus content"),
    ACADEMIC_DISHONESTY("Academic Dishonesty", "Selling exam papers or cheating services");

    companion object {
        fun fromRaw(raw: String?): ReportReason =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: INAPPROPRIATE_CONTENT
    }
}

enum class ReportStatus(val raw: String, val label: String) {
    PENDING("pending", "Pending Review"),
    REVIEWED("reviewed", "Reviewed"),
    DISMISSED("dismissed", "Dismissed");

    companion object {
        fun fromRaw(raw: String?): ReportStatus =
            entries.firstOrNull { it.raw.equals(raw, ignoreCase = true) } ?: PENDING
    }
}

@Keep
@IgnoreExtraProperties
data class UserPrivate(
    @DocumentId val id: String = "",
    val userId: String = "",
    val googleEmail: String = "",
    val studentEmail: String = "",
    val verifiedDomain: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StudentProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val university: String = University.UB.code,
    val faculty: String = "",
    val yearOfStudy: Long = 1L,
    val bio: String = "",
    val verifiedDomain: String = "",
    val acBwVerified: Boolean = false,
    val discoverable: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CampusPost(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorUniversity: String = University.UB.code,
    val category: String = PostCategory.CAMPUS_LIFE.name,
    val title: String = "",
    val body: String = "",
    val visibility: String = "public",
    val likedByUids: List<String> = emptyList(),
    val commentCount: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PostComment(
    @DocumentId val id: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorUniversity: String = University.UB.code,
    val body: String = "",
    val visibility: String = "public",
    val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StudyGroup(
    @DocumentId val id: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val creatorUniversity: String = University.UB.code,
    val university: String = University.UB.code,
    val courseCode: String = "",
    val studyTopic: String = "",
    val description: String = "",
    val meetingSchedule: String = "",
    val maxMembers: Long = 8L,
    val memberUids: List<String> = emptyList(),
    val pendingRequestUids: List<String> = emptyList(),
    val visibility: String = "public",
    val groupStatus: String = StudyGroupStatus.OPEN.raw,
    val linkedChannelId: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StudyJoinRequest(
    @DocumentId val id: String = "",
    val groupId: String = "",
    val groupCreatorId: String = "",
    val requesterId: String = "",
    val requesterName: String = "",
    val requesterUniversity: String = University.UB.code,
    val requesterFaculty: String = "",
    val note: String = "",
    val requestStatus: String = JoinRequestStatus.PENDING.raw,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatChannel(
    @DocumentId val id: String = "",
    val name: String = "",
    val description: String = "",
    val university: String = "ALL",
    val courseCode: String = "",
    val channelType: String = ChannelType.CAMPUS_LOUNGE.name,
    val visibility: String = "public",
    val creatorId: String = "",
    val memberUids: List<String> = emptyList(),
    val lastMessageText: String = "",
    val lastMessageSender: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val channelId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderUniversity: String = University.UB.code,
    val text: String = "",
    val visibility: String = "public",
    val memberUids: List<String> = emptyList(),
    val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MarketListing(
    @DocumentId val id: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val sellerUniversity: String = University.UB.code,
    val title: String = "",
    val description: String = "",
    val category: String = MarketCategory.TEXTBOOKS.name,
    val priceBwp: Long = 0L,
    val condition: String = ListingCondition.GOOD.name,
    val pickupSpot: String = "",
    val listingStatus: String = "available",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CampusEvent(
    @DocumentId val id: String = "",
    val organizerId: String = "",
    val organizerName: String = "",
    val university: String = University.UB.code,
    val title: String = "",
    val description: String = "",
    val venue: String = "",
    val eventDateLabel: String = "",
    val eventDateIso: String = "",
    val category: String = EventCategory.CLUBS.name,
    val visibility: String = "public",
    val attendeeUids: List<String> = emptyList(),
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserPresence(
    @DocumentId val id: String = "",
    val userId: String = "",
    val status: String = PresenceStatus.OFFLINE.raw,
    val visibility: String = "public",
    val lastActiveAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun effectiveStatus(nowMs: Long = System.currentTimeMillis()): PresenceStatus {
        val declared = PresenceStatus.fromRaw(status)
        if (declared == PresenceStatus.OFFLINE) return PresenceStatus.OFFLINE
        val lastMs = lastActiveAt?.toDate()?.time ?: return declared
        val ageMs = (nowMs - lastMs).coerceAtLeast(0L)
        return when (declared) {
            PresenceStatus.ONLINE -> when {
                ageMs <= 3 * 60_000L -> PresenceStatus.ONLINE
                ageMs <= 12 * 60_000L -> PresenceStatus.AWAY
                else -> PresenceStatus.OFFLINE
            }
            PresenceStatus.AWAY -> if (ageMs <= 20 * 60_000L) PresenceStatus.AWAY else PresenceStatus.OFFLINE
            PresenceStatus.OFFLINE -> PresenceStatus.OFFLINE
        }
    }
}

@Keep
@IgnoreExtraProperties
data class UserNotification(
    @DocumentId val id: String = "",
    val recipientId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderUniversity: String = University.UB.code,
    val notifType: String = NotificationType.CHAT_MESSAGE.name,
    val title: String = "",
    val body: String = "",
    val targetId: String = "",
    val read: Boolean = false,
    val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PostReport(
    @DocumentId val id: String = "",
    val postId: String = "",
    val postAuthorId: String = "",
    val postAuthorName: String = "",
    val postTitle: String = "",
    val postExcerpt: String = "",
    val reporterId: String = "",
    val reporterName: String = "",
    val reporterUniversity: String = University.UB.code,
    val reason: String = ReportReason.INAPPROPRIATE_CONTENT.name,
    val details: String = "",
    val status: String = ReportStatus.PENDING.raw,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AdminNotification(
    @DocumentId val id: String = "",
    val reportId: String = "",
    val postId: String = "",
    val postAuthorId: String = "",
    val postTitle: String = "",
    val reporterId: String = "",
    val reporterName: String = "",
    val reporterUniversity: String = University.UB.code,
    val reason: String = ReportReason.INAPPROPRIATE_CONTENT.name,
    val details: String = "",
    val title: String = "",
    val body: String = "",
    val read: Boolean = false,
    val createdAt: Timestamp? = null
)

