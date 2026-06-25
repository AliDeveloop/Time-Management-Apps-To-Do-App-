package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.Task
import com.example.data.GeminiParser
import com.example.ui.AnalyticsHelper
import com.example.ui.AppViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun RoozAraAppContent(viewModel: AppViewModel) {
    val currentLanguage = viewModel.currentLanguage
    val currentRoute = remember { mutableStateOf("timeline") }
    val showAddDialog = remember { mutableStateOf(false) }

    // Floating Alert banner for simulated notification triggers
    val notificationMsg = viewModel.activeNotificationMsg

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = TrueBlack,
        bottomBar = {
            if (viewModel.activeFocusTask == null) {
                RoozAraBottomNavigation(
                    currentRoute = currentRoute.value,
                    onNavigate = { currentRoute.value = it },
                    language = currentLanguage
                )
            }
        },
        floatingActionButton = {
            if (viewModel.activeFocusTask == null && currentRoute.value == "timeline") {
                FloatingActionButton(
                    onClick = { showAddDialog.value = true },
                    containerColor = AccentPureWhite,
                    contentColor = PureBlack,
                    shape = CircleShape,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .testTag("add_task_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Task")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(TrueBlack)
        ) {
            // Main views with smooth crossfades
            AnimatedContent(
                targetState = currentRoute.value,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) with fadeOut(animationSpec = tween(220))
                },
                label = "ScreenNavigation"
            ) { route ->
                when (route) {
                    "timeline" -> TimelineScreen(viewModel, onAddTaskClick = { showAddDialog.value = true })
                    "calendar" -> CalendarScreen(viewModel)
                    "analytics" -> AnalyticsScreen(viewModel)
                    "settings" -> SettingsScreen(viewModel)
                }
            }

            // Simulated Notification Floating Banner
            if (notificationMsg != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.TopCenter)
                        .border(1.dp, AccentPureWhite.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.NotificationsActive,
                            contentDescription = "Alert",
                            tint = EnergyHigh,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = notificationMsg,
                            color = PrimarySilver,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1.of)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { viewModel.clearNotification() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = SecondaryGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Immersive Full-Screen Focus Mode Overlay
            val focusTask = viewModel.activeFocusTask
            if (focusTask != null) {
                FocusModeOverlay(viewModel = viewModel, task = focusTask)
            }
        }
    }

    if (showAddDialog.value) {
        AddTaskDialog(
            viewModel = viewModel,
            onDismiss = { showAddDialog.value = false }
        )
    }
}

@Composable
fun RoozAraBottomNavigation(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    language: AppLanguage
) {
    NavigationBar(
        containerColor = PanelBackground,
        tonalElevation = 0.dp,
        modifier = Modifier
            .border(TfUnitBorderWidth, Color.White.copy(alpha = 0.05f), RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        val items = listOf(
            Triple("timeline", Icons.Outlined.Timeline, "timeline"),
            Triple("calendar", Icons.Outlined.CalendarToday, "calendar"),
            Triple("analytics", Icons.Outlined.Analytics, "analytics"),
            Triple("settings", Icons.Outlined.Settings, "settings")
        )

        items.forEach { (route, icon, langKey) ->
            val isSelected = currentRoute == route
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(route) },
                icon = {
                    Icon(
                        icon,
                        contentDescription = Translation.getString(langKey, language),
                        tint = if (isSelected) AccentPurple else SecondaryGray
                    )
                },
                label = {
                    Text(
                        text = Translation.getString(langKey, language),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) AccentPurple else SecondaryGray
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = AccentPurple.copy(alpha = 0.15f),
                    selectedIconColor = AccentPurple,
                    unselectedIconColor = SecondaryGray
                ),
                modifier = Modifier.testTag("nav_tab_$route")
            )
        }
    }
}

// ---------------- TIMELINE SCREEN ----------------

