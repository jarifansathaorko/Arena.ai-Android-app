package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PromptItem
import com.example.ui.theme.ArenaPrimary
import com.example.ui.theme.Dimensions
import com.example.ui.theme.getCategoryColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptsScreen(
    prompts: List<PromptItem>,
    categories: List<String>,
    selectedCategory: String,
    searchQuery: String,
    onCategorySelected: (String) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onUsePrompt: (String) -> Unit,
    onToggleFavorite: (PromptItem) -> Unit,
    onAddPrompt: (title: String, category: String, content: String) -> Unit,
    onUpdatePrompt: (id: Long, title: String, category: String, content: String) -> Unit,
    onDuplicatePrompt: (PromptItem) -> Unit,
    onDeletePrompt: (PromptItem) -> Unit,
    onShowSnackbar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var showAddDialog by remember { mutableStateOf(false) }
    var promptToEdit by remember { mutableStateOf<PromptItem?>(null) }
    var promptToDelete by remember { mutableStateOf<PromptItem?>(null) }

    val allCategories = remember(categories) {
        listOf("All", "⭐ Favorites") + categories
    }

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
                    // Header title
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = ArenaPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(Dimensions.spaceSmall))
                            Text(
                                text = "Benchmark Prompts",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(Dimensions.radiusSmall),
                            color = ArenaPrimary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "${prompts.size} items",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ArenaPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(Dimensions.spaceMedium))

                    // Search Input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChanged,
                        placeholder = { Text("Search benchmark prompts...", fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChanged("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(Dimensions.radiusMedium),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("search_prompt_input")
                    )

                    Spacer(modifier = Modifier.height(Dimensions.spaceSmall))

                    // Category chips carousel
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(allCategories) { cat ->
                            val isSelected = selectedCategory == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = { onCategorySelected(cat) },
                                label = {
                                    Text(
                                        text = cat,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ArenaPrimary,
                                    selectedLabelColor = Color.White
                                ),
                                shape = RoundedCornerShape(Dimensions.radiusPill)
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = ArenaPrimary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Prompt", fontWeight = FontWeight.SemiBold) },
                modifier = Modifier.testTag("add_custom_prompt_button")
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (prompts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(Dimensions.space2xl)
                ) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(Dimensions.spaceMedium))
                    Text(
                        text = if (searchQuery.isBlank()) "No prompts in \"$selectedCategory\""
                        else "No prompts matching \"$searchQuery\"",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(Dimensions.spaceSmall))
                    Text(
                        text = "Try adjusting your category filter or search keywords.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (searchQuery.isNotBlank() || selectedCategory != "All") {
                        Spacer(modifier = Modifier.height(Dimensions.spaceStandard))
                        OutlinedButton(
                            onClick = {
                                onSearchQueryChanged("")
                                onCategorySelected("All")
                            }
                        ) {
                            Text("Clear Filters")
                        }
                    }
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
                items(prompts, key = { it.id }) { prompt ->
                    NativePromptCard(
                        prompt = prompt,
                        onSendToArena = {
                            onUsePrompt(prompt.content)
                            onShowSnackbar("Prompt sent to Arena!")
                        },
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(prompt.content))
                            onShowSnackbar("Copied prompt to clipboard")
                        },
                        onDuplicate = {
                            onDuplicatePrompt(prompt)
                            onShowSnackbar("Prompt duplicated")
                        },
                        onToggleFavorite = { onToggleFavorite(prompt) },
                        onEdit = { promptToEdit = prompt },
                        onDelete = { promptToDelete = prompt }
                    )
                }
            }
        }
    }

    // Add Prompt Dialog
    if (showAddDialog) {
        PromptEditorDialog(
            titleText = "Add Benchmark Prompt",
            confirmButtonText = "Save Prompt",
            initialTitle = "",
            initialCategory = "Reasoning",
            initialContent = "",
            categories = categories.ifEmpty { listOf("Reasoning", "Coding", "Math", "Creative", "Factuality") },
            onDismiss = { showAddDialog = false },
            onConfirm = { title, cat, content ->
                onAddPrompt(title, cat, content)
                showAddDialog = false
                onShowSnackbar("Custom prompt saved!")
            }
        )
    }

    // Edit Prompt Dialog
    promptToEdit?.let { target ->
        PromptEditorDialog(
            titleText = "Edit Prompt",
            confirmButtonText = "Update",
            initialTitle = target.title,
            initialCategory = target.category,
            initialContent = target.content,
            categories = categories,
            onDismiss = { promptToEdit = null },
            onConfirm = { title, cat, content ->
                onUpdatePrompt(target.id, title, cat, content)
                promptToEdit = null
                onShowSnackbar("Prompt updated!")
            }
        )
    }

    // Delete Confirmation Dialog
    promptToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { promptToDelete = null },
            title = { Text("Delete Prompt?") },
            text = { Text("Are you sure you want to delete \"${target.title}\"? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeletePrompt(target)
                        promptToDelete = null
                        onShowSnackbar("Prompt deleted")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { promptToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun NativePromptCard(
    prompt: PromptItem,
    onSendToArena: () -> Unit,
    onCopy: () -> Unit,
    onDuplicate: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val categoryColor = getCategoryColor(prompt.category)
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(Dimensions.radiusCard),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(Dimensions.spaceStandard)) {
            // Card Top Row: Category tag, Title, Favorite & Menu
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
                        color = categoryColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(end = Dimensions.spaceSmall)
                    ) {
                        Text(
                            text = prompt.category,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = categoryColor,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = prompt.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Favorite Star
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(Dimensions.minTouchTarget)
                ) {
                    Icon(
                        imageVector = if (prompt.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = if (prompt.isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (prompt.isFavorite) Color(0xFFFBBF24) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimensions.spaceSmall))

            // Prompt content with clickable expansion
            Surface(
                onClick = { isExpanded = !isExpanded },
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = prompt.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(Dimensions.spaceMedium))

            // Action Buttons Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Secondary actions: Copy, Duplicate, Edit, Delete
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "Copy prompt",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDuplicate,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ControlPointDuplicate,
                            contentDescription = "Duplicate prompt",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (prompt.isCustom) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Edit prompt",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Delete prompt",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Primary CTA: Send to Arena
                Button(
                    onClick = onSendToArena,
                    shape = RoundedCornerShape(Dimensions.radiusSmall),
                    colors = ButtonDefaults.buttonColors(containerColor = ArenaPrimary),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("use_prompt_button_${prompt.id}")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(Dimensions.spaceSmall))
                    Text("Send to Arena", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun PromptEditorDialog(
    titleText: String,
    confirmButtonText: String,
    initialTitle: String,
    initialCategory: String,
    initialContent: String,
    categories: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (title: String, category: String, content: String) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var category by remember { mutableStateOf(initialCategory) }
    var content by remember { mutableStateOf(initialContent) }

    val canSave = title.isNotBlank() && content.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titleText, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Prompt Title") },
                    placeholder = { Text("e.g. Strawberry Test, Python Cache") },
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
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Prompt Content") },
                    placeholder = { Text("Type or paste benchmark prompt here...") },
                    minLines = 4,
                    maxLines = 8,
                    supportingText = {
                        Text("${content.length} characters")
                    },
                    shape = RoundedCornerShape(Dimensions.radiusSmall),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (canSave) {
                        onConfirm(title.trim(), category, content.trim())
                    }
                },
                enabled = canSave,
                colors = ButtonDefaults.buttonColors(containerColor = ArenaPrimary)
            ) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
