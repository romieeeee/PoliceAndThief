package com.d104.pnt.ui.game.end

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.d104.pnt.R
import com.d104.pnt.domain.model.common.UiState
import com.d104.pnt.ui.component.PixelButtonCode
import com.d104.pnt.ui.component.PixelContainer
import com.d104.pnt.ui.theme.AccentYellow
import com.d104.pnt.ui.theme.DarkBackground
import com.d104.pnt.ui.theme.LoseColor
import com.d104.pnt.ui.theme.PixelFont
import com.d104.pnt.ui.theme.TextPrimary
import com.d104.pnt.ui.theme.TextSecondary
import com.d104.pnt.ui.theme.WinColor

data class MvpData(
    val type: String,
    val role: String,
    val nickname: String,
    val statLabel: String,
    val statValue: String,
    @DrawableRes val iconRes: Int
)

enum class ReportStep { NONE, INPUT, CONFIRM, SUCCESS }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GameResultScreen(
    gameId: Long,
    onBackToHome: () -> Unit,
    onBackToWaitingRoom: (Long) -> Unit,
    isPolice: Boolean = true, // 기본값
    isWin: Boolean = true,
    mvpList: List<MvpData> = listOf(),
    viewModel: GameResultViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState is UiState.Loading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.CircularProgressIndicator(
                color = AccentYellow
            )
        }
        return
    }

    val realData = (uiState as? UiState.Success<GameResultUiData>)?.data

    val finalIsPolice = realData?.isPolice ?: isPolice
    val finalIsWin = realData?.isWin ?: isWin
    val finalMvpList = realData?.mvpList ?: mvpList

    val myGameStat = realData?.myGameStat ?: "0명"
    val myBestStat = realData?.myBestStat ?: "기록 없음"
    val myTierIcon = realData?.myTierIconRes ?: R.drawable.police_lv1

    DisposableEffect(Unit) {
        onDispose { viewModel.cleanupGameSocket() }
    }

    val blinkAlpha by rememberInfiniteTransition(label = "winlose-blink")
        .animateFloat(
            initialValue = 1f,
            targetValue = 0.7f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 500, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )

    val pagerState = rememberPagerState(pageCount = { finalMvpList.size })

    val titleColor = if (finalIsWin) WinColor else LoseColor
    val titleText = if (finalIsWin) "WIN!" else "LOSE"
    val statsLabel = if (finalIsPolice) "검거한 도둑 수" else "최장 생존 시간"
    val mvpBoxBgColor = Color(0xFF35384F)
    val participantNames = finalMvpList.map { it.nickname }

    val characterImageRes = when {
        finalIsPolice && finalIsWin -> R.drawable.img_police_win
        finalIsPolice && !finalIsWin -> R.drawable.img_police_lose
        !finalIsPolice && finalIsWin -> R.drawable.img_thief_win
        else -> R.drawable.img_thief_lose
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.bg_dawn),
            contentDescription = "Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
        )

        // 메인 레이아웃
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 신고 버튼
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier.clickable { viewModel.openReportDialog() },
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        modifier = Modifier.size(25.dp),
                        painter = painterResource(id = R.drawable.report_siren),
                        contentDescription = "report button",
                    )
                    Text(
                        modifier = Modifier.offset(y = 18.dp),
                        text = "신고하기",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                    )
                }
            }

            Text(
                text = "결과 리포트",
                fontSize = 24.sp,
                fontFamily = PixelFont,
                color = Color.LightGray
            )

            // WIN/LOSE
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = titleText,
                    fontFamily = PixelFont,
                    fontSize = 90.sp,
                    fontWeight = FontWeight.Bold,
                    color = titleColor.copy(alpha = blinkAlpha),
                    style = TextStyle(
                        shadow = Shadow(color = Color.Black)
                    ),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .zIndex(1f)
                )

                Image(
                    painter = painterResource(id = characterImageRes),
                    contentDescription = "Character",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(190.dp)
                        .align(Alignment.Center)
                        .offset(y = 40.dp)
                        .zIndex(2f)
                )
            }

            // Rank / Stat
            PixelContainer(
                backgroundColor = Color.Black.copy(alpha = 0.45f),
                borderColor = AccentYellow,
                borderWidth = 3f,
                cornerSize = 12f,
                innerVerticalPadding = 20,
                innerHorizontalPadding = 0,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // RANK (내 정보)
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "RANK",
                            fontFamily = PixelFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                        ) {
                            Image(
                                painter = painterResource(id = myTierIcon),
                                contentDescription = "Tier",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .scale(1.6f)
                            )
                        }
                    }

                    // Stat (내 이번 판 기록)
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = statsLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = myGameStat,
                            fontFamily = PixelFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 36.sp,
                            color = AccentYellow
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_best),
                                contentDescription = null,
                                tint = Color.Unspecified
                            )
                            Text(
                                textAlign = TextAlign.Center,
                                text = "최고기록 $myBestStat",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            val isFirst = pagerState.currentPage == 0
            val isLast = pagerState.currentPage == pagerState.pageCount - 1

            // MVP 카드
            Box(modifier = Modifier.fillMaxWidth()) {
                HorizontalPager(
                    state = pagerState,
                    pageSpacing = 16.dp,
                    contentPadding = PaddingValues(
                        start = if (isLast) 32.dp else 0.dp,
                        end = if (isFirst) 32.dp else 0.dp
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) { page ->
                    MvpCard(
                        mvpData = finalMvpList[page],
                        backgroundColor = mvpBoxBgColor,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PixelButtonCode(
                    text = "대기방",
                    onClick = {
                        viewModel.backToLobby(gameId) {
                            onBackToWaitingRoom(gameId)
                        }
                    },
                    mainColor = Color(0xFF6B728E),
                    textColor = Color.White,
                    blockHeight = 14,
                    blockWidth = 20,
                    fontSize = 18,
                    modifier = Modifier.weight(1f)
                )

                PixelButtonCode(
                    text = "홈으로",
                    onClick = {
                        onBackToHome()
                    },
                    mainColor = TextPrimary,
                    textColor = Color.Black,
                    blockHeight = 14,
                    blockWidth = 20,
                    fontSize = 18,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 신고 다이얼로그
        when (viewModel.reportStep) {
            ReportStep.INPUT -> {
                ReportDialog(
                    initialTargetUser = viewModel.draft.nickname,
                    initialReason = viewModel.draft.reasonKr,
                    initialDescription = viewModel.draft.detail,
                    validNicknames = participantNames,
                    onDismissRequest = { viewModel.closeReportDialog() },
                    onReport = { user, reason, desc ->
                        viewModel.onDraftSubmitted(user, reason, desc)
                    }
                )
            }

            ReportStep.CONFIRM -> {
                val isLoading = viewModel.reportSendState is UiState.Loading
                val errorMsg = (viewModel.reportSendState as? UiState.Error)?.message

                ConfirmReportDialog(
                    isLoading = isLoading,
                    errorText = errorMsg,
                    onDismissRequest = { viewModel.backToInput() },
                    onConfirm = { viewModel.confirmReport() }
                )
            }

            ReportStep.SUCCESS -> {
                SuccessReportDialog(
                    onDismissRequest = { viewModel.closeReportDialog() }
                )
            }

            else -> Unit
        }
    }
}

@Composable
fun MvpCard(
    mvpData: MvpData,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    PixelContainer(
        backgroundColor = backgroundColor,
        borderColor = Color(0xFF6B728E),
        borderWidth = 2f,
        cornerSize = 8f,
        innerVerticalPadding = 20,
        innerHorizontalPadding = 16,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(start = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier
                    .size(56.dp)
                    .weight(0.3f),
                painter = painterResource(id = mvpData.iconRes),
                contentDescription = null,
                tint = Color.Unspecified
            )

            Spacer(modifier = Modifier.width(20.dp))

            Column(
                modifier = Modifier.weight(0.7f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = mvpData.type,
                    fontFamily = PixelFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = AccentYellow
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = mvpData.role,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray
                    )
                    Text(
                        text = mvpData.nickname,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = mvpData.statLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray
                    )
                    Text(
                        text = mvpData.statValue,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

data class GameResultUiData(
    val isPolice: Boolean,
    val isWin: Boolean,
    val mvpList: List<MvpData>,
    val myTierIconRes: Int,
    val myGameStat: String,
    val myBestStat: String
)