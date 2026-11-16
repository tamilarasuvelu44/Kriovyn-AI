package com.orbita.personalagent.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbita.personalagent.data.local.db.ApprovedMemoryEntity
import com.orbita.personalagent.ui.components.EmptyStatePanel
import com.orbita.personalagent.ui.components.KriovynCard
import com.orbita.personalagent.ui.components.ResponsiveCenteredContainer
import com.orbita.personalagent.ui.theme.KriovynBlueAccent
import com.orbita.personalagent.ui.theme.KriovynError
import com.orbita.personalagent.ui.theme.KriovynNavyBlack
import com.orbita.personalagent.ui.theme.KriovynPurpleAccent
import com.orbita.personalagent.ui.theme.KriovynSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryScreen(
    viewModel: MemoryViewModel
) {
    val memories by viewModel.memories.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var editingMemory by remember { mutableStateOf<ApprovedMemoryEntity?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                title = { Text("Approved Personal Memory") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = KriovynBlueAccent,
                contentColor = KriovynNavyBlack
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Approved Memory")
            }
        }
    ) { innerPadding ->
        ResponsiveCenteredContainer(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    KriovynCard(accentBorder = KriovynSuccess.copy(alpha = 0.6f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.VerifiedUser,
                                contentDescription = null,
                                tint = KriovynSuccess
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Explicit Consent Guarantee",
                                style = MaterialTheme.typography.titleMedium,
                                color = KriovynSuccess
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Kriovyn AI never silently saves personal facts or sensitive data. Only memories you explicitly approve or add here are stored locally, and you can pause, edit, or delete them anytime.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (memories.isEmpty()) {
                    item {
                        EmptyStatePanel(
                            title = "No Approved Memories",
                            subtitle = "Tap + to add a preference, or tell Kriovyn \"Remember that...\" in Chat to review a memory proposal.",
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.Memory,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = KriovynPurpleAccent
                                )
                            }
                        )
                    }
                } else {
                    items(memories, key = { it.id }) { memory ->
                        KriovynCard(
                            accentBorder = if (memory.isActive) KriovynPurpleAccent else MaterialTheme.colorScheme.outline
                        ) {
                            Row(verticalAlignment = Alignment.Top) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${memory.category} • ${memory.keyLabel}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = KriovynPurpleAccent
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = memory.content,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                Switch(
                                    checked = memory.isActive,
                                    onCheckedChange = { viewModel.toggleMemoryActive(memory) }
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(onClick = { editingMemory = memory }) {
                                    Icon(Icons.Rounded.Edit, contentDescription = "Edit Memory")
                                }
                                IconButton(onClick = { viewModel.deleteMemory(memory) }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Delete Memory", tint = KriovynError)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        MemoryEditorDialog(
            dialogTitle = "Add Approved Memory",
            initialCategory = "Preference",
            initialKey = "",
            initialContent = "",
            onDismiss = { showCreateDialog = false },
            onSave = { category, key, content ->
                viewModel.addMemory(category, key, content)
                showCreateDialog = false
            }
        )
    }

    editingMemory?.let { target ->
        MemoryEditorDialog(
            dialogTitle = "Edit Approved Memory",
            initialCategory = target.category,
            initialKey = target.keyLabel,
            initialContent = target.content,
            onDismiss = { editingMemory = null },
            onSave = { category, key, content ->
                viewModel.updateMemory(target, category, key, content)
                editingMemory = null
            }
        )
    }
}

@Composable
private fun MemoryEditorDialog(
    dialogTitle: String,
    initialCategory: String,
    initialKey: String,
    initialContent: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var category by remember { mutableStateOf(initialCategory) }
    var keyLabel by remember { mutableStateOf(initialKey) }
    var content by remember { mutableStateOf(initialContent) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialogTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (e.g. Preference, Work)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = keyLabel,
                    onValueChange = { keyLabel = it },
                    label = { Text("Short label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Memory fact") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = keyLabel.isNotBlank() && content.isNotBlank(),
                onClick = { onSave(category, keyLabel, content) }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
