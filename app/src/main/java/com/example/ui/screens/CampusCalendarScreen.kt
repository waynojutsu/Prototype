package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.CampusEvent
import com.example.model.EventCategory
import com.example.model.StudentProfile
import com.example.model.UiState
import com.example.model.University
import com.example.ui.components.StateErrorCard
import com.example.ui.components.StateLoadingBox
import com.example.ui.components.UniversityBadge
import com.example.ui.components.VarsityEmptyState
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.CarbonSurface
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
import com.example.ui.theme.WarmAmberText
import com.example.ui.viewmodel.MarketAndEventsViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(
    viewModel: MarketAndEventsViewModel,
    myProfile: StudentProfile,
    modifier: Modifier = Modifier
) {
    val eventsState by viewModel.eventsState.collectAsStateWithLifecycle()
    val campusFilter by viewModel.eventCampusFilter.collectAsStateWithLifecycle()
    val categoryFilter by viewModel.eventCategoryFilter.collectAsStateWithLifecycle()
    val selectedDateIso by viewModel.selectedDateIso.collectAsStateWithLifecycle()

    var showCalendarGrid by remember { mutableStateOf(true) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showJumpDatePicker by remember { mutableStateOf(false) }

    val initialCal = remember { Calendar.getInstance() }
    var displayYear by remember { mutableIntStateOf(initialCal.get(Calendar.YEAR)) }
    var displayMonth by remember { mutableIntStateOf(initialCal.get(Calendar.MONTH)) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header controls: Calendar / Agenda segmented switcher + Jump to Date
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.testTag("calendar_view_toggle")
                ) {
                    SegmentedButton(
                        selected = showCalendarGrid,
                        onClick = { showCalendarGrid = true },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = DeepCyanContainer,
                            activeContentColor = ElectricCyan,
                            inactiveContainerColor = CarbonSurface,
                            inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.CalendarMonth,
                                contentDescription = "Calendar view",
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        label = { Text("Calendar") }
                    )
                    SegmentedButton(
                        selected = !showCalendarGrid,
                        onClick = { showCalendarGrid = false },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = DeepCyanContainer,
                            activeContentColor = ElectricCyan,
                            inactiveContainerColor = CarbonSurface,
                            inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.ViewAgenda,
                                contentDescription = "Agenda view",
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        label = { Text("Agenda") }
                    )
                }

                AssistChip(
                    onClick = { showJumpDatePicker = true },
                    label = { Text("Pick Date") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.CalendarToday,
                            contentDescription = "Pick date",
                            tint = ElectricCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = CarbonSurface,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(1.dp, HairlineBorder),
                    modifier = Modifier.testTag("jump_to_date_chip")
                )
            }

            // University filter strip with quick "My Campus" shortcut
            UniversityCalendarFilterRow(
                myUniversityCode = myProfile.university,
                selectedCampus = campusFilter,
                onSelectCampus = viewModel::setEventCampusFilter
            )

            // Category filter row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = categoryFilter == null,
                    onClick = { viewModel.setEventCategoryFilter(null) },
                    label = { Text("All Types") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElevatedCarbon,
                        selectedLabelColor = ElectricCyan,
                        containerColor = CarbonSurface
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = categoryFilter == null,
                        borderColor = HairlineBorder,
                        selectedBorderColor = ElectricCyan
                    )
                )
                EventCategory.entries.forEach { cat ->
                    val selected = categoryFilter == cat
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.setEventCategoryFilter(if (selected) null else cat) },
                        label = { Text(cat.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElevatedCarbon,
                            selectedLabelColor = ElectricCyan,
                            containerColor = CarbonSurface
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = HairlineBorder,
                            selectedBorderColor = ElectricCyan
                        )
                    )
                }
            }

            when (val state = eventsState) {
                is UiState.Loading -> StateLoadingBox("Loading campus calendar...")
                is UiState.Error -> StateErrorCard(state.message)
                is UiState.Success -> {
                    val allMatchingEvents = state.data
                    val eventsByIso = remember(allMatchingEvents) {
                        allMatchingEvents
                            .filter { it.eventDateIso.isNotBlank() }
                            .groupBy { it.eventDateIso }
                    }
                    val displayedEvents = remember(allMatchingEvents, selectedDateIso) {
                        if (selectedDateIso.isNullOrBlank()) {
                            allMatchingEvents
                        } else {
                            allMatchingEvents.filter { it.eventDateIso == selectedDateIso }
                        }
                    }

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
                        if (showCalendarGrid) {
                            item(key = "calendar_card") {
                                MonthlyCalendarCard(
                                    year = displayYear,
                                    month = displayMonth,
                                    selectedDateIso = selectedDateIso,
                                    eventsByIso = eventsByIso,
                                    onPreviousMonth = {
                                        if (displayMonth == Calendar.JANUARY) {
                                            displayMonth = Calendar.DECEMBER
                                            displayYear -= 1
                                        } else {
                                            displayMonth -= 1
                                        }
                                    },
                                    onNextMonth = {
                                        if (displayMonth == Calendar.DECEMBER) {
                                            displayMonth = Calendar.JANUARY
                                            displayYear += 1
                                        } else {
                                            displayMonth += 1
                                        }
                                    },
                                    onTodayClick = {
                                        val now = Calendar.getInstance()
                                        displayYear = now.get(Calendar.YEAR)
                                        displayMonth = now.get(Calendar.MONTH)
                                        viewModel.setSelectedDateIso(null)
                                    },
                                    onDayClick = { iso ->
                                        viewModel.setSelectedDateIso(
                                            if (selectedDateIso == iso) null else iso
                                        )
                                    }
                                )
                            }
                        }

                        if (!selectedDateIso.isNullOrBlank()) {
                            item(key = "selected_day_banner") {
                                Surface(
                                    color = DeepCyanContainer.copy(alpha = 0.7f),
                                    shape = MaterialTheme.shapes.small,
                                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Events on ${formatIsoReadable(selectedDateIso!!)}",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = IceCyanText
                                        )
                                        TextButton(
                                            onClick = { viewModel.setSelectedDateIso(null) },
                                            modifier = Modifier.testTag("clear_date_filter_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Clear,
                                                contentDescription = "Clear date filter",
                                                tint = ElectricCyan,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text("Show All", color = ElectricCyan)
                                        }
                                    }
                                }
                            }
                        }

                        if (displayedEvents.isEmpty()) {
                            item(key = "empty_events") {
                                VarsityEmptyState(
                                    icon = Icons.Outlined.Event,
                                    title = if (!selectedDateIso.isNullOrBlank()) {
                                        "No events on this date"
                                    } else {
                                        "No upcoming campus events"
                                    },
                                    subtitle = if (campusFilter == "ALL") {
                                        "Post hackathons, club nights, sports fixtures, or academic seminars across Botswana campuses."
                                    } else {
                                        "No events listed for $campusFilter yet. Be the first to post one!"
                                    },
                                    actionLabel = if (!selectedDateIso.isNullOrBlank()) "Show All Upcoming" else "Post Event",
                                    onAction = {
                                        if (!selectedDateIso.isNullOrBlank()) {
                                            viewModel.setSelectedDateIso(null)
                                        } else {
                                            showCreateDialog = true
                                        }
                                    }
                                )
                            }
                        } else {
                            items(displayedEvents, key = { it.id }) { event ->
                                CampusEventCalendarCard(
                                    event = event,
                                    currentUserId = viewModel.currentUserId,
                                    onToggleRsvp = { viewModel.toggleRsvp(event) }
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
                .testTag("create_event_fab")
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Post campus event")
        }
    }

    if (showJumpDatePicker) {
        val jumpPickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showJumpDatePicker = false },
            colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
            confirmButton = {
                TextButton(
                    onClick = {
                        jumpPickerState.selectedDateMillis?.let { millis ->
                            val cal = utcMillisToLocalCalendar(millis)
                            displayYear = cal.get(Calendar.YEAR)
                            displayMonth = cal.get(Calendar.MONTH)
                            viewModel.setSelectedDateIso(formatCalendarIso(cal))
                        }
                        showJumpDatePicker = false
                    }
                ) {
                    Text("Filter Date", color = ElectricCyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { showJumpDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = jumpPickerState)
        }
    }

    if (showCreateDialog) {
        val defaultUni = if (campusFilter != "ALL") {
            University.fromCode(campusFilter)
        } else {
            University.fromCode(myProfile.university)
        }
        CreateCalendarEventDialog(
            defaultUniversity = defaultUni,
            initialDateIso = selectedDateIso,
            onDismiss = { showCreateDialog = false },
            onSubmit = { uni, title, desc, venue, dateLabel, dateIso, cat ->
                viewModel.createEvent(myProfile, uni, title, desc, venue, dateLabel, dateIso, cat) {
                    showCreateDialog = false
                }
            }
        )
    }
}

@Composable
private fun UniversityCalendarFilterRow(
    myUniversityCode: String,
    selectedCampus: String,
    onSelectCampus: (String) -> Unit
) {
    val options = buildList {
        add("ALL" to "All BW")
        val myUni = University.fromCode(myUniversityCode)
        add(myUni.code to "My Campus (${myUni.code})")
        University.entries
            .filter { it.code != myUni.code }
            .forEach { uni ->
                add(uni.code to if (uni == University.OTHER) "Other BW" else uni.code)
            }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (code, label) ->
            val selected = selectedCampus == code
            val isMyCampus = code == myUniversityCode && label.startsWith("My Campus")
            FilterChip(
                selected = selected,
                onClick = { onSelectCampus(code) },
                leadingIcon = if (isMyCampus) {
                    {
                        Icon(
                            imageVector = Icons.Outlined.School,
                            contentDescription = "My university",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else null,
                label = { Text(label, style = MaterialTheme.typography.labelLarge) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
                    selectedLabelColor = ElectricCyan,
                    selectedLeadingIconColor = ElectricCyan,
                    containerColor = CarbonSurface,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    borderColor = HairlineBorder,
                    selectedBorderColor = ElectricCyan
                ),
                modifier = Modifier.testTag("calendar_uni_filter_$code")
            )
        }
    }
}

@Composable
private fun MonthlyCalendarCard(
    year: Int,
    month: Int,
    selectedDateIso: String?,
    eventsByIso: Map<String, List<CampusEvent>>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTodayClick: () -> Unit,
    onDayClick: (String) -> Unit
) {
    val monthTitle = remember(year, month) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
    }
    val todayIso = remember { formatCalendarIso(Calendar.getInstance()) }
    val cells = remember(year, month) { buildMonthGridCells(year, month) }
    val weekdayHeaders = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = CarbonSurface),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, HairlineBorder, MaterialTheme.shapes.large)
            .testTag("monthly_calendar_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onPreviousMonth,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("calendar_prev_month")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous month",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = monthTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onNextMonth,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("calendar_next_month")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next month",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                TextButton(
                    onClick = onTodayClick,
                    modifier = Modifier.testTag("calendar_today_button")
                ) {
                    Text("Today", color = ElectricCyan)
                }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                weekdayHeaders.forEach { day ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            cells.chunked(7).forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    week.forEach { cell ->
                        if (cell == null) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                            )
                        } else {
                            val dayEvents = eventsByIso[cell.iso].orEmpty()
                            val isSelected = selectedDateIso == cell.iso
                            val isToday = todayIso == cell.iso
                            CalendarDayCell(
                                dayNumber = cell.dayOfMonth,
                                eventCount = dayEvents.size,
                                isSelected = isSelected,
                                isToday = isToday,
                                onClick = { onDayClick(cell.iso) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("calendar_day_${cell.iso}")
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    dayNumber: Int,
    eventCount: Int,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        isSelected -> ElectricCyan
        eventCount > 0 -> DeepCyanContainer.copy(alpha = 0.65f)
        else -> ElevatedCarbon.copy(alpha = 0.35f)
    }
    val textColor = when {
        isSelected -> DeepCyanNavy
        eventCount > 0 -> IceCyanText
        else -> MaterialTheme.colorScheme.onSurface
    }
    val borderStroke = when {
        isSelected -> BorderStroke(1.5.dp, ElectricCyan)
        isToday -> BorderStroke(1.5.dp, KalahariAmber)
        eventCount > 0 -> BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.35f))
        else -> null
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .then(
                if (borderStroke != null) Modifier.border(borderStroke, RoundedCornerShape(10.dp))
                else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = dayNumber.toString(),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (isSelected || eventCount > 0 || isToday) FontWeight.Bold else FontWeight.Normal
                ),
                color = textColor
            )
            if (eventCount > 0) {
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(eventCount.coerceAtMost(3)) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) DeepCyanNavy else KalahariAmber)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CampusEventCalendarCard(
    event: CampusEvent,
    currentUserId: String,
    onToggleRsvp: () -> Unit
) {
    val going = event.attendeeUids.contains(currentUserId)
    val category = EventCategory.fromRaw(event.category)
    val (dayBadge, monthBadge) = remember(event.eventDateIso, event.eventDateLabel) {
        extractDayMonthBadge(event.eventDateIso, event.eventDateLabel)
    }

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = CarbonSurface),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (going) EmeraldPula.copy(alpha = 0.55f) else HairlineBorder,
                MaterialTheme.shapes.medium
            )
            .testTag("event_card_${event.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Calendar Date Tile
                Surface(
                    color = DeepCyanContainer,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.45f)),
                    modifier = Modifier.width(54.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = monthBadge,
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan,
                            maxLines = 1
                        )
                        Text(
                            text = dayBadge,
                            style = MaterialTheme.typography.headlineSmall,
                            color = IceCyanText,
                            maxLines = 1
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        UniversityBadge(event.university)
                        Surface(color = AmberContainer, shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = category.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = WarmAmberText,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleLarge,
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
                }
            }

            Text(
                text = event.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = "Venue",
                    tint = ElectricCyan,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "${event.venue} • Host: ${event.organizerName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(color = HairlineBorder)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${event.attendeeUids.size} students going",
                    style = MaterialTheme.typography.labelMedium,
                    color = ElectricCyan
                )
                if (going) {
                    FilledTonalButton(
                        onClick = onToggleRsvp,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = EmeraldContainer,
                            contentColor = MintPulaText
                        ),
                        modifier = Modifier.testTag("rsvp_event_${event.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Going",
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Going")
                    }
                } else {
                    Button(
                        onClick = onToggleRsvp,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCyan,
                            contentColor = DeepCyanNavy
                        ),
                        modifier = Modifier.testTag("rsvp_event_${event.id}")
                    ) {
                        Text("RSVP Going")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateCalendarEventDialog(
    defaultUniversity: University,
    initialDateIso: String?,
    onDismiss: () -> Unit,
    onSubmit: (University, String, String, String, String, String, EventCategory) -> Unit
) {
    var university by remember { mutableStateOf(defaultUniversity) }
    var title by remember { mutableStateOf("") }
    var selectedDateIso by remember {
        mutableStateOf(initialDateIso?.takeIf { it.isNotBlank() } ?: formatCalendarIso(Calendar.getInstance()))
    }
    var timeLabel by remember { mutableStateOf("14:00") }
    var venue by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(EventCategory.CLUBS) }
    var showDatePicker by remember { mutableStateOf(false) }

    val formattedDateLabel = remember(selectedDateIso, timeLabel) {
        val cleanTime = timeLabel.trim().ifBlank { "All Day" }
        "${formatIsoReadable(selectedDateIso)} • $cleanTime".take(50)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Add Campus Event", style = MaterialTheme.typography.headlineSmall) },
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
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    EventCategory.entries.forEach { cat ->
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
                    label = { Text("Event Title") },
                    placeholder = { Text("e.g. GDSC Botswana AI & Kotlin Jam") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("event_title_input")
                )
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pick_event_date_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = "Choose event date",
                        tint = ElectricCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Date: ${formatIsoReadable(selectedDateIso)}", color = IceCyanText)
                }
                OutlinedTextField(
                    value = timeLabel,
                    onValueChange = { if (it.length <= 20) timeLabel = it },
                    label = { Text("Time") },
                    placeholder = { Text("e.g. 14:00 or 18:30") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("event_date_input")
                )
                OutlinedTextField(
                    value = venue,
                    onValueChange = { if (it.length <= 100) venue = it },
                    label = { Text("Campus Venue") },
                    placeholder = { Text("e.g. UB Block 247 Auditorium") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("event_venue_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 600) description = it },
                    label = { Text("Details") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("event_description_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(
                        university,
                        title,
                        description,
                        venue,
                        formattedDateLabel,
                        selectedDateIso,
                        category
                    )
                },
                enabled = title.trim().length >= 2 &&
                    venue.trim().length >= 2 &&
                    description.trim().isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricCyan,
                    contentColor = DeepCyanNavy
                ),
                modifier = Modifier.testTag("submit_event_button")
            ) {
                Text("Publish Event")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = isoToUtcMillis(selectedDateIso)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            selectedDateIso = formatCalendarIso(utcMillisToLocalCalendar(millis))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Confirm", color = ElectricCyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

private data class CalendarCell(
    val dayOfMonth: Int,
    val iso: String
)

private fun buildMonthGridCells(year: Int, month: Int): List<CalendarCell?> {
    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val firstDayOffset = cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val cells = mutableListOf<CalendarCell?>()
    repeat(firstDayOffset) { cells.add(null) }
    for (day in 1..daysInMonth) {
        cal.set(Calendar.DAY_OF_MONTH, day)
        cells.add(CalendarCell(dayOfMonth = day, iso = formatCalendarIso(cal)))
    }
    while (cells.size % 7 != 0) {
        cells.add(null)
    }
    return cells
}

private fun formatCalendarIso(cal: Calendar): String =
    String.format(
        Locale.US,
        "%04d-%02d-%02d",
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH)
    )

private fun utcMillisToLocalCalendar(utcMillis: Long): Calendar {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = utcMillis
    }
    return Calendar.getInstance().apply {
        set(Calendar.YEAR, utc.get(Calendar.YEAR))
        set(Calendar.MONTH, utc.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, utc.get(Calendar.DAY_OF_MONTH))
    }
}

private fun isoToUtcMillis(iso: String): Long? {
    val parts = iso.split("-")
    if (parts.size != 3) return null
    val y = parts[0].toIntOrNull() ?: return null
    val m = (parts[1].toIntOrNull() ?: return null) - 1
    val d = parts[2].toIntOrNull() ?: return null
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(y, m, d)
    }.timeInMillis
}

private fun formatIsoReadable(iso: String): String {
    val parts = iso.split("-")
    if (parts.size != 3) return iso
    val y = parts[0].toIntOrNull() ?: return iso
    val m = (parts[1].toIntOrNull() ?: return iso) - 1
    val d = parts[2].toIntOrNull() ?: return iso
    val cal = Calendar.getInstance().apply { set(y, m, d) }
    return SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(cal.time)
}

private fun extractDayMonthBadge(iso: String, fallbackLabel: String): Pair<String, String> {
    val parts = iso.split("-")
    if (parts.size == 3) {
        val y = parts[0].toIntOrNull()
        val m = parts[1].toIntOrNull()?.minus(1)
        val d = parts[2].toIntOrNull()
        if (y != null && m != null && d != null) {
            val cal = Calendar.getInstance().apply { set(y, m, d) }
            val monthStr = SimpleDateFormat("MMM", Locale.getDefault()).format(cal.time).uppercase()
            return d.toString() to monthStr
        }
    }
    val digits = Regex("\\b(\\d{1,2})\\b").find(fallbackLabel)?.groupValues?.get(1) ?: "--"
    val monthWord = Regex("\\b(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\b", RegexOption.IGNORE_CASE)
        .find(fallbackLabel)?.value?.uppercase() ?: "EVT"
    return digits to monthWord
}
