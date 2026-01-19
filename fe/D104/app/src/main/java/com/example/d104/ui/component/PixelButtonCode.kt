package com.example.d104.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PixelButtonCode(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    mainColor: Color = Color.White,
    borderColor: Color = Color(0xFF2B2B2B),
    textColor: Color = borderColor,
    pixelSize: Dp = 4.dp,
    blockHeight: Int = 16,
    blockWidth: Int? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressOffset = if (isPressed) pixelSize else 0.dp
    val shadowVisibility = if (isPressed) Color.Transparent else borderColor

    val buttonHeight = pixelSize * blockHeight

    val sizeModifier = if (blockWidth != null) {
        modifier
            .width(pixelSize * blockWidth)
            .height(buttonHeight)
    } else {
        modifier
            .height(buttonHeight)
    }

    Box(
        modifier = sizeModifier
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
    ) {
        FiveLayerPixelShape(
            color = shadowVisibility,
            pixelUnit = pixelSize,
            baseHorizontalPadding = 0.dp,
            modifier = Modifier.offset(x = pixelSize, y = pixelSize)
        )

        FiveLayerPixelShape(
            color = borderColor,
            pixelUnit = pixelSize,
            baseHorizontalPadding = 0.dp,
            modifier = Modifier.offset(x = pressOffset, y = pressOffset)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = pressOffset, y = pressOffset)
                .padding(vertical = pixelSize)
        ) {
            FiveLayerPixelShape(
                color = mainColor,
                pixelUnit = pixelSize,
                baseHorizontalPadding = pixelSize,
                modifier = Modifier.fillMaxSize()
            )
        }

        Text(
            text = text,
            color = textColor,
            fontSize = (pixelSize.value * 5).sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = pressOffset, y = pressOffset)
        )
    }
}

@Composable
fun FiveLayerPixelShape(
    color: Color,
    pixelUnit: Dp,
    baseHorizontalPadding: Dp,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().height(pixelUnit).padding(horizontal = baseHorizontalPadding + (pixelUnit * 2)).background(color))
        Box(Modifier.fillMaxWidth().height(pixelUnit).padding(horizontal = baseHorizontalPadding + pixelUnit).background(color))
        Box(Modifier.fillMaxWidth().weight(1f).padding(horizontal = baseHorizontalPadding).background(color))
        Box(Modifier.fillMaxWidth().height(pixelUnit).padding(horizontal = baseHorizontalPadding + pixelUnit).background(color))
        Box(Modifier.fillMaxWidth().height(pixelUnit).padding(horizontal = baseHorizontalPadding + (pixelUnit * 2)).background(color))
    }
}
//
//@Preview(showBackground = true)
//@Composable
//fun PreviewWidthControl() {
//    Column(modifier = Modifier.padding(20.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
//
//        Text("기본 (blockWidth = null)")
//        PixelButtonCode(
//            text = "꽉 찬 버튼",
//            onClick = {},
//            modifier = Modifier.fillMaxWidth()
//        )
//
//        Box(Modifier.height(20.dp))
//
//        Text("고정 크기 (blockWidth = 40)")
//        PixelButtonCode(
//            text = "40칸",
//            onClick = {},
//            blockWidth = 40
//        )
//
//        Box(Modifier.height(20.dp))
//
//        Text("정사각형 (16x16)")
//        PixelButtonCode(
//            text = "OK",
//            onClick = {},
//            blockWidth = 16,
//            blockHeight = 16
//        )
//    }
//}