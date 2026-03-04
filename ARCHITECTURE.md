# Android Clean Architecture 프로젝트 세팅 매뉴얼

> **대상**: Jetpack Compose + Hilt + Coroutines/Flow 기반 Android 프로젝트
> **목표**: 새 프로젝트를 시작할 때 이 문서만 보고 패키지 구조를 잡을 수 있게 하는 것

---

## 1. 전체 구조 한눈에 보기

```
com.example.app/
│
├── core/           ← 앱 인프라 (DI, 네트워크 설정, 유틸)
├── domain/         ← 비즈니스 로직의 중심 (순수 Kotlin, Android 의존성 0)
├── data/           ← 외부 세계와의 통신 (API, DB, Socket, 센서)
└── presentation/   ← 사용자가 보고 만지는 화면 (Compose UI)
```

### 의존성 방향 (절대 규칙)

```
presentation  ──→  domain  ←──  data
     │                ↑           │
     └──→  core  ←────┘──────────┘
```

- **domain은 아무것도 참조하지 않는다** (최상위 계층)
- data는 domain을 참조한다 (인터페이스 구현)
- presentation은 domain을 참조한다 (UseCase 호출)
- **presentation은 data를 직접 참조하지 않는다** (DI가 연결해줌)
- core는 모든 계층에서 참조 가능

---

## 2. 각 계층 상세 설명

---

### 2-1. domain/ — 앱의 심장

```
domain/
├── model/          ← 데이터 클래스 (Entity)
├── repository/     ← 인터페이스만 (구현체 없음)
├── usecase/        ← 비즈니스 로직 단위
├── state/          ← 상태 머신용 Sealed Class, Enum
└── common/         ← Result 래퍼, 에러 타입
```

#### 왜 domain이 최상위인가?

**domain은 "이 앱이 무엇을 하는가"를 정의하는 계층이다.**

서버가 REST에서 GraphQL로 바뀌어도, UI가 Compose에서 XML로 바뀌어도,
domain 코드는 한 줄도 바뀌지 않아야 한다.
그래서 Android import가 하나도 없어야 한다.

#### domain/model/ — 순수 Kotlin 데이터 클래스

```kotlin
// ✅ 올바른 domain model
data class Coordinates(
    val latitude: Double,
    val longitude: Double
)

data class GameRoom(
    val id: Long,
    val code: String,
    val maxPlayers: Int,
    val prison: Coordinates,         // ← LatLng가 아닌 순수 타입
    val boundary: List<Coordinates>
)

// ❌ domain model에 들어가면 안 되는 것
data class GameRole(
    val color: Color,               // ← Compose 의존
    val icon: Int                   // ← R.drawable 의존
)
// → 이건 presentation/model/로 가야 한다
```

**판별 기준**: `import android.*`, `import androidx.*`, `import com.google.android.*`가 있으면 domain이 아니다.

#### domain/repository/ — 인터페이스만

```kotlin
// domain/repository/AuthRepository.kt
interface AuthRepository {
    fun getAccessToken(): Flow<String>
    suspend fun login(email: String, password: String): BaseResult<TokenPair>
    suspend fun signup(request: SignupParam): BaseResult<Unit>
}
```

**왜 인터페이스가 domain에 있는가?**

Repository 인터페이스는 "이 앱에 어떤 데이터 조작이 필요한가"를 선언한다.
이것은 비즈니스 요구사항이지, 구현 세부사항이 아니다.

- "로그인이 필요하다" → domain이 선언
- "Retrofit으로 POST /auth/login을 호출한다" → data가 구현

```
domain/repository/AuthRepository.kt       ← "무엇이 필요한가" (인터페이스)
data/repository/AuthRepositoryImpl.kt     ← "어떻게 구현하는가" (구현체)
core/di/RepositoryModule.kt               ← "둘을 연결해준다" (Hilt @Binds)
```

#### domain/usecase/ — 비즈니스 로직의 실체

```kotlin
// domain/usecase/game/CctvPhaseCalculator.kt
class CctvPhaseCalculator {

    fun calculate(gameTime: Int, interval: Int): CctvPhase {
        if (interval == 0 || gameTime <= 100) return CctvPhase.IDLE
        val cyclePosition = gameTime % (interval * 60)
        return when {
            cyclePosition < WARNING_TIME -> CctvPhase.NOTIFY
            cyclePosition < WARNING_TIME + CCTV_TIME -> CctvPhase.REVEAL
            else -> CctvPhase.IDLE
        }
    }

    companion object {
        private const val WARNING_TIME = 5
        private const val CCTV_TIME = 10
    }
}
```

