# DatePick

데이트 장소와 코스를 추천하고 후기를 공유할 수 있는 웹 게시판 프로젝트입니다.

사용자는 게시글을 자유롭게 조회할 수 있으며, 로그인한 사용자는 게시글을 작성할 수 있습니다. 게시글 수정과 삭제는 해당 게시글의 작성자에게만 허용됩니다.

## 1. 개발 환경

| 구분 | 기술 |
|---|---|
| 운영체제 | Windows 11 |
| 프론트엔드 | Vue 3, Vite |
| 백엔드 | Spring Boot, Java 21 |
| 데이터베이스 | SQLite |
| 인증 방식 | JWT (HS256) |
| 개발 도구 | Visual Studio Code, IntelliJ IDEA |
| Node.js | 24.14.1 |

## 2. 프로젝트 구조

```text
JWT-Assignment/
├── backend-spring/    # Spring Boot 백엔드
├── frontend-vue/      # Vue 3 프론트엔드
├── tools/             # API 자동 검사 스크립트
├── docs/              # 보고서 및 관련 자료
└── README.md          # 프로젝트 설명
```

## 3. 주요 기능

- 게시글 목록 및 상세 내용 조회
- JWT 기반 사용자 로그인
- 로그인한 사용자의 게시글 작성
- 작성자 본인의 게시글 수정 및 삭제
- 다른 사용자의 게시글 수정 및 삭제 제한
- 입력값 검증 및 오류 응답 처리
- JWT 만료 시 인증 상태 초기화
- 새로고침 시 로그인 상태 초기화
- SQLite를 이용한 게시글 데이터 저장

## 4. 실행 방법

### 4.1 사전 준비

다음 프로그램이 설치되어 있어야 합니다.

- Java 21
- Node.js 및 npm
- Python 3 (자동 검사 실행 시 필요)

### 4.2 백엔드 실행

PowerShell에서 백엔드 폴더로 이동합니다.

```powershell
cd backend-spring
```

JWT 서명에 사용할 비밀키를 생성하고 환경변수를 설정합니다.

```powershell
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$rng.Dispose()

$env:JWT_SECRET = [BitConverter]::ToString($bytes).Replace("-", "").ToLower()
$env:TOKEN_TTL = "90"
```

Spring Boot 서버를 실행합니다.

```powershell
.\mvnw.cmd spring-boot:run
```

정상 실행 시 백엔드 서버는 다음 주소에서 동작합니다.

```text
http://127.0.0.1:8000
```

서버 실행 상태는 아래 주소에서 확인할 수 있습니다.

```text
http://127.0.0.1:8000/api/health
```

**주의:** 실제 JWT 비밀키는 README 또는 GitHub 저장소에 공개하지 않습니다.

### 4.3 프론트엔드 실행

새 PowerShell 터미널을 열고 프로젝트의 프론트엔드 폴더로 이동합니다.

```powershell
cd frontend-vue
```

의존성을 설치합니다.

```powershell
npm ci
```

프론트엔드 개발 서버를 실행합니다.

```powershell
npm run dev
```

터미널에 표시되는 로컬 주소로 접속하여 DatePick 화면을 확인합니다.

### 4.4 프로덕션 빌드 및 실행

프론트엔드 폴더에서 다음 명령어를 실행합니다.

```powershell
npm run build
npm run preview
```

배포용 화면은 다음 주소에서 확인할 수 있습니다.

```text
http://localhost:4173/
```

## 5. API 주요 기능

| HTTP 메서드 | API 경로 | 설명 |
|---|---|---|
| GET | `/api/health` | 서버 상태 확인 |
| POST | `/api/auth/login` | 사용자 로그인 |
| GET | `/api/me` | 현재 로그인 사용자 조회 |
| GET | `/api/posts` | 게시글 목록 조회 |
| GET | `/api/posts/{id}` | 게시글 상세 조회 |
| POST | `/api/posts` | 게시글 작성 |
| PUT | `/api/posts/{id}` | 게시글 수정 |
| DELETE | `/api/posts/{id}` | 게시글 삭제 |

게시글 조회는 로그인하지 않은 사용자도 가능하며, 작성·수정·삭제는 인증 및 권한 검사를 거칩니다.

## 6. 테스트 방법

백엔드 서버를 실행한 상태에서 **별도의 PowerShell 터미널**을 열고 프로젝트 최상위 폴더에서 다음 명령어를 실행합니다.

### 6.1 기본 자동 검사

```powershell
py tools/check_api.py --mode final
```

실제 테스트 결과:

```text
All 40 checks passed (final).
```

### 6.2 JWT 만료 포함 자동 검사

백엔드 서버를 종료한 뒤 `TOKEN_TTL`을 `3`으로 설정하고 서버를 다시 실행합니다.

```powershell
$env:TOKEN_TTL = "3"
.\mvnw.cmd spring-boot:run
```

프로젝트 최상위 폴더의 다른 터미널에서 다음 명령어를 실행합니다.

```powershell
py tools/check_api.py --mode final --expiry
```

실제 테스트 결과:

```text
All 48 checks passed (final).
```

검사가 끝나면 백엔드 서버를 종료하고 `TOKEN_TTL`을 다시 `90`으로 설정하여 실행합니다.

### 6.3 프론트엔드 검증

다음 항목을 확인했습니다.

- Vue 프로덕션 빌드 성공
- 배포용 화면에서 게시글 목록 표시
- Alice 계정 로그인 및 게시글 수정
- Bob 계정에서 Alice 게시글 수정·삭제 버튼 미표시
- 로그아웃 후 게시글 공개 조회
- 브라우저 새로고침 시 로그인 상태 초기화
- 서버 재시작 후 SQLite 게시글 데이터 유지

## 7. 인증 및 보안

- JWT HS256 서명 방식 사용
- JWT 비밀키를 환경변수로 관리
- JWT 유효기간 검증
- 로그인하지 않은 요청에 대한 인증 오류 처리
- 작성자가 아닌 사용자의 수정·삭제 요청 차단
- 게시글 입력값 검증
- 프론트엔드 JWT를 브라우저 영구 저장소가 아닌 메모리에서 관리

## 8. AI 도구 활용

프로젝트 개발 과정에서 생성형 AI를 활용하여 개발 환경 설정, 코드 작성 및 수정, 오류 원인 분석, 테스트 절차 점검, 문서 작성에 도움을 받았습니다.

실제 기능 실행과 자동 검사, 브라우저 동작 확인을 통해 구현 결과를 검증했습니다.
