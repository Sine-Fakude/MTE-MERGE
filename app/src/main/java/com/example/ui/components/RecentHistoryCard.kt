package com.example.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CallMerge
import androidx.compose.material.icons.rounded.CallSplit
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.DocumentHistoryEntity
import com.example.ui.theme.CyberMagenta
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.liquidGlass
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecentHistoryCard(
    item: DocumentHistoryEntity,
    onOpen: () -> Unit,
    onShare: () -> Unit
) {
    val formatter = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    val dateStr = formatter.format(Date(item.timestamp))

    val (badgeColor, badgeIcon) = when (item.operationType) {
        "MERGE" -> Pair(ElectricCyan, Icons.Rounded.CallMerge)
        "SPLIT" -> Pair(CyberMagenta, Icons.Rounded.CallSplit)
        "IMAGE_TO_PDF" -> Pair(ElectricCyan, Icons.Rounded.PictureAsPdf)
        "LOCK" -> Pair(ElectricCyan, Icons.Rounded.Lock)
        "UNLOCK" -> Pair(CyberMagenta, Icons.Rounded.LockOpen)
        else -> Pair(CyberMagenta, Icons.Rounded.QrCode2)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(shape = RoundedCornerShape(18.dp), surfaceAlpha = 0.45f)
            .clickable(onClick = onOpen)
            .padding(14.dp)
            .testTag("history_card_${item.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sleek minimalist icon: directly tinted with Electric Cyan or Cyber Magenta, no blurry box
                Icon(
                    imageVector = badgeIcon,
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = item.fileName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$dateStr • ${item.formattedSize}" + if (item.pageCount > 0) " • ${item.pageCount}p" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilledTonalIconButton(
                    onClick = onShare,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("history_share_btn_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = "Share",
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = onOpen,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("history_open_btn_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.OpenInNew,
                        contentDescription = "Actions",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
