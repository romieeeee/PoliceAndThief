# 프로젝트 회고: Police & Thief (경찰과 도둑) — Android 프론트엔드 개발

> **작성자**: romieeeee (이새롬)
> **프로젝트**: Police & Thief — 위치 기반 실시간 멀티플레이어 게임 (Android)
> **분석 대상**: `fe/` 디렉토리 하위 코드 (커밋 author: 이새롬 / romi)
> **기술 스택**: Kotlin, Jetpack Compose, Hilt, Coroutines/Flow, Socket.IO, Google Maps, LiveKit(WebRTC), CameraX, Kakao SDK
> **팀 구성**: 6인 (FE: 이새롬 / BE: 권기범, 박정후 외 / Infra: 이승호, 민승환 외)

---

## 기여 요약

- **Android 프론트엔드 전체 아키텍처 설계 및 구현** — MVVM + Repository 패턴 기반, Hilt DI, Kotlin Flow 상태 관리
- **390개 파일, 약 30,000라인 규모의 프론트엔드 코드베이스 단독 개발** (Kotlin 소스 222개 파일, 약 27,000라인)
- **실시간 게임 플레이 시스템 구현** — Socket.IO 기반 3중 소켓 통신 (Game/Room/Chat), GPS 실시간 추적, 체포/미션/CCTV 메커니즘
- **Google Maps 기반 게임 맵 시스템** — 실시간 플레이어 위치 표시, 드래그 가능한 폴리곤 경계 설정, 교도소 위치 제약 조건 검증
- **인증 및 네트워크 계층 구현** — JWT 토큰 자동 갱신 인터셉터, Kakao OAuth 소셜 로그인, DataStore 기반 토큰 관리
- **커스텀 Pixel Art UI 디자인 시스템** — PixelContainer, PixelButton 등 Canvas API 기반 레트로 스타일 컴포넌트 설계
- **게임 부가 기능 구현** — 무전기(LiveKit WebRTC), 카메라 미션 촬영/이미지 압축, QR코드 스캔, 만보기 연동, 거리 기반 진동/사운드 피드백
- **게임 결과 및 뉴스 시스템** — AI 생성 뉴스 화면, 게임 통계, 신고 기능, 티어/레벨 시스템 UI

---

## Keep / Problem / Try

### Keep — 잘한 점, 유지할 것

**1. 계층 분리가 명확한 아키텍처 설계**

프로젝트 초기부터 Data(Repository/Remote) → Domain(Model) → Presentation(ViewModel/UI) 계층을 명확히 분리했다. 모든 Repository는 인터페이스와 구현체를 분리하여 정의했고(`AuthRepository` / `AuthRepositoryImpl`), Hilt `@Binds`를 통해 주입했다. 이 구조 덕분에 기능 추가 시 영향 범위가 명확했고, 각 계층의 변경이 다른 계층으로 전파되지 않았다.

```
예시: GameSessionRepository(interface, 72라인) → GameSessionRepositoryImpl(구현, 830라인)
     GameRoomRepository(interface, 120라인) → GameRoomRepositoryImpl(구현, 226라인)
```

**2. Flow 기반 반응형 상태 관리**

모든 상태를 `StateFlow`로 관리하고, UI에서는 `collectAsStateWithLifecycle()`로 수집하는 단방향 데이터 흐름을 일관되게 적용했다. 특히 `GameSessionRepositoryImpl`에서 `combine()`을 활용한 파생 상태 관리가 효과적이었다.

```kotlin
// 도둑 팀원 리스트: 멤버 정보 + 위치 정보를 결합하여 실시간 상태 반영
override val thiefMembers = combine(_members, _memberLocation) { members, locations ->
    members.filter { it.position.equals("THIEF", ignoreCase = true) }
        .map { member ->
            val location = locations.find { it.memberId == member.memberId }
            member.copy(rawStatus = location?.status ?: "FREE")
        }
}.stateIn(scope = repositoryScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())
```

**3. 소켓 통신의 안정성 확보**

