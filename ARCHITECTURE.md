# Police & Thief — Android Clean Architecture 패키지 구조

> **원칙**: domain 계층은 순수 Kotlin (Android 의존성 0), data가 domain을 구현, presentation이 domain을 참조
>
> **의존 방향**: `presentation` → `domain` ← `data`, `core`는 모든 계층에서 참조 가능

---

## 최종 패키지 구조

```
com.d104.pnt/
│
├── MainActivity.kt
│
│
│ ══════════════════════════════════════════════════════
│  core/ — 모든 계층에서 참조하는 앱 인프라
│  (Android 의존성 허용, 단 비즈니스 로직 없음)
│ ══════════════════════════════════════════════════════
│
├── core/
│   │
│   ├── app/
│   │   ├── BaseApplication.kt
│   │   └── Constants.kt
│   │
│   ├── network/
│   │   ├── AuthTokenInterceptor.kt
│   │   └── AuthEventBus.kt
│   │
│   ├── di/
│   │   ├── NetworkModule.kt
│   │   ├── DataStoreModule.kt
│   │   ├── RepositoryModule.kt          ← @Binds domain interface → data impl
│   │   └── SocketModule.kt
│   │
│   ├── service/
│   │   ├── GameActiveService.kt
│   │   ├── LocationService.kt
│   │   └── ChatMessagingService.kt
│   │
│   └── util/
│       ├── PermissionHelper.kt
│       ├── LocationServiceHelper.kt
│       ├── SoundPlayer.kt
│       └── QRHelper.kt
│
│
│ ══════════════════════════════════════════════════════
│  domain/ — 앱의 중심, 순수 Kotlin (Android 의존성 0)
│  다른 계층을 절대 참조하지 않음
│ ══════════════════════════════════════════════════════
│
├── domain/
│   │
│   ├── model/                               ← 순수 Kotlin 데이터 클래스
│   │   ├── Coordinates.kt                   ★ 신규: LatLng 대체 (lat: Double, lng: Double)
│   │   ├── GameRoleType.kt                  ★ 신규: GameRole에서 UI 속성 제거한 순수 enum
│   │   ├── PlayerStatus.kt                  ★ GameRole.kt에서 분리, Color 제거
│   │   ├── GameResult.kt                    ★ GameRole.kt에서 분리, Color 제거
│   │   ├── ChatMessage.kt
│   │   ├── ChatRoomData.kt
│   │   ├── ChatsData.kt
│   │   ├── CurrentGameRoomData.kt           ← LatLng → Coordinates로 교체
│   │   ├── GeoLocationInfo.kt
│   │   ├── Mission.kt
│   │   ├── NaverAddressDto.kt
│   │   ├── PlayerData.kt
│   │   ├── GameRoomModels.kt
│   │   └── RoomInfoResponse.kt
│   │
│   ├── state/                               ★ 게임 상태 머신 (sealed class / enum)
│   │   ├── GameSessionEvent.kt              ← GameSessionRepositoryImpl.kt 하단에서 분리
│   │   ├── WalkieConnectionState.kt         ← GameSessionRepositoryImpl.kt 하단에서 분리
│   │   ├── CctvPhase.kt                     ← GameSessionRepositoryImpl.kt 하단에서 분리
│   │   ├── HelicopterPhase.kt               ← GameSessionRepositoryImpl.kt 하단에서 분리
│   │   ├── MissionStatus.kt                 ← GameSessionRepositoryImpl.kt 하단에서 분리
│   │   └── ArrestStatus.kt                  ← GameSessionRepositoryImpl.kt 하단에서 분리
│   │
│   ├── repository/                          ★ 인터페이스만 (구현체 없음)
│   │   ├── AuthRepository.kt
│   │   ├── ChatRepository.kt
│   │   ├── GameRepository.kt
│   │   ├── GameRoomRepository.kt
│   │   ├── GameSessionRepository.kt
│   │   ├── ImageRepository.kt
│   │   ├── LocationRepository.kt            ← Location → Coordinates, LatLng → Coordinates
│   │   ├── ProfileRepository.kt
│   │   ├── ReportRepository.kt
│   │   └── WalkieRepository.kt
│   │
│   ├── usecase/                             ★ 신규: 비즈니스 로직 단위
│   │   ├── game/
│   │   │   ├── CctvPhaseCalculator.kt       ★ CCTV 사이클 상태 전이 로직
│   │   │   ├── HelicopterPhaseCalculator.kt ★ 헬기 스킬 상태 전이 로직
│   │   │   ├── ArrestThiefUseCase.kt        ★ 체포 요청 + 결과 상태 관리
│   │   │   ├── TrackGpsUseCase.kt           ★ GPS 전송 주기 관리
│   │   │   ├── UploadMissionUseCase.kt      ★ 미션 이미지 업로드 + 분석 흐름
│   │   │   └── ManageWalkieUseCase.kt       ★ PTT 연결/송수신/하트비트 조율
│   │   ├── auth/
│   │   │   ├── LoginUseCase.kt              ★ 로그인 + FCM 토큰 등록
│   │   │   └── SignupUseCase.kt             ★ 유효성 검증 + 회원가입 요청
│   │   └── location/
│   │       └── ReverseGeocodeUseCase.kt     ★ 좌표 → 주소 + 지역코드 변환
│   │
│   └── common/                              ← 순수 Kotlin 공통 타입
│       ├── BaseResult.kt                    ← domain/model/common/BaseResult.kt
│       └── ApiError.kt                      ← domain/model/common/ApiError.kt
│
│
│ ══════════════════════════════════════════════════════
│  data/ — domain 인터페이스의 실제 구현
│  domain을 참조함, presentation은 참조하지 않음
│ ══════════════════════════════════════════════════════
│
├── data/
│   │
│   ├── remote/
│   │   ├── api/                             ← Retrofit 서비스 (유지)
│   │   │   ├── AuthApiService.kt
│   │   │   ├── ChatApiService.kt
│   │   │   ├── GameApiService.kt
│   │   │   ├── GameRoomApiService.kt
│   │   │   ├── ImageApiService.kt
│   │   │   ├── NaverApiService.kt
│   │   │   ├── ProfileApiService.kt
│   │   │   └── ReportApiService.kt
│   │   │
│   │   └── dto/                             ← 네트워크 DTO (model → dto 리네이밍)
│   │       ├── request/
│   │       │   ├── ChatRequest.kt
│   │       │   ├── FcmTokenRequest.kt
│   │       │   ├── GameRoomRequest.kt
│   │       │   ├── KickRequest.kt
│   │       │   ├── LiveKitTokenRequest.kt
│   │       │   ├── LoginRequest.kt
│   │       │   ├── RefreshRequest.kt
│   │       │   ├── ReportRequest.kt
│   │       │   ├── SignupRequest.kt
│   │       │   ├── SocialLoginRequest.kt
│   │       │   ├── UpdateProfileRequest.kt
│   │       │   └── UpdateRoomSettingsRequest.kt
│   │       │
│   │       └── response/
│   │           ├── BaseResponse.kt
│   │           ├── BeepUseResponse.kt
│   │           ├── ChatResponse.kt
│   │           ├── ChatRoomMemberResponse.kt
│   │           ├── ChatRoomResponse.kt
│   │           ├── ErrorResponse.kt
│   │           ├── GameNewsResponse.kt
│   │           ├── GameResponse.kt
│   │           ├── GameResultResponse.kt
│   │           ├── GameRoomResponse.kt
│   │           ├── ImageResponse.kt
│   │           ├── JoinChatRoomResponse.kt
│   │           ├── LiveKitTokenResponse.kt
│   │           ├── LoginResponse.kt
│   │           ├── PoliceStatResponse.kt
│   │           ├── ProfileResponse.kt
│   │           ├── RefreshResponse.kt
│   │           ├── ReportResponse.kt
│   │           ├── SignupResponse.kt
│   │           └── ThiefStatResponse.kt
│   │
│   ├── socket/                              ← util/socket/ 에서 이동 (Remote Data Source)
│   │   ├── BaseSocketManager.kt
│   │   ├── GameSocketManager.kt
│   │   ├── RoomSocketManager.kt
│   │   ├── ChatSocketManager.kt
│   │   ├── GameSocketModels.kt              ← remote/model/response/ 에서 이동
│   │   └── MissionSocketDto.kt              ← remote/model/response/ 에서 이동
│   │
│   ├── local/
│   │   └── RegionCodeManager.kt
│   │
│   ├── sensor/
│   │   └── StepSensorManager.kt             ← util/StepSensorManager.kt
│   │
│   ├── mapper/                              ★ 신규: DTO ↔ Domain Model 변환
│   │   ├── AuthMapper.kt                    ★ LoginResponse → domain 토큰/유저 모델
│   │   ├── GameMapper.kt                    ★ GameSocketModels ↔ domain 모델
│   │   ├── LocationMapper.kt               ★ LatLng ↔ Coordinates, Location → Coordinates
│   │   ├── ChatMapper.kt                    ★ ChatResponse → ChatMessage
│   │   └── ProfileMapper.kt                ★ ProfileResponse → domain 프로필 모델
│   │
│   └── repository/                          ★ 구현체만 (인터페이스는 domain에)
│       ├── BaseRepository.kt                ← safeApiCall 유틸
│       ├── AuthRepositoryImpl.kt
│       ├── ChatRepositoryImpl.kt
│       ├── GameRepositoryImpl.kt
│       ├── GameRoomRepositoryImpl.kt
│       ├── GameSessionRepositoryImpl.kt     ← 비즈니스 로직은 domain/usecase로 이관
│       ├── ImageRepositoryImpl.kt
│       ├── LocationRepositoryImpl.kt
│       ├── ProfileRepositoryImpl.kt
│       ├── ReportRepositoryImpl.kt
│       └── WalkieRepositoryImpl.kt
│
│
│ ══════════════════════════════════════════════════════
│  presentation/ — UI 계층 (Feature-first 구조)
│  domain을 참조함, data는 직접 참조하지 않음
│ ══════════════════════════════════════════════════════
│
├── presentation/
│   │
│   ├── model/                               ★ UI 전용 모델
│   │   ├── UiState.kt                       ← domain/model/common/UiState.kt
│   │   ├── GameRole.kt                      ← domain/model/GameRole.kt (Color, R.drawable 포함)
│   │   └── DraggableLatLng.kt               ← domain/model/DraggableLatLng.kt (LatLng 사용)
│   │
│   ├── designsystem/                        ← 앱 전체 공유 UI 컴포넌트
│   │   ├── PixelContainer.kt
│   │   ├── PixelButtonCode.kt
│   │   ├── PixelIconButton.kt
│   │   ├── PixelInputField.kt
│   │   ├── PixelDropdown.kt
│   │   ├── PixelDropdownItem.kt
│   │   ├── PixelDashedDivider.kt
│   │   ├── PixelAlertDialog.kt
│   │   ├── PixelLoading.kt
│   │   ├── PixelMarker.kt
│   │   ├── CustomTextField.kt
│   │   ├── RoundedButton.kt
│   │   ├── OutlinedText.kt
│   │   ├── GifImage.kt
│   │   ├── ExpandableCard.kt
│   │   ├── CountDownUI.kt
│   │   ├── GoogleMaps.kt
│   │   ├── UserProfileCard.kt
│   │   ├── QRcodeContainer.kt
│   │   ├── QRcodeScanner.kt
│   │   ├── NetworkErrorScreen.kt
│   │   └── UnexpectedErrorScreen.kt
│   │
│   ├── theme/
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   └── Type.kt
│   │
│   ├── navigation/
│   │   ├── AppNavigation.kt
│   │   ├── BottomNavBar.kt
│   │   ├── BottomNavItem.kt
│   │   ├── NavArgs.kt
│   │   └── Routes.kt
│   │
│   ├── permission/
│   │   ├── PermissionDialog.kt
│   │   └── PermissionDeniedDialog.kt
│   │
│   ├── main/
│   │   ├── MainScreen.kt
│   │   └── MainViewModel.kt
│   │
│   ├── intro/
│   │   └── IntroScreen.kt
│   │
│   ├── auth/
│   │   ├── LoginScreen.kt
│   │   ├── LoginViewModel.kt
│   │   ├── SignupScreen.kt
│   │   ├── SignupViewModel.kt
│   │   ├── BirthDatePicker.kt
│   │   └── KaKaoLoginHelper.kt
│   │
│   ├── home/
│   │   ├── HomeScreen.kt
│   │   └── HomeViewModel.kt
│   │
│   ├── chat/
│   │   ├── list/
│   │   │   ├── ChatRoomListScreen.kt
│   │   │   ├── ChatRoomListHeader.kt
│   │   │   ├── ChatRoomListViewModel.kt
│   │   │   ├── RoleSelectDialog.kt
│   │   │   ├── RoomList.kt
│   │   │   └── RoomListItem.kt
│   │   ├── create/
│   │   │   ├── ChatRoomCreateScreen.kt
│   │   │   └── ChatRoomCreateViewModel.kt
│   │   └── room/
│   │       ├── ChatRoomScreen.kt
│   │       ├── ChatRoomViewModel.kt
│   │       ├── ChatRoomHeader.kt
│   │       ├── ChatRoomFooter.kt
│   │       ├── ChatRoomMemberDrawer.kt
│   │       ├── ChatBubble.kt
│   │       ├── Chats.kt
│   │       └── KickedNoticeDialog.kt
│   │
│   ├── game/
│   │   ├── component/                       ← 게임 전용 공통 UI
│   │   │   ├── AlertOverlay.kt
│   │   │   ├── ArrestOverlay.kt
│   │   │   ├── GameEndOverlay.kt
│   │   │   ├── WarningOverlay.kt
│   │   │   ├── GameTimer.kt
│   │   │   ├── Count.kt
│   │   │   ├── FlipImage.kt
│   │   │   └── PhoneFrame.kt
│   │   │
│   │   ├── create/
│   │   │   ├── GameCreateScreen.kt
│   │   │   ├── GameCreateViewModel.kt
│   │   │   ├── CounterControl.kt
│   │   │   ├── FactionRatioBar.kt
│   │   │   ├── GameGuideDialog.kt
│   │   │   ├── MapLoadDialog.kt
│   │   │   ├── MapSettingDialog.kt
│   │   │   ├── MapSettingViewModel.kt
│   │   │   └── SectionTitle.kt
│   │   │
│   │   ├── wait/
│   │   │   ├── GameRoomScreen.kt
│   │   │   ├── GameRoomViewModel.kt
│   │   │   ├── GameRoomBoard.kt
│   │   │   ├── GameRoomHeader.kt
│   │   │   ├── GameRoomDialog.kt
│   │   │   ├── GameRoomMapSettingDialog.kt
│   │   │   ├── GameSettingsDialog.kt
│   │   │   └── role/
│   │   │       ├── RoleSelectScreen.kt
│   │   │       ├── RoleSelectViewModel.kt
│   │   │       └── RoleCard.kt
│   │   │
│   │   ├── loading/
│   │   │   ├── GameLoadingScreen.kt
│   │   │   ├── GameLoadingViewModel.kt
│   │   │   ├── GameRoleScreen.kt
│   │   │   └── GameRoleViewModel.kt
│   │   │
│   │   ├── play/
│   │   │   ├── GamePlayScreen.kt
│   │   │   ├── GamePlayViewModel.kt
│   │   │   ├── GameFeedbackManager.kt
│   │   │   ├── mission/
│   │   │   │   ├── CameraScreen.kt
│   │   │   │   ├── CameraPreview.kt
│   │   │   │   ├── CameraViewModel.kt
│   │   │   │   ├── ImageCompressor.kt
│   │   │   │   ├── MissionBottomSheet.kt
│   │   │   │   └── PhotoPreviewScreen.kt
│   │   │   └── walkietalkie/
│   │   │       ├── WalkieTalkieScreen.kt
│   │   │       └── WalkietBottomSheet.kt
│   │   │
│   │   └── result/
│   │       ├── GameResultScreen.kt
│   │       ├── GameResultViewModel.kt
│   │       ├── ReportDialog.kt
│   │       └── news/
│   │           ├── GameNewsScreen.kt
│   │           ├── GameNewsViewModel.kt
│   │           ├── NewsAnchor.kt
│   │           ├── NewsLoadingScreen.kt
│   │           ├── NewsLoadingViewModel.kt
│   │           ├── NewsScriptBox.kt
│   │           ├── NewsTickerBar.kt
│   │           └── SkipConfirmationDialog.kt
│   │
│   └── profile/
│       ├── ProfileScreen.kt
│       ├── ProfileViewModel.kt
│       ├── ProfileCardSection.kt
│       ├── ProfileImageSelectionDialog.kt
│       ├── StatSummarySection.kt
│       └── TierGuideDialog.kt
```

