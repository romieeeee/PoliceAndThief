package com.d104.pnt.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.d104.pnt.R
import com.d104.pnt.ui.component.PixelButtonCode
import com.d104.pnt.ui.component.PixelContainer
import com.d104.pnt.ui.component.PixelIconButton
import com.d104.pnt.ui.component.PixelInputField
import com.d104.pnt.ui.theme.BorderDefault
import com.d104.pnt.ui.theme.TextPrimary

@Composable
fun HomeScreen(
    goToGameCreate: () -> Unit
) {
    var showJoinDialog by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {

            Image(
                modifier = Modifier.fillMaxSize(),
                painter = painterResource(R.drawable.img_main_bg),
                contentDescription = null,
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 50.dp)
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                PixelIconButton(
                    onClick = { showJoinDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = "게임 입장하기",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Black
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))

                PixelIconButton(
                    onClick = { goToGameCreate() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = "게임 생성하기",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Black
                    )
                }
            }

            if (showJoinDialog) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable { showJoinDialog = false },
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.clickable(enabled = false) {}) {
                        JoinGameDialog(
                            onDismiss = { showJoinDialog = false },
                            onConfirm = {
                                showJoinDialog = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun JoinGameDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    PixelContainer(
        modifier = Modifier.width(320.dp),
        backgroundColor = TextPrimary,
        borderColor = BorderDefault,
        borderWidth = 6f,
        cornerSize = 20f
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 30.dp, horizontal = 20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "참여 코드 입력",
                style = MaterialTheme.typography.titleLarge,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(30.dp))

            PixelInputField(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(Alignment.CenterVertically),
                placeholder = "참여코드를 입력해주세요",
                borderColor = BorderDefault
            )

            Spacer(modifier = Modifier.height(30.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                PixelButtonCode(
                    text = "취소",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    mainColor = TextPrimary,
                    borderColor = BorderDefault,
                    textColor = Color.Black,
                    fontSize = 16,
                    blockHeight = 10
                )

                PixelButtonCode(
                    text = "확인",
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    mainColor = TextPrimary,
                    borderColor = BorderDefault,
                    textColor = Color.Black,
                    fontSize = 16,
                    blockHeight = 10
                )
            }
        }
    }
}