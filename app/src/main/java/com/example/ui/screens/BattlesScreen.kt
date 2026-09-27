package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BattleRecord
import com.example.data.model.BattleWinner
import com.example.data.repository.ModelStats
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BattlesScreen(
    battles: List<BattleRecord>,
    modelStats: List<ModelStats>,
    categories: List<String>,
    searchQuery: String,
    selectedCategory: String,
    selectedWinnerFilter: BattleWinner?,
    onSearchQueryChanged: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onWinnerFilterSelected: (BattleWinner?) -> Unit,
    onLogBattle: (modelA: String, modelB: String, winner: BattleWinner, topic: String, category: String, notes: String) -> Unit,
    onDeleteBattle: (BattleRecord) -> Unit,
    onClearAllBattles: () -> Unit,
    onShowSnackbar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: History, 1: Scorecard
    var showRecordDialog by remember { mutableStateOf(false) }
    var battleToDelete by remember { mutableStateOf<BattleRecord?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }

    val allCategories = remember(categories) { listOf("All") + categories }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimensions.spaceStandard, vertical = Dimensions.spaceMedium)
                ) {
                    // Header title & clear button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = ArenaWarning,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(Dimensions.spaceSmall))
                            Text(
                                text = "Battle Tracker",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (battles.isNotEmpty()) {
                            TextButton(
                                onClick = { showClearConfirm = true },
                                modifier = Modifier.testTag("clear_battles_button")
                            ) {
                                Text("Clear all", color = ArenaDanger, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(Dimensions.spaceSmall))

                    // Segmented Tabs: History vs Scorecard
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        divider = {}
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = {
                                Text(
                                    "History (${battles.size})",
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = {
                                Text(
                                    "Model Scorecard (${modelStats.size})",
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }

                    // Search & Filters for History tab
                    if (selectedTab == 0) {
                        Spacer(modifier = Modifier.height(Dimensions.spaceSmall))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChanged,
                            placeholder = { Text("Filter by model, topic, or notes...", fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchQueryChanged("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(Dimensions.radiusMedium),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        )

                        Spacer(modifier = Modifier.height(Dimensions.spaceSmall))

                        // Category & Outcome Filter Carousel
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(allCategories) { cat ->
                                val isSelected = selectedCategory == cat
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onCategorySelected(cat) },
                                    label = { Text(cat, fontSize = 11.sp) },
                                    shape = RoundedCornerShape(Dimensions.radiusPill)
                                )
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showRecordDialog = true },
                containerColor = ArenaPrimary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Record Battle", fontWeight = FontWeight.SemiBold) },
                modifier = Modifier.testTag("record_battle_button")
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        when (selectedTab) {
            0 -> {
                // Battle History View
                if (battles.isEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(Dimensions.space2xl)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = ArenaPrimary.copy(alpha = 0.12f),
                                modifier = Modifier.size(80.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.SportsKabaddi,
                                        contentDescription = null,
                                        tint = ArenaPrimary,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(Dimensions.spaceStandard))
                            Text(
                                text = "No Battles Recorded Yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(Dimensions.spaceSmall))
                            Text(
                                text = "Vote on Arena battles, discover which model was which, and tap \"Record Battle\" to log the matchup!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = Dimensions.spaceStandard)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = Dimensions.spaceStandard,
                            end = Dimensions.spaceStandard,
                            top = Dimensions.spaceMedium,
                            bottom = 88.dp // Space for FAB
                        ),
                        verticalArrangement = Arrangement.spacedBy(Dimensions.spaceMedium),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        items(battles, key = { it.id }) { battle ->
                            NativeBattleCard(
                                battle = battle,
                                onDelete = { battleToDelete = battle }
                            )
                        }
                    }
                }
            }
            1 -> {
                // Model Scorecard View
                if (modelStats.isEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Leaderboard,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(Dimensions.spaceSmall))
                            Text(
                                text = "No Model Statistics Yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Log your battles to see which AI models win most often!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = Dimensions.spaceStandard,
                            end = Dimensions.spaceStandard,
                            top = Dimensions.spaceMedium,
                            bottom = 88.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        items(modelStats.size) { index ->
                            val stat = modelStats[index]
                            ModelScorecardCard(stat = stat, rank = index + 1)
                        }
                    }
                }
            }
        }
    }

    // Record Battle Dialog
    if (showRecordDialog) {
        RecordBattleDialog(
            categories = categories.ifEmpty { listOf("Reasoning", "Coding", "Math", "Creative", "Factuality", "General") },
            onDismiss = { showRecordDialog = false },
            onConfirm = { mA, mB, win, topic, cat, notes ->
                onLogBattle(mA, mB, win, topic, cat, notes)
                showRecordDialog = false
                onShowSnackbar("Battle logged to scorecard!")
            }
        )
    }

    // Delete Battle Dialog
    battleToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { battleToDelete = null },
            title = { Text("Delete Battle Record?") },
            text = { Text("Are you sure you want to remove ${target.modelA} vs ${target.modelB} from your history?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteBattle(target)
                        battleToDelete = null
                        onShowSnackbar("Battle removed")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { battleToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear All Dialog
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear All Battles?") },
            text = { Text("This will permanently delete all ${battles.size} recorded battles and reset your model scorecard. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllBattles()
                        showClearConfirm = false
                        onShowSnackbar("All battles cleared")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ArenaDanger)
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun NativeBattleCard(
    battle: BattleRecord,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    val categoryColor = getCategoryColor(battle.category)

    val isAWinner = battle.winner == BattleWinner.MODEL_A
    val isBWinner = battle.winner == BattleWinner.MODEL_B
    val isTie = battle.winner == BattleWinner.TIE
    val isBothBad = battle.winner == BattleWinner.BOTH_BAD

    Card(
        shape = RoundedCornerShape(Dimensions.radiusCard),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(Dimensions.spaceStandard)) {
            // Header Row: Category badge, timestamp, delete button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = categoryColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = battle.category,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = categoryColor,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dateFormat.format(Date(battle.timestamp)),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Delete battle",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimensions.spaceSmall))

            // Matchup duel row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Model A Box
                Surface(
                    shape = RoundedCornerShape(Dimensions.radiusSmall),
                    color = if (isAWinner) ArenaSuccess.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(Dimensions.spaceSmall)) {
                        Text(
                            text = battle.modelA,
                            fontWeight = if (isAWinner) FontWeight.Bold else FontWeight.Medium,
                            color = if (isAWinner) ArenaSuccess else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isAWinner) {
                            Text("★ Winner", fontSize = 10.sp, color = ArenaSuccess, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Text(
                    text = "VS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                // Model B Box
                Surface(
                    shape = RoundedCornerShape(Dimensions.radiusSmall),
                    color = if (isBWinner) ArenaSuccess.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(Dimensions.spaceSmall)) {
                        Text(
                            text = battle.modelB,
                            fontWeight = if (isBWinner) FontWeight.Bold else FontWeight.Medium,
                            color = if (isBWinner) ArenaSuccess else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isBWinner) {
                            Text("★ Winner", fontSize = 10.sp, color = ArenaSuccess, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Outcome badge for Tie / Both Bad
            if (isTie || isBothBad) {
                Spacer(modifier = Modifier.height(Dimensions.spaceXs))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isTie) ArenaWarning.copy(alpha = 0.15f) else ArenaDanger.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (isTie) "🤝 Outcome: Tie" else "✕ Outcome: Both Bad",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isTie) ArenaWarning else ArenaDanger,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (battle.promptTopic.isNotBlank()) {
                Spacer(modifier = Modifier.height(Dimensions.spaceXs))
                Text(
                    text = "Topic: ${battle.promptTopic}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (battle.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = battle.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun ModelScorecardCard(
    stat: ModelStats,
    rank: Int
) {
    val rankColor = when (rank) {
        1 -> Color(0xFFFBBF24) // Gold
        2 -> Color(0xFF94A3B8) // Silver
        3 -> Color(0xFFCD7F32) // Bronze
        else -> ArenaPrimary.copy(alpha = 0.6f)
    }

    val animatedWinRate by animateFloatAsState(
        targetValue = stat.winRate / 100f,
        label = "winRateProgress"
    )

    Card(
        shape = RoundedCornerShape(Dimensions.radiusCard),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(Dimensions.spaceStandard)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = rankColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "#$rank",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = rankColor
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(Dimensions.spaceMedium))
                    Column {
                        Text(
                            text = stat.modelName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${stat.wins} wins out of ${stat.battles} comparisons",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(Dimensions.radiusSmall),
                    color = ArenaPrimary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "%.0f%% Win Rate".format(stat.winRate),
                        fontWeight = FontWeight.Bold,
                        color = ArenaPrimary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimensions.spaceSmall))

            // Progress bar showing win percentage
            LinearProgressIndicator(
                progress = { animatedWinRate },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = ArenaSuccess,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun RecordBattleDialog(
    categories: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (modelA: String, modelB: String, winner: BattleWinner, topic: String, category: String, notes: String) -> Unit
) {
    var modelA by remember { mutableStateOf("") }
    var modelB by remember { mutableStateOf("") }
    var winner by remember { mutableStateOf(BattleWinner.MODEL_A) }
    var topic by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(categories.firstOrNull() ?: "Reasoning") }
    var notes by remember { mutableStateOf("") }

    val duplicateModels = modelA.isNotBlank() && modelB.isNotBlank() &&
            modelA.trim().equals(modelB.trim(), ignoreCase = true)
    val canSave = modelA.isNotBlank() && modelB.isNotBlank() && !duplicateModels

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Revealed Models", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)
                ) {
                    OutlinedTextField(
                        value = modelA,
                        onValueChange = { modelA = it },
                        label = { Text("Model A") },
                        placeholder = { Text("e.g. Claude 3.5") },
                        singleLine = true,
                        isError = duplicateModels,
                        shape = RoundedCornerShape(Dimensions.radiusSmall),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("battle_model_a_input")
                    )
                    OutlinedTextField(
                        value = modelB,
                        onValueChange = { modelB = it },
                        label = { Text("Model B") },
                        placeholder = { Text("e.g. GPT-4o") },
                        singleLine = true,
                        isError = duplicateModels,
                        shape = RoundedCornerShape(Dimensions.radiusSmall),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("battle_model_b_input")
                    )
                }

                if (duplicateModels) {
                    Text(
                        text = "Model A and Model B must be different.",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }

                Text(
                    text = "Who won this comparison?",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Winner Chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = winner == BattleWinner.MODEL_A,
                        onClick = { winner = BattleWinner.MODEL_A },
                        label = { Text("Model A", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ArenaSuccess,
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = winner == BattleWinner.MODEL_B,
                        onClick = { winner = BattleWinner.MODEL_B },
                        label = { Text("Model B", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ArenaSuccess,
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = winner == BattleWinner.TIE,
                        onClick = { winner = BattleWinner.TIE },
                        label = { Text("Tie", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ArenaWarning,
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = winner == BattleWinner.BOTH_BAD,
                        onClick = { winner = BattleWinner.BOTH_BAD },
                        label = { Text("Both Bad", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ArenaDanger,
                            selectedLabelColor = Color.White
                        )
                    )
                }

                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    label = { Text("Prompt / Topic") },
                    placeholder = { Text("e.g. Python Async, Creative Writing") },
                    singleLine = true,
                    shape = RoundedCornerShape(Dimensions.radiusSmall),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    placeholder = { Text("e.g. Model A gave better code formatting") },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(Dimensions.radiusSmall),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (canSave) {
                        onConfirm(modelA.trim(), modelB.trim(), winner, topic.trim(), category, notes.trim())
                    }
                },
                enabled = canSave,
                colors = ButtonDefaults.buttonColors(containerColor = ArenaPrimary),
                modifier = Modifier.testTag("save_battle_button")
            ) {
                Text("Save Battle")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