`BaseSocketManager`(284라인)에서 Template Method 패턴으로 공통 소켓 로직을 추상화하고, `GameSocketManager`(595라인), `RoomSocketManager`(488라인), `ChatSocketManager`(340라인)로 관심사를 분리했다. 특히 `ConcurrentHashMap` 기반 Emit 추적과 지수 백오프 재전송 로직은 게임 중 네트워크 불안정 상황에서의 데이터 유실을 방지하는 데 기여했다.

**4. 재사용 가능한 커스텀 UI 컴포넌트**

`PixelContainer`(Canvas Path API 기반 레트로 테두리), `PixelButtonCode`(3레이어 깊이감 + 터치 애니메이션), `PixelInputField`, `PixelDropdown` 등 일관된 디자인 시스템을 구축했다. 이 컴포넌트들은 프로젝트 전반에서 재사용되어 UI 일관성을 유지했다.

**5. 에러 처리의 계층화**

`BaseRepository.safeApiCall()`에서 네트워크 에러(SocketTimeoutException, IOException)를 `BaseResult<T>` sealed class로 래핑하고, ViewModel에서 `UiState<T>`(Idle/Loading/Success/Error)로 변환하여 UI에 전달하는 구조를 일관되게 적용했다. `AuthTokenInterceptor`에서는 401 응답 시 `synchronized` 블록 내에서 토큰 갱신을 시도하고, 실패 시 `AuthEventBus`를 통해 전역 로그아웃 이벤트를 발행하여 인증 만료를 안전하게 처리했다.

---

### Problem — 어려웠던 점, 개선이 필요한 것

**1. 프론트엔드 단독 개발로 인한 코드 리뷰 부재**

FE 개발자가 1인이었기 때문에 코드 리뷰 없이 진행되었다. 이로 인해 일부 파일이 과도하게 커졌다(`GameSessionRepositoryImpl` 830라인, `GameRoomViewModel` 684라인, `GamePlayScreen` 670라인). 단일 클래스에 너무 많은 책임이 집중된 부분이 있으며, 이는 유지보수성과 테스트 용이성을 저하시킨다.

**2. 테스트 코드 부재**

프로젝트 일정상 테스트 코드를 작성하지 못했다. 특히 `GameSessionRepositoryImpl`의 게임 상태 머신, `BaseSocketManager`의 재전송 로직, `ImageCompressor`의 이미지 처리 등은 단위 테스트가 필수적인 영역이었으나 커버되지 않았다. 실시간 게임 특성상 재현이 어려운 버그가 발생할 수 있는 구조다.

**3. Socket 이벤트 핸들러 내 수동 JSON 파싱**

소켓 이벤트 처리에서 `JSONObject.optString()`, `optLong()` 등 수동 파싱을 사용했다. Gson이나 Kotlin Serialization을 활용한 타입 안전 역직렬화를 적용했다면 런타임 파싱 오류를 줄일 수 있었을 것이다. 일부 catch 블록이 빈 상태로 방치된 곳도 있어 디버깅이 어려운 상황이 발생할 수 있다.

**4. Navigation 구조의 확장성 한계**

`AppNavigation.kt`에서 `when` 분기 기반 수동 화면 전환을 사용했다. 프로젝트 규모가 커지면 Jetpack Navigation Compose로 전환하는 것이 딥링크, 백스택 관리, 화면 전환 애니메이션 측면에서 유리하다. 현재 구조는 단일 Activity 내 간단한 플로우에서는 동작하지만, 중첩 네비게이션이나 조건부 네비게이션 처리가 복잡해질 수 있다.

**5. 오프라인/네트워크 끊김 대응 미흡**

게임 중 소켓 연결이 끊어졌을 때 reconnect 이벤트는 처리하지만, 연결 끊김 동안 발생한 GPS 데이터나 게임 이벤트에 대한 오프라인 큐잉 전략이 없다. 게임 특성상 짧은 끊김도 치명적일 수 있어 이 부분의 보완이 필요하다.

