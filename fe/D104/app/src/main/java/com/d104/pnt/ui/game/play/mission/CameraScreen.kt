package com.d104.pnt.ui.game.play.mission

import android.widget.Toast
import androidx.camera.core.ImageCapture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavArgs
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    onPhotoConfirmed: () -> Unit, // 사진 확인 완료 콜백
    modifier: Modifier = Modifier,
    compressionQuality: Int = 80, // 압축 품질 (0-100)
    maxWidth: Int = 1280,        // 최대 가로 해상도
    maxHeight: Int = 720,          // 최대 세로 해상도
    missionId: Long,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // ImageCapture 객체 저장
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    // 촬영된 사진 파일들
    var originalPhotoFile by remember { mutableStateOf<File?>(null) }
    var compressedPhotoFile by remember { mutableStateOf<File?>(null) }

    // 사진 저장 디렉토리
    val outputDirectory = remember {
        File(context.cacheDir, "mission_photos").apply {
            if (!exists()) mkdirs()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (compressedPhotoFile == null) {
            // === 촬영 모드 ===
            // 카메라 프리뷰
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                onImageCaptureReady = { capture ->
                    imageCapture = capture
                }
            )

            // 촬영 버튼 (하단 중앙)
            FloatingActionButton(
                onClick = {
                    imageCapture?.let { capture ->
                        takePicture(
                            imageCapture = capture,
                            outputDirectory = outputDirectory,
                            executor = ContextCompat.getMainExecutor(context),
                            onImageCaptured = { photoFile ->
                                // 촬영 성공 시 백그라운드에서 압축 처리
                                originalPhotoFile = photoFile

                                coroutineScope.launch {
                                    try {
                                        // IO 스레드에서 압축 작업
                                        val compressed = withContext(Dispatchers.IO) {
                                            compressImage(
                                                originalFile = photoFile,
                                                maxWidth = maxWidth,
                                                maxHeight = maxHeight,
                                                quality = compressionQuality
                                            )
                                        }

                                        // UI 업데이트
                                        compressedPhotoFile = compressed

                                        // 원본 파일 삭제 (압축본만 사용)
                                        originalPhotoFile?.delete()

                                    } catch (e: Exception) {
                                        Toast.makeText(
                                            context,
                                            "이미지 처리 실패: ${e.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        photoFile.delete()
                                    }
                                }
                            },
                            onError = { exception ->
                                Toast.makeText(
                                    context,
                                    "촬영 실패: ${exception.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .size(72.dp),
                shape = CircleShape,
                containerColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Default.Camera,
                    contentDescription = "사진 촬영",
                    tint = Color.Black,
                    modifier = Modifier.size(40.dp)
                )
            }
        } else {
            // === 미리보기 모드 ===
            PhotoPreviewScreen(
                photoFile = compressedPhotoFile!!,
                onConfirm = {
                    // 확인 버튼 클릭 시 - 압축된 파일을 서버로 전송
                    viewModel.uploadImage(compressedPhotoFile!!, missionId)
                    onPhotoConfirmed()
                },
                onCancel = {
                    // 취소 버튼 클릭 시 - 파일 삭제하고 다시 촬영
                    compressedPhotoFile?.delete()
                    originalPhotoFile?.delete()
                    compressedPhotoFile = null
                    originalPhotoFile = null
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
