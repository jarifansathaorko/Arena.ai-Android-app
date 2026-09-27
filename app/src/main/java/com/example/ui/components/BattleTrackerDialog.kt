package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BattleRecord
import com.example.data.model.BattleWinner
import com.example.ui.theme.ArenaDanger
import com.example.ui.theme.ArenaPrimary
import com.example.ui.theme.ArenaSuccess
import com.example.ui.theme.ArenaWarning
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BattleTrackerDialog(
    battles: List<BattleRecord>,
    onDismiss: () -> Unit,
    onLogBattle: (modelA: String, modelB: String, winner: BattleWinner, topic: String, category: String, notes: String) -> Unit,
    onDeleteBattle: (BattleRecord) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddForm by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) } // 0: History, 1: Win Stats

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxHeight(0.88f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Header
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
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Arena Battle Log",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = { showAddForm = !showAddForm },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ArenaPrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (showAddForm) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (showAddForm) "Close" else "Record Battle", fontSize = 12.sp)
                }
            }

            Text(
                text = "Keep track of your model comparisons and personal benchmark scorecard.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )

            // Tabs: History vs Stats
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Battles (${battles.size})", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Model Scorecard", fontWeight = FontWeight.SemiBold) }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (showAddForm) {
                AddBattleForm(
                    onSave = { mA, mB, win, topic, cat, notes ->
                        onLogBattle(mA, mB, win, topic, cat, notes)
                        showAddForm = false
                    },
                    onCancel = { showAddForm = false }
                )
            } else {
                when (selectedTab) {
                    0 -> BattleHistoryView(
                        battles = battles,
                        onDeleteBattle = onDeleteBattle,
                        onClearAll = onClearAll
                    )
                    1 -> ModelStatsView(battles = battles)
                }
            }
        }
    }
}

