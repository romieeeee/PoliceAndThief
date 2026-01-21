package com.d104.pnt.permission

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.d104.utils.helper.PermissionHelper
import com.google.accompanist.permissions.*
import timber.log.Timber

/**
 * 권한 요청 결과를 처리하는 Composable
 *
 * @param permissionType 요청할 권한 타입
 * @param onPermissionGranted 권한이 승인 콜백
 * @param onPermissionDenied 권한이 거부 콜백
 * @param showRationale 권한 설명을 보여줄지 여부
 * @param content UI 컨텐츠
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionHandler(
    permissionType: PermissionHelper.PermissionType,
    onPermissionGranted: () -> Unit = {},
    onPermissionDenied: () -> Unit = {},
    showRationale: Boolean = true,
    content: @Composable (PermissionState) -> Unit
) {
    val context = LocalContext.current

    // Android 버전에서 필요하지 않은 권한은 바로 승인으로 처리
    if (!permissionType.isRequired()) {
        LaunchedEffect(Unit) {
            onPermissionGranted()
        }
        return
    }

    // 단일 권한
    if (permissionType.permissions.size == 1) {
        val permissionState = rememberPermissionState(
            permission = permissionType.permissions[0]
        ) { granted ->
            if (granted) {
                Timber.d("${permissionType.name} permission granted")
                onPermissionGranted()
            } else {
                Timber.w("${permissionType.name} permission denied")
                onPermissionDenied()
            }
        }

        HandlePermissionState(
            permissionState = permissionState,
            permissionType = permissionType,
            showRationale = showRationale,
            content = content
        )
    }
    // 다중 권한
    else {
        val multiplePermissionsState = rememberMultiplePermissionsState(
            permissions = permissionType.permissions.toList()
        ) { permissionsMap ->
            val allGranted = permissionsMap.values.all { it }
            if (allGranted) {
                Timber.d("All ${permissionType.name} permissions granted")
                onPermissionGranted()
            } else {
                Timber.w("Some ${permissionType.name} permissions denied")
                onPermissionDenied()
            }
        }

        HandleMultiplePermissionsState(
            multiplePermissionsState = multiplePermissionsState,
            permissionType = permissionType,
            showRationale = showRationale
        )
    }
}

/**
 * 단일 권한 상태 처리
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun HandlePermissionState(
    permissionState: PermissionState,
    permissionType: PermissionHelper.PermissionType,
    showRationale: Boolean,
    content: @Composable (PermissionState) -> Unit
) {
    var showRationaleDialog by remember { mutableStateOf(false) }

    when {
        // 권한이 이미 승인됨
        permissionState.status.isGranted -> { }

        // 권한 설명을 보여주기
        permissionState.status.shouldShowRationale && showRationale -> {
            showRationaleDialog = true
        }

        // 권한 요청이 필요함
        else -> {
            LaunchedEffect(Unit) {
                permissionState.launchPermissionRequest()
            }
        }
    }

    // 권한 설명 다이얼로그
    if (showRationaleDialog) {
//        PermissionRationaleDialog(
//            permissionType = permissionType,
//            onConfirm = {
//                showRationaleDialog = false
//                permissionState.launchPermissionRequest()
//            },
//            onDismiss = {
//                showRationaleDialog = false
//            }
//        )
    }

    content(permissionState)
}

/**
 * 다중 권한 상태 처리
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun HandleMultiplePermissionsState(
    multiplePermissionsState: MultiplePermissionsState,
    permissionType: PermissionHelper.PermissionType,
    showRationale: Boolean
) {
    var showRationaleDialog by remember { mutableStateOf(false) }

    when {
        // 모든 권한이 승인됨
        multiplePermissionsState.allPermissionsGranted -> { }

        // 권한 설명 보여주기
        multiplePermissionsState.shouldShowRationale && showRationale -> {
            showRationaleDialog = true
        }

        // 권한 요청이 필요함
        else -> {
            LaunchedEffect(Unit) {
                multiplePermissionsState.launchMultiplePermissionRequest()
            }
        }
    }

    // 권한 설명 다이얼로그
    if (showRationaleDialog) {
//        PermissionRationaleDialog(
//            permissionType = permissionType,
//            onConfirm = {
//                showRationaleDialog = false
//                multiplePermissionsState.launchMultiplePermissionRequest()
//            },
//            onDismiss = {
//                showRationaleDialog = false
//            }
//        )
    }
}

/**
 * 모든 필수 권한을 한 번에 요청하는 Composable
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequestEssentialPermissions(
    onAllGranted: () -> Unit,
    onSomeDenied: () -> Unit
) {
    val essentialPermissions = remember {
        listOf(
            PermissionHelper.PermissionType.LOCATION,
            PermissionHelper.PermissionType.CAMERA,
            PermissionHelper.PermissionType.AUDIO,
            PermissionHelper.PermissionType.NOTIFICATION
        ).flatMap { it.permissions.toList() }
    }

    val multiplePermissionsState = rememberMultiplePermissionsState(
        permissions = essentialPermissions
    ) { permissionsMap ->
        if (permissionsMap.values.all { it }) {
            Timber.d("All essential permissions granted")
            onAllGranted()
        } else {
            Timber.w("Some essential permissions denied")
            onSomeDenied()
        }
    }

    LaunchedEffect(Unit) {
        if (!multiplePermissionsState.allPermissionsGranted) {
            multiplePermissionsState.launchMultiplePermissionRequest()
        } else {
            onAllGranted()
        }
    }
}

/**
 * 권한 상태를 감시하는 Hook
 */
@Composable
fun rememberPermissionState(
    permissionType: PermissionHelper.PermissionType
): State<Boolean> {
    val context = LocalContext.current
    return remember {
        derivedStateOf {
            PermissionHelper.isPermissionGranted(context, permissionType)
        }
    }
}