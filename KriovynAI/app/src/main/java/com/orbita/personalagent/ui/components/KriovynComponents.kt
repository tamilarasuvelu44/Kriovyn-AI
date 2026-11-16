package com.orbita.personalagent.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.orbita.personalagent.ui.theme.KriovynBlueAccent
import com.orbita.personalagent.ui.theme.KriovynOutline
import com.orbita.personalagent.ui.theme.KriovynPurpleAccent
import com.orbita.personalagent.ui.theme.KriovynSuccess
import com.orbita.personalagent.ui.theme.KriovynWarning
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ResponsiveCenteredContainer(
    modifier: Modifier = Modifier,
    maxWidth: Dp = 760.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .fillMaxWidth(),
            content = content
        )
    }
}

@Composable
fun KriovynOrbGraphic(
    size: Dp = 96.dp,
    isAnimating: Boolean = true
) {
    val transition = rememberInfiniteTransition(label = "kriovyn_orb")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbital_angle"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "core_pulse"
    )

    val effectiveAngle = if (isAnimating) angle else 45f
    val effectivePulse = if (isAnimating) pulse else 1f

    Canvas(modifier = Modifier.size(size)) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val radius = this.size.minDimension * 0.38f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    KriovynBlueAccent.copy(alpha = 0.35f),
                    KriovynPurpleAccent.copy(alpha = 0.12f),
                    Color.Transparent
                ),
                center = center,
                radius = radius * 1.45f * effectivePulse
            ),
            radius = radius * 1.45f * effectivePulse,
            center = center
        )

        drawCircle(
            brush = Brush.linearGradient(listOf(KriovynBlueAccent, KriovynPurpleAccent)),
            radius = radius,
            center = center,
            style = Stroke(width = 4.dp.toPx())
        )

        val rad = Math.toRadians(effectiveAngle.toDouble())
        val satellite = Offset(
            x = center.x + (radius * cos(rad)).toFloat(),
            y = center.y + (radius * sin(rad)).toFloat()
        )
        drawCircle(
            color = Color(0xFFEAF1FF),
            radius = 5.5.dp.toPx(),
            center = satellite
        )

        drawCircle(
            brush = Brush.linearGradient(listOf(KriovynBlueAccent, KriovynPurpleAccent)),
            radius = radius * 0.34f * effectivePulse,
            center = center
        )
    }
}

@Composable
fun ConnectionStatusBadge(
    isDemoMode: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dotColor = if (isDemoMode) KriovynWarning else KriovynSuccess
    val label = if (isDemoMode) "Demo Mode" else "Connected"

    Surface(
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = CircleShape,
        border = BorderStroke(1.dp, KriovynOutline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun KriovynCard(
    modifier: Modifier = Modifier,
    accentBorder: Color = KriovynOutline,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, accentBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun EmptyStatePanel(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        icon()
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
