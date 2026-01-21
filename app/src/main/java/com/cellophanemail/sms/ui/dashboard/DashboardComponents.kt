package com.cellophanemail.sms.ui.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cellophanemail.sms.R
import com.cellophanemail.sms.domain.model.RiskLevel

@Composable
fun ScanProgressCard(
    scanState: ScanState,
    onStartScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = scanState.scanProgress,
        label = "scan_progress"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                scanState.isInitialScanCompleted -> MaterialTheme.colorScheme.primaryContainer
                scanState.isScanInProgress -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        scanState.isInitialScanCompleted -> stringResource(R.string.scan_complete)
                        scanState.isScanInProgress -> stringResource(R.string.scanning_messages)
                        else -> stringResource(R.string.initial_scan_required)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when {
                scanState.isScanInProgress -> {
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(
                            R.string.scan_progress,
                            scanState.messagesScanned,
                            scanState.totalMessagesToScan
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                scanState.isInitialScanCompleted -> {
                    Text(
                        text = stringResource(
                            R.string.scan_progress,
                            scanState.messagesScanned,
                            scanState.totalMessagesToScan
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                else -> {
                    Text(
                        text = stringResource(R.string.tap_to_analyze),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onStartScan,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.start_scan))
                    }
                }
            }
        }
    }
}

/**
 * Risk Senders Card - shows senders with toxic patterns that contain important info
 * These are the "toxic + logistics" cases - harmful but you need to see them
 */
@Composable
fun RiskSendersCard(
    riskSenders: List<RiskSenderStats>,
    onSenderClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Header with count
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.risk_senders),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (riskSenders.isNotEmpty()) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.error
                    ) {
                        Text(
                            text = riskSenders.size.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (riskSenders.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_risks_detected),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                // Show top risk senders with signal ratio
                riskSenders.take(5).forEach { sender ->
                    RiskSenderRow(
                        sender = sender,
                        onClick = { onSenderClick(sender.senderId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RiskSenderRow(
    sender: RiskSenderStats,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val riskColor = when (sender.riskLevel) {
        RiskLevel.HIGH -> MaterialTheme.colorScheme.error
        RiskLevel.MEDIUM -> MaterialTheme.colorScheme.tertiary
        RiskLevel.LOW -> MaterialTheme.colorScheme.secondary
        RiskLevel.NONE -> MaterialTheme.colorScheme.outline
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        color = riskColor.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Compact avatar
            Surface(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape),
                color = riskColor.copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = sender.displayName.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = riskColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Name and pattern info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sender.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Show toxic+signal count prominently
                    if (sender.toxicLogisticsCount > 0) {
                        Text(
                            text = stringResource(R.string.signal_toxic_count, sender.toxicLogisticsCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFF6B6B),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    // Show dominant pattern
                    sender.dominantHorseman?.let { horseman ->
                        Text(
                            text = horseman,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Total filtered badge
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = sender.filteredCount.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = riskColor
                )
                Text(
                    text = stringResource(R.string.filtered_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Compact 2x2 Message Matrix Card - maximizes use of width/height
 *
 *              │ Signal      │ Noise
 * ─────────────┼─────────────┼─────────────
 * Toxic        │ ⚠️ Review   │ 🚫 Filtered
 * ─────────────┼─────────────┼─────────────
 * Safe         │ ✅ Clear    │ 😐 Low Pri
 */
@Composable
fun MessageMatrixCard(
    matrix: MessageMatrix,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Compact header row with column labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Empty corner cell
                Box(modifier = Modifier.size(32.dp))
                // Column headers
                Text(
                    text = stringResource(R.string.matrix_actionable),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.matrix_noise),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Toxic Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rotated row label
                Box(
                    modifier = Modifier.size(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⚠",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                MatrixCell(
                    count = matrix.toxicLogistics,
                    label = stringResource(R.string.toxic_logistics),
                    color = Color(0xFFFF6B6B), // Warm red - needs attention
                    modifier = Modifier.weight(1f)
                )
                MatrixCell(
                    count = matrix.toxicNoise,
                    label = stringResource(R.string.toxic_noise),
                    color = Color(0xFFE57373), // Lighter red - filtered
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Safe Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✓",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF4CAF50)
                    )
                }
                MatrixCell(
                    count = matrix.safeLogistics,
                    label = stringResource(R.string.safe_logistics),
                    color = Color(0xFF4CAF50), // Green - good
                    modifier = Modifier.weight(1f)
                )
                MatrixCell(
                    count = matrix.safeNoise,
                    label = stringResource(R.string.safe_noise),
                    color = Color(0xFF90A4AE), // Blue grey - low priority
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MatrixCell(
    count: Int,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TopRiskSendersCard(
    riskSenders: List<RiskSenderStats>,
    onSenderClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.top_risk_relationships),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (riskSenders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_risks_detected),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                riskSenders.forEachIndexed { index, sender ->
                    RiskSenderItem(
                        sender = sender,
                        onClick = { onSenderClick(sender.senderId) }
                    )
                    if (index < riskSenders.lastIndex) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun RiskSenderItem(
    sender: RiskSenderStats,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val riskColor = when (sender.riskLevel) {
        RiskLevel.HIGH -> MaterialTheme.colorScheme.error
        RiskLevel.MEDIUM -> MaterialTheme.colorScheme.tertiary
        RiskLevel.LOW -> MaterialTheme.colorScheme.secondary
        RiskLevel.NONE -> MaterialTheme.colorScheme.outline
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = riskColor.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with risk indicator
            Box {
                Surface(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = sender.displayName.firstOrNull()?.uppercase() ?: "?",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                // Risk indicator dot
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(12.dp)
                        .background(riskColor, CircleShape)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sender.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.filtered_count, sender.filteredCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    sender.dominantHorseman?.let { horseman ->
                        Text(
                            text = "\u2022",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = horseman,
                            style = MaterialTheme.typography.bodySmall,
                            color = riskColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Risk level badge
            RiskLevelBadge(riskLevel = sender.riskLevel)
        }
    }
}

@Composable
private fun RiskLevelBadge(
    riskLevel: RiskLevel,
    modifier: Modifier = Modifier
) {
    val (color, text) = when (riskLevel) {
        RiskLevel.HIGH -> MaterialTheme.colorScheme.error to stringResource(R.string.risk_level_high)
        RiskLevel.MEDIUM -> MaterialTheme.colorScheme.tertiary to stringResource(R.string.risk_level_medium)
        RiskLevel.LOW -> MaterialTheme.colorScheme.secondary to stringResource(R.string.risk_level_low)
        RiskLevel.NONE -> MaterialTheme.colorScheme.outline to ""
    }

    if (riskLevel != RiskLevel.NONE) {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(4.dp),
            color = color.copy(alpha = 0.15f)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun FourHorsemenCard(
    horsemenCounts: com.cellophanemail.sms.domain.model.HorsemenCounts,
    modifier: Modifier = Modifier
) {
    val totalCount = horsemenCounts.total
    if (totalCount == 0) return

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.four_horsemen_detected),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Horsemen bars
            HorsemanBar(
                label = stringResource(R.string.horseman_criticism),
                count = horsemenCounts.criticism,
                total = totalCount,
                color = Color(0xFFE57373) // Red
            )
            Spacer(modifier = Modifier.height(8.dp))
            HorsemanBar(
                label = stringResource(R.string.horseman_contempt),
                count = horsemenCounts.contempt,
                total = totalCount,
                color = Color(0xFFBA68C8) // Purple
            )
            Spacer(modifier = Modifier.height(8.dp))
            HorsemanBar(
                label = stringResource(R.string.horseman_defensiveness),
                count = horsemenCounts.defensiveness,
                total = totalCount,
                color = Color(0xFFFFB74D) // Orange
            )
            Spacer(modifier = Modifier.height(8.dp))
            HorsemanBar(
                label = stringResource(R.string.horseman_stonewalling),
                count = horsemenCounts.stonewalling,
                total = totalCount,
                color = Color(0xFF90A4AE) // Blue Grey
            )
        }
    }
}

@Composable
private fun HorsemanBar(
    label: String,
    count: Int,
    total: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val fraction = if (total > 0) count.toFloat() / total else 0f
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        label = "horseman_bar"
    )

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color.copy(alpha = 0.2f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedFraction)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}
