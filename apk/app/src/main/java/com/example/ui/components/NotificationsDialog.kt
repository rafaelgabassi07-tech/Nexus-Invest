package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.viewmodel.PortfolioViewModel
import com.example.data.DbNotification
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsDialog(
    viewModel: PortfolioViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val notificationsList by viewModel.notifications.collectAsStateWithLifecycle()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Notificações",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            val unreadCount = notificationsList.count { !it.isRead }
                            Text(
                                text = if (unreadCount > 0) "$unreadCount novas mensagens pendentes" else "Nenhum novo alerta no momento",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.5.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .size(32.dp)
                            .testTag("close_notifications_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar Janela",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Actions Bar (Mark all as read, clean all)
                if (notificationsList.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                viewModel.markAllNotificationsAsRead()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("mark_all_read_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DoneAll,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Marcar todas como lidas",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        TextButton(
                            onClick = { viewModel.clearAllNotifications() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("clear_all_notifications_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = DangerRed
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Limpar tudo",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = DangerRed
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(4.dp))

                // Notifications Container
                Box(modifier = Modifier.weight(1f)) {
                    if (notificationsList.isEmpty()) {
                        // Empty State Visual Layout
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Notifications,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Tudo em ordem por aqui!",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Nenhuma notificação por enquanto. Seus proventos e variações de preços serão exibidos em tempo real.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )
                        }
                    } else {
                        // Notifications Scrollable list
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            notificationsList.forEach { notification ->
                                var isExpanded by remember(notification.id) { mutableStateOf(false) }

                                val icon = when (notification.iconName) {
                                    "WALLET" -> Icons.Default.Wallet
                                    "TRENDING" -> Icons.AutoMirrored.Filled.TrendingUp
                                    "UPDATE" -> Icons.Default.SystemUpdate
                                    "FILE" -> Icons.Default.FileOpen
                                    else -> Icons.Default.Notifications
                                }

                                val iconColor = when (notification.iconName) {
                                    "WALLET" -> GoldPrimary
                                    "TRENDING" -> SuccessGreen
                                    "UPDATE" -> MaterialTheme.colorScheme.primary
                                    "FILE" -> Color(0xFF3B82F6)
                                    else -> MaterialTheme.colorScheme.primary
                                }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            isExpanded = !isExpanded
                                            if (!notification.isRead) {
                                                viewModel.markNotificationAsRead(notification)
                                            }
                                        }
                                        .testTag("notification_item_${notification.id}"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (notification.isRead) {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        }
                                    ),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (!notification.isRead) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        } else {
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.05f)
                                        }
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .background(iconColor.copy(alpha = 0.12f), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = icon,
                                                        contentDescription = null,
                                                        tint = iconColor,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = notification.title,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            fontSize = 13.sp,
                                                            fontWeight = if (notification.isRead) FontWeight.Bold else FontWeight.Black
                                                        )
                                                        if (!notification.isRead) {
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(6.dp)
                                                                    .background(iconColor, CircleShape)
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = notification.category,
                                                            color = iconColor,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            letterSpacing = 0.5.sp
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = "•",
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                            fontSize = 9.sp
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = notification.date,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            fontSize = 9.sp
                                                        )
                                                    }
                                                }
                                            }

                                            IconButton(
                                                onClick = {
                                                    viewModel.deleteNotificationById(notification.id)
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remover",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Text(
                                            text = notification.description,
                                            color = if (notification.isRead) {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            },
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp,
                                            maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