---

## 의존성 규칙

```
┌──────────────┐
│ presentation │──→ domain ←── data
│  (Android)   │      ↑         ↑
└──────────────┘      │         │
                    core ←──────┘
                  (Android)
```

| 계층 | 참조 가능 | 참조 불가 | Android 의존성 |
|------|----------|----------|---------------|
| **domain** | 없음 (최상위) | data, presentation, core | **불가** (순수 Kotlin) |
| **data** | domain, core | presentation | 허용 |
| **presentation** | domain, core | data (DI로만 주입) | 허용 |
| **core** | domain | data, presentation | 허용 |

---

## domain 순수 Kotlin 보장을 위한 필수 변경

### 1. LatLng → Coordinates (domain 전용 좌표 타입)

```kotlin
// domain/model/Coordinates.kt ★ 신규
data class Coordinates(
    val latitude: Double,
    val longitude: Double
)
```

**영향 범위:**
- `CurrentGameRoomData.kt` — `prison: LatLng` → `prison: Coordinates`
- `LocationRepository.kt` — 파라미터/반환 타입 교체
- `DraggableLatLng.kt` → `presentation/model/`로 이동 (UI 전용)

**변환은 `data/mapper/LocationMapper.kt`에서:**
```kotlin
// data/mapper/LocationMapper.kt ★ 신규
fun LatLng.toCoordinates() = Coordinates(latitude, longitude)
fun Coordinates.toLatLng() = LatLng(latitude, longitude)
fun Location.toCoordinates() = Coordinates(latitude, longitude)
```