---

### Try — 다음에 시도할 것

**1. 대규모 클래스 분리 및 책임 분산**

`GameSessionRepositoryImpl`(830라인)을 게임 상태 관리, GPS 통신, CCTV/헬리콥터 로직, 무전기 관리 등 도메인 단위로 분리한다. UseCase 계층을 도입하여 Repository와 ViewModel 사이의 비즈니스 로직을 독립적으로 테스트 가능하게 만든다.

**2. 테스트 전략 수립**

- Repository 계층: 소켓 매니저를 모킹하여 게임 상태 전이 테스트
- ViewModel 계층: Flow emit 시나리오별 UI 상태 변환 테스트
- 유틸리티: `ImageCompressor`, `GameFeedbackManager` 단위 테스트
- UI: Compose Testing 라이브러리를 활용한 스크린 스냅샷/인터랙션 테스트

**3. 소켓 통신 타입 안전성 강화**

수동 JSON 파싱을 Kotlin Serialization 기반 타입 안전 역직렬화로 교체한다. 소켓 이벤트별 Request/Response 모델을 정의하고, 파싱 실패 시 구조화된 로깅을 추가한다.

**4. Jetpack Navigation Compose 도입 검토**

현재 수동 네비게이션을 Jetpack Navigation Compose로 마이그레이션하여 딥링크 지원, 타입 안전 인자 전달, 화면 전환 애니메이션 등의 이점을 확보한다.

**5. CI/CD 파이프라인에 정적 분석 도입**

ktlint, detekt 등 정적 분석 도구를 CI에 통합하여 코드 스타일과 복잡도를 자동으로 관리한다.

---

## 기술적 성장 포인트

### 1. 실시간 멀티플레이어 게임 아키텍처 설계 경험

단순 CRUD 앱이 아닌, 다수의 사용자가 동시에 위치 정보를 공유하고 상호작용하는 실시간 게임 시스템을 설계했다. Socket.IO 기반 3중 소켓 관리(Game/Room/Chat), GPS 1초 주기 전송, CCTV/헬리콥터 사이클 기반 상태 머신, 체포/미션/탈출 이벤트 처리 등 복잡한 실시간 상태 동기화를 구현하면서 비동기 프로그래밍과 상태 관리에 대한 실질적인 이해를 얻었다.

### 2. Kotlin Coroutines/Flow 심화 활용

`SupervisorJob`을 활용한 코루틴 스코프 격리, `SharingStarted.WhileSubscribed(5000)`을 통한 리소스 효율적 Flow 공유, `combine()`을 통한 파생 상태 계산, `Mutex`를 활용한 동시성 제어 등 Coroutines/Flow의 고급 패턴을 실전에서 적용했다.

### 3. Jetpack Compose 기반 복잡한 UI 구현

Canvas API를 활용한 커스텀 드로잉(PixelContainer), Gesture 기반 바텀시트(MissionBottomSheet), Google Maps Compose 통합, AndroidView를 통한 레거시 뷰 래핑(QR 스캐너, 카메라 프리뷰) 등 Compose의 다양한 API를 활용하여 게임 UI를 구현했다. 특히 `Modifier.drawBehind`, `MutableInteractionSource`, `AnimatedVisibility` 등의 활용도가 높았다.

### 4. 네트워크 안정성 및 인증 보안 처리

OkHttp Interceptor 기반 JWT 토큰 자동 갱신, `synchronized` 블록을 활용한 동시 갱신 방지, 만료 5분 전 선제적 갱신 전략, `AuthEventBus`를 통한 전역 세션 만료 처리 등 프로덕션 수준의 인증 관리를 구현했다.

### 5. 하드웨어 센서 및 미디어 통합

만보기(TYPE_STEP_COUNTER), 카메라(CameraX), 마이크(LiveKit WebRTC), 진동(VibrationEffect), 사운드(SoundPool/MediaPlayer) 등 Android 하드웨어 센서와 미디어 API를 게임 메커니즘과 통합하는 경험을 얻었다. 특히 `GameFeedbackManager`에서 거리 기반으로 진동 강도와 사운드 패턴을 차등 적용한 것은 게임 몰입감에 직접적으로 기여했다.

