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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.d104.pnt.R
import com.d104.pnt.ui.component.PixelButtonCode
import com.d104.pnt.ui.component.PixelContainer
import com.d104.pnt.ui.theme.AccentYellow
import com.d104.pnt.ui.theme.DarkBackground
import com.d104.pnt.ui.theme.LoseColor
import com.d104.pnt.ui.theme.PixelFont
import com.d104.pnt.ui.theme.TextPrimary
import com.d104.pnt.ui.theme.TextSecondary
import com.d104.pnt.ui.theme.WinColor
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

// MVP 데이터 모델
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
    isPolice: Boolean = true,
    isWin: Boolean = true,
    mvpList: List<MvpData> = listOf(
        MvpData(
            "MVP",
            if (isPolice) "경찰" else "도둑",
            "박정후",
            "생존 시간",
            "16:39",
            android.R.drawable.btn_star_big_on
        ),
        MvpData(
            "ACE",
            if (isPolice) "경찰" else "도둑",
            "김철수",
            "검거 수",
            "5명",
            android.R.drawable.ic_menu_myplaces
        )
    ),
    @DrawableRes tierIconRes: Int = R.drawable.img_tier_police_2,
    tierName: String = "경장",
    statsValue: String = "24명",
    onExitClick: () -> Unit = {}
) {
    val blinkAlpha by rememberInfiniteTransition(label = "winlose-blink")
        .animateFloat(
            initialValue = 1f,
            targetValue = 0.7f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 500,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )

    val pagerState = rememberPagerState(pageCount = { mvpList.size })

    val titleColor = if (isWin) WinColor else LoseColor
    val titleText = if (isWin) "WIN!" else "LOSE"
    val statsLabel = if (isPolice) "검거한 도둑 수" else "최장 생존 시간"
    val mvpBoxBgColor = Color(0xFF35384F)
    val participantNames = remember(mvpList) {
        mvpList.map { it.nickname } + listOf("치와와", "이래롬") // 게임 참여자 임시 목록
    }

    val characterImageRes = when {
        isPolice && isWin -> R.drawable.img_police_win
        isPolice && !isWin -> R.drawable.img_police_lose
        !isPolice && isWin -> R.drawable.img_thief_win
        else -> R.drawable.img_thief_win
    }

    var reportStep by remember { mutableStateOf(ReportStep.NONE) }
    var tempReportData by remember { mutableStateOf(Triple("", "", "")) }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .clickable { reportStep = ReportStep.INPUT },
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
                        shadow = Shadow(
                            color = Color.Black,
                        )
                    ),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .zIndex(1f)
                )

                // 캐릭터 이미지
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
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // RANK
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
                        Spacer(modifier = Modifier.height(8.dp))
                        Image(
                            painter = painterResource(id = tierIconRes),
                            contentDescription = "Tier",
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = tierName,
                            fontFamily = PixelFont,
                            fontWeight = FontWeight.Normal,
                            fontSize = 16.sp,
                            color = Color.LightGray
                        )
                    }

                    // Stat
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
                            text = statsValue,
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
                                text = "최고기록 13:30",
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
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
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
                        mvpData = mvpList[page],
                        backgroundColor = mvpBoxBgColor,
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            PixelButtonCode(
                text = "나가기",
                onClick = onExitClick,
                mainColor = TextPrimary,
                textColor = Color.Black,
                blockHeight = 14,
                blockWidth = 40,
                fontSize = 20,
                modifier = Modifier.fillMaxWidth()
            )
        }

        when (reportStep) {
            ReportStep.INPUT -> {
                ReportDialog(
                    initialTargetUser = tempReportData.first,
                    initialReason = tempReportData.second,
                    initialDescription = tempReportData.third,
                    validNicknames = participantNames, // 참여자 명단
                    // 입력창을 닫으면 데이터 비우기
                    onDismissRequest = {
                        tempReportData = Triple("", "", "")
                        reportStep = ReportStep.NONE
                    },

                    onReport = { user, reason, desc ->
                        tempReportData = Triple(user, reason, desc)
                        reportStep = ReportStep.CONFIRM
                    }
                )
            }
            ReportStep.CONFIRM -> {
                ConfirmReportDialog(
                    onDismissRequest = { reportStep = ReportStep.INPUT },
                    onConfirm = {
                        println("신고 전송: $tempReportData") // 실제 서버 전송 로직이 들어갈 곳

                        tempReportData = Triple("", "", "") // 신고 후 데이터 비우기

                        reportStep = ReportStep.SUCCESS
                    }
                )
            }
            ReportStep.SUCCESS -> {
                SuccessReportDialog(
                    onDismissRequest = { reportStep = ReportStep.NONE }
                )
            }
            else -> {}
        }
    }
}

// MVP 카드
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

@Preview
@Composable
fun GameResultScreenPreview() {
    GameResultScreen()
}