### 2. GameRole 분리 (domain 순수 enum + presentation UI enum)

```kotlin
// domain/model/GameRoleType.kt ★ 신규 — 순수 Kotlin
enum class GameRoleType(val roleNameKo: String, val roleNameEn: String) {
    POLICE("경찰", "POLICE"),
    THIEF("도둑", "THIEF"),
    ANY("랜덤", "ANY"),
    UNDECIDED("미정", "UNDECIDED");

    companion object {
        fun fromName(name: String): GameRoleType =
            values().find { it.name.equals(name, true) || it.roleNameEn.equals(name, true) }
                ?: UNDECIDED
    }
}

// presentation/model/GameRole.kt — Android/Compose 의존성 허용
enum class GameRole(
    val type: GameRoleType,    // domain 타입 참조
    val color: Color,
    val emoji: Int,
    val badge: Int
) {
    POLICE(GameRoleType.POLICE, CustomBlue, R.drawable.ic_police, R.drawable.ic_police_badge),
    THIEF(GameRoleType.THIEF, CustomRed, R.drawable.ic_thief, R.drawable.ic_thief_badge),
    // ...
}
```

### 3. UiState → presentation/model/

`UiState`는 UI 렌더링 상태이므로 domain이 아닌 presentation에 위치:

```kotlin
// presentation/model/UiState.kt
sealed class UiState<out T> {
    object Idle : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}
```