@Composable
fun TimelineScreen(viewModel: AppViewModel, onAddTaskClick: () -> Unit) {
    val language = viewModel.currentLanguage
    val dateStr by viewModel.currentDate.collectAsState()
    val tasks by viewModel.tasksForDate.collectAsState()

    val formattedDateDisplay = remember(dateStr, language) {
        formatDateForHeader(dateStr, language)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp)
    ) {
        // Upper Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = Translation.getString("app_name", language),
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 28.sp,
                    color = AccentPureWhite,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = formattedDateDisplay,
                        style = MaterialTheme.typography.labelSmall,
                        color = SecondaryGray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Right side: Reshuffle and Productivity Score
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (tasks.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.triggerReshuffle() },
                        modifier = Modifier
                            .background(DividerColor, CircleShape)
                            .size(36.dp)
                            .testTag("reshuffle_icon_btn")
                    ) {
                        Icon(
                            Icons.Default.Autorenew,
                            contentDescription = "Reshuffle",
                            tint = AccentPureWhite,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Dynamic Score Display
                val score = remember(tasks) {
                    val total = tasks.size
                    if (total > 0) (tasks.count { it.isCompleted } * 100) / total else 0
                }

                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$score",
                            style = MaterialTheme.typography.displayMedium,
                            color = AccentPurple,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Light
                        )
                        Text(
                            text = "%",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentPurple.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 4.dp, start = 1.dp)
                        )
                    }
                    Text(
                        text = Translation.getString("score_label", language).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = SecondaryGray.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Sync and status labels if any
        if (viewModel.syncStatus.isNotEmpty()) {
            Text(
                text = Translation.getString(viewModel.syncStatus, language),
                color = EnergyLow,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DividerColor, RoundedCornerShape(6.dp))
                    .padding(8.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Empty State or List
        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1.of)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        Icons.Outlined.Spa,
                        contentDescription = null,
                        tint = DarkMutedGray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = Translation.getString("empty_timeline", language),
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onAddTaskClick,
                        colors = ButtonDefaults.buttonColors(containerColor = DividerColor),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            Translation.getString("add_task", language),
                            color = AccentPureWhite
                        )
                    }
                }
            }
        } else {
            // Task Timeline Layout
            val firstPendingTaskId = remember(tasks) { tasks.firstOrNull { !it.isCompleted }?.id }
            LazyColumn(
                modifier = Modifier.weight(1.of),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                itemsIndexed(tasks, key = { _, task -> task.id }) { index, task ->
                    val isFirstPending = task.id == firstPendingTaskId
                    TimelineTaskRow(
                        task = task,
                        isFirstPending = isFirstPending,
                        isLast = index == tasks.lastIndex,
                        onToggle = { viewModel.toggleTaskCompletion(task) },
                        onFocus = { viewModel.startFocusMode(task) },
                        onDelete = { viewModel.deleteTask(task) },
                        language = language
                    )
                }
            }
        }
    }
}