**왜 UseCase를 분리하는가?**

| UseCase 없이 (현재) | UseCase 있을 때 |
|---------------------|----------------|
| GameSessionRepositoryImpl 830라인 | Repository 400라인 + UseCase 6개 × 70라인 |
| CCTV 로직 테스트 불가 (소켓 의존) | `CctvPhaseCalculator` 단독 테스트 가능 |
| 헬기 로직 변경 시 Repository 전체 재배포 | `HelicopterPhaseCalculator`만 수정 |
| ViewModel이 Repository 40개 StateFlow 구독 | UseCase가 필요한 Flow만 조합해서 제공 |

**UseCase 도입 기준** (모든 Repository에 다 만들 필요 없음):
1. Repository 클래스가 300라인을 넘을 때
2. 같은 로직을 여러 ViewModel이 사용할 때
3. 비즈니스 규칙이 단위 테스트가 필요할 때
4. 여러 Repository를 조합하는 로직이 있을 때

단순 CRUD (프로필 조회, 신고 등)는 UseCase 없이 ViewModel → Repository 직접 호출해도 된다.

#### domain/state/ — 상태 정의

```kotlin
// domain/state/CctvPhase.kt
enum class CctvPhase { IDLE, NOTIFY, REVEAL }

// domain/state/GameSessionEvent.kt
sealed class GameSessionEvent {
    data class GameStarted(val gameId: Long, val startTime: String) : GameSessionEvent()
    data class NavigateToLoading(val gameId: Long) : GameSessionEvent()
    data class NavigateToNews(val gameId: Long, val newsId: Long) : GameSessionEvent()
}
```

**왜 별도 파일/패키지인가?**

현재 `GameSessionRepositoryImpl.kt` 파일 하단에 6개의 sealed class/enum이 모여있다.
Repository 구현체 안에 있으면:
- 다른 계층에서 참조할 때 `data.repository` 패키지를 import해야 함 (계층 위반)
- RepositoryImpl 파일을 열어야 상태 정의를 볼 수 있음
- 테스트에서 불필요한 의존성이 생김

`domain/state/`에 있으면 모든 계층이 자연스럽게 참조 가능.

#### domain/common/ — 공통 타입

```kotlin
// domain/common/BaseResult.kt
sealed class BaseResult<out T> {
    data class Success<T>(val data: T) : BaseResult<T>()
    data class Error(val error: ApiError) : BaseResult<Nothing>()
}
```

`BaseResult`와 `ApiError`는 모든 계층에서 사용하지만,
UI 렌더링 상태가 아니라 **데이터 처리 결과**이므로 domain에 위치한다.

반면 `UiState`(Idle/Loading/Success/Error)는 **화면 렌더링 상태**이므로 presentation에 위치한다.

| 타입 | 위치 | 이유 |
|------|------|------|
| `BaseResult<T>` | domain/common | 데이터 성공/실패 (모든 계층에서 사용) |
| `ApiError` | domain/common | 에러 정보 (비즈니스 의미) |
| `UiState<T>` | presentation/model | 화면 렌더링 상태 (UI에서만 사용) |

---

### 2-2. data/ — 외부 세계와의 통신

```
data/
├── remote/
│   ├── api/            ← Retrofit 서비스 인터페이스
│   └── dto/            ← 네트워크 Request/Response 모델
│       ├── request/
│       └── response/
├── socket/             ← Socket.IO 매니저 + 소켓 DTO
├── local/              ← SharedPreferences, DataStore, Room, CSV 등
├── sensor/             ← 만보기, GPS 등 하드웨어 센서
├── mapper/             ← DTO ↔ Domain Model 변환
└── repository/         ← domain/repository 인터페이스의 구현체
```

#### data/remote/dto/ — 왜 model이 아니라 dto인가

```
data/remote/dto/request/LoginRequest.kt     ← 서버에 보내는 형태
data/remote/dto/response/LoginResponse.kt   ← 서버에서 오는 형태
domain/model/TokenPair.kt                   ← 앱 내부에서 쓰는 형태
```

