package com.d104.pnt.ui.game.wait

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.d104.pnt.R
import com.d104.pnt.domain.model.DraggableLatLng
import com.d104.pnt.domain.model.GameRole
import com.d104.pnt.ui.component.GoogleMaps
import com.d104.pnt.ui.component.PixelContainer
import com.d104.pnt.ui.component.PixelIconButton
import com.d104.pnt.ui.theme.PixelFont
import com.google.android.gms.maps.model.LatLng

import com.d104.pnt.ui.theme.*
@Composable
fun UnifiedWaitingInfoCard(
    players: List<WaitingPlayer>,
    myMemberId: Long,
    policeCount: Int,
    thiefCount: Int,
    isHost: Boolean,
    selectedPlayerId: Long?,
    prisonLocation: LatLng,
    polygonPoints: List<LatLng>,
    onPlayerClick: (WaitingPlayer) -> Unit,
    onMenuDismiss: () -> Unit,
    onInfoClick: (WaitingPlayer) -> Unit,
    onKickClick: (WaitingPlayer) -> Unit,
    modifier: Modifier = Modifier,
    onChangeRole: () -> Unit
) {
    PixelContainer(
        modifier = modifier,
        backgroundColor = Color(0xFF2D3242),
        borderColor = Color(0xFF6591E9),
        borderWidth = 4f,
        cornerSize = 20f,
        innerHorizontalPadding = 2,
        innerVerticalPadding = 12
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 맵 & 통계
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp, start = 20.dp, end = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MapPreviewContent(
                    prisonLocation = prisonLocation,
                    polygonPoints = polygonPoints
                )
                Spacer(modifier = Modifier.height(16.dp))
                RoleCountInfo(policeCount, thiefCount)
                Spacer(modifier = Modifier.height(20.dp))
                Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color(0xFF6591E9).copy(alpha = 0.5f)))
            }

            // 플레이어 그리드
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(players) { player ->
                    PlayerSlotCard(
                        player = player,
                        isMe = (player.id == myMemberId),
                        isHost = isHost,
                        showMenu = (player.id == selectedPlayerId),
                        onClick = { onPlayerClick(player) },
                        onDismissMenu = onMenuDismiss,
                        onInfoClick = { onMenuDismiss(); onInfoClick(player) },
                        onKickClick = { onMenuDismiss(); onKickClick(player) }
                    )
                }
            }

            // 역할 변경 버튼
            Box(modifier = Modifier.fillMaxWidth().height(50.dp).padding(horizontal = 20.dp), contentAlignment = Alignment.CenterEnd) {
                PixelIconButton(
                    onClick = onChangeRole, modifier = Modifier.width(90.dp), pixelSize = 2.dp, blockHeight = 16,
                    content = {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = "역할 변경", style = MaterialTheme.typography.labelMedium, color = Color.Black)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun PlayerSlotCard(
    player: WaitingPlayer,
    isMe: Boolean,
    isHost: Boolean,
    showMenu: Boolean,
    onClick: () -> Unit,
    onDismissMenu: () -> Unit,
    onInfoClick: () -> Unit,
    onKickClick: () -> Unit
) {
    val borderColor = when {
        player.isChangingRole -> Color(0xFFFF5252)
        player.isHost -> AccentYellow
        player.isReady -> Color(0xFF76FF03)
        else -> Color(0xFF8D90B3)
    }
    val cardBackgroundColor = if (isMe) Color(0xFFE3F2FD) else Color.White
    val roleIcon = if (player.isChangingRole) "?" else if (player.role == GameRole.POLICE) "👮" else "🕵️"

    Box {
        PixelContainer(
            modifier = Modifier.fillMaxWidth().height(48.dp).clickable { onClick() },
            backgroundColor = cardBackgroundColor, borderColor = borderColor, borderWidth = 5f, cornerSize = 8f
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(0xFFFFF59D)))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = player.nickname, style = MaterialTheme.typography.bodySmall,
                    color = if (isMe) Color(0xFF1565C0) else Color.Black,
                    fontWeight = if (isMe) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = roleIcon, fontSize = 18.sp, color = if (player.isChangingRole) Color.Red else Color.Black)
            }
        }

        if (showMenu) {
            PlayerActionMenu(isHost, isMe, onDismissMenu, onInfoClick, onKickClick)
        }
    }
}

@Composable
fun PlayerActionMenu(isHost: Boolean, isTargetMe: Boolean, onDismiss: () -> Unit, onInfoClick: () -> Unit, onKickClick: () -> Unit) {
    val density = LocalDensity.current
    val yOffset = remember(density) { with(density) { (48.dp + 4.dp).roundToPx() } }

    Popup(alignment = Alignment.TopCenter, offset = IntOffset(0, yOffset), onDismissRequest = onDismiss) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            MenuButton(text = "정보 확인", textColor = Color.Black, onClick = onInfoClick)
            if (isHost && !isTargetMe) {
                MenuButton(text = "강퇴하기", textColor = Color(0xFFFF5252), onClick = onKickClick)
            }
        }
    }
}

@Composable
fun MenuButton(text: String, textColor: Color, onClick: () -> Unit) {
    PixelContainer(modifier = Modifier.width(140.dp), backgroundColor = Color(0xFFD4E3FF), borderColor = Color(0xFF6591E9), borderWidth = 8f, cornerSize = 8f) {
        Box(modifier = Modifier.fillMaxWidth().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick), contentAlignment = Alignment.Center) {
            Text(text = text, fontFamily = PixelFont, color = textColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
fun MapPreviewContent(
    prisonLocation: LatLng,
    polygonPoints: List<LatLng>
) {
    GoogleMaps(
        modifier = Modifier
            .height(130.dp)
            .width(200.dp),
        prisonLocation = prisonLocation,
        polygonPoints = polygonPoints.map { DraggableLatLng(position = it) },
        inGameMinimap = false,
        isPreview = true,
    )
}

@Composable
fun RoleCountInfo(policeCount: Int, thiefCount: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(32.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "👮", fontSize = 20.sp); Spacer(modifier = Modifier.width(8.dp))
            Text(text = "$policeCount", fontFamily = PixelFont, color = Color.White, fontSize = 20.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "🕵️", fontSize = 20.sp); Spacer(modifier = Modifier.width(8.dp))
            Text(text = "$thiefCount", fontFamily = PixelFont, color = Color.White, fontSize = 20.sp)
        }
    }
}