---

## 파일 이동 체크리스트 (전체 158개 + 신규 18개)

### core/ (13개)
| # | 현재 경로 | 이동 경로 |
|---|----------|----------|
| 1 | `base/BaseApplication.kt` | `core/app/BaseApplication.kt` |
| 2 | `base/Constants.kt` | `core/app/Constants.kt` |
| 3 | `base/AuthTokenInterceptor.kt` | `core/network/AuthTokenInterceptor.kt` |
| 4 | `util/AuthEventBus.kt` | `core/network/AuthEventBus.kt` |
| 5 | `di/NetworkModule.kt` | `core/di/NetworkModule.kt` |
| 6 | `di/DataStoreModule.kt` | `core/di/DataStoreModule.kt` |
| 7 | `di/RepositoryModule.kt` | `core/di/RepositoryModule.kt` |
| 8 | `di/SocketModule.kt` | `core/di/SocketModule.kt` |
| 9 | `service/game/GameActiveService.kt` | `core/service/GameActiveService.kt` |
| 10 | `service/location/LocationService.kt` | `core/service/LocationService.kt` |
| 11 | `service/notification/ChatMessagingService.kt` | `core/service/ChatMessagingService.kt` |
| 12 | `util/PermissionHelper.kt` | `core/util/PermissionHelper.kt` |
| 13 | `util/LocationServiceHelper.kt` | `core/util/LocationServiceHelper.kt` |
| 14 | `util/SoundPlayer.kt` | `core/util/SoundPlayer.kt` |
| 15 | `util/QRHelper.kt` | `core/util/QRHelper.kt` |