서버 API가 바뀌면 DTO만 수정하고, domain model은 그대로 유지할 수 있다.
`model`이라고 부르면 `domain/model/`과 혼동되므로 `dto`가 역할을 더 명확히 드러낸다.

#### data/socket/ — 왜 util이 아닌가

소켓 매니저는 **서버와 데이터를 주고받는 Data Source**다.
`util`은 "어디서든 쓰는 순수 유틸리티"를 의미하는데,
`GameSocketManager`는 게임 서버 `/game` 네임스페이스에 특화된 데이터 소스이므로
`data/socket/`이 정확하다.

Retrofit의 `AuthApiService`가 `data/remote/api/`에 있는 것과 같은 논리:

| 통신 방식 | 위치 | 역할 |
|-----------|------|------|
| REST API | `data/remote/api/` | HTTP 요청/응답 |
| WebSocket | `data/socket/` | 실시간 이벤트 송수신 |
| 로컬 DB | `data/local/` | 디스크 읽기/쓰기 |
| 센서 | `data/sensor/` | 하드웨어 데이터 수집 |

#### data/mapper/ — 언제 만드는가

**즉시 필요한 mapper:**
```kotlin
// data/mapper/LocationMapper.kt
fun LatLng.toCoordinates() = Coordinates(latitude, longitude)
fun Coordinates.toLatLng() = LatLng(latitude, longitude)
fun Location.toCoordinates() = Coordinates(latitude, longitude)
```
→ domain에서 Android `LatLng`를 제거하려면 반드시 필요

**나중에 만들어도 되는 mapper:**
```kotlin
// data/mapper/AuthMapper.kt
fun LoginResponse.toTokenPair() = TokenPair(accessToken, refreshToken)
```
→ DTO와 domain model이 실제로 달라지는 시점에 추가

**규칙**: DTO를 domain model로 직접 쓰고 있어도 동작에 문제가 없으면
mapper를 미리 만들 필요 없다. 과도한 추상화는 오히려 해가 된다.

#### data/repository/ — 구현체만

```kotlin
// data/repository/AuthRepositoryImpl.kt
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApiService: AuthApiService,
    private val dataStore: DataStore<Preferences>,
    private val authMapper: AuthMapper    // ← mapper 주입
) : AuthRepository {                      // ← domain 인터페이스 구현

    override suspend fun login(email: String, password: String): BaseResult<TokenPair> {
        return safeApiCall {
            authApiService.login(LoginRequest(email, password))
        }
    }
}
```

---

### 2-3. presentation/ — 화면

```
presentation/
├── model/              ← UI 전용 모델 (UiState, GameRole with Color/Drawable)
├── designsystem/       ← 공통 UI 컴포넌트 (PixelButton, PixelContainer 등)
├── theme/              ← Color, Typography, Theme
├── navigation/         ← 라우트 정의, NavHost, BottomNav
├── permission/         ← 권한 요청 다이얼로그
│
├── main/               ← MainScreen + MainViewModel
├── intro/              ← 인트로 화면
├── auth/               ← 로그인, 회원가입
├── home/               ← 홈 화면
├── chat/               ← 채팅 (list / create / room)
├── game/               ← 게임 (create / wait / loading / play / result)
│   ├── component/      ← 게임 전용 공통 UI (AlertOverlay, ArrestOverlay 등)
│   ├── create/
│   ├── wait/
│   │   └── role/
│   ├── loading/
│   ├── play/
│   │   ├── mission/
│   │   └── walkietalkie/
│   └── result/
│       └── news/
└── profile/            ← 프로필
```

#### Feature-first vs Layer-first

```
❌ Layer-first (파일 찾기 어려움)
presentation/
├── screen/
│   ├── GamePlayScreen.kt
│   ├── GameResultScreen.kt
│   ├── LoginScreen.kt
│   └── ChatRoomScreen.kt
├── viewmodel/
│   ├── GamePlayViewModel.kt
│   ├── GameResultViewModel.kt
│   └── LoginViewModel.kt
└── component/
    ├── GameTimer.kt
    └── ChatBubble.kt

✅ Feature-first (관련 파일이 한 곳에)
presentation/
├── game/
│   ├── play/
│   │   ├── GamePlayScreen.kt
│   │   └── GamePlayViewModel.kt
│   └── result/
│       ├── GameResultScreen.kt
│       └── GameResultViewModel.kt
├── auth/
│   ├── LoginScreen.kt
│   └── LoginViewModel.kt
└── chat/
    └── room/
        ├── ChatRoomScreen.kt
        └── ChatBubble.kt
```

