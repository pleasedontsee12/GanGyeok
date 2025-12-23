# 비친족 다가구 생활 매니지먼트 플랫폼 
> "따로 또 같이, 우리 사이"
> 
> 간격은 룸메이트와 함께하는 생활을 좀 더 편하고 즐겁게 만들어주는 어플리케이션입니다

## 프로젝트 소개
- **간격**은 기숙사, 자취방, 셰어하우스 등에서 함께 사는 룸메이트들이 서로의 생활 패턴을 존중하며 원활하게 소통할 수 있도록 돕는 라이프스타일 앱입니다. 
- 서로의 현재 상태(재실, 외출, 방해 금지)를 직관적으로 확인하고, 집안일(퀘스트)을 재미있게 관리하며, 부담 없는 소통 기능을 제공합니다.

## 1. 개발 환경 (Development Environment)
* **IDE:** Android Studio Ladybug 
* **Language:** Kotlin 1.9.0+
* **JDK:** Java 11
* **Minimum SDK:** 24 (Android 7.0 Nougat)
* **Target SDK:** 36 (Android 15)
* **Build System:** Gradle (Kotlin DSL)
* **버전 및 이슈관리** : Github
  
## 2. 프로젝트 구조
```
📂 com.example.gangyeok
 ┣ 📂 model          # 데이터 모델 (User, House, Quest, HouseLog)
 ┣ 📂 ui
 ┃ ┣ 📂 camera       # 카메라 촬영 화면 (CameraX)
 ┃ ┣ 📂 home         # 홈 화면 구성요소 (상태 카드, 퀘스트 카드 등)
 ┃ ┣ 📂 login / auth # 로그인 및 회원가입 화면
 ┃ ┣ 📂 navigation   # 바텀 네비게이션 및 화면 라우팅
 ┃ ┣ 📂 screens      # 주요 화면 (Home, Quest, HouseLog, Settings)
 ┃ ┣ 📂 setup        # 하우스 생성 및 참여 화면
 ┃ ┗ 📂 widget       # 홈 화면 위젯 (Glance)
 ┣ 📂 util           # 유틸리티 (이미지 압축 등)
 ┗ 📜 MainActivity.kt # 앱 진입점 및 주요 로직
```
## 3. 개발 기간
전체 개발 기간 : 2025-10-01 ~ 2022-12-17 <br>

## 4. 트러블 슈팅 (추후 작성예정)
데이터 동기화 문제 : 일회성 get() 호출로 인해서 상태변경이 이루어지지 않음  
Snapshot Listener 도입 : 실시간 구독을 통해 UI를 재구성함
```kotlin
// 1. 내 정보 실시간 감지
    // 상태 변경이나 룸메이트의 찌르기를 즉시 받기 위함
    DisposableEffect(firebaseUser) {
        val user = firebaseUser
        if (user != null) {
            val registration = db.collection("users").document(user.uid)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && snapshot.exists()) {
                        val newUser = snapshot.toObject(User::class.java)
                        // 핑 감지 (이전 데이터와 비교하여 알림 표시)
                        if (userProfile != null && newUser != null && newUser.lastPing > userProfile!!.lastPing) {
                            vibratePhone(context)
                            Toast.makeText(context, "🤫 룸메이트가 콕! 찔렀습니다.", Toast.LENGTH_LONG).show()
                        }
                        // 상태 업데이트 -> UI 리컴포지션 발생
                        userProfile = newUser
                    }
                }
            onDispose { registration.remove() } // 컴포지션이 해제될 때 리스너 제거 (메모리 누수 방지)
        } else { onDispose { } }
    }
```

## 5. 페이지 별 기능
### 0. 로그인 
-
-
<img width="270" height="600" alt="image" src="https://github.com/user-attachments/assets/ffefd40b-12be-4a1a-a000-410ca18c9569" />

### 1. 회원가입
-
-
<img width="270" height="600" alt="image" src="https://github.com/user-attachments/assets/d91f658b-981c-4010-b651-10ff5734aad8" />

### 2. 비밀번호 찾기
-
-
<img width="270" height="600" alt="image" src="https://github.com/user-attachments/assets/52d46965-13c5-4253-8a02-88c582edf395" />

### 3. 하우스 찾기 / 만들기
-
-
<img width="270" height="600" alt="image" src="https://github.com/user-attachments/assets/0819a5a8-67cf-48dd-8c9d-894b6d6fd701" />

### 4. 하우스 만들기
-
-
<img width="270" height="600" alt="image" src="https://github.com/user-attachments/assets/7b77b9fd-3253-492f-8e01-65bdc6fe4d8e" />

### 5. 하우스 찾기
-
-
<img width="270" height="600" alt="image" src="https://github.com/user-attachments/assets/c13e62ad-10b3-47c1-aa2c-619ab776d9c6" />

### 6. 홈 화면</br>
-
-
<img width="270" height="600" alt="Screenshot_20251223_021109" src="https://github.com/user-attachments/assets/45bab754-96f9-4244-ad5b-d4ca13d69f9c" />

### 7. 미션</br>
-
-
<img width="270" height="600" alt="image" src="https://github.com/user-attachments/assets/a50c868a-a483-4287-9512-0cc855366f03" />   
<img width="270" height="600" alt="image" src="https://github.com/user-attachments/assets/941ebdfe-0f52-476d-a519-248f52a21191" />

### 8. 수다방</br>
-
-
<img width="270" height="600" alt="image" src="https://github.com/user-attachments/assets/17fbd795-52c7-45ec-8f67-fca314d83bf9" />  

### 9. 설정
-
-
<img width="270" height="600" alt="image" src="https://github.com/user-attachments/assets/ff8b8877-3fb3-4c1b-90f1-f386e981ca43" />