@Composable
fun TimelineTaskRow(
    task: Task,
    isFirstPending: Boolean = false,
    isLast: Boolean = false,
    onToggle: () -> Unit,
    onFocus: () -> Unit,
    onDelete: () -> Unit,
    language: AppLanguage
) {
    val energyColor = when (task.energyTag) {
        "LOW" -> EnergyLow
        "HIGH" -> EnergyHigh
        else -> EnergyMedium
    }

    var showMenu by remember { mutableStateOf(false) }

    val (cardContainerColor, cardBorderColor, cardShape, cardOpacity) = when {
        task.isCompleted -> Quadruple(CardNormal, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp), 0.5f)
        isFirstPending -> Quadruple(CardActive, AccentPurple.copy(alpha = 0.3f), RoundedCornerShape(24.dp), 1.0f)
        else -> Quadruple(CardFuture, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp), 1.0f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Left geometric timeline connector axis
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Circle Box Indicator
            Box(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .testTag("timeline_dot_${task.id}"),
                contentAlignment = Alignment.Center
            ) {
                if (task.isCompleted) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .border(2.dp, Emerald500.copy(alpha = 0.5f), CircleShape)
                            .background(Emerald500.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Emerald400, CircleShape)
                        )
                    }
                } else if (isFirstPending) {
                    val infiniteTransition = rememberInfiniteTransition()
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 0.85f,
                        targetValue = 1.15f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        )
                    )
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.5f,
                        targetValue = 1.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        )
                    )

                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .border(2.dp, AccentPurple.copy(alpha = pulseAlpha), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(AccentPurple, CircleShape)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .border(1.dp, Slate700, CircleShape)
                    )
                }
            }

            // Connector Line down to next element
            if (!isLast) {
                Spacer(
                    modifier = Modifier
                        .weight(1f)
                        .width(if (isFirstPending) 2.dp else 1.dp)
                        .background(
                            if (task.isCompleted) {
                                Brush.verticalGradient(
                                    listOf(Emerald500.copy(alpha = 0.5f), Slate800)
                                )
                            } else if (isFirstPending) {
                                Brush.verticalGradient(
                                    listOf(AccentPurple.copy(alpha = 0.5f), Slate800)
                                )
                            } else {
                                Brush.verticalGradient(
                                    listOf(Slate800, Slate800)
                                )
                            }
                        )
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        // Card content on the right
        Card(
            modifier = Modifier
                .weight(1f)
                .alpha(cardOpacity)
                .padding(bottom = 12.dp)
                .clickable { showMenu = !showMenu }
                .testTag("task_card_${task.id}"),
            colors = CardDefaults.cardColors(containerColor = cardContainerColor),
            shape = cardShape,
            border = androidx.compose.foundation.BorderStroke(1.dp, cardBorderColor)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Customized Circle Checkbox
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .border(1.5.dp, if (task.isCompleted) CompletedGreen else SecondaryGray, CircleShape)
                            .background(if (task.isCompleted) CompletedGreen.copy(alpha = 0.15f) else Color.Transparent, CircleShape)
                            .clickable { onToggle() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (task.isCompleted) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Completed",
                                tint = CompletedGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Title and Time Info
                    Column(modifier = Modifier.weight(1.of)) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = if (task.isCompleted) SecondaryGray else AccentPureWhite,
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = SecondaryGray,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = task.time,
                                style = MaterialTheme.typography.labelSmall,
                                color = SecondaryGray
                            )
                            if (task.isReshuffled) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .background(DividerColor, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = Translation.getString("reshuffled_tag", language),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        color = EnergyMedium
                                    )
                                }
                            }
                        }
                    }

                    // Energy Tag Badge Indicator
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(energyColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = Translation.getString("energy_${task.energyTag.lowercase()}", language),
                            style = MaterialTheme.typography.labelSmall,
                            color = energyColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Expanded Actions menu on card click
                AnimatedVisibility(visible = showMenu) {
                    Column {
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = DividerColor, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Focus mode action
                            Row(
                                modifier = Modifier
                                    .clickable {
                                        showMenu = false
                                        onFocus()
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.FilterCenterFocus,
                                    contentDescription = "Focus",
                                    tint = AccentPureWhite,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = Translation.getString("focus_mode_title", language),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AccentPureWhite
                                )
                            }

                            // Delete button
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = EnergyHigh.copy(alpha = 0.8f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Simple Quadruple helper data class for state variables
data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

// ---------------- CALENDAR VIEW ----------------

@Composable
fun CalendarScreen(viewModel: AppViewModel) {
    val language = viewModel.currentLanguage
    val selectedDate by viewModel.currentDate.collectAsState()
    val allTasksList by viewModel.allTasks.collectAsState()

    // Create a 7-day strip centered around today
    val dates = remember {
        val list = mutableListOf<String>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -3) // Start 3 days ago
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        for (i in 0..6) {
            list.add(sdf.format(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp)
    ) {
        Text(
            text = Translation.getString("calendar", language),
            style = MaterialTheme.typography.displayMedium,
            color = AccentPureWhite,
            fontWeight = FontWeight.Light
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Calendar Date Slider Strip
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            items(dates) { dateStr ->
                val isSelected = dateStr == selectedDate
                val parsedDate = remember(dateStr) {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    sdf.parse(dateStr) ?: Date()
                }
                val dayOfWeek = remember(parsedDate) {
                    val sdfDay = SimpleDateFormat("EEE", Locale.US)
                    sdfDay.format(parsedDate)
                }
                val dayOfMonth = remember(parsedDate) {
                    val sdfDom = SimpleDateFormat("dd", Locale.US)
                    sdfDom.format(parsedDate)
                }

                // Count pending tasks for this date
                val pendingCount = remember(allTasksList, dateStr) {
                    allTasksList.count { it.date == dateStr && !it.isCompleted }
                }

                Column(
                    modifier = Modifier
                        .width(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) AccentPureWhite else DividerColor)
                        .clickable { viewModel.setDate(dateStr) }
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = dayOfWeek.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) PureBlack else SecondaryGray,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dayOfMonth,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isSelected) PureBlack else AccentPureWhite,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    if (pendingCount > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .background(if (isSelected) PureBlack else EnergyHigh, CircleShape)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Selected Date Tasks Timeline
        Text(
            text = formatDateForHeader(selectedDate, language),
            style = MaterialTheme.typography.titleLarge,
            color = AccentPureWhite
        )
        Spacer(modifier = Modifier.height(16.dp))

        val tasksForSelectedDate = remember(allTasksList, selectedDate) {
            allTasksList.filter { it.date == selectedDate }.sortedBy { it.time }
        }

        if (tasksForSelectedDate.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1.of)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = Translation.getString("empty_timeline", language),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryGray,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            val firstPendingTaskId = remember(tasksForSelectedDate) { tasksForSelectedDate.firstOrNull { !it.isCompleted }?.id }
            LazyColumn(
                modifier = Modifier.weight(1.of),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                itemsIndexed(tasksForSelectedDate, key = { _, task -> task.id }) { index, task ->
                    val isFirstPending = task.id == firstPendingTaskId
                    TimelineTaskRow(
                        task = task,
                        isFirstPending = isFirstPending,
                        isLast = index == tasksForSelectedDate.lastIndex,
                        onToggle = { viewModel.toggleTaskCompletion(task) },
                        onFocus = { viewModel.startFocusMode(task) },
                        onDelete = { viewModel.deleteTask(task) },
                        language = language
                    )
                }
            }
        }
    }
}

// ---------------- ANALYTICS SCREEN ----------------

@Composable
fun AnalyticsScreen(viewModel: AppViewModel) {
    val language = viewModel.currentLanguage
    val tasksList by viewModel.allTasks.collectAsState()

    val stats = remember(tasksList) {
        AnalyticsHelper.calculateAnalytics(tasksList)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text(
                text = Translation.getString("analytics", language),
                style = MaterialTheme.typography.displayMedium,
                color = AccentPureWhite,
                fontWeight = FontWeight.Light
            )
        }

        // DAILY METRICS SUMMARY CARD
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = Translation.getString("daily_label", language),
                        style = MaterialTheme.typography.titleLarge,
                        color = AccentPureWhite
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Productivity score large dial / text
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = Translation.getString("productivity_score", language),
                                style = MaterialTheme.typography.labelSmall,
                                color = SecondaryGray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${stats.productivityScore}%",
                                style = MaterialTheme.typography.displayMedium,
                                color = AccentPureWhite,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Circular simple Indicator
                        Box(
                            modifier = Modifier.size(60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                progress = stats.productivityScore / 100f,
                                strokeWidth = 5.dp,
                                color = AccentPureWhite,
                                trackColor = DividerColor,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Divider(color = DividerColor, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Triple stats column layout
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = Translation.getString("completed", language),
                                style = MaterialTheme.typography.labelSmall,
                                color = SecondaryGray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${stats.completedCount}",
                                style = MaterialTheme.typography.titleLarge,
                                color = EnergyLow,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = Translation.getString("missed", language),
                                style = MaterialTheme.typography.labelSmall,
                                color = SecondaryGray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${stats.missedCount}",
                                style = MaterialTheme.typography.titleLarge,
                                color = EnergyHigh,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = Translation.getString("pending", language),
                                style = MaterialTheme.typography.labelSmall,
                                color = SecondaryGray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${stats.pendingCount}",
                                style = MaterialTheme.typography.titleLarge,
                                color = EnergyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // WEEKLY DASHBOARD CARD WITH CANVAS LINE
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = Translation.getString("weekly_label", language),
                        style = MaterialTheme.typography.titleLarge,
                        color = AccentPureWhite
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = Translation.getString("weekly_completion", language),
                                style = MaterialTheme.typography.labelSmall,
                                color = SecondaryGray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${stats.weeklyCompletionRate}%",
                                style = MaterialTheme.typography.titleMedium,
                                color = AccentPureWhite,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = Translation.getString("most_productive", language),
                                style = MaterialTheme.typography.labelSmall,
                                color = SecondaryGray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stats.mostProductiveDay,
                                style = MaterialTheme.typography.titleMedium,
                                color = EnergyLow,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = Translation.getString("consistency", language),
                        style = MaterialTheme.typography.labelSmall,
                        color = SecondaryGray
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Minimal custom-drawn consistency trend graph on Canvas
                    ConsistencyLineChart(trend = stats.weeklyTrend)
                }
            }
        }

        // MONTHLY DASHBOARD CARD
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = Translation.getString("monthly_title", language),
                            style = MaterialTheme.typography.titleLarge,
                            color = AccentPureWhite
                        )

                        // performance label
                        val labelKey = when (stats.monthlyPerformanceLabel) {
                            "Disciplined" -> "monthly_disciplined"
                            "Focused" -> "monthly_focused"
                            else -> "monthly_irregular"
                        }
                        Box(
                            modifier = Modifier
                                .background(DividerColor, RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = Translation.getString(labelKey, language),
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentPureWhite,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = DividerColor, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = Translation.getString("behavior_pattern", language),
                        style = MaterialTheme.typography.labelSmall,
                        color = SecondaryGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = EnergyLow,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (stats.completedCount > 0) 
                                Translation.getString("pattern_desc_high", language)
                            else 
                                Translation.getString("pattern_desc_med", language),
                            style = MaterialTheme.typography.bodyMedium,
                            color = PrimarySilver
                        )
                    }
                }
            }
        }

        // LIFE HIGHLIGHTS
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = Translation.getString("life_highlights", language),
                        style = MaterialTheme.typography.titleLarge,
                        color = AccentPureWhite
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = EnergyMedium,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = Translation.getString("longest_streak", language),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SecondaryGray
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${stats.longestStreak} ${Translation.getString("days", language)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = AccentPureWhite,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.FilterCenterFocus,
                                contentDescription = null,
                                tint = AccentPureWhite,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = Translation.getString("highest_focus", language),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SecondaryGray
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${stats.highestFocusDurationMinutes} min",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = AccentPureWhite,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ConsistencyLineChart(trend: List<Int>) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        val width = size.width
        val height = size.height
        val maxVal = (trend.maxOrNull() ?: 1).coerceAtLeast(1)
        
        val points = trend.mapIndexed { index, value ->
            val x = (width / (trend.size - 1)) * index
            val y = height - ((value.toFloat() / maxVal) * (height - 20f)) - 10f
            Offset(x, y)
        }

        val path = Path().apply {
            if (points.isNotEmpty()) {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    lineTo(points[i].x, points[i].y)
                }
            }
        }

        // Draw background horizontal faint helper line
        drawLine(
            color = DividerColor,
            start = Offset(0f, height / 2),
            end = Offset(width, height / 2),
            strokeWidth = 1f
        )

        // Draw trend line
        drawPath(
            path = path,
            color = AccentPurple,
            style = Stroke(width = 3.dp.toPx())
        )

        // Draw dots at points
        points.forEach { point ->
            drawCircle(
                color = AccentPurple,
                radius = 4.dp.toPx(),
                center = point
            )
        }
    }
}

