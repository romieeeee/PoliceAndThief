package com.d104.pnt.ui.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.d104.pnt.R
import com.d104.pnt.ui.component.PixelContainer
import com.d104.pnt.ui.theme.PixelFont

@Composable
fun ProfileCardSection(
    nickname: String,
    avatarUrl: String?,
    policeGrade: String,
    thiefGrade: String,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
    onUpdateNickname: (String) -> Unit = {},
    onUpdateAvatar: () -> Unit = {}
) {
    var isEditing by remember { mutableStateOf(false) }
    var editedNickname by remember { mutableStateOf(nickname) }
    var showTierGuide by remember { mutableStateOf(false) }

    LaunchedEffect(nickname) {
        editedNickname = nickname
    }

    if (showTierGuide) {
        TierGuideDialog(onDismissRequest = { showTierGuide = false })
    }

    PixelContainer(
        modifier = modifier,
        backgroundColor = Color(0xFF3F3F68),
        borderColor = Color(0xFF8D90B3),
        borderWidth = 8f,
        cornerSize = 30f
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 아바타, 닉네임
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .border(3.dp, Color.Black, CircleShape)
                    .background(Color.White)
                    .clickable { onUpdateAvatar() }
            ) {
                val painter = when (avatarUrl) {
                    "POLICE_1" -> painterResource(id = R.drawable.profile_img_police_1)
                    "POLICE_2" -> painterResource(id = R.drawable.profile_img_police_2)
                    "THIEF_1" -> painterResource(id = R.drawable.profile_img_thief_1)
                    "THIEF_2" -> painterResource(id = R.drawable.profile_img_thief_2)
                    "DEFAULT", null, "" -> painterResource(id = R.drawable.profile_img_default)
                    else -> rememberAsyncImagePainter(
                        model = avatarUrl,
                        error = painterResource(id = R.drawable.profile_img_default),
                        placeholder = painterResource(id = R.drawable.profile_img_default)
                    )
                }

                Image(
                    painter = painter,
                    contentDescription = "나의 아바타",
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 닉네임, 편집버튼
            val iconSize = 24.dp
            val iconSpacing = 8.dp
            val counterBalanceWidth = iconSize + iconSpacing

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.width(counterBalanceWidth))

                if (isEditing) {
                    BasicTextField(
                        value = editedNickname,
                        onValueChange = { if (it.length <= 10) editedNickname = it },
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = PixelFont,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        ),
                        modifier = Modifier
                            .width(140.dp)
                            .background(Color.Black.copy(alpha = 0.2f))
                            .padding(4.dp),
                        singleLine = true
                    )
                } else {
                    Text(
                        text = nickname,
                        fontFamily = PixelFont,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(4.dp)
                    )
                }

                Spacer(modifier = Modifier.width(iconSpacing))

                Image(
                    painter = painterResource(id = if (isEditing) R.drawable.check else R.drawable.ic_edit),
                    contentDescription = "닉네임 변경",
                    colorFilter = if (isEditing) null else ColorFilter.tint(Color.White),
                    modifier = Modifier
                        .size(iconSize)
                        .padding(if (isEditing) 3.dp else 0.dp)
                        .clickable {
                            if (isEditing) {
                                if (editedNickname.isNotBlank() && editedNickname != nickname) {
                                    onUpdateNickname(editedNickname)
                                }
                                isEditing = false
                            } else {
                                editedNickname = nickname
                                isEditing = true
                            }
                        }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp),
                thickness = 2.dp,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 티어 이미지
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                // 기존 티어 Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val policeRes = getPoliceTierImage(policeGrade)
                    GradeItem(
                        title = "경찰",
                        iconRes = policeRes,
                        modifier = Modifier.weight(1f)
                    )

                    // 중앙 점선
                    Canvas(
                        modifier = Modifier
                            .width(2.dp)
                            .height(130.dp)
                    ) {
                        drawLine(
                            color = Color(0xFF8D90B3),
                            start = Offset(0f, 0f),
                            end = Offset(0f, size.height),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f),
                            strokeWidth = 2.dp.toPx()
                        )
                    }

                    val thiefRes = getThiefTierImage(thiefGrade)
                    GradeItem(
                        title = "도둑",
                        iconRes = thiefRes,
                        modifier = Modifier.weight(1f)
                    )
                }

                Icon(
                    imageVector = Icons.Outlined.HelpOutline,
                    contentDescription = "티어 가이드",
                    tint = Color(0xFFC4C4C4), // 은은한 회색
                    modifier = Modifier
                        .align(Alignment.TopEnd) // 우측 상단 정렬
                        .padding(end = 8.dp, top = 0.dp) // 위치 미세 조정
                        .size(24.dp)
                        .clickable { showTierGuide = true }
                )
            }
        }
    }
}

// 등급이름 -> 이미지
fun getPoliceTierImage(grade: String): Int {
    return when (grade) {
        "순경" -> R.drawable.police_lv1
        "경장" -> R.drawable.police_lv2
        "경사" -> R.drawable.police_lv3
        "경위" -> R.drawable.police_lv4
        "경감" -> R.drawable.police_lv5
        "경정" -> R.drawable.police_lv6
        "총경" -> R.drawable.police_lv7
        "경무관" -> R.drawable.police_lv8
        "치안감" -> R.drawable.police_lv9
        "치안정감" -> R.drawable.police_lv10
        "치안총감" -> R.drawable.police_lv11
        else -> R.drawable.police_lv1
    }
}

fun getThiefTierImage(grade: String): Int {
    return when (grade) {
        "바늘도둑", "바늘 도둑" -> R.drawable.thief_lv1
        "좀도둑", "좀 도둑" -> R.drawable.thief_lv2
        "소매치기" -> R.drawable.thief_lv3
        "빈집털이" -> R.drawable.thief_lv4
        "소도둑", "소 도둑" -> R.drawable.thief_lv5
        "금고털이" -> R.drawable.thief_lv6
        "은행털이" -> R.drawable.thief_lv7
        "홍길동" -> R.drawable.thief_lv8
        "인비저블" -> R.drawable.thief_lv9
        "괴도" -> R.drawable.thief_lv10
        "대도" -> R.drawable.thief_lv11
        else -> R.drawable.thief_lv1
    }
}

@Composable
private fun GradeItem(
    title: String,
    iconRes: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        // 타이틀
        Text(
            text = title,
            fontFamily = PixelFont,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 이미지
        Box(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(androidx.compose.ui.graphics.RectangleShape)
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = "$title 티어",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    // 여백이 있어서, 확대 후 테두리 제거
                    .graphicsLayer(
                        scaleX = 2f,
                        scaleY = 2f,
                        translationY = 0f
                    )
            )
        }
    }
}

