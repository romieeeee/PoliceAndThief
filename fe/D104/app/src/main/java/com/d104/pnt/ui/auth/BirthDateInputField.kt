package com.d104.pnt.ui.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.d104.pnt.ui.component.PixelInputField
import com.d104.pnt.ui.theme.BorderDefault

@Composable
fun BirthDateInputField(
    label: String,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(start = 10.dp, bottom = 4.dp),
            color = Color.White
        )

        PixelInputField(
            value = value,
            onValueChange = { newValue ->
                // 숫자만 추출 (최대 8자리)
                val digits = newValue.text.filter { it.isDigit() }.take(8)

                // YYYY.MM.DD 형식으로 포맷팅
                val formatted = buildString {
                    digits.forEachIndexed { index, char ->
                        append(char)
                        // 4번째 자리(년도 끝)와 6번째 자리(월 끝) 뒤에 점 추가
                        if (index == 3 || index == 5) {
                            if (index < digits.length - 1) {
                                append('.')
                            }
                        }
                    }
                }

                // 커서 위치 계산
                val oldText = value.text
                val oldCursor = value.selection.start

                // 새 커서 위치 계산
                val newCursor = when {
                    // 텍스트가 늘어난 경우
                    formatted.length > oldText.length -> {
                        // 점이 자동으로 추가된 경우 커서를 점 다음으로 이동
                        if (oldCursor == 4 || oldCursor == 7) {
                            oldCursor + 2
                        } else {
                            oldCursor + 1
                        }
                    }
                    // 텍스트가 줄어든 경우 (백스페이스)
                    formatted.length < oldText.length -> {
                        // 점을 지운 경우 점 앞의 숫자도 함께 지워지도록
                        if (oldCursor == 5 || oldCursor == 8) {
                            oldCursor - 2
                        } else {
                            oldCursor - 1
                        }
                    }
                    else -> oldCursor
                }.coerceIn(0, formatted.length)

                onValueChange(
                    TextFieldValue(
                        text = formatted,
                        selection = TextRange(newCursor)
                    )
                )
            },
            placeholder = "YYYY.MM.DD",
            keyboardType = KeyboardType.Number,
            borderColor = BorderDefault
        )
    }
}