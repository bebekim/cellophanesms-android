package com.cellophanemail.sms.ui.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cellophanemail.sms.R
import com.cellophanemail.sms.domain.model.RiskLevel

@Composable
fun ContactsScreen(
    viewModel: ContactsViewModel = hiltViewModel(),
    onContactClick: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        ContactsContent(
            contacts = uiState.contacts,
            onContactClick = onContactClick
        )
    }
}

@Composable
private fun ContactsContent(
    contacts: List<ContactWithRisk>,
    onContactClick: (String) -> Unit
) {
    if (contacts.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.no_data_yet),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = contacts,
                key = { it.senderId }
            ) { contact ->
                ContactItem(
                    contact = contact,
                    onClick = { onContactClick(contact.senderId) }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun ContactItem(
    contact: ContactWithRisk,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val riskColor = when (contact.riskLevel) {
        RiskLevel.HIGH -> MaterialTheme.colorScheme.error
        RiskLevel.MEDIUM -> MaterialTheme.colorScheme.tertiary
        RiskLevel.LOW -> MaterialTheme.colorScheme.secondary
        RiskLevel.NONE -> MaterialTheme.colorScheme.outline
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar with risk indicator
        Box {
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                color = if (contact.riskLevel != RiskLevel.NONE) {
                    riskColor.copy(alpha = 0.15f)
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = contact.displayName.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (contact.riskLevel != RiskLevel.NONE) {
                            riskColor
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        }
                    )
                }
            }

            // Risk indicator dot
            if (contact.riskLevel != RiskLevel.NONE) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(14.dp)
                        .background(riskColor, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Contact info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.message_count, contact.messageCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (contact.filteredCount > 0) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.filtered_count, contact.filteredCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = riskColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Dominant horseman if present
            contact.dominantHorseman?.let { horseman ->
                Text(
                    text = horseman,
                    style = MaterialTheme.typography.labelSmall,
                    color = riskColor.copy(alpha = 0.8f)
                )
            }
        }

        // Risk badge
        if (contact.riskLevel != RiskLevel.NONE) {
            RiskBadge(riskLevel = contact.riskLevel)
        }
    }
}

@Composable
private fun RiskBadge(
    riskLevel: RiskLevel,
    modifier: Modifier = Modifier
) {
    val (color, text) = when (riskLevel) {
        RiskLevel.HIGH -> MaterialTheme.colorScheme.error to stringResource(R.string.risk_level_high)
        RiskLevel.MEDIUM -> MaterialTheme.colorScheme.tertiary to stringResource(R.string.risk_level_medium)
        RiskLevel.LOW -> MaterialTheme.colorScheme.secondary to stringResource(R.string.risk_level_low)
        RiskLevel.NONE -> return
    }

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
