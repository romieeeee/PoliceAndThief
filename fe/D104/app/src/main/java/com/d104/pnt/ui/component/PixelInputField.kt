package com.d104.pnt.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation


@Composable
fun PixelInputField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    backgroundColor: Color = Color.White,
    borderColor: Color = Color.White,
    isPassword: Boolean = false
) {
    // 배경 컨테이너
    PixelContainer(
        modifier = modifier,
        borderWidth = 10f,
        cornerSize = 20f,
        backgroundColor = backgroundColor,
        borderColor = borderColor
    ) {
        // 내부에 들어갈 입력 필드
        BasicTextField(
            modifier = Modifier.fillMaxWidth(),
            value = value,  // 외부 value 사용
            onValueChange = onValueChange,  //  외부로 전달
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
            visualTransformation = if (isPassword) {  // 비밀번호 마스킹
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            decorationBox = { innerTextField ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 입력값이 비었을 때 placeholder 표시
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // 실제 입력 필드 렌더링
                innerTextField()
            },
        )
    }
}