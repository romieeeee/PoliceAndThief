package com.d104.pnt.permission

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.d104.utils.helper.PermissionHelper

/**
 * 권한 설명 다이얼로그
 */
@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun PermissionDialog(
    permissionType: PermissionHelper.PermissionType,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = getPermissionIcon(permissionType),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "${permissionType.title} 필요",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = permissionType.rationale,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("권한 허용")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

/**
 * 권한 거부 시 설정 안내 다이얼로그
 */
@Composable
fun PermissionDeniedDialog(
    permissionType: PermissionHelper.PermissionType,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = "${permissionType.title} 거부됨",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = permissionType.rationale,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "설정에서 권한을 허용해주세요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(onClick = onOpenSettings) {
                Text("설정으로 이동")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("나중에")
            }
        }
    )
}

/**
 * 필수 권한 전체 요청 다이얼로그
 */
@Composable
fun EssentialPermissionsDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "권한 허용 필요",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "게임 플레이를 위해 다음 권한이 필요합니다:",
                    style = MaterialTheme.typography.bodyMedium
                )

                PermissionItem(
                    icon = Icons.Default.LocationOn,
                    title = "위치",
                    description = "실시간 위치 추적"
                )

                PermissionItem(
                    icon = Icons.Default.CameraAlt,
                    title = "카메라",
                    description = "미션 수행 및 QR 스캔"
                )

                PermissionItem(
                    icon = Icons.Default.Mic,
                    title = "마이크",
                    description = "무전기 기능"
                )

                PermissionItem(
                    icon = Icons.Default.Notifications,
                    title = "알림",
                    description = "게임 이벤트 알림"
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("권한 허용")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

/**
 * 권한 항목 표시 컴포넌트
 */
@Composable
private fun PermissionItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 권한 타입에 맞는 아이콘 반환
 */
@RequiresApi(Build.VERSION_CODES.P)
private fun getPermissionIcon(permissionType: PermissionHelper.PermissionType): ImageVector {
    return when (permissionType) {
        PermissionHelper.PermissionType.LOCATION -> Icons.Default.LocationOn
        PermissionHelper.PermissionType.CAMERA -> Icons.Default.CameraAlt
        PermissionHelper.PermissionType.AUDIO -> Icons.Default.Mic
        PermissionHelper.PermissionType.NOTIFICATION -> Icons.Default.Notifications
        PermissionHelper.PermissionType.FOREGROUND_SERVICE -> Icons.Default.MyLocation
    }
}