### domain/ (기존 이동 15개 + 신규 14개 = 29개)
| # | 현재 경로 | 이동 경로 | 비고 |
|---|----------|----------|------|
| 1 | — | `domain/model/Coordinates.kt` | ★ 신규 |
| 2 | — | `domain/model/GameRoleType.kt` | ★ 신규 |
| 3 | `domain/model/GameRole.kt` 중 `PlayerStatus` | `domain/model/PlayerStatus.kt` | ★ 분리, Color 제거 |
| 4 | `domain/model/GameRole.kt` 중 `GameResult` | `domain/model/GameResult.kt` | ★ 분리, Color 제거 |
| 5 | `domain/model/ChatMessage.kt` | `domain/model/ChatMessage.kt` | 유지 |
| 6 | `domain/model/ChatRoomData.kt` | `domain/model/ChatRoomData.kt` | 유지 |
| 7 | `domain/model/ChatsData.kt` | `domain/model/ChatsData.kt` | 유지 |
| 8 | `domain/model/CurrentGameRoomData.kt` | `domain/model/CurrentGameRoomData.kt` | LatLng→Coordinates |
| 9 | `domain/model/GeoLocationInfo.kt` | `domain/model/GeoLocationInfo.kt` | 유지 |
| 10 | `domain/model/Mission.kt` | `domain/model/Mission.kt` | 유지 |
| 11 | `domain/model/NaverAddressDto.kt` | `domain/model/NaverAddressDto.kt` | 유지 |
| 12 | `domain/model/PlayerData.kt` | `domain/model/PlayerData.kt` | 유지 |
| 13 | `domain/model/GameRoomModels.kt` | `domain/model/GameRoomModels.kt` | 유지 |
| 14 | `domain/model/RoomInfoResponse.kt` | `domain/model/RoomInfoResponse.kt` | 유지 |
| 15 | `domain/model/common/BaseResult.kt` | `domain/common/BaseResult.kt` | 이동 |
| 16 | `domain/model/common/ApiError.kt` | `domain/common/ApiError.kt` | 이동 |
| 17 | `GameSessionRepositoryImpl.kt` 하단 | `domain/state/GameSessionEvent.kt` | ★ 분리 |
| 18 | `GameSessionRepositoryImpl.kt` 하단 | `domain/state/WalkieConnectionState.kt` | ★ 분리 |
| 19 | `GameSessionRepositoryImpl.kt` 하단 | `domain/state/CctvPhase.kt` | ★ 분리 |
| 20 | `GameSessionRepositoryImpl.kt` 하단 | `domain/state/HelicopterPhase.kt` | ★ 분리 |
| 21 | `GameSessionRepositoryImpl.kt` 하단 | `domain/state/MissionStatus.kt` | ★ 분리 |
| 22 | `GameSessionRepositoryImpl.kt` 하단 | `domain/state/ArrestStatus.kt` | ★ 분리 |
| 23 | `data/repository/AuthRepository.kt` | `domain/repository/AuthRepository.kt` | ★ 이동 |
| 24 | `data/repository/ChatRepository.kt` | `domain/repository/ChatRepository.kt` | ★ 이동 |
| 25 | `data/repository/GameRepository.kt` | `domain/repository/GameRepository.kt` | ★ 이동 |
| 26 | `data/repository/GameRoomRepository.kt` | `domain/repository/GameRoomRepository.kt` | ★ 이동 |
| 27 | `data/repository/GameSessionRepository.kt` | `domain/repository/GameSessionRepository.kt` | ★ 이동 |
| 28 | `data/repository/ImageRepository.kt` | `domain/repository/ImageRepository.kt` | ★ 이동 |
| 29 | `data/repository/LocationRepository.kt` | `domain/repository/LocationRepository.kt` | ★ 이동, 타입 변경 |
| 30 | `data/repository/ProfileRepository.kt` | `domain/repository/ProfileRepository.kt` | ★ 이동 |
| 31 | `data/repository/ReportRepository.kt` | `domain/repository/ReportRepository.kt` | ★ 이동 |
| 32 | `data/repository/WalkieRepository.kt` | `domain/repository/WalkieRepository.kt` | ★ 이동 |
| 33 | — | `domain/usecase/game/CctvPhaseCalculator.kt` | ★ 신규 |
| 34 | — | `domain/usecase/game/HelicopterPhaseCalculator.kt` | ★ 신규 |
| 35 | — | `domain/usecase/game/ArrestThiefUseCase.kt` | ★ 신규 |
| 36 | — | `domain/usecase/game/TrackGpsUseCase.kt` | ★ 신규 |
| 37 | — | `domain/usecase/game/UploadMissionUseCase.kt` | ★ 신규 |
| 38 | — | `domain/usecase/game/ManageWalkieUseCase.kt` | ★ 신규 |
| 39 | — | `domain/usecase/auth/LoginUseCase.kt` | ★ 신규 |
| 40 | — | `domain/usecase/auth/SignupUseCase.kt` | ★ 신규 |
| 41 | — | `domain/usecase/location/ReverseGeocodeUseCase.kt` | ★ 신규 |

