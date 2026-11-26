package com.example.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.HomeWork
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.ListingCondition
import com.example.model.MarketCategory
import com.example.model.MarketListing
import com.example.model.StudentProfile
import com.example.model.UiState
import com.example.ui.components.CampusFilterStrip
import com.example.ui.components.NeubrutalBadge
import com.example.ui.components.StateErrorCard
import com.example.ui.components.StateLoadingBox
import com.example.ui.components.UniversityBadge
import com.example.ui.components.VarsityEmptyState
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.DeepAmberInk
import com.example.ui.theme.DeepCyanContainer
import com.example.ui.theme.DeepCyanNavy
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElevatedCarbon
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPula
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.IceCyanText
import com.example.ui.theme.KalahariAmber
import com.example.ui.theme.MintPulaText
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.WarmAmberText
import com.example.ui.viewmodel.MarketAndEventsViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun MarketplaceScreen(
    viewModel: MarketAndEventsViewModel,
    myProfile: StudentProfile,
    onMessageSeller: (sellerId: String, sellerName: String, sellerUni: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listingsState by viewModel.listingsState.collectAsStateWithLifecycle()
    val myListingsState by viewModel.myListingsState.collectAsStateWithLifecycle()
    val campusFilter by viewModel.marketCampusFilter.collectAsStateWithLifecycle()
    val categoryFilter by viewModel.marketCategoryFilter.collectAsStateWithLifecycle()

    var showMyListingsOnly by remember { mutableStateOf(false) }
    var showBooksAndHousingOnly by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var inspectedListing by remember { mutableStateOf<MarketListing?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search bar for textbooks, modules, or campus accommodation
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Search books, rooms, or campus spots...",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search marketplace",
                        tint = ElectricCyan
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CarbonSurface,
                    unfocusedContainerColor = CarbonSurface,
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = HairlineBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("market_search_input")
            )

            CampusFilterStrip(
                selectedCampus = campusFilter,
                onSelectCampus = viewModel::setMarketCampusFilter
            )

            // Category chips emphasizing Books & Housing alongside all marketplace categories
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !showMyListingsOnly && !showBooksAndHousingOnly && categoryFilter == null,
                    onClick = {
                        showMyListingsOnly = false
                        showBooksAndHousingOnly = false
                        viewModel.setMarketCategoryFilter(null)
                    },
                    label = { Text("All") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DeepCyanContainer,
                        selectedLabelColor = ElectricCyan,
                        containerColor = CarbonSurface
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = !showMyListingsOnly && !showBooksAndHousingOnly && categoryFilter == null,
                        borderColor = HairlineBorder,
                        selectedBorderColor = ElectricCyan
                    ),
                    modifier = Modifier.testTag("market_filter_all")
                )

                FilterChip(
                    selected = !showMyListingsOnly && showBooksAndHousingOnly,
                    onClick = {
                        showMyListingsOnly = false
                        showBooksAndHousingOnly = !showBooksAndHousingOnly
                        if (showBooksAndHousingOnly) {
                            viewModel.setMarketCategoryFilter(null)
                        }
                    },
                    label = { Text("Books & Housing") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DeepCyanContainer,
                        selectedLabelColor = ElectricCyan,
                        containerColor = CarbonSurface
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = !showMyListingsOnly && showBooksAndHousingOnly,
                        borderColor = HairlineBorder,
                        selectedBorderColor = ElectricCyan
                    ),
                    modifier = Modifier.testTag("market_filter_books_housing")
                )

                MarketCategory.entries.forEach { cat ->
                    val selected = !showMyListingsOnly && !showBooksAndHousingOnly && categoryFilter == cat
                    val leadingIcon = when (cat) {
                        MarketCategory.TEXTBOOKS -> Icons.Outlined.AutoStories
                        MarketCategory.HOUSING -> Icons.Outlined.HomeWork
                        else -> null
                    }
                    FilterChip(
                        selected = selected,
                        onClick = {
                            showMyListingsOnly = false
                            showBooksAndHousingOnly = false
                            viewModel.setMarketCategoryFilter(if (selected) null else cat)
                        },
                        leadingIcon = if (leadingIcon != null) {
                            {
                                Icon(
                                    imageVector = leadingIcon,
                                    contentDescription = cat.label,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        label = { Text(cat.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElevatedCarbon,
                            selectedLabelColor = ElectricCyan,
                            selectedLeadingIconColor = ElectricCyan,
                            containerColor = CarbonSurface
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = HairlineBorder,
                            selectedBorderColor = ElectricCyan
                        ),
                        modifier = Modifier.testTag("market_filter_${cat.name.lowercase()}")
                    )
                }

                FilterChip(
                    selected = showMyListingsOnly,
                    onClick = {
                        showMyListingsOnly = !showMyListingsOnly
                        if (showMyListingsOnly) showBooksAndHousingOnly = false
                    },
                    label = { Text("My Listings") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AmberContainer,
                        selectedLabelColor = WarmAmberText,
                        containerColor = CarbonSurface
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = showMyListingsOnly,
                        borderColor = HairlineBorder,
                        selectedBorderColor = KalahariAmber
                    ),
                    modifier = Modifier.testTag("market_filter_mine")
                )
            }

            val activeState = if (showMyListingsOnly) myListingsState else listingsState
            when (val state = activeState) {
                is UiState.Loading -> StateLoadingBox("Loading student marketplace...")
                is UiState.Error -> StateErrorCard(state.message)
                is UiState.Success -> {
                    val query = searchQuery.trim()
                    val filteredListings = remember(state.data, showBooksAndHousingOnly, showMyListingsOnly, query) {
                        state.data.filter { item ->
                            val matchesBookHousing = if (!showMyListingsOnly && showBooksAndHousingOnly) {
                                item.category == MarketCategory.TEXTBOOKS.name ||
                                    item.category == MarketCategory.HOUSING.name
                            } else true
                            val matchesSearch = if (query.isEmpty()) {
                                true
                            } else {
                                item.title.contains(query, ignoreCase = true) ||
                                    item.description.contains(query, ignoreCase = true) ||
                                    item.pickupSpot.contains(query, ignoreCase = true) ||
                                    item.sellerUniversity.contains(query, ignoreCase = true) ||
                                    item.sellerName.contains(query, ignoreCase = true)
                            }
                            matchesBookHousing && matchesSearch
                        }
                    }

                    if (filteredListings.isEmpty()) {
                        VarsityEmptyState(
                            icon = Icons.Outlined.Storefront,
                            title = if (query.isNotEmpty()) "No matching listings" else "No listings yet",
                            subtitle = "List university textbooks, study gear, or off-campus rooms for verified Botswana students.",
                            actionLabel = if (query.isNotEmpty()) "Clear Search" else "Post Listing",
                            onAction = {
                                if (query.isNotEmpty()) searchQuery = ""
                                else showCreateDialog = true
                            }
                        )
                    } else {
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val columnCount = when {
                                maxWidth >= 840.dp -> 4
                                maxWidth >= 600.dp -> 3
                                else -> 2
                            }
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(columnCount),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("marketplace_grid"),
                                contentPadding = PaddingValues(
                                    start = 14.dp,
                                    end = 14.dp,
                                    top = 8.dp,
                                    bottom = 96.dp
                                ),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(filteredListings, key = { it.id }) { listing ->
                                    MarketListingGridCard(
                                        listing = listing,
                                        isMine = listing.sellerId == viewModel.currentUserId,
                                        onCardClick = { inspectedListing = listing },
                                        onMarkSold = { viewModel.markSold(listing.id) },
                                        onContactSeller = {
                                            onMessageSeller(
                                                listing.sellerId,
                                                listing.sellerName,
                                                listing.sellerUniversity
                                            )
                                        }
                                    )
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
            contentColor = DeepCyanNavy,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("create_listing_fab")
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Create marketplace listing")
        }
    }

    inspectedListing?.let { listing ->
        ListingDetailDialog(
            listing = listing,
            isMine = listing.sellerId == viewModel.currentUserId,
            onDismiss = { inspectedListing = null },
            onMarkSold = {
                viewModel.markSold(listing.id)
                inspectedListing = null
            },
            onContactSeller = {
                inspectedListing = null
                onMessageSeller(
                    listing.sellerId,
                    listing.sellerName,
                    listing.sellerUniversity
                )
            }
        )
    }

    if (showCreateDialog) {
        CreateListingDialog(
            defaultCategory = categoryFilter ?: MarketCategory.TEXTBOOKS,
            onDismiss = { showCreateDialog = false },
            onSubmit = { title, desc, cat, price, cond, pickup ->
                viewModel.createListing(myProfile, title, desc, cat, price, cond, pickup) {
                    showCreateDialog = false
                }
            }
        )
    }
}

@Composable
private fun MarketListingGridCard(
    listing: MarketListing,
    isMine: Boolean,
    onCardClick: () -> Unit,
    onMarkSold: () -> Unit,
    onContactSeller: () -> Unit
) {
    val category = MarketCategory.fromRaw(listing.category)
    val condition = ListingCondition.fromRaw(listing.condition)
    val isSold = listing.listingStatus == "sold"
    val placeholderRes = categoryPlaceholderDrawable(category)

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = CarbonSurface),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isSold) EmeraldPula.copy(alpha = 0.4f) else HairlineBorder,
                MaterialTheme.shapes.medium
            )
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onCardClick)
            .testTag("listing_card_${listing.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Image placeholder with gradient scrim, university chip, and prominent BWP price badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
            ) {
                Image(
                    painter = painterResource(id = placeholderRes),
                    contentDescription = "${category.label} listing placeholder for ${listing.title}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Top & bottom scrim for legible overlays
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    ObsidianVoid.copy(alpha = 0.58f),
                                    Color.Transparent,
                                    ObsidianVoid.copy(alpha = 0.85f)
                                )
                            )
                        )
                )

                // Top-left university badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    UniversityBadge(listing.sellerUniversity)
                }

                // Top-right category or SOLD tag
                Surface(
                    color = if (isSold) EmeraldContainer else ObsidianVoid.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isSold) EmeraldPula else HairlineBorder
                    ),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = if (isSold) "SOLD" else category.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSold) MintPulaText else IceCyanText,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                // Bottom-left Facebook Marketplace style price pill
                Surface(
                    color = if (isSold) ElevatedCarbon else ObsidianVoid.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = formatPriceBwp(listing.priceBwp, category, isSold),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isSold) MaterialTheme.colorScheme.onSurfaceVariant else com.example.ui.theme.CrispSnow,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = listing.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = DeepCyanContainer.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(5.dp)
                    ) {
                        Text(
                            text = condition.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = IceCyanText,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = listing.sellerName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = "Pickup spot",
                        tint = ElectricCyan,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = listing.pickupSpot,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(2.dp))

                if (isMine) {
                    if (!isSold) {
                        OutlinedButton(
                            onClick = onMarkSold,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, EmeraldPula),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("mark_sold_${listing.id}")
                        ) {
                            Text(
                                text = "Mark Sold",
                                color = EmeraldPula,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1
                            )
                        }
                    } else {
                        Surface(
                            color = EmeraldContainer.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Sold",
                                    tint = MintPulaText,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = "Sold",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MintPulaText
                                )
                            }
                        }
                    }
                } else {
                    Button(
                        onClick = onContactSeller,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCyan,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("message_seller_${listing.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Chat,
                            contentDescription = "Contact seller",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "Contact Seller",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ListingDetailDialog(
    listing: MarketListing,
    isMine: Boolean,
    onDismiss: () -> Unit,
    onMarkSold: () -> Unit,
    onContactSeller: () -> Unit
) {
    val category = MarketCategory.fromRaw(listing.category)
    val condition = ListingCondition.fromRaw(listing.condition)
    val isSold = listing.listingStatus == "sold"
    val placeholderRes = categoryPlaceholderDrawable(category)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = null,
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Image(
                        painter = painterResource(id = placeholderRes),
                        contentDescription = "${category.label} preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                    ) {
                        UniversityBadge(listing.sellerUniversity)
                    }
                    Surface(
                        color = KalahariAmber,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = formatPriceBwp(listing.priceBwp, category, isSold),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = DeepAmberInk,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(color = ElevatedCarbon, shape = RoundedCornerShape(6.dp)) {
                        Text(
                            text = category.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = IceCyanText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Surface(color = DeepCyanContainer, shape = RoundedCornerShape(6.dp)) {
                        Text(
                            text = condition.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = IceCyanText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = listing.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = listing.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(color = HairlineBorder)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = "Pickup spot",
                        tint = ElectricCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = listing.pickupSpot,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = "Seller",
                        tint = KalahariAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Listed by ${listing.sellerName} (${listing.sellerUniversity})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            if (isMine) {
                if (!isSold) {
                    Button(
                        onClick = onMarkSold,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPula,
                            contentColor = DeepCyanNavy
                        )
                    ) {
                        Text("Mark as Sold")
                    }
                }
            } else {
                Button(
                    onClick = onContactSeller,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = DeepCyanNavy
                    ),
                    modifier = Modifier.testTag("detail_contact_seller_${listing.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Chat,
                        contentDescription = "Contact seller",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Contact ${listing.sellerName}")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun CreateListingDialog(
    defaultCategory: MarketCategory,
    onDismiss: () -> Unit,
    onSubmit: (String, String, MarketCategory, Long, ListingCondition, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(defaultCategory) }
    var priceText by remember { mutableStateOf("") }
    var condition by remember { mutableStateOf(ListingCondition.GOOD) }
    var pickupSpot by remember { mutableStateOf("") }

    val parsedPrice = priceText.trim().toLongOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("New Marketplace Listing", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MarketCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 100) title = it },
                    label = { Text(if (category == MarketCategory.HOUSING) "Room / Housing Title" else "Item or Book Title") },
                    placeholder = {
                        Text(
                            if (category == MarketCategory.HOUSING) {
                                "e.g. Furnished Single Room near UB"
                            } else {
                                "e.g. Calculus Stewart 8th Ed"
                            }
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("listing_title_input")
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() } && input.length <= 6) priceText = input
                    },
                    label = { Text(if (category == MarketCategory.HOUSING) "Monthly Rent (BWP)" else "Price (BWP / Pula)") },
                    prefix = { Text("P ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("listing_price_input")
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ListingCondition.entries.forEach { cond ->
                        FilterChip(
                            selected = condition == cond,
                            onClick = { condition = cond },
                            label = { Text(cond.label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = pickupSpot,
                    onValueChange = { if (it.length <= 100) pickupSpot = it },
                    label = { Text(if (category == MarketCategory.HOUSING) "Area / Distance to Campus" else "Campus / Meetup Spot") },
                    placeholder = {
                        Text(
                            if (category == MarketCategory.HOUSING) {
                                "e.g. Block 8 / 5 min walk to UB"
                            } else {
                                "e.g. UB Student Centre / Palapye Mall"
                            }
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("listing_pickup_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 600) description = it },
                    label = { Text("Description") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("listing_description_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (parsedPrice != null) {
                        onSubmit(title, description, category, parsedPrice, condition, pickupSpot)
                    }
                },
                enabled = title.trim().length >= 2 &&
                    description.trim().isNotEmpty() &&
                    parsedPrice != null &&
                    pickupSpot.trim().isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricCyan,
                    contentColor = DeepCyanNavy
                ),
                modifier = Modifier.testTag("submit_listing_button")
            ) {
                Text("Post Item")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@DrawableRes
private fun categoryPlaceholderDrawable(category: MarketCategory): Int = when (category) {
    MarketCategory.TEXTBOOKS -> R.drawable.img_market_books
    MarketCategory.HOUSING -> R.drawable.img_market_housing
    MarketCategory.TECH -> R.drawable.img_market_tech
    MarketCategory.SERVICES -> R.drawable.img_market_services
}

private fun formatPriceBwp(priceBwp: Long, category: MarketCategory, isSold: Boolean): String {
    if (isSold) return "SOLD"
    val formatted = NumberFormat.getIntegerInstance(Locale.US).format(priceBwp)
    return if (category == MarketCategory.HOUSING) "P $formatted/mo" else "P $formatted"
}
