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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.NoteAlt
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import com.orbita.personalagent.data.local.db.NoteEntity
import com.orbita.personalagent.ui.components.EmptyStatePanel
import com.orbita.personalagent.ui.components.KriovynCard
import com.orbita.personalagent.ui.components.ResponsiveCenteredContainer
import com.orbita.personalagent.ui.theme.KriovynBlueAccent
import com.orbita.personalagent.ui.theme.KriovynError
import com.orbita.personalagent.ui.theme.KriovynNavyBlack
import com.orbita.personalagent.ui.theme.KriovynPurpleAccent
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    viewModel: NotesViewModel
) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<NoteEntity?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                title = { Text("Local Notes") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = KriovynBlueAccent,
                contentColor = KriovynNavyBlack
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Create Note")
            }
        }
    ) { innerPadding ->
        ResponsiveCenteredContainer(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::updateQuery,
                placeholder = { Text("Search notes by title, content, or tag...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )

            if (notes.isEmpty()) {
                EmptyStatePanel(
                    title = "No Notes Yet",
                    subtitle = "Tap + to write a local note or ask Kriovyn AI to save one for you.",
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.NoteAlt,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = KriovynBlueAccent
                        )
                    }
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(notes, key = { it.id }) { note ->
                        KriovynCard(
                            accentBorder = if (note.isPinned) KriovynPurpleAccent else MaterialTheme.colorScheme.outline
                        ) {
                            Row(verticalAlignment = Alignment.Top) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = note.title,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = note.content,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val dateText = remember(note.updatedAt) {
                                        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                                            .format(Date(note.updatedAt))
                                    }
                                    Text(
                                        text = "#${note.tag} • $dateText",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = KriovynPurpleAccent
                                    )
                                }
                                IconButton(onClick = { viewModel.togglePin(note) }) {
                                    Icon(
                                        imageVector = Icons.Rounded.PushPin,
                                        contentDescription = if (note.isPinned) "Unpin Note" else "Pin Note",
                                        tint = if (note.isPinned) KriovynPurpleAccent
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { editingNote = note }) {
                                    Icon(Icons.Rounded.Edit, contentDescription = "Edit Note")
                                }
                                IconButton(onClick = { viewModel.deleteNote(note) }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Delete Note", tint = KriovynError)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        NoteEditorDialog(
            dialogTitle = "New Local Note",
            initialTitle = "",
            initialContent = "",
            initialTag = "General",
            onDismiss = { showCreateDialog = false },
            onSave = { title, content, tag ->
                viewModel.saveNote(title, content, tag)
                showCreateDialog = false
            }
        )
    }

    editingNote?.let { target ->
        NoteEditorDialog(
            dialogTitle = "Edit Note",
            initialTitle = target.title,
            initialContent = target.content,
            initialTag = target.tag,
            onDismiss = { editingNote = null },
            onSave = { title, content, tag ->
                viewModel.updateNote(target, title, content, tag, target.isPinned)
                editingNote = null
            }
        )
    }
}

@Composable
private fun NoteEditorDialog(
    dialogTitle: String,
    initialTitle: String,
    initialContent: String,
    initialTag: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var content by remember { mutableStateOf(initialContent) }
    var tag by remember { mutableStateOf(initialTag) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialogTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Content") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tag,
                    onValueChange = { tag = it },
                    label = { Text("Tag") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank() || content.isNotBlank(),
                onClick = { onSave(title, content, tag) }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
