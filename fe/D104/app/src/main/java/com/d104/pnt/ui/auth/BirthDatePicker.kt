package com.d104.pnt.ui.auth


import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.commandiron.wheel_picker_compose.WheelDatePicker
import com.d104.pnt.ui.component.PixelAlertDialog
import com.d104.pnt.ui.component.PixelContainer
import com.d104.pnt.ui.component.PixelInputField
import com.d104.pnt.ui.theme.AccentRed
import com.d104.pnt.ui.theme.BorderDefault
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicReference


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BirthDatePicker(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White
) {
    var showDatePicker by remember { mutableStateOf(false) }

    val initialDate = remember(value) {
        try {
            if (value.isNotEmpty()) {
                val parts = value.split(".")
                LocalDate.of(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
            } else LocalDate.of(2000, 1, 1)
        } catch (e: Exception) {
            LocalDate.of(2000, 1, 1)
        }
    }

    val snappedDateRef = remember { AtomicReference(initialDate) }

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(start = 10.dp, bottom = 4.dp),
            color = Color.White
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    snappedDateRef.set(initialDate)
                    showDatePicker = true
                }
        ) {
            PixelInputField(
                value = value,
                onValueChange = { },
                placeholder = "YYYY.MM.DD",
                borderColor = BorderDefault,
                backgroundColor = backgroundColor,
                modifier = Modifier.fillMaxWidth(),
                enabled = false
            )
        }
    }

    if (showDatePicker) {
        Dialog(
            onDismissRequest = { showDatePicker = false }
        ) {
            PixelAlertDialog(
                title = "생년월일 선택",
                message = "태어난 날짜를 선택해주세요.",
                borderColor = Color.Black,
                buttonContent = {
                    WheelDatePicker(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        startDate = initialDate,
                        minDate = LocalDate.of(1900, 1, 1),
                        maxDate = LocalDate.now(),
                        yearsRange = IntRange(1900, LocalDate.now().year),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        textColor = Color.White,
                        onSnappedDate = { snapped ->
                            snappedDateRef.set(snapped)
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        PixelContainer(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(
                                    onClick = { showDatePicker = false },
                                ),
                            backgroundColor = Color.Gray,
                            borderColor = Color.Black,
                        ) {
                            Text(
                                text = "취소",
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                            )
                        }

                        PixelContainer(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(
                                    onClick = {
                                        val finalDate = snappedDateRef.get()
                                        val formatted =
                                            finalDate.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
                                        onValueChange(formatted)
                                        showDatePicker = false
                                    }
                                ),
                            backgroundColor = AccentRed,
                            borderColor = Color.Black,
                        ) {
                            Text(
                                text = "확인",
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                            )
                        }
                    }
                }
            )
        }
    }
}