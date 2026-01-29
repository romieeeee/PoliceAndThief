package com.d104.pnt.ui.auth

import android.content.pm.PackageManager
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.d104.pnt.R
import com.d104.pnt.data.remote.model.response.LoginResponse
import com.d104.pnt.domain.model.common.UiState
import com.d104.pnt.ui.component.PixelButtonCode
import com.d104.pnt.ui.component.PixelInputField
import com.d104.pnt.ui.theme.AccentRed
import com.d104.pnt.ui.theme.BorderDefault
import timber.log.Timber
import java.security.MessageDigest

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    goToSignup: () -> Unit,
    onLoginSuccess: (String) -> Unit
) {

    val id by viewModel.id.collectAsState()
    val password by viewModel.password.collectAsState()
    val loginState by viewModel.loginState.collectAsState()

    val context = LocalContext.current

    // 로그인 성공 처리
    LaunchedEffect(loginState) {
        if (loginState is UiState.Success) {
            val response = (loginState as UiState.Success<LoginResponse>).data
            onLoginSuccess(response.member.id)
        }
    }

    LaunchedEffect(Unit) {
        try {
            val info = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNATURES
            )
            for (signature in info.signatures!!) {
                val md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                val keyHash = Base64.encodeToString(md.digest(), Base64.NO_WRAP)
                Timber.d("========================================")
                Timber.d("📱 Current KeyHash: $keyHash")
                Timber.d("========================================")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting key hash")
        }
    }

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
                .padding(horizontal = 28.dp)
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
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "게임 시작하기",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 40.dp),
                color = Color.White
            )

            // Login Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.7f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    PixelInputField(
                        value = id,
                        onValueChange = { viewModel.updateId(it) },
                        placeholder = "아이디",
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = BorderDefault
                    )

                    PixelInputField(
                        value = password,
                        onValueChange = { viewModel.updatePassword(it) },
                        placeholder = "비밀번호",
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = BorderDefault,
                    )
                }

                PixelButtonCode(
                    modifier = Modifier
                        .weight(0.3f)
                        .fillMaxHeight(),
                    text = "로그인",
                    fontSize = 16,
                    onClick = {
                        if (!viewModel.isLoginEnabled()) {
                            return@PixelButtonCode
                        }

                        viewModel.login()
                    },
                    mainColor = AccentRed,
                    borderColor = BorderDefault,
                )
            }

            if (loginState is UiState.Error) {
                Text(
                    text = (loginState as UiState.Error).message,
                    color = Color.Red,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            // 로딩 표시
            if (loginState is UiState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(8.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "회원가입",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    IconButton(
                        onClick = { goToSignup() }
                    ) {
                        Image(
                            painter = painterResource(R.drawable.arrow),
                            contentDescription = null
                        )
                    }
                }


                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "비밀번호 찾기",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    IconButton(
                        onClick = {}
                    ) {
                        Image(
                            painter = painterResource(R.drawable.arrow),
                            contentDescription = null
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))


            // ----- Divider -----
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.divider),
                    contentDescription = null
                )

                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = "OR",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )

                Image(
                    painter = painterResource(R.drawable.divider),
                    contentDescription = null
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Social Login Section
            Image(
                modifier = Modifier
                    .aspectRatio(7f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        viewModel.loginWithKakao(context)
                    },
                painter = painterResource(R.drawable.kakao_login_btn),
                contentDescription = "카카오 로그인",
            )

            Spacer(modifier = Modifier.height(20.dp))

            Image(
                modifier = Modifier
                    .aspectRatio(7f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {

                    },
                painter = painterResource(R.drawable.google_login_btn),
                contentDescription = "구글 로그인",
            )
        }
    }

}

