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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbita.personalagent.agent.ToolApprovalRequest
import com.orbita.personalagent.agent.ToolAuditEntry
import com.orbita.personalagent.ui.components.EmptyStatePanel
import com.orbita.personalagent.ui.components.KriovynCard
import com.orbita.personalagent.ui.components.ResponsiveCenteredContainer
import com.orbita.personalagent.ui.theme.KriovynBlueAccent
import com.orbita.personalagent.ui.theme.KriovynError
import com.orbita.personalagent.ui.theme.KriovynNavyBlack
import com.orbita.personalagent.ui.theme.KriovynPurpleAccent
import com.orbita.personalagent.ui.theme.KriovynSuccess
import com.orbita.personalagent.ui.theme.KriovynWarning
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolApprovalScreen(
    viewModel: ApprovalViewModel
) {
    val pending by viewModel.pendingRequests.collectAsStateWithLifecycle()
    val auditLog by viewModel.auditLog.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                title = { Text("Tool Approval & Safety Log") },
                actions = {
                    if (auditLog.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearAuditLog() }) {
                            Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear Audit Log")
                        }
                    }
                }
            )
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
                    KriovynCard(accentBorder = KriovynBlueAccent.copy(alpha = 0.5f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Security, contentDescription = null, tint = KriovynBlueAccent)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Controlled Android Tool System",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "External actions (Web Search, Maps, Dialer, SMS Draft, Email Draft, Calendar, Alarm, Share) use Android intents, enforce argument validation + 10s timeouts, and never execute without your explicit approval.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item {
                    Text(
                        text = "Pending Approvals (${pending.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (pending.isNotEmpty()) KriovynWarning else MaterialTheme.colorScheme.onBackground
                    )
                }

                if (pending.isEmpty()) {
                    item {
                        EmptyStatePanel(
                            title = "No Pending Tool Approvals",
                            subtitle = "When Kriovyn AI prepares an external Android action, it will pause here for you to inspect and approve or reject.",
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.VerifiedUser,
                                    contentDescription = null,
                                    modifier = Modifier.size(44.dp),
                                    tint = KriovynSuccess
                                )
                            }
                        )
                    }
                } else {
                    items(pending, key = { it.requestId }) { req ->
                        PendingApprovalCard(
                            request = req,
                            onApprove = { viewModel.approve(req.requestId) },
                            onReject = { viewModel.reject(req.requestId) }
                        )
                    }
                }

                if (auditLog.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Recent Tool Execution Audit Log",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    items(auditLog, key = { it.id }) { entry ->
                        AuditEntryCard(entry = entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingApprovalCard(
    request: ToolApprovalRequest,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    KriovynCard(accentBorder = KriovynWarning) {
        Text(
            text = request.step.toolName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = KriovynWarning
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = request.step.riskLevel.label,
            style = MaterialTheme.typography.labelMedium,
            color = KriovynPurpleAccent
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = request.step.rationale,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "System intent: ${request.targetActionSummary}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (request.step.arguments.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Validated Arguments:",
                style = MaterialTheme.typography.labelMedium,
                color = KriovynBlueAccent
            )
            request.step.arguments.forEach { (k, v) ->
                Text(
                    text = "• $k: $v",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onApprove,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = KriovynSuccess,
                    contentColor = KriovynNavyBlack
                )
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Approve", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onReject,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = null,
                    tint = KriovynError,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reject", color = KriovynError)
            }
        }
    }
}

@Composable
private fun AuditEntryCard(entry: ToolAuditEntry) {
    val badgeColor = when (entry.status) {
        "EXECUTED" -> KriovynSuccess
        "REJECTED", "CANCELLED" -> KriovynWarning
        else -> KriovynError
    }
    val timeText = remember(entry.timestamp) {
        DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(entry.timestamp))
    }

    KriovynCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = entry.toolName,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "${entry.status} • $timeText",
                style = MaterialTheme.typography.labelMedium,
                color = badgeColor
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = entry.detail,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