**Feature-first를 쓰는 이유:**
- "게임 플레이" 기능을 수정할 때 `presentation/game/play/` 폴더 하나만 보면 된다
- 새 기능 추가 시 새 폴더 하나를 만들면 끝
- 파일 수가 100개가 넘어도 혼란이 없다

#### presentation/model/ — UI 전용 모델

```kotlin
// presentation/model/UiState.kt
sealed class UiState<out T> {
    object Idle : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

// presentation/model/GameRole.kt — Android 의존 OK
enum class GameRole(
    val type: GameRoleType,     // ← domain 타입 참조
    val color: Color,           // ← Compose Color (Android 의존)
    @DrawableRes val emoji: Int // ← R.drawable (Android 의존)
) {
    POLICE(GameRoleType.POLICE, CustomBlue, R.drawable.ic_police),
    THIEF(GameRoleType.THIEF, CustomRed, R.drawable.ic_thief),
    // ...
}
```

#### presentation/designsystem/ — 왜 component가 아닌가

`component`라고 부르면 모든 Composable이 다 들어가게 된다.
`designsystem`이라고 부르면 **앱 전체에서 재사용되는 기본 UI 블록**만 들어간다:

| 폴더 | 들어가는 것 | 안 들어가는 것 |
|------|-----------|-------------|
| `designsystem/` | PixelButton, PixelContainer, PixelInput | ArrestOverlay, ChatBubble |
| `game/component/` | AlertOverlay, ArrestOverlay, WarningOverlay | PixelButton |
| `chat/room/` | ChatBubble, ChatRoomHeader | PixelButton |

기준: **2개 이상의 Feature에서 사용되면** designsystem, **하나의 Feature에서만 사용되면** 해당 Feature 폴더.

---

### 2-4. core/ — 앱 인프라

```
core/
├── app/            ← Application 클래스, 상수
├── network/        ← OkHttp Interceptor, AuthEventBus
├── di/             ← Hilt Module (Network, Repository, Socket, DataStore)
├── service/        ← Android Service (Foreground, FCM)
└── util/           ← 순수 유틸리티 (Permission, Sound, QR)
```

**core에 들어가는 기준:**
1. 특정 Feature에 속하지 않는다
2. 여러 계층에서 참조해야 한다
3. Android 인프라 설정이다

**core에 들어가면 안 되는 것:**
- 비즈니스 로직 → domain
- UI 컴포넌트 → presentation
- API 호출 → data

---

## 3. 데이터 흐름 예시

### 로그인 흐름

```
[사용자가 로그인 버튼 탭]
    │
    ▼
presentation/auth/LoginViewModel.kt
    │  loginUseCase.execute(email, password)
    ▼
domain/usecase/auth/LoginUseCase.kt
    │  authRepository.login(email, password)
    ▼
domain/repository/AuthRepository.kt          ← 인터페이스 (domain 계층)
    │  (Hilt가 AuthRepositoryImpl을 주입)
    ▼
data/repository/AuthRepositoryImpl.kt        ← 구현체 (data 계층)
    │  authApiService.login(LoginRequest(...))
    ▼
data/remote/api/AuthApiService.kt
    │  POST /auth/login
    ▼
[서버 응답]
    │
    ▼
data/repository/AuthRepositoryImpl.kt
    │  mapper로 DTO → domain model 변환
    │  BaseResult.Success(tokenPair)
    ▼
domain/usecase/auth/LoginUseCase.kt
    │  FCM 토큰 등록 등 후처리
    │  BaseResult.Success(tokenPair)
    ▼
presentation/auth/LoginViewModel.kt
    │  _uiState.value = UiState.Success
    ▼
presentation/auth/LoginScreen.kt
    │  when (uiState) { Success → 홈으로 이동 }
    ▼
[화면 전환]
```

### 게임 플레이 실시간 GPS 흐름