### data/ (기존 이동 49개 + 신규 5개 = 54개)
| # | 현재 경로 | 이동 경로 |
|---|----------|----------|
| 1~8 | `data/remote/api/*.kt` (8개) | `data/remote/api/*.kt` | 유지 |
| 9~20 | `data/remote/model/request/*.kt` (12개) | `data/remote/dto/request/*.kt` | 리네이밍 |
| 21~40 | `data/remote/model/response/*.kt` (20개, Socket 제외) | `data/remote/dto/response/*.kt` | 리네이밍 |
| 41 | `data/remote/model/response/GameSocketModels.kt` | `data/socket/GameSocketModels.kt` | 이동 |
| 42 | `data/remote/model/response/MissionSocketDto.kt` | `data/socket/MissionSocketDto.kt` | 이동 |
| 43 | `util/socket/BaseSocketManager.kt` | `data/socket/BaseSocketManager.kt` | 이동 |
| 44 | `util/socket/GameSocketManager.kt` | `data/socket/GameSocketManager.kt` | 이동 |
| 45 | `util/socket/RoomSocketManager.kt` | `data/socket/RoomSocketManager.kt` | 이동 |
| 46 | `util/socket/ChatSocketManager.kt` | `data/socket/ChatSocketManager.kt` | 이동 |
| 47 | `data/source/local/RegionCodeManager.kt` | `data/local/RegionCodeManager.kt` | 이동 |
| 48 | `util/StepSensorManager.kt` | `data/sensor/StepSensorManager.kt` | 이동 |
| 49 | — | `data/mapper/AuthMapper.kt` | ★ 신규 |
| 50 | — | `data/mapper/GameMapper.kt` | ★ 신규 |
| 51 | — | `data/mapper/LocationMapper.kt` | ★ 신규 |
| 52 | — | `data/mapper/ChatMapper.kt` | ★ 신규 |
| 53 | — | `data/mapper/ProfileMapper.kt` | ★ 신규 |
| 54 | `data/repository/BaseRepository.kt` | `data/repository/BaseRepository.kt` | 유지 |
| 55 | `data/repository/AuthRepositoryImpl.kt` | `data/repository/AuthRepositoryImpl.kt` | 유지 |
| 56 | `data/repository/ChatRepositoryImpl.kt` | `data/repository/ChatRepositoryImpl.kt` | 유지 |
| 57 | `data/repository/GameRepositoryImpl.kt` | `data/repository/GameRepositoryImpl.kt` | 유지 |
| 58 | `data/repository/GameRoomRepositoryImpl.kt` | `data/repository/GameRoomRepositoryImpl.kt` | 유지 |
| 59 | `data/repository/GameSessionRepositoryImpl.kt` | `data/repository/GameSessionRepositoryImpl.kt` | 축소 |
| 60 | `data/repository/ImageRepositoryImpl.kt` | `data/repository/ImageRepositoryImpl.kt` | 유지 |
| 61 | `data/repository/LocationRepositoryImpl.kt` | `data/repository/LocationRepositoryImpl.kt` | 유지 |
| 62 | `data/repository/ProfileRepositoryImpl.kt` | `data/repository/ProfileRepositoryImpl.kt` | 유지 |
| 63 | `data/repository/ReportRepositoryImpl.kt` | `data/repository/ReportRepositoryImpl.kt` | 유지 |
| 64 | `data/repository/WalkieRepositoryImpl.kt` | `data/repository/WalkieRepositoryImpl.kt` | 유지 |

