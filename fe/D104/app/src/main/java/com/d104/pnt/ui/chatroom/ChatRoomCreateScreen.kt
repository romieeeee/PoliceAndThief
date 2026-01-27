package com.d104.pnt.ui.chatroom

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.d104.pnt.R
import com.d104.pnt.ui.component.PixelContainer
import com.d104.pnt.ui.component.PixelInputField
import com.d104.pnt.ui.component.RoundedButton
import com.d104.pnt.ui.game.create.CounterControl
import com.d104.pnt.ui.game.create.SectionTitle
import com.d104.pnt.ui.theme.BorderDefault
import com.d104.pnt.ui.theme.DarkSurface
import com.d104.pnt.ui.theme.DialogBorderColor
import com.d104.pnt.ui.theme.TextPrimary

@Composable
fun ChatRoomCreateScreen(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    viewModel: ChatRoomCreateViewModel = hiltViewModel()
){
    val title by viewModel.title.collectAsStateWithLifecycle()
    val description by viewModel.description.collectAsStateWithLifecycle()
    val currentAddress by viewModel.currentAddress.collectAsStateWithLifecycle()

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        // ViewModel에게 Context를 줘서 위치를 가져오고 저장하게 시킴
        viewModel.getLocationInfo(context)
    }
    Surface(modifier = Modifier.fillMaxSize()) {

        Image(
            modifier = Modifier.fillMaxSize(),
            painter = painterResource(R.drawable.img_main_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 게임 생성 컨테이너
                PixelContainer(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = DarkSurface,
                    borderColor = DialogBorderColor,
                    borderWidth = 8f,
                    cornerSize = 16f
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "채팅방 생성하기",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        SectionTitle(text = "채팅방 제목 설정")
                        Spacer(modifier = Modifier.height(8.dp))
                        PixelInputField(
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = "채팅방 제목을 입력하세요",
                            borderColor = DialogBorderColor,
                            value = title,
                            onValueChange = { viewModel.updateTitle(it) }
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        SectionTitle(text = "채팅방 설명")

                        Spacer(modifier = Modifier.height(8.dp))

                        PixelInputField(
                            modifier = Modifier.fillMaxWidth()
                                .height(150.dp),
                            placeholder = "채팅방 설명을 입력하세요",
                            borderColor = DialogBorderColor,
                            value = description,
                            onValueChange = { viewModel.updateDescription(it) }
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        SectionTitle(text = "채팅방 최대 인원수")

                        Spacer(modifier = Modifier.height(8.dp))

                        CounterControl(
                            modifier = Modifier.padding(horizontal = 80.dp),
                            label = "최대 인원수",
                            icon = Icons.Default.People,
                            value = "30",
                            unit = "명",
                            onDecrease = {  },
                            onIncrease = {  }
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        SectionTitle(text = "게시 지역")

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "현재 위치를 기반으로 게시 장소가 결정됩니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        PixelContainer (
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = TextPrimary,
                            borderColor = DialogBorderColor,
                        ) {
                            Row (
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    modifier = Modifier.weight(0.3f),
                                    text = currentAddress.major,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = BorderDefault
                                )
                                Text(
                                    modifier = Modifier.weight(0.05f),
                                    text = "-",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = BorderDefault
                                )
                                Text(
                                    modifier = Modifier.weight(0.3f),
                                    text = currentAddress.middle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = BorderDefault
                                )
                                Text(
                                    modifier = Modifier.weight(0.05f),
                                    text = "-",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = BorderDefault
                                )
                                Text(
                                    modifier = Modifier.weight(0.3f),
                                    text = currentAddress.sub,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = BorderDefault
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            RoundedButton(
                                text = "취소",
                                onClick = {
                                    onCancel()
                                },
                                containerColor = Color.White,
                                modifier = Modifier.weight(1f)
                            )

                            RoundedButton(
                                text = "생성",
                                onClick = { onConfirm() },
                                containerColor = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}