```
[1초마다 GPS 전송]
    │
    ▼
presentation/game/play/GamePlayViewModel.kt
    │  trackGpsUseCase.start()
    ▼
domain/usecase/game/TrackGpsUseCase.kt
    │  locationRepository.currentLocation (Flow 구독)
    │  gameSessionRepository.sendGps(coordinates)
    ▼
data/repository/GameSessionRepositoryImpl.kt
    │  locationMapper.toLatLng(coordinates)
    │  gameSocketManager.sendGPS(lat, lng, walk)
    ▼
data/socket/GameSocketManager.kt
    │  socket.emit("post gps", data)
    ▼
[서버]

[서버에서 GPS 수신]
    │
    ▼
data/socket/GameSocketManager.kt
    │  "get gps" 이벤트 수신
    │  _events.emit(GameSocketEvent.GpsUpdate(...))
    ▼
data/repository/GameSessionRepositoryImpl.kt
    │  gameSocketManager.events.collect
    │  gameMapper.toMemberLocations(dto)
    │  _memberLocations.value = domainModel
    ▼
domain/usecase/game/CctvPhaseCalculator.kt
    │  cctvPhase = calculator.calculate(gameTime, interval)
    ▼
presentation/game/play/GamePlayViewModel.kt
    │  gameSessionRepository.memberLocations.collect
    │  cctvPhaseCalculator로 UI 상태 결정
    ▼
presentation/game/play/GamePlayScreen.kt
    │  지도에 마커 갱신 + CCTV 오버레이 표시
```

---

## 4. 새 프로젝트 세팅 순서

### Step 1: 빈 패키지 구조 생성

```bash
# 프로젝트 루트/app/src/main/java/com/example/app/ 하위
mkdir -p core/{app,network,di,service,util}
mkdir -p domain/{model,repository,usecase,state,common}
mkdir -p data/{remote/{api,dto/{request,response}},socket,local,sensor,mapper,repository}
mkdir -p presentation/{model,designsystem,theme,navigation,permission}
```

### Step 2: core 기반 세팅 (가장 먼저)

```
1. core/app/Constants.kt        ← BASE_URL, 타임아웃 등
2. core/app/BaseApplication.kt  ← @HiltAndroidApp
3. core/di/NetworkModule.kt     ← OkHttp, Retrofit, Gson Provide
4. core/di/DataStoreModule.kt   ← DataStore Provide
5. core/network/AuthTokenInterceptor.kt
6. core/network/AuthEventBus.kt
```

### Step 3: domain 모델 + 인터페이스 정의

```
1. domain/common/BaseResult.kt
2. domain/common/ApiError.kt
3. domain/model/필요한_모델.kt
4. domain/repository/필요한_Repository.kt (인터페이스)
```

### Step 4: data 구현

```
1. data/remote/api/ApiService.kt
2. data/remote/dto/request/, response/
3. data/repository/RepositoryImpl.kt
4. data/mapper/필요한_Mapper.kt
5. core/di/RepositoryModule.kt  ← @Binds 연결
```

### Step 5: presentation 화면

```
1. presentation/model/UiState.kt
2. presentation/theme/
3. presentation/designsystem/  ← 기본 컴포넌트
4. presentation/navigation/    ← 라우트 정의
5. 각 Feature 폴더 생성
```

---

## 5. 판단 기준 요약 (치트시트)

### 이 파일은 어디에 넣어야 하는가?

```
Q: Android import가 있는가?
├── No  → domain/ 후보
│   Q: 데이터 클래스인가?
│   ├── Yes → domain/model/
│   Q: 인터페이스인가? (Repository, UseCase 계약)
│   ├── Yes → domain/repository/
│   Q: 비즈니스 로직인가? (계산, 검증, 조합)
│   ├── Yes → domain/usecase/
│   Q: 상태 정의인가? (enum, sealed class)
│   └── Yes → domain/state/
│
├── Yes → data/ 또는 presentation/ 또는 core/
│   Q: 서버/DB/센서와 통신하는가?
│   ├── Yes → data/
│   │   Q: API 인터페이스? → data/remote/api/
│   │   Q: 네트워크 DTO? → data/remote/dto/
│   │   Q: 소켓 매니저? → data/socket/
│   │   Q: Repository 구현체? → data/repository/
│   │   Q: DTO↔Model 변환? → data/mapper/
│   │
│   Q: 사용자에게 보여지는가? (Screen, ViewModel, Composable)
│   ├── Yes → presentation/
│   │   Q: 2개 이상 Feature에서 재사용? → presentation/designsystem/
│   │   Q: 하나의 Feature 전용? → presentation/[feature]/
│   │   Q: UI 전용 모델? → presentation/model/
│   │
│   Q: 특정 Feature에 속하지 않는 인프라인가?
│   └── Yes → core/
│       Q: DI 모듈? → core/di/
│       Q: 네트워크 설정? → core/network/
│       Q: Android Service? → core/service/
│       Q: 범용 유틸? → core/util/
```