### presentation/ (기존 이동 88개)
| # | 현재 경로 | 이동 경로 |
|---|----------|----------|
| 1 | `domain/model/common/UiState.kt` | `presentation/model/UiState.kt` |
| 2 | `domain/model/GameRole.kt` (UI 속성) | `presentation/model/GameRole.kt` |
| 3 | `domain/model/DraggableLatLng.kt` | `presentation/model/DraggableLatLng.kt` |
| 4~25 | `ui/component/*.kt` (22개, 범용) | `presentation/designsystem/*.kt` |
| 26~28 | `ui/theme/*.kt` (3개) | `presentation/theme/*.kt` |
| 29~33 | `navigation/*.kt` (5개) | `presentation/navigation/*.kt` |
| 34~35 | `permission/*.kt` (2개) | `presentation/permission/*.kt` |
| 36~37 | `ui/MainScreen.kt`, `MainViewModel.kt` | `presentation/main/*.kt` |
| 38 | `IntroScreen.kt` | `presentation/intro/IntroScreen.kt` |
| 39~43 | `ui/auth/*.kt` (5개) | `presentation/auth/*.kt` |
| 44 | `util/KaKaoLoginHelper.kt` | `presentation/auth/KaKaoLoginHelper.kt` |
| 45~46 | `ui/home/*.kt` (2개) | `presentation/home/*.kt` |
| 47~52 | `ui/chatroomlist/*.kt` (4개) + RoomList, RoomListItem | `presentation/chat/list/*.kt` |
| 53~54 | `ui/chatroom/create/*.kt` (2개) | `presentation/chat/create/*.kt` |
| 55~61 | `ui/chatroom/chat/*.kt` (7개) | `presentation/chat/room/*.kt` |
| 62 | `ui/component/KickedNoticeDialog.kt` | `presentation/chat/room/KickedNoticeDialog.kt` |
| 63~70 | 게임 전용 오버레이 + play UI (8개) | `presentation/game/component/*.kt` |
| 71~79 | `ui/game/create/*.kt` (9개) | `presentation/game/create/*.kt` |
| 80~86 | `ui/game/wait/*.kt` (7개) | `presentation/game/wait/*.kt` |
| 87~89 | `ui/game/wait/role/*.kt` (3개) | `presentation/game/wait/role/*.kt` |
| 90~91 | `ui/game/load/*.kt` (2개) | `presentation/game/loading/*.kt` |
| 92~93 | `ui/game/play/GameRoleScreen.kt`, `GameRoleViewModel.kt` | `presentation/game/loading/*.kt` |
| 94~95 | `ui/game/play/GamePlayScreen.kt`, `GamePlayViewModel.kt` | `presentation/game/play/*.kt` |
| 96 | `util/GameFeedbackManager.kt` | `presentation/game/play/GameFeedbackManager.kt` |
| 97~102 | `ui/game/play/mission/*.kt` (6개) | `presentation/game/play/mission/*.kt` |
| 103~104 | `ui/game/play/walkietalkie/*.kt` (2개) | `presentation/game/play/walkietalkie/*.kt` |
| 105~107 | `ui/game/end/*.kt` (3개) | `presentation/game/result/*.kt` |
| 108~115 | `ui/game/end/news/*.kt` (8개) | `presentation/game/result/news/*.kt` |
| 116~121 | `ui/profile/*.kt` (6개) | `presentation/profile/*.kt` |

