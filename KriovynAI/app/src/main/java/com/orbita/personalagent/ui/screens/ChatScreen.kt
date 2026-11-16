package com.orbita.personalagent.ui.screens

import android.content.ClipData
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbita.personalagent.agent.AgentState
import com.orbita.personalagent.ai.MemoryCandidate
import com.orbita.personalagent.data.local.db.ChatMessageEntity
import com.orbita.personalagent.data.local.db.MessageRole
import com.orbita.personalagent.data.preferences.AiBackendMode
import com.orbita.personalagent.ui.components.ConnectionStatusBadge
import com.orbita.personalagent.ui.components.KriovynCard
import com.orbita.personalagent.ui.components.KriovynOrbGraphic
import com.orbita.personalagent.ui.theme.KriovynBlueAccent
import com.orbita.personalagent.ui.theme.KriovynError
import com.orbita.personalagent.ui.theme.KriovynNavyBlack
import com.orbita.personalagent.ui.theme.KriovynNavyCard
import com.orbita.personalagent.ui.theme.KriovynNavyElevated
import com.orbita.personalagent.ui.theme.KriovynOutline
import com.orbita.personalagent.ui.theme.KriovynPurpleAccent
import com.orbita.personalagent.ui.theme.KriovynWarning
import kotlinx.coroutines.launch