### UseCase를 만들어야 하는가?

```
Q: Repository 하나만 호출하고 결과를 그대로 전달하는가?
├── Yes → UseCase 불필요, ViewModel에서 Repository 직접 호출
│
Q: 여러 Repository를 조합하는가?
├── Yes → UseCase 필요
│
Q: 비즈니스 규칙이 있는가? (계산, 검증, 상태 전이)
├── Yes → UseCase 필요
│
Q: 같은 로직을 2개 이상의 ViewModel이 사용하는가?
├── Yes → UseCase 필요
│
Q: 단위 테스트가 필요한 로직인가?
└── Yes → UseCase 필요
```

### Mapper를 만들어야 하는가?

```
Q: DTO와 Domain Model의 필드가 1:1로 같은가?
├── Yes → Mapper 불필요 (나중에 달라지면 그때 추가)
│
Q: 서버 응답을 변환해야 하는가? (필드명 변경, 타입 변환, 중첩 해제)
├── Yes → Mapper 필요
│
Q: Android 타입 ↔ 순수 Kotlin 타입 변환이 필요한가?
│   (LatLng ↔ Coordinates, Location ↔ Coordinates)
└── Yes → Mapper 필요
```

---

## 6. 흔한 실수와 방지법

### ❌ 실수 1: Repository 구현체에 비즈니스 로직 넣기

```kotlin
// ❌ data/repository/GameSessionRepositoryImpl.kt에서
if (_gameTime.value % (_cctvInterval.value * 60) < WARNING_TIME) {
    _cctvPhase.value = CctvPhase.NOTIFY  // ← 이건 비즈니스 로직
}
```

```kotlin
// ✅ domain/usecase/game/CctvPhaseCalculator.kt로 분리
class CctvPhaseCalculator {
    fun calculate(gameTime: Int, interval: Int): CctvPhase { ... }
}
```

### ❌ 실수 2: domain model에 Compose/Android 타입 사용

```kotlin
// ❌ domain/model/GameRole.kt
enum class GameRole(val color: Color)  // ← Compose Color
```

```kotlin
// ✅ domain에는 순수 데이터만
enum class GameRoleType(val roleNameEn: String)

// ✅ presentation에서 UI 속성 매핑
enum class GameRole(val type: GameRoleType, val color: Color)
```

### ❌ 실수 3: ViewModel에서 data 계층 직접 참조

```kotlin
// ❌ ViewModel에서 data 패키지 import
import com.example.app.data.remote.dto.response.LoginResponse

class LoginViewModel @Inject constructor(
    private val authApiService: AuthApiService  // ← data 직접 참조
)
```

```kotlin
// ✅ domain 인터페이스만 참조
import com.example.app.domain.repository.AuthRepository

class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository  // ← domain 인터페이스
)
```

### ❌ 실수 4: StateFlow에 MutableList 넣기

```kotlin
// ❌ 참조 동일성으로 인해 Flow가 변경 감지 못 함
private val _list = MutableStateFlow<MutableList<Long>>(mutableListOf())
_list.value.add(item)  // 같은 참조 → emit 안 됨

// ✅ 불변 리스트 + update 사용
private val _list = MutableStateFlow<List<Long>>(emptyList())
_list.update { it + item }  // 새 리스트 생성 → emit 됨
```

### ❌ 실수 5: 소켓 매니저를 util에 넣기

```kotlin
// ❌ util/socket/GameSocketManager.kt
// "유틸"이 아니라 서버와 통신하는 데이터 소스

// ✅ data/socket/GameSocketManager.kt
// Retrofit ApiService가 data/remote/api/에 있는 것과 같은 논리
```

### ❌ 실수 6: 하나의 sealed class 파일에 여러 도메인 정의

```kotlin
// ❌ GameSessionRepositoryImpl.kt 하단에 6개 enum/sealed class 모아놓기
sealed class GameSessionEvent { ... }
sealed class WalkieConnectionState { ... }
enum class CctvPhase { ... }
enum class HelicopterPhase { ... }
enum class MissionStatus { ... }
enum class ArrestStatus { ... }

// ✅ 각각 별도 파일로 domain/state/에 분리
```
