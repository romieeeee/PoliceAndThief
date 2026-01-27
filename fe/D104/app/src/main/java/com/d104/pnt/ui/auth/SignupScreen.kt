package com.d104.pnt.ui.auth

import android.os.Build
import androidx.annotation.RequiresApi
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.d104.pnt.R
import com.d104.pnt.domain.model.common.UiState
import com.d104.pnt.ui.component.PixelIconButton
import com.d104.pnt.ui.component.PixelInputField
import com.d104.pnt.ui.theme.BorderDefault
import com.d104.pnt.ui.theme.CheckGreen
import timber.log.Timber

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun SignupScreen(
    viewModel: SignupViewModel = hiltViewModel(),
    onSuccess: (String) -> Unit,
    onBack: () -> Unit = {}
) {
    // user input
    val id by viewModel.id.collectAsStateWithLifecycle()
    val pw by viewModel.pw.collectAsStateWithLifecycle()
    val pwConfirm by viewModel.pwConfirm.collectAsStateWithLifecycle()
    val nickname by viewModel.nickname.collectAsStateWithLifecycle()
    val birth by viewModel.birth.collectAsStateWithLifecycle()

    // 유효성 검사
    val isIdValid by viewModel.isIdValid.collectAsStateWithLifecycle()

    // 에러 메세지
    val idErrorMessage by viewModel.idErrorMessage.collectAsStateWithLifecycle()
    val pwErrorMessage by viewModel.pwErrorMessage.collectAsStateWithLifecycle()
    val pwConfirmErrorMessage by viewModel.pwConfirmErrorMessage.collectAsStateWithLifecycle()
    val nicknameErrorMessage by viewModel.nicknameErrorMessage.collectAsStateWithLifecycle()

    // 상태 체크
    val isDuplicateChecked by viewModel.isDuplicateChecked.collectAsStateWithLifecycle()
    val isDuplicated by viewModel.isDuplicated.collectAsStateWithLifecycle()
    val isDuplicateCheckLoading by viewModel.isDuplicateCheckLoading.collectAsStateWithLifecycle()
    val signupState by viewModel.signupState.collectAsStateWithLifecycle()

    // 회원가입 성공 처리
    LaunchedEffect(signupState) {
        when (signupState) {
            is UiState.Success -> {
                Timber.d("Signup success - navigating with nickname: ${(signupState as UiState.Success).data.nickname}")
                onSuccess((signupState as UiState.Success).data.nickname)
                viewModel.resetSignupState()
            }

            is UiState.Error -> {
                Timber.e("Signup error: ${(signupState as UiState.Error).message}")
            }

            else -> {}
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
                )
                .verticalScroll(rememberScrollState()),
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
                // 아이디
                LabeledInputField(
                    label = "아이디",
                    value = id,
                    onValueChange = { viewModel.updateId(it) },
                    placeholder = "아이디 (5~12자 이내)",
                    errorMessage = idErrorMessage,
                    trailingContent = {
                        PixelIconButton(
                            onClick = {
                                Timber.d("Duplicate check button clicked - ID: $id, isValid: $isIdValid")
                                viewModel.checkDuplicate()
                            },
                            mainColor = when {
                                isDuplicateChecked && !isDuplicated -> CheckGreen  // 체크 완료
                                !isIdValid -> Color.Gray  // 비활성화
                                else -> Color.White  // 체크 전 또는 중복
                            },
                            modifier = Modifier
                                .fillMaxHeight()
                                .aspectRatio(1f)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.check),
                                contentDescription = "중복 확인",
                                tint = when {
                                    isDuplicateChecked && !isDuplicated -> Color.White
                                    !isIdValid -> Color.DarkGray
                                    else -> CheckGreen
                                }
                            )
                        }
                    }
                )

                Spacer(Modifier.height(16.dp))

                // 비밀번호
                LabeledInputField(
                    label = "비밀번호",
                    value = pw,
                    onValueChange = { viewModel.updatePw(it) },
                    placeholder = "비밀번호 (8~16자)",
                    errorMessage = pwErrorMessage,
                    isPassword = true
                )

                Spacer(Modifier.height(16.dp))

                // 비밀번호 확인
                LabeledInputField(
                    label = "비밀번호 확인",
                    value = pwConfirm,
                    onValueChange = { viewModel.updatePwConfirm(it) },
                    placeholder = "비밀번호 확인",
                    errorMessage = pwConfirmErrorMessage,
                    isPassword = true
                )

                Spacer(Modifier.height(16.dp))

                // 닉네임
                LabeledInputField(
                    label = "닉네임",
                    value = nickname,
                    onValueChange = { viewModel.updateNickname(it) },
                    placeholder = "닉네임 (2~10자)",
                    errorMessage = nicknameErrorMessage
                )

                Spacer(Modifier.height(16.dp))

                // 생년월일
                BirthDateInputField(
                    label = "생년월일",
                    value = birth,
                    onValueChange = { viewModel.updateBirth(it) }
                )
            }

            Spacer(Modifier.height(32.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 이전 버튼
                PixelIconButton(
                    onClick = onBack,
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

                // 확인 버튼
                PixelIconButton(
                    onClick = {
                        Timber.d("Signup button clicked - isEnabled: ${viewModel.isSignupEnabled()}")
                        viewModel.signup()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                ) {
                    if (signupState is UiState.Loading) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
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
}

@Composable
fun LabeledInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    errorMessage: String = "",
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Column {
        // 라벨
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(start = 10.dp, bottom = 4.dp),
            color = Color.White
        )

        if (trailingContent != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PixelInputField(
                    value = value,
                    onValueChange = onValueChange,
                    placeholder = placeholder,
                    errorMessage = errorMessage,
                    isPassword = isPassword,
                    keyboardType = keyboardType,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    borderColor = BorderDefault
                )

                trailingContent()
            }
        } else {
            PixelInputField(
                value = value,
                onValueChange = onValueChange,
                placeholder = placeholder,
                errorMessage = errorMessage,
                isPassword = isPassword,
                keyboardType = keyboardType,
                borderColor = BorderDefault
            )
        }
    }
}