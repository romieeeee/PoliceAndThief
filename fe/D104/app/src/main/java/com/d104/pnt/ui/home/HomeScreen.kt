package com.d104.pnt.ui.home

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.d104.pnt.R
import com.d104.pnt.ui.component.PixelButtonCode
import com.d104.pnt.ui.component.PixelContainer
import com.d104.pnt.ui.component.PixelIconButton
import com.d104.pnt.ui.component.PixelInputField
import com.d104.pnt.ui.theme.BorderDefault
import com.d104.pnt.ui.theme.TextPrimary

@Composable
fun HomeScreen(
    goToGameCreate: () -> Unit,
    navigateToGameRoom: (Long) -> Unit,
    navigateToIntro: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var showJoinDialog by remember { mutableStateOf(false) }
    val joinCode by viewModel.joinCode.collectAsStateWithLifecycle()

    // 이벤트 수집
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is HomeViewModel.HomeUiEvent.NavigateToIntro -> {
                    navigateToIntro()
                }

                is HomeViewModel.HomeUiEvent.ShowMessage -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }

                is HomeViewModel.HomeUiEvent.ShowError -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    Surface(modifier = Modifier.fillMaxSize()) {

        Image(
            modifier = Modifier.fillMaxSize(),
            painter = painterResource(R.drawable.img_main_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 50.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            PixelIconButton(
                onClick = {
                    showJoinDialog = true
//                    navigateToGameRoom(1)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    modifier = Modifier.padding(vertical = 2.dp),
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
                        },
                        joinCode = joinCode,
                        onUpdateCode = {
                            viewModel.updateJoinCode(it)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun JoinGameDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    joinCode: String,
    onUpdateCode: (String) -> Unit
) {
    var code by remember { mutableStateOf("") }

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
                borderColor = BorderDefault,
                value = joinCode,
                onValueChange = { onUpdateCode(it) }
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