---

## 정량 비교

| | 현재 | 제안 |
|---|---|---|
| 최상위 패키지 수 | 9개 (base, data, di, domain, navigation, permission, service, ui, util) | **4개** (core, domain, data, presentation) |
| domain Android 의존 파일 | 3개 (GameRole, CurrentGameRoomData, DraggableLatLng) | **0개** |
| repository 인터페이스 위치 | `data/repository/` (구현체와 혼재) | **`domain/repository/`** (분리) |
| 비즈니스 로직 위치 | `GameSessionRepositoryImpl` (830라인) | **`domain/usecase/`** (9개 UseCase) |
| 게임 상태 enum 위치 | `GameSessionRepositoryImpl.kt` 하단 | **`domain/state/`** (6개 파일) |
| 소켓 매니저 위치 | `util/socket/` | **`data/socket/`** |
| DTO 명명 | `data/remote/model/` | **`data/remote/dto/`** |
| 총 파일 수 | 158개 | **176개** (신규 18개 포함) |

이 구조를 그대로 새 프로젝트 세팅 시 템플릿으로 사용하면 됩니다. `domain/` 계층이 순수 Kotlin으로 격리되어 있어 KMP(Kotlin Multiplatform) 전환도 가능합니다.

<function_calls>
<invoke name="TodoWrite">
<parameter name="todos">[{"activeForm": "Creating final architecture structure", "content": "Create definitive Clean Architecture package structure", "status": "completed"}, {"activeForm": "Committing and pushing", "content": "Commit and push ARCHITECTURE.md to branch", "status": "in_progress"}]