private val SuggestedPrompts = listOf(
    "Create a task to review weekly goals",
    "Save a note about Kriovyn Phase 1 architecture",
    "Show my pending tasks",
    "Set alarm for 7:30 am",
    "Search the web for Jetpack Compose best practices",
    "Remember that I prefer morning deep work sessions"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenConnectionStatus: () -> Unit,
    onOpenToolApproval: () -> Unit
) {
    val session by viewModel.activeSession.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val agentState by viewModel.agentState.collectAsStateWithLifecycle()
    val pendingApprovals by viewModel.pendingApprovals.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val inputText by viewModel.inputText.collectAsStateWithLifecycle()
    val memoryCandidates by viewModel.memoryCandidates.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    val clipboard = LocalClipboard.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val isBusy = agentState is AgentState.Planning ||
        agentState is AgentState.ExecutingTool ||
        agentState is AgentState.Synthesizing ||
        agentState is AgentState.AwaitingApproval

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                title = {
                    Column {
                        Text(
                            text = session?.title ?: "Kriovyn AI",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        ConnectionStatusBadge(
                            isDemoMode = preferences.aiBackendMode == AiBackendMode.DEMO_OFFLINE,
                            onClick = onOpenConnectionStatus
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.startNewConversation() }) {
                        Icon(Icons.Rounded.Add, contentDescription = "New Conversation")
                    }
                    IconButton(onClick = onOpenHistory) {
                        Icon(Icons.Rounded.History, contentDescription = "Conversation History")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            AnimatedVisibility(visible = pendingApprovals.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .clickable(onClick = onOpenToolApproval),
                    shape = RoundedCornerShape(14.dp),
                    color = KriovynWarning.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, KriovynWarning)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Security,
                            contentDescription = null,
                            tint = KriovynWarning
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Approval Required (${pendingApprovals.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = KriovynWarning
                            )
                            Text(
                                text = "Tap to review & approve ${pendingApprovals.firstOrNull()?.step?.toolName ?: "action"}.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "Review",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = KriovynWarning
                        )
                    }
                }
            }

            if (messages.isEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(12.dp))
                            KriovynOrbGraphic(size = 84.dp, isAnimating = true)
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "How can I assist you?",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (preferences.aiBackendMode == AiBackendMode.DEMO_OFFLINE) {
                                    "Offline Demo Mode is active. External Android actions always ask for approval first."
                                } else {
                                    "Connected to your configured secure backend adapter."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    item {
                        Text(
                            text = "Suggested Prompts",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SuggestedPrompts.forEach { prompt ->
                                AssistChip(
                                    onClick = { viewModel.sendMessage(prompt) },
                                    label = { Text(prompt) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = KriovynNavyCard,
                                        labelColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    border = BorderStroke(1.dp, KriovynOutline)
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        ChatBubbleItem(
                            message = msg,
                            onCopy = {
                                scope.launch {
                                    val entry = ClipData.newPlainText("Kriovyn AI", msg.content).toClipEntry()
                                    clipboard.setClipEntry(entry)
                                    snackbarHostState.showSnackbar("Copied to clipboard")
                                }
                            },
                            onRetry = { viewModel.retryLastMessage() }
                        )
                    }

                    items(memoryCandidates, key = { it.id }) { candidate ->
                        MemoryCandidateCard(
                            candidate = candidate,
                            onApprove = {
                                viewModel.approveMemoryCandidate(candidate)
                                scope.launch { snackbarHostState.showSnackbar("Memory saved") }
                            },
                            onDismiss = { viewModel.dismissMemoryCandidate(candidate) }
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isBusy) {
                val statusLabel = when (val s = agentState) {
                    is AgentState.Planning -> "Planning controlled response..."
                    is AgentState.AwaitingApproval -> "Waiting for your approval on ${s.request.step.toolName}..."
                    is AgentState.ExecutingTool -> "Running step ${s.step.stepIndex}/${s.totalSteps}: ${s.step.toolName}..."
                    is AgentState.Synthesizing -> "Finalizing response..."
                    else -> "Processing..."
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = KriovynBlueAccent
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = KriovynBlueAccent
                    )
                }
            }

            val errorState = agentState as? AgentState.Error
            AnimatedVisibility(visible = errorState != null) {
                if (errorState != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(KriovynError.copy(alpha = 0.14f))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Warning, contentDescription = null, tint = KriovynError)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = KriovynError,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        OutlinedButton(onClick = { viewModel.retryLastMessage() }) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Retry")
                        }
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, KriovynOutline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = viewModel::updateInputText,
                        placeholder = { Text("Ask Kriovyn AI...") },
                        modifier = Modifier.weight(1f),
                        maxLines = 4,
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = KriovynBlueAccent,
                            unfocusedBorderColor = KriovynOutline,
                            focusedContainerColor = KriovynNavyCard,
                            unfocusedContainerColor = KriovynNavyCard
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (inputText.isNotBlank() && !isBusy) KriovynBlueAccent
                                else KriovynNavyElevated
                            )
                            .clickable(enabled = inputText.isNotBlank() && !isBusy) {
                                viewModel.sendMessage()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = "Send",
                            tint = if (inputText.isNotBlank() && !isBusy) KriovynNavyBlack
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubbleItem(
    message: ChatMessageEntity,
    onCopy: () -> Unit,
    onRetry: () -> Unit
) {
    val isUser = message.role == MessageRole.USER
    val isError = message.role == MessageRole.ERROR || message.isError
    val bubbleColor = when {
        isUser -> KriovynBlueAccent.copy(alpha = 0.22f)
        isError -> KriovynError.copy(alpha = 0.16f)
        else -> KriovynNavyCard
    }
    val borderColor = when {
        isUser -> KriovynBlueAccent.copy(alpha = 0.55f)
        isError -> KriovynError
        else -> KriovynOutline
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 330.dp),
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 18.dp
            ),
            color = bubbleColor,
            border = BorderStroke(1.dp, borderColor)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!message.toolSummary.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = message.toolSummary,
                        style = MaterialTheme.typography.labelMedium,
                        color = KriovynPurpleAccent
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onCopy)
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = "Copy message",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Copy",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (isError) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onRetry)
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Retry message",
                                modifier = Modifier.size(14.dp),
                                tint = KriovynError
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Retry",
                                style = MaterialTheme.typography.labelMedium,
                                color = KriovynError
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryCandidateCard(
    candidate: MemoryCandidate,
    onApprove: () -> Unit,
    onDismiss: () -> Unit
) {
    KriovynCard(accentBorder = KriovynPurpleAccent) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.Memory,
                contentDescription = null,
                tint = KriovynPurpleAccent
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Approve Memory Suggestion?",
                style = MaterialTheme.typography.titleMedium,
                color = KriovynPurpleAccent
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "${candidate.category} • ${candidate.keyLabel}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = candidate.content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onApprove,
                colors = ButtonDefaults.buttonColors(
                    containerColor = KriovynBlueAccent,
                    contentColor = KriovynNavyBlack
                )
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Approve & Save")
            }
            OutlinedButton(onClick = onDismiss) {
                Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Dismiss")
            }
        }
    }
}
