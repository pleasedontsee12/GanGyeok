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

## 4. 신경 쓴 부분

## 5. 페이지 별 기능

## 6. 
