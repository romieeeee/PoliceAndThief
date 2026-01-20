package com.d104.pnt.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.request.ImageRequest

@Composable
fun GifImage(
    modifier: Modifier = Modifier,
    @DrawableRes imageRes: Int
) {
    val context = LocalContext.current

    val imageLoader = ImageLoader.Builder(context)
        .components {
            add(GifDecoder.Factory())
        }
        .build()

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(imageRes)
            .build(),
        imageLoader = imageLoader,
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Crop
    )
}