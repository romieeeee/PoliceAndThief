package com.d104.pnt.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun PixelInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    errorMessage: String = "",
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    backgroundColor: Color = Color.White,
    borderColor: Color = Color.White,
    singleLine: Boolean = true,
    enabled: Boolean = true,
) {
    Column(modifier = modifier) {
        PixelContainer(
            modifier = Modifier.fillMaxWidth(),
            borderWidth = 10f,
            cornerSize = 20f,
            backgroundColor = backgroundColor,
            borderColor = borderColor
        ) {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = singleLine,
                    maxLines = if (singleLine) 1 else Int.MAX_VALUE,
                    minLines = if (singleLine) 1 else 4,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.Black),
                    cursorBrush = SolidColor(Color.Black),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = keyboardType
                    ),
                    visualTransformation = if (isPassword) {
                        PasswordVisualTransformation()
                    } else {
                        VisualTransformation.None
                    }
                )

                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                }
            }
        }

        if (errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage,
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}