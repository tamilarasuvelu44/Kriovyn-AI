package com.orbita.personalagent.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.orbita.personalagent.ui.components.KriovynCard
import com.orbita.personalagent.ui.components.KriovynOrbGraphic
import com.orbita.personalagent.ui.components.ResponsiveCenteredContainer
import com.orbita.personalagent.ui.theme.KriovynBlueAccent
import com.orbita.personalagent.ui.theme.KriovynNavyBlack
import com.orbita.personalagent.ui.theme.KriovynNavySurface
import com.orbita.personalagent.ui.theme.KriovynPurpleAccent
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onboardingCompleted: Boolean,
    onNavigateToWelcome: () -> Unit,
    onNavigateToChat: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(onboardingCompleted) {
        visible = true
        delay(1200L)
        if (onboardingCompleted) onNavigateToChat() else onNavigateToWelcome()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(KriovynNavyBlack, KriovynNavySurface, KriovynNavyBlack)
                )
            )
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(visible = visible) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                KriovynOrbGraphic(size = 124.dp, isAnimating = true)
                Spacer(modifier = Modifier.height(22.dp))
                Text(
                    text = "KRIOVYN AI",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Controlled Personal Agent • Local First",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        ResponsiveCenteredContainer(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                KriovynOrbGraphic(size = 96.dp, isAnimating = true)
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Welcome to Kriovyn AI",
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your privacy-respecting Android personal agent with human-in-the-loop tool safety.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(28.dp))

            WelcomeFeatureCard(
                icon = Icons.Rounded.Shield,
                title = "Approval-Gated Android Tools",
                description = "Kriovyn uses a strict 12-tool allowlist. Calls, SMS drafts, emails, maps, web searches, calendar forms, and alarms always require your explicit approval."
            )
            Spacer(modifier = Modifier.height(12.dp))
            WelcomeFeatureCard(
                icon = Icons.Rounded.Memory,
                title = "Zero Silent Memory Storage",
                description = "Kriovyn never secretly stores personal facts. Every memory suggestion appears as a confirmation card you can approve, edit, or delete."
            )
            Spacer(modifier = Modifier.height(12.dp))
            WelcomeFeatureCard(
                icon = Icons.Rounded.Lock,
                title = "Offline Demo Mode & Local Vault",
                description = "Phase 1 runs out of the box with an offline Demo AI engine and local Room storage—no API keys embedded in the APK."
            )
            Spacer(modifier = Modifier.height(12.dp))
            WelcomeFeatureCard(
                icon = Icons.Rounded.VerifiedUser,
                title = "No Hidden Background Actions",
                description = "SMS, emails, calls, payments, and protected settings are never triggered automatically."
            )

            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = onGetStarted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = KriovynBlueAccent,
                    contentColor = KriovynNavyBlack
                )
            ) {
                Text(
                    text = "Launch Kriovyn AI",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun WelcomeFeatureCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    KriovynCard {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = KriovynPurpleAccent,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
