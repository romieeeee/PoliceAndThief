package com.d104.pnt.ui.auth


import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.commandiron.wheel_picker_compose.WheelDatePicker
import com.d104.pnt.ui.component.PixelInputField
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
    modifier: Modifier = Modifier
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
                modifier = Modifier.fillMaxWidth(),
                enabled = false
            )
        }
    }

    if (showDatePicker) {
        AlertDialog(
            onDismissRequest = { showDatePicker = false },
            title = {
                Text(
                    text = "생년월일 선택",
                    style = MaterialTheme.typography.titleMedium,
                )
            },
            text = {
                key(showDatePicker) {
                    WheelDatePicker(
                        startDate = initialDate,
                        minDate = LocalDate.of(1900, 1, 1),
                        maxDate = LocalDate.now(),
                        yearsRange = IntRange(1900, LocalDate.now().year),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        textColor = Color.White,
                        onSnappedDate = { snapped -> snappedDateRef.set(snapped) }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val finalDate = snappedDateRef.get()
                        val formatted = finalDate.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
                        onValueChange(formatted)
                        showDatePicker = false
                    }
                ) {
                    Text("확인")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("취소")
                }
            }
        )
    }
}