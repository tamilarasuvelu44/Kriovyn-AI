package com.orbita.personalagent.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbita.personalagent.data.local.db.ChatSessionEntity
import com.orbita.personalagent.ui.components.EmptyStatePanel
import com.orbita.personalagent.ui.components.KriovynCard
import com.orbita.personalagent.ui.components.ResponsiveCenteredContainer
import com.orbita.personalagent.ui.theme.KriovynBlueAccent
import com.orbita.personalagent.ui.theme.KriovynError
import com.orbita.personalagent.ui.theme.KriovynNavyBlack
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onSelectSession: (Long) -> Unit,
    onBack: () -> Unit
) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()

    var renamingSession by remember { mutableStateOf<ChatSessionEntity?>(null) }
    var renameText by remember { mutableStateOf("") }
    var deletingSession by remember { mutableStateOf<ChatSessionEntity?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Conversation History") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.createNewSession(onSelectSession) },
                containerColor = KriovynBlueAccent,
                contentColor = KriovynNavyBlack
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "New Conversation")
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
                onValueChange = viewModel::updateSearch,
                placeholder = { Text("Search conversations...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )

            if (sessions.isEmpty()) {
                EmptyStatePanel(
                    title = "No Conversations Yet",
                    subtitle = "Start a new chat with Kriovyn AI to build your conversation history.",
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.History,
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
                    items(sessions, key = { it.id }) { session ->
                        KriovynCard(
                            modifier = Modifier.clickable { onSelectSession(session.id) }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = session.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (session.lastMessagePreview.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = session.lastMessagePreview,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val formatted = remember(session.updatedAt) {
                                        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                                            .format(Date(session.updatedAt))
                                    }
                                    Text(
                                        text = "${session.messageCount} messages • $formatted",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = KriovynBlueAccent
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        renamingSession = session
                                        renameText = session.title
                                    }
                                ) {
                                    Icon(Icons.Rounded.Edit, contentDescription = "Rename Conversation")
                                }
                                IconButton(onClick = { deletingSession = session }) {
                                    Icon(
                                        Icons.Rounded.Delete,
                                        contentDescription = "Delete Conversation",
                                        tint = KriovynError
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    renamingSession?.let { target ->
        AlertDialog(
            onDismissRequest = { renamingSession = null },
            title = { Text("Rename Conversation") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Title") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.renameSession(target.id, renameText)
                        renamingSession = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { renamingSession = null }) { Text("Cancel") }
            }
        )
    }

    deletingSession?.let { target ->
        AlertDialog(
            onDismissRequest = { deletingSession = null },
            title = { Text("Delete Conversation?") },
            text = { Text("This permanently deletes \"${target.title}\" and all its messages.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSession(target.id)
                        deletingSession = null
                    }
                ) {
                    Text("Delete", color = KriovynError)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingSession = null }) { Text("Cancel") }
            }
        )
    }
}
