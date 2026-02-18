package com.cellophanemail.sms.ui.components.text

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cellophanemail.sms.R
import com.cellophanemail.sms.domain.model.AnnotationType

data class EntitySheetState(
    val type: AnnotationType,
    val text: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntityActionSheet(
    state: EntitySheetState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Header: type label + matched text
            Text(
                text = labelForType(state.type),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = state.text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Contextual actions
            when (state.type) {
                AnnotationType.DATE_TIME -> {
                    ActionRow(Icons.Default.CalendarMonth, stringResource(R.string.entity_action_calendar)) {
                        openCalendar(context, state.text)
                        onDismiss()
                    }
                    ActionRow(Icons.Default.ContentCopy, stringResource(R.string.entity_action_copy)) {
                        copyToClipboard(context, state.text)
                        onDismiss()
                    }
                }
                AnnotationType.URL -> {
                    ActionRow(Icons.Default.Language, stringResource(R.string.entity_action_open_browser)) {
                        openUrl(context, state.text)
                        onDismiss()
                    }
                    ActionRow(Icons.Default.ContentCopy, stringResource(R.string.entity_action_copy)) {
                        copyToClipboard(context, state.text)
                        onDismiss()
                    }
                }
                AnnotationType.EMAIL -> {
                    ActionRow(Icons.Default.Email, stringResource(R.string.entity_action_send_email)) {
                        openEmail(context, state.text)
                        onDismiss()
                    }
                    ActionRow(Icons.Default.ContentCopy, stringResource(R.string.entity_action_copy)) {
                        copyToClipboard(context, state.text)
                        onDismiss()
                    }
                }
                AnnotationType.PHONE_NUMBER -> {
                    ActionRow(Icons.Default.Phone, stringResource(R.string.entity_action_call)) {
                        openDialer(context, state.text)
                        onDismiss()
                    }
                    ActionRow(Icons.Default.Sms, stringResource(R.string.entity_action_send_sms)) {
                        openSms(context, state.text)
                        onDismiss()
                    }
                    ActionRow(Icons.Default.ContentCopy, stringResource(R.string.entity_action_copy)) {
                        copyToClipboard(context, state.text)
                        onDismiss()
                    }
                }
                else -> {
                    ActionRow(Icons.Default.ContentCopy, stringResource(R.string.entity_action_copy)) {
                        copyToClipboard(context, state.text)
                        onDismiss()
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

private fun labelForType(type: AnnotationType): String = when (type) {
    AnnotationType.DATE_TIME -> "Date / Time"
    AnnotationType.URL -> "Link"
    AnnotationType.EMAIL -> "Email Address"
    AnnotationType.PHONE_NUMBER -> "Phone Number"
    else -> "Entity"
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Entity", text))
    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
}

private fun safeLaunch(context: Context, intent: Intent) {
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "No app available for this action", Toast.LENGTH_SHORT).show()
    }
}

private fun openCalendar(context: Context, dateText: String) {
    val intent = Intent(Intent.ACTION_INSERT).apply {
        data = CalendarContract.Events.CONTENT_URI
        putExtra(CalendarContract.Events.TITLE, "Event from message")
        putExtra(CalendarContract.Events.DESCRIPTION, "Date reference: $dateText")
    }
    safeLaunch(context, intent)
}

private fun openUrl(context: Context, url: String) {
    val fullUrl = if (url.startsWith("http")) url else "https://$url"
    safeLaunch(context, Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl)))
}

private fun openEmail(context: Context, email: String) {
    safeLaunch(context, Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")))
}

private fun openDialer(context: Context, phone: String) {
    safeLaunch(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
}

private fun openSms(context: Context, phone: String) {
    safeLaunch(context, Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone")))
}