// ---------------- SETTINGS SCREEN ----------------

@Composable
fun SettingsScreen(viewModel: AppViewModel) {
    val language = viewModel.currentLanguage

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = Translation.getString("settings", language),
            style = MaterialTheme.typography.displayMedium,
            color = AccentPureWhite,
            fontWeight = FontWeight.Light
        )

        // 1. LANGUAGE SELECTOR
        Column {
            Text(
                text = Translation.getString("language_label", language),
                style = MaterialTheme.typography.titleMedium,
                color = AccentPureWhite
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // English Button
                Button(
                    onClick = { viewModel.setLanguage(AppLanguage.EN) },
                    modifier = Modifier.weight(1.of),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (language == AppLanguage.EN) AccentPureWhite else DividerColor
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = Translation.getString("lang_en", language),
                        color = if (language == AppLanguage.EN) PureBlack else PrimarySilver,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Persian Button
                Button(
                    onClick = { viewModel.setLanguage(AppLanguage.FA) },
                    modifier = Modifier.weight(1.of),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (language == AppLanguage.FA) AccentPureWhite else DividerColor
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = Translation.getString("lang_fa", language),
                        color = if (language == AppLanguage.FA) PureBlack else PrimarySilver,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Divider(color = DividerColor)

        // 2. AUTO-RESHUFFLE TOGGLE
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1.of)) {
                Text(
                    text = Translation.getString("auto_reshuffle_toggle", language),
                    style = MaterialTheme.typography.titleMedium,
                    color = AccentPureWhite
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = Translation.getString("auto_reshuffle_desc", language),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryGray
                )
            }
            Switch(
                checked = viewModel.isAutoReshuffleEnabled,
                onCheckedChange = { viewModel.setAutoReshuffle(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = PureBlack,
                    checkedTrackColor = AccentPureWhite,
                    uncheckedThumbColor = SecondaryGray,
                    uncheckedTrackColor = DividerColor
                )
            )
        }

        Divider(color = DividerColor)

        // 3. NOTIFICATION TONE STYLE
        Column {
            Text(
                text = Translation.getString("notification_style_label", language),
                style = MaterialTheme.typography.titleMedium,
                color = AccentPureWhite
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val styles = listOf("FRIENDLY" to "not_friendly", "MOTIVATIONAL" to "not_motivational", "SERIOUS" to "not_serious")
                styles.forEach { (style, key) ->
                    val isSelected = viewModel.notificationStyle == style
                    val displayLabel = when (style) {
                        "FRIENDLY" -> Translation.getString("notif_friendly", language)
                        "MOTIVATIONAL" -> Translation.getString("notif_motivational", language)
                        else -> Translation.getString("notif_serious", language)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1.of)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) AccentPureWhite else DividerColor)
                            .clickable { viewModel.updateNotificationStyle(style) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = displayLabel,
                            color = if (isSelected) PureBlack else PrimarySilver,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Divider(color = DividerColor)

        // 4. GOOGLE DRIVE BACKUP AND RESTORE (LOCAL/CLOUD SECURE ARCHITECTURE)
        Column {
            Text(
                text = Translation.getString("drive_sync_title", language),
                style = MaterialTheme.typography.titleMedium,
                color = AccentPureWhite
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = Translation.getString("drive_sync_desc", language),
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryGray
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.backupToLocalDrive() },
                    modifier = Modifier.weight(1.of),
                    colors = ButtonDefaults.buttonColors(containerColor = DividerColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = Translation.getString("backup_now", language),
                        color = AccentPureWhite
                    )
                }

                Button(
                    onClick = { viewModel.restoreFromLocalDrive() },
                    modifier = Modifier.weight(1.of),
                    colors = ButtonDefaults.buttonColors(containerColor = DividerColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = Translation.getString("restore_now", language),
                        color = AccentPureWhite
                    )
                }
            }

            if (viewModel.syncStatus.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = Translation.getString(viewModel.syncStatus, language),
                    color = EnergyLow,
                    style = MaterialTheme.typography.labelSmall
                )
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = Translation.getString("sync_status_idle", language),
                    color = SecondaryGray,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

// ---------------- ADD TASK DIALOG WITH NLP PARSING ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(viewModel: AppViewModel, onDismiss: () -> Unit) {
    val language = viewModel.currentLanguage

    var titleText by remember { mutableStateOf("") }
    var smartText by remember { mutableStateOf("") }
    var hourInput by remember { mutableStateOf("09") }
    var minInput by remember { mutableStateOf("00") }
    var energyTag by remember { mutableStateOf("MEDIUM") }
    var isRepeat by remember { mutableStateOf(false) }
    var repeatFrequency by remember { mutableStateOf("NONE") }

    val softwareKeyboardController = LocalSoftwareKeyboardController.current

    // Local suggested text based on active energy level
    val activeSuggestion = when (energyTag) {
        "HIGH" -> Translation.getString("suggestion_text_high", language)
        "LOW" -> Translation.getString("suggestion_text_low", language)
        else -> null
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .border(1.dp, DividerColor, RoundedCornerShape(16.dp))
                .testTag("add_task_dialog_card"),
            colors = CardDefaults.cardColors(containerColor = TrueBlack),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Text(
                    text = Translation.getString("add_task", language),
                    style = MaterialTheme.typography.titleLarge,
                    color = AccentPureWhite,
                    fontWeight = FontWeight.Bold
                )

                // SMART SMART PARSER NLP INPUT
                Column {
                    Text(
                        text = Translation.getString("title_label", language),
                        style = MaterialTheme.typography.labelSmall,
                        color = SecondaryGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = titleText,
                        onValueChange = { titleText = it },
                        placeholder = { Text(Translation.getString("title_placeholder", language), color = DarkMutedGray) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("task_title_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = AccentPureWhite,
                            unfocusedTextColor = AccentPureWhite,
                            focusedBorderColor = AccentPureWhite,
                            unfocusedBorderColor = DividerColor,
                            focusedContainerColor = CardBackground,
                            unfocusedContainerColor = CardBackground
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            softwareKeyboardController?.hide()
                            if (titleText.isNotBlank()) {
                                // Run instant NLP
                                val parsed = GeminiParser.parseLocally(titleText, viewModel.currentDate.value)
                                titleText = parsed.title
                                hourInput = parsed.time.split(":")[0]
                                minInput = parsed.time.split(":")[1]
                                energyTag = parsed.energyTag
                                isRepeat = parsed.isRepeat
                                repeatFrequency = parsed.repeatFrequency
                            }
                        })
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Buttons for Quick Parser triggers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (titleText.isNotBlank()) {
                                    val parsed = GeminiParser.parseLocally(titleText, viewModel.currentDate.value)
                                    titleText = parsed.title
                                    hourInput = parsed.time.split(":")[0]
                                    minInput = parsed.time.split(":")[1]
                                    energyTag = parsed.energyTag
                                    isRepeat = parsed.isRepeat
                                    repeatFrequency = parsed.repeatFrequency
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DividerColor),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.of)
                        ) {
                            Text(Translation.getString("quick_add_btn", language), color = AccentPureWhite, fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                if (titleText.isNotBlank()) {
                                    viewModel.addSmartTask(titleText)
                                    onDismiss()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPureWhite),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text(Translation.getString("smart_parse_btn", language), color = PureBlack, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // TIME PICKER FIELD
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.of)) {
                        Text(
                            text = Translation.getString("time_label", language),
                            style = MaterialTheme.typography.labelSmall,
                            color = SecondaryGray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = hourInput,
                                onValueChange = { if (it.length <= 2) hourInput = it },
                                modifier = Modifier.width(60.dp),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = AccentPureWhite,
                                    unfocusedTextColor = AccentPureWhite,
                                    focusedBorderColor = AccentPureWhite,
                                    unfocusedBorderColor = DividerColor,
                                    focusedContainerColor = CardBackground,
                                    unfocusedContainerColor = CardBackground
                                )
                            )
                            Text(":", color = AccentPureWhite, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = minInput,
                                onValueChange = { if (it.length <= 2) minInput = it },
                                modifier = Modifier.width(60.dp),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = AccentPureWhite,
                                    unfocusedTextColor = AccentPureWhite,
                                    focusedBorderColor = AccentPureWhite,
                                    unfocusedBorderColor = DividerColor,
                                    focusedContainerColor = CardBackground,
                                    unfocusedContainerColor = CardBackground
                                )
                            )
                        }
                    }

                    // Pre-fill Preset Buttons
                    Row(
                        modifier = Modifier.weight(1.of),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("09:00", "14:00", "18:00").forEach { preset ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(DividerColor)
                                    .clickable {
                                        val parts = preset.split(":")
                                        hourInput = parts[0]
                                        minInput = parts[1]
                                    }
                                    .padding(6.dp)
                            ) {
                                Text(preset, color = PrimarySilver, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                // ENERGY LEVEL SELECTOR
                Column {
                    Text(
                        text = Translation.getString("energy_label", language),
                        style = MaterialTheme.typography.labelSmall,
                        color = SecondaryGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val levels = listOf("LOW", "MEDIUM", "HIGH")
                        levels.forEach { level ->
                            val isSelected = energyTag == level
                            val activeColor = when (level) {
                                "LOW" -> EnergyLow
                                "HIGH" -> EnergyHigh
                                else -> EnergyMedium
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1.of)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) activeColor else DividerColor)
                                    .clickable { energyTag = level }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = Translation.getString("energy_${level.lowercase()}", language),
                                    color = if (isSelected) PureBlack else PrimarySilver,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                // AI scheduler helper suggestions
                if (activeSuggestion != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CardBackground)
                            .padding(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = SecondaryGray,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = activeSuggestion,
                                style = MaterialTheme.typography.labelSmall,
                                color = SecondaryGray
                            )
                        }
                    }
                }

                // REPEAT CONFIGURATION SELECTOR
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = Translation.getString("repeat_label", language),
                            style = MaterialTheme.typography.labelSmall,
                            color = SecondaryGray
                        )
                        Switch(
                            checked = isRepeat,
                            onCheckedChange = {
                                isRepeat = it
                                repeatFrequency = if (it) "DAILY" else "NONE"
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PureBlack,
                                checkedTrackColor = AccentPureWhite
                            )
                        )
                    }

                    if (isRepeat) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("DAILY", "WEEKLY").forEach { freq ->
                                val isSelected = repeatFrequency == freq
                                Box(
                                    modifier = Modifier
                                        .weight(1.of)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) AccentPureWhite else DividerColor)
                                        .clickable { repeatFrequency = freq }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = Translation.getString("repeat_${freq.lowercase()}", language),
                                        color = if (isSelected) PureBlack else PrimarySilver,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // FOOTER BUTTONS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = Translation.getString("cancel", language),
                            color = SecondaryGray
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = {
                            if (titleText.isNotBlank()) {
                                // Pad hours and minutes
                                val hh = hourInput.padStart(2, '0')
                                val mm = minInput.padStart(2, '0')
                                viewModel.addTask(
                                    title = titleText,
                                    time = "$hh:$mm",
                                    date = viewModel.currentDate.value,
                                    energyTag = energyTag,
                                    isRepeat = isRepeat,
                                    repeatFrequency = repeatFrequency
                                )
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPureWhite),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("save_task_btn")
                    ) {
                        Text(
                            text = Translation.getString("save", language),
                            color = PureBlack,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ---------------- IMMERSIVE FOCUS MODE SCREEN OVERLAY ----------------

@Composable
fun FocusModeOverlay(viewModel: AppViewModel, task: Task) {
    val language = viewModel.currentLanguage

    val formattedSeconds = remember(viewModel.focusTimerSeconds) {
        val m = viewModel.focusTimerSeconds / 60
        val s = viewModel.focusTimerSeconds % 60
        String.format(Locale.US, "%02d:%02d", m, s)
    }

    // Gentle pulse animation for deep focus state
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .clickable(enabled = false) {}, // absorb clicks to prevent background leaking
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // Calm headers
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = Translation.getString("focus_mode_title", language).uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = SecondaryGray,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = Translation.getString("focus_mode_desc", language),
                    style = MaterialTheme.typography.labelSmall,
                    color = DarkMutedGray
                )
            }

            // Big Isolated Task title
            Text(
                text = task.title,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontSize = 36.sp,
                    lineHeight = 44.sp,
                    textAlign = TextAlign.Center
                ),
                color = AccentPureWhite,
                fontWeight = FontWeight.Light,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // Stopwatch
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .drawBehind {
                        drawCircle(
                            color = AccentPureWhite.copy(alpha = 0.03f),
                            radius = (size.minDimension / 2) * scale
                        )
                        drawCircle(
                            color = AccentPureWhite.copy(alpha = 0.05f),
                            radius = size.minDimension / 2,
                            style = Stroke(width = 1.dp.toPx())
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = formattedSeconds,
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 40.sp),
                    color = AccentPureWhite,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Done / End buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Exit button
                OutlinedButton(
                    onClick = { viewModel.exitFocusMode() },
                    border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SecondaryGray)
                ) {
                    Text(Translation.getString("exit_focus", language))
                }

                // Done button
                Button(
                    onClick = { viewModel.toggleTaskCompletion(task) },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPureWhite),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = PureBlack, modifier = Modifier.size(16.dp))
                        Text(Translation.getString("complete_task", language), color = PureBlack, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ---------------- HELPER UTILS ----------------

private fun formatDateForHeader(dateStr: String, language: AppLanguage): String {
    try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = sdf.parse(dateStr) ?: return dateStr

        if (language == AppLanguage.FA) {
            // Simulated Persian/Solar dates or simple structural representations
            // to avoid bulky external libraries in local tests
            val cal = Calendar.getInstance()
            cal.time = date
            val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
            val monthNames = listOf(
                "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
                "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
            )
            // Solar shift approximation for calendar month
            val monthIndex = (cal.get(Calendar.MONTH) + 9) % 12
            return "$dayOfMonth ${monthNames[monthIndex]}"
        } else {
            val sdfDisplay = SimpleDateFormat("EEEE, MMMM dd", Locale.US)
            return sdfDisplay.format(date)
        }
    } catch (e: Exception) {
        return dateStr
    }
}

// Custom mock/stub of Int extension for fraction formatting safety
private val Int.of: Float
    get() = this.toFloat()

private val TfUnitBorderWidth = 1.dp