---

## 협업 및 커뮤니케이션 회고

### API 인터페이스 협업

백엔드와의 협업에서 REST API와 Socket.IO 이벤트 스펙을 기반으로 작업했다. `data/remote/api/` 하위 8개 API 서비스(`AuthApiService`, `ChatApiService`, `GameApiService`, `GameRoomApiService`, `ImageApiService`, `NaverApiService`, `ProfileApiService`, `ReportApiService`)와 `data/remote/model/` 하위 Request/Response 모델이 이를 반영한다. 소켓 이벤트의 경우 `post *`(클라이언트→서버)와 `get *`(서버→클라이언트) 네이밍 컨벤션을 통해 방향성을 명확히 했다.

### 역할 분담의 명확성

프론트엔드를 단독으로 담당한 만큼 백엔드 팀과의 경계가 명확했다. Repository 계층에서 API 호출과 소켓 이벤트를 래핑하여 외부 의존성을 격리한 것이 백엔드 변경에 대한 영향 범위를 최소화하는 데 도움이 되었다. 다만, 1인 FE 체제의 한계로 병목이 발생한 부분도 있었을 것이며, 이는 페어 프로그래밍이나 크로스 리뷰로 보완할 수 있다.

### 커밋 컨벤션 및 브랜치 전략

프로젝트 전체에서 `[FE:feat]`, `[BE:feat]` 등의 커밋 프리픽스와 브랜치 분리 전략을 따랐다. 다만, FE 코드가 단일 커밋(`37950d0`)으로 main에 병합된 점은 작업 이력 추적 측면에서 아쉬운 부분이다. 기능 단위 커밋과 PR 기반 병합이 이루어졌다면 변경 이력 파악이 용이했을 것이다.

---

## 개선 계획

| 영역 | 현재 상태 | 개선 방향 | 우선순위 |
|------|----------|----------|---------|
| **코드 구조** | 단일 파일 800라인 이상 | UseCase 계층 도입, 책임 분리 | 높음 |
| **테스트** | 테스트 코드 없음 | Repository/ViewModel 단위 테스트, UI 테스트 | 높음 |
| **타입 안전성** | 수동 JSON 파싱 | Kotlin Serialization 도입 | 중간 |
| **네비게이션** | 수동 when 분기 | Jetpack Navigation Compose | 중간 |
| **오프라인 대응** | reconnect 이벤트만 처리 | 이벤트 큐잉, 재전송 버퍼 | 중간 |
| **로깅** | 빈 catch 블록 존재 | 구조화된 로깅 프레임워크 도입 | 중간 |
| **코드 리뷰** | 1인 개발로 리뷰 부재 | 페어 프로그래밍, 크로스 리뷰 프로세스 | 높음 |
| **CI/CD** | 미구축 | ktlint, detekt, 빌드 검증 자동화 | 낮음 |
| **커밋 전략** | 대규모 단일 커밋 병합 | 기능 단위 커밋, PR 기반 병합 | 높음 |

---

## 부록: 코드베이스 정량 분석

| 항목 | 수치 |
|------|------|
| 총 변경 파일 수 | 390개 |
| 총 삽입 라인 | 29,977라인 |
| Kotlin 소스 파일 | 222개 |
| Kotlin 소스 라인 | ~27,000라인 |
| API 서비스 인터페이스 | 8개 |
| Repository (인터페이스 + 구현) | 18개 (9쌍) |
| ViewModel | 14개 |
| Composable Screen | 20개 이상 |
| 커스텀 UI 컴포넌트 | 20개 이상 |
| 소켓 매니저 | 3개 (Game/Room/Chat) |
| DI 모듈 | 4개 (Network/Repository/Socket/DataStore) |
| Domain Model | 15개 이상 |
| Request/Response 모델 | 30개 이상 |