@Composable
private fun AddBattleForm(
    onSave: (String, String, BattleWinner, String, String, String) -> Unit,
    onCancel: () -> Unit
) {
    var modelA by remember { mutableStateOf("") }
    var modelB by remember { mutableStateOf("") }
    var winner by remember { mutableStateOf(BattleWinner.MODEL_A) }
    var topic by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Reasoning") }
    var notes by remember { mutableStateOf("") }

    val categories = listOf("Reasoning", "Coding", "Math", "Creative", "Factuality", "General")

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("Record Revealed Models & Winner", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = modelA,
                    onValueChange = { modelA = it },
                    label = { Text("Model A (e.g. Claude 3.5)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = modelB,
                    onValueChange = { modelB = it },
                    label = { Text("Model B (e.g. GPT-4o)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text("Who won this battle?", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = winner == BattleWinner.MODEL_A,
                    onClick = { winner = BattleWinner.MODEL_A },
                    label = { Text("Model A Won", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ArenaSuccess, selectedLabelColor = Color.White)
                )
                FilterChip(
                    selected = winner == BattleWinner.MODEL_B,
                    onClick = { winner = BattleWinner.MODEL_B },
                    label = { Text("Model B Won", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ArenaSuccess, selectedLabelColor = Color.White)
                )
                FilterChip(
                    selected = winner == BattleWinner.TIE,
                    onClick = { winner = BattleWinner.TIE },
                    label = { Text("Tie", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ArenaWarning, selectedLabelColor = Color.White)
                )
                FilterChip(
                    selected = winner == BattleWinner.BOTH_BAD,
                    onClick = { winner = BattleWinner.BOTH_BAD },
                    label = { Text("Both Bad", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ArenaDanger, selectedLabelColor = Color.White)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = topic,
                onValueChange = { topic = it },
                label = { Text("Prompt / Topic (e.g. Python Async Lock)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text("Task Category", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                items(categories) { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat, fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Comparison Notes (Optional)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onCancel) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (modelA.isNotBlank() && modelB.isNotBlank()) {
                            onSave(modelA, modelB, winner, topic, category, notes)
                        }
                    },
                    enabled = modelA.isNotBlank() && modelB.isNotBlank()
                ) {
                    Text("Save to Scorecard")
                }
            }
        }
    }
}

@Composable
private fun BattleHistoryView(
    battles: List<BattleRecord>,
    onDeleteBattle: (BattleRecord) -> Unit,
    onClearAll: () -> Unit
) {
    if (battles.isEmpty()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.SportsKabaddi,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No battles recorded yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Vote in Arena, see the revealed models, then tap 'Record Battle' to track them!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            items(battles, key = { it.id }) { battle ->
                BattleItemCard(battle = battle, onDelete = { onDeleteBattle(battle) })
            }
        }
    }
}

@Composable
private fun BattleItemCard(
    battle: BattleRecord,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = ArenaPrimary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = battle.category,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ArenaPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dateFormat.format(Date(battle.timestamp)),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Models Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Model A
                val isAWinner = battle.winner == BattleWinner.MODEL_A
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = battle.modelA,
                        fontWeight = if (isAWinner) FontWeight.Bold else FontWeight.Normal,
                        color = if (isAWinner) ArenaSuccess else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isAWinner) {
                        Text("★ Winner", fontSize = 10.sp, color = ArenaSuccess, fontWeight = FontWeight.Bold)
                    }
                }

                Text(
                    text = "vs",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                // Model B
                val isBWinner = battle.winner == BattleWinner.MODEL_B
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = battle.modelB,
                        fontWeight = if (isBWinner) FontWeight.Bold else FontWeight.Normal,
                        color = if (isBWinner) ArenaSuccess else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isBWinner) {
                        Text("★ Winner", fontSize = 10.sp, color = ArenaSuccess, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (battle.winner == BattleWinner.TIE || battle.winner == BattleWinner.BOTH_BAD) {
                Surface(
                    color = if (battle.winner == BattleWinner.TIE) ArenaWarning.copy(alpha = 0.2f) else ArenaDanger.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = if (battle.winner == BattleWinner.TIE) "Outcome: Tie" else "Outcome: Both were bad",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (battle.winner == BattleWinner.TIE) ArenaWarning else ArenaDanger,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (battle.promptTopic.isNotBlank()) {
                Text(
                    text = "Topic: ${battle.promptTopic}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (battle.notes.isNotBlank()) {
                Text(
                    text = battle.notes,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun ModelStatsView(battles: List<BattleRecord>) {
    val stats = remember(battles) {
        val modelCounts = mutableMapOf<String, Pair<Int, Int>>() // model -> (wins, total)
        for (b in battles) {
            val a = b.modelA.trim()
            val bModel = b.modelB.trim()

            val curA = modelCounts.getOrDefault(a, Pair(0, 0))
            val curB = modelCounts.getOrDefault(bModel, Pair(0, 0))

            val aWon = if (b.winner == BattleWinner.MODEL_A) 1 else 0
            val bWon = if (b.winner == BattleWinner.MODEL_B) 1 else 0

            modelCounts[a] = Pair(curA.first + aWon, curA.second + 1)
            modelCounts[bModel] = Pair(curB.first + bWon, curB.second + 1)
        }

        modelCounts.entries.map { (name, pair) ->
            val winRate = if (pair.second > 0) (pair.first.toFloat() / pair.second.toFloat()) * 100f else 0f
            Triple(name, pair.first, pair.second) to winRate
        }.sortedByDescending { it.second }
    }

    if (stats.isEmpty()) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().fillMaxHeight(0.6f)) {
            Text("No statistics yet. Log battles to build your leaderboard!", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            items(stats) { (info, winRate) ->
                val (name, wins, total) = info
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(12.dp).fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("$wins wins in $total battles", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Surface(
                            color = ArenaPrimary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "%.0f%% Win Rate".format(winRate),
                                fontWeight = FontWeight.Bold,
                                color = ArenaPrimary,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
