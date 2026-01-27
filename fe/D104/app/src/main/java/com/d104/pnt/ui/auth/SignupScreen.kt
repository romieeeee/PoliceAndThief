package com.d104.pnt.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.d104.pnt.R
import com.d104.pnt.ui.component.PixelIconButton
import com.d104.pnt.ui.component.PixelInputField
import com.d104.pnt.ui.theme.BorderDefault
import com.d104.pnt.ui.theme.CheckGreen

@Composable
fun SignupScreen(
    onSuccess: (String) -> Unit
) {
    var clicked by remember { mutableStateOf(false) }

    var id by remember { mutableStateOf("") }
    var pw by remember { mutableStateOf("") }
    var pwCheck by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var bDay by remember { mutableStateOf("") }

    Surface(modifier = Modifier.fillMaxSize()) {

        // 배경 이미지
        Image(
            modifier = Modifier.fillMaxSize(),
            painter = painterResource(R.drawable.img_login_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .windowInsetsPadding(
                    WindowInsets
                        .statusBars
                        .only(WindowInsetsSides.Top)
                )
                .windowInsetsPadding(
                    WindowInsets
                        .navigationBars
                        .only(WindowInsetsSides.Bottom)
                ),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "회원가입",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 40.dp),
                color = Color.White
            )

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "아이디",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(start = 10.dp, bottom = 4.dp)
                        .fillMaxWidth(),
                    color = Color.White
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    PixelInputField(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(end = 4.dp)
                            .weight(1f),
                        value = id,
                        onValueChange = { },
                        placeholder = "아이디 (8~16자 이내)",
                        borderColor = BorderDefault
                    )

                    PixelIconButton(
                        onClick = { clicked = !clicked },
                        mainColor = if (clicked) Color.White else CheckGreen,
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(1f)  // 정사각형으로 만들기
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.check),
                            contentDescription = null,
                            tint = if (clicked) CheckGreen else Color.White
                        )
                    }
                }
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                )

                Text(
                    text = "비밀번호",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(start = 10.dp, bottom = 4.dp)
                        .fillMaxWidth(),
                    color = Color.White
                )

                PixelInputField(
                    value = pw,
                    onValueChange = { },
                    placeholder = "비밀번호",
                    borderColor = BorderDefault
                )
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                )

                Text(
                    text = "비밀번호 확인",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(start = 10.dp, bottom = 4.dp)
                        .fillMaxWidth(),
                    color = Color.White
                )

                PixelInputField(
                    value = pwCheck,
                    onValueChange = { },
                    placeholder = "비밀번호 확인",
                    borderColor = BorderDefault
                )

                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                )

                Text(
                    text = "생년월일",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(start = 10.dp, bottom = 4.dp)
                        .fillMaxWidth(),
                    color = Color.White
                )

                PixelInputField(
                    value = bDay,
                    onValueChange = { },
                    placeholder = "생년월일",
                    borderColor = BorderDefault
                )
            }

            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PixelIconButton(
                    onClick = {},
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                ) {
                    Text(
                        text = "이전",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.Black
                    )
                }

                PixelIconButton(
                    onClick = { onSuccess("keroro") },
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                ) {
                    Text(
                        text = "확인",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.Black
                    )
                }
            }
        }
    }

}

