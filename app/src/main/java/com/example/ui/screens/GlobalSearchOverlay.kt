package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.DynamicFeed
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.CampusEvent
import com.example.model.CampusPost
import com.example.model.EventCategory
import com.example.model.MarketCategory
import com.example.model.MarketListing
import com.example.model.PostCategory
import com.example.ui.components.GlassCard
import com.example.ui.components.NeubrutalBadge
import com.example.ui.components.SoftNeuCard
import com.example.ui.components.UniversityBadge
import com.example.ui.components.VarsityEmptyState
import com.example.ui.components.formatTimestampShort
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.DeepAmberInk
import com.example.ui.theme.DeepCyanContainer
import com.example.ui.theme.DeepCyanNavy
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.IceCyanText
import com.example.ui.theme.KalahariAmber
import com.example.ui.theme.MintPulaText
import com.example.ui.theme.WarmAmberText

enum class GlobalSearchScope(val label: String) {
    ALL("All"),
    POSTS("Posts"),
    MARKET("Marketplace"),
    EVENTS("Events")
}

@Composable
fun GlobalSearchOverlay(
    query: String,
    posts: List<CampusPost>,
    listings: List<MarketListing>,
    events: List<CampusEvent>,
    currentUserId: String,
    onSuggestionClick: (String) -> Unit,
    onDismiss: () -> Unit,
    onOpenPost: (CampusPost) -> Unit,
    onOpenMarketItem: (MarketListing) -> Unit,
    onContactSeller: (MarketListing) -> Unit,
    onOpenEvent: (CampusEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedScope by remember { mutableStateOf(GlobalSearchScope.ALL) }

    BackHandler(onBack = onDismiss)

    val trimmed = query.trim()

    val matchedPosts = remember(posts, trimmed) {
        if (trimmed.isEmpty()) emptyList()
        else posts.filter { post ->
            post.title.contains(trimmed, ignoreCase = true) ||
                post.body.contains(trimmed, ignoreCase = true) ||
                post.authorName.contains(trimmed, ignoreCase = true) ||
                post.authorUniversity.contains(trimmed, ignoreCase = true) ||
                PostCategory.fromRaw(post.category).label.contains(trimmed, ignoreCase = true)
        }
    }

    val matchedListings = remember(listings, trimmed) {
        if (trimmed.isEmpty()) emptyList()
        else listings.filter { item ->
            item.title.contains(trimmed, ignoreCase = true) ||
                item.description.contains(trimmed, ignoreCase = true) ||
                item.pickupSpot.contains(trimmed, ignoreCase = true) ||
                item.sellerName.contains(trimmed, ignoreCase = true) ||
                item.sellerUniversity.contains(trimmed, ignoreCase = true) ||
                MarketCategory.fromRaw(item.category).label.contains(trimmed, ignoreCase = true)
        }
    }

    val matchedEvents = remember(events, trimmed) {
        if (trimmed.isEmpty()) emptyList()
        else events.filter { event ->
            event.title.contains(trimmed, ignoreCase = true) ||
                event.description.contains(trimmed, ignoreCase = true) ||
                event.venue.contains(trimmed, ignoreCase = true) ||
                event.university.contains(trimmed, ignoreCase = true) ||
                event.organizerName.contains(trimmed, ignoreCase = true) ||
                event.eventDateLabel.contains(trimmed, ignoreCase = true) ||
                EventCategory.fromRaw(event.category).label.contains(trimmed, ignoreCase = true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("global_search_results")
    ) {
        if (trimmed.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Search across VarsityBW",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Find campus announcements, textbooks, off-campus rooms, and upcoming events across UB, BIUST, BAC, Botho, and BUAN.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Calculus", "Room", "UB", "BIUST", "Hackathon").forEach { hint ->
                                Surface(
                                    color = DeepCyanContainer.copy(alpha = 0.65f),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { onSuggestionClick(hint) }
                                ) {
                                    Text(
                                        text = hint,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = IceCyanText,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            return@Column
        }

        // Scope filter chips with live hit counts
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val total = matchedPosts.size + matchedListings.size + matchedEvents.size
            GlobalSearchScope.entries.forEach { scope ->
                val count = when (scope) {
                    GlobalSearchScope.ALL -> total
                    GlobalSearchScope.POSTS -> matchedPosts.size
                    GlobalSearchScope.MARKET -> matchedListings.size
                    GlobalSearchScope.EVENTS -> matchedEvents.size
                }
                val selected = selectedScope == scope
                FilterChip(
                    selected = selected,
                    onClick = { selectedScope = scope },
                    label = { Text("${scope.label} ($count)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DeepCyanContainer,
                        selectedLabelColor = ElectricCyan,
                        containerColor = CarbonSurface
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = HairlineBorder,
                        selectedBorderColor = ElectricCyan
                    ),
                    modifier = Modifier.testTag("global_search_scope_${scope.name.lowercase()}")
                )
            }
        }

        val showPosts = selectedScope == GlobalSearchScope.ALL || selectedScope == GlobalSearchScope.POSTS
        val showMarket = selectedScope == GlobalSearchScope.ALL || selectedScope == GlobalSearchScope.MARKET
        val showEvents = selectedScope == GlobalSearchScope.ALL || selectedScope == GlobalSearchScope.EVENTS

        val visibleCount = (if (showPosts) matchedPosts.size else 0) +
            (if (showMarket) matchedListings.size else 0) +
            (if (showEvents) matchedEvents.size else 0)

        if (visibleCount == 0) {
            VarsityEmptyState(
                icon = Icons.Outlined.Search,
                title = "No results for \"$trimmed\"",
                subtitle = "Try searching another keyword, course code, university (UB, BIUST, BAC), or venue.",
                actionLabel = "Clear Search",
                onAction = onDismiss
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 4.dp,
                    bottom = 32.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (showPosts && matchedPosts.isNotEmpty()) {
                    item(key = "header_posts") {
                        SearchSectionHeader(
                            icon = Icons.Outlined.DynamicFeed,
                            title = "Campus Posts (${matchedPosts.size})"
                        )
                    }
                    items(matchedPosts, key = { "post_${it.id}" }) { post ->
                        SoftNeuCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_result_post_${post.id}"),
                            shape = MaterialTheme.shapes.medium,
                            onClick = { onOpenPost(post) }
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    UniversityBadge(post.authorUniversity)
                                    Surface(color = DeepCyanContainer, shape = RoundedCornerShape(6.dp)) {
                                        Text(
                                            text = PostCategory.fromRaw(post.category).label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = IceCyanText,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        text = formatTimestampShort(post.createdAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = post.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = post.body,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                if (showMarket && matchedListings.isNotEmpty()) {
                    item(key = "header_market") {
                        SearchSectionHeader(
                            icon = Icons.Outlined.Storefront,
                            title = "Marketplace (${matchedListings.size})"
                        )
                    }
                    items(matchedListings, key = { "market_${it.id}" }) { listing ->
                        val isMine = listing.sellerId == currentUserId
                        val category = MarketCategory.fromRaw(listing.category)
                        SoftNeuCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_result_market_${listing.id}"),
                            shape = MaterialTheme.shapes.medium,
                            onClick = { onOpenMarketItem(listing) }
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        UniversityBadge(listing.sellerUniversity)
                                        Surface(color = AmberContainer, shape = RoundedCornerShape(6.dp)) {
                                            Text(
                                                text = category.label,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = WarmAmberText,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    NeubrutalBadge(
                                        text = if (category == MarketCategory.HOUSING) {
                                            "P ${listing.priceBwp}/mo"
                                        } else {
                                            "P ${listing.priceBwp}"
                                        },
                                        containerColor = KalahariAmber,
                                        contentColor = DeepAmberInk
                                    )
                                }
                                Text(
                                    text = listing.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.LocationOn,
                                        contentDescription = "Pickup spot",
                                        tint = ElectricCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "${listing.pickupSpot} • ${listing.sellerName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (!isMine) {
                                    Button(
                                        onClick = { onContactSeller(listing) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ElectricCyan,
                                            contentColor = DeepCyanNavy
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .defaultMinSize(minHeight = 48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Chat,
                                            contentDescription = "Contact seller",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text("Contact ${listing.sellerName}")
                                    }
                                }
                            }
                        }
                    }
                }

                if (showEvents && matchedEvents.isNotEmpty()) {
                    item(key = "header_events") {
                        SearchSectionHeader(
                            icon = Icons.Outlined.Event,
                            title = "Campus Events (${matchedEvents.size})"
                        )
                    }
                    items(matchedEvents, key = { "event_${it.id}" }) { event ->
                        SoftNeuCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_result_event_${event.id}"),
                            shape = MaterialTheme.shapes.medium,
                            onClick = { onOpenEvent(event) }
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    UniversityBadge(event.university)
                                    Surface(color = EmeraldContainer, shape = RoundedCornerShape(6.dp)) {
                                        Text(
                                            text = EventCategory.fromRaw(event.category).label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MintPulaText,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        text = "${event.attendeeUids.size} going",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ElectricCyan
                                    )
                                }
                                Text(
                                    text = event.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Schedule,
                                        contentDescription = "Event time",
                                        tint = KalahariAmber,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = event.eventDateLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = WarmAmberText
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.LocationOn,
                                        contentDescription = "Venue",
                                        tint = ElectricCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = event.venue,
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
            }
        }
    }
}

@Composable
private fun SearchSectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = ElectricCyan,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = ElectricCyan
        )
    }
}
