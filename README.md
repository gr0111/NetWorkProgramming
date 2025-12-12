# NetworkProgrammong Project
## Rummikub Network Game

> 네트워크프로그래밍 팀 프로젝트 – TCP 기반 멀티플레이 루미큐브 게임
Java Socket + Swing UI로 구현한 **4인 멀티플레이 루미큐브**입니다.  
서버가 모든 게임 규칙과 점수를 관리하고, 클라이언트는 UI/입력/표시만 담당하는 구조입니다.

---

## ✨ 주요 기능

### 🔵 공통(common)

- 텍스트 기반 프로토콜 정의 (`TYPE|payload` 형식)
- 타일 모델 (`색상`, `숫자`, `조커 여부`)
- 메시지 / 처리 결과 공통 구조

### 🖥 서버 (rummikub-server)

- 여러 클라이언트 동시 접속 관리 (스레드 기반 `ClientSession`)
- 방(Room) 생성 / 입장 / 퇴장 / 방장 관리
- 루미큐브 룰 엔진 (`GameCore`)
  - SET / RUN 멜드 검증
  - Joker(조커) 값 추론 및 고정 (`RJoker(5)` 형태)
  - 초기 30점 규칙 검증
  - 멜드 재조합 후 남은 조각까지 유효한지 검사
- 멜드 재배치 알고리즘 (`MeldRearranger`)
- 점수 시스템
  - 라운드 종료 시 손패 점수 합산
  - 승자는 다른 플레이어 점수 합만큼 +
  - 패자는 자신의 손패 점수만큼 –
  - `SCORE|name|score` 브로드캐스트

### 💻 클라이언트 (rummikub-client)

- 로그인 화면 (서버 주소/포트/닉네임 입력)
- 로비 화면
  - 방 목록 조회 (`LIST`)
  - 방 생성 (`CREATE`)
  - 방 입장 (`JOIN`)
- 게임룸 화면
  - 테이블 보드 (`BoardPanel`) – **서버 보드를 그대로 그림**
  - 손패 패널 (`TwoRowHandPanel`) – 2줄 손패 + 정렬(색상/숫자)
  - 드래그&드롭으로 멜드 구성 / 재조합
  - 규칙 위반 시 턴 내에 낸 타일만 롤백
  - 현재 턴 / 내 점수 라벨 표시
  - 승리/패배 + 점수 표시 팝업

---

## 🧱 구조 / 디렉터리

```text
NetWorkProgramming/
└─ rummikub/
   ├─ rummikub-server/
   │  └─ src/
   │     ├─ common/
   │     │  ├─ Message.java       // 공통 메시지 구조
   │     │  ├─ Protocol.java      // 프로토콜 상수 정의
   │     │  ├─ Result.java        // 처리 결과 표현
   │     │  └─ Tile.java          // 타일 데이터 모델
   │     │
   │     └─ server/
   │        ├─ ServerMain.java    // 서버 실행 진입점 (main)
   │        ├─ GameServer.java    // ServerSocket, ClientSession 관리
   │        ├─ ClientSession.java // 클라이언트별 세션 스레드
   │        ├─ Room.java          // 방(룸) 관리, 턴/PLAY/NO_TILE 처리
   │        ├─ GameCore.java      // 게임 규칙, 멜드/조커 검증, 점수 계산
   │        └─ MeldRearranger.java// 서버 측 멜드 재조합 알고리즘
   │
   ├─ rummikub-client/
   │  └─ src/
   │     ├─ client/
   │     │  ├─ ClientMain.java      // 클라이언트 실행 진입점
   │     │  ├─ ClientApp.java       // 전체 앱 로직, 화면 전환, 메시지 핸들러
   │     │  ├─ NetIO.java           // 소켓 연결, send()/수신 스레드
   │     │  ├─ LoginView.java       // 로그인 화면
   │     │  ├─ LobbyView.java       // 로비(방 목록/생성/입장)
   │     │  ├─ RoomView.java        // 게임 화면(보드, 손패, 채팅, 점수, 팝업)
   │     │  ├─ BoardPanel.java      // 테이블 보드 UI, 멜드/타일 배치·재조합
   │     │  ├─ TwoRowHandPanel.java // 손패 2줄 표시 및 정렬
   │     │  └─ TileView.java        // 개별 타일 UI, 드래그/조커 표시
   │     │
   │     └─ assets/
   │        └─ images/
   │           ├─ login_bg.png
   │           ├─ R1.png, R2.png, ...
   │           ├─ BL1.png, ...
   │           ├─ RJoker.png
   │           └─ BJoker.png
   │
   └─ (docs, README 등)

---

## ⚙️ 기술 스택
- 언어: Java
- UI: Swing + FlatLaf (모던 룩앤필)
- 네트워크: TCP Socket, 텍스트 기반 프로토콜
- 구조
  - 공통 모듈(common) + 서버(server) + 클라이언트(client) 3계층
  - 서버 authoritative (모든 룰/점수는 서버에서만 결정)

---

## 🚀 실행 방법
Java SDK 17 이상 기준 (IDE: IntelliJ / VS Code / Eclipse 등)

### 1) 서버 실행
1. IDE에서 rummikub-server 프로젝트 열기
2. src/server/ServerMain.java 실행
3. 기본 포트(예: 5000)로 서버가 열림
또는 터미널에서 (예시)
```
cd rummikub-server/src
javac common/*.java server/*.java
java server.ServerMain
```
### 2) 클라이언트 실행
1. IDE에서 rummikub-client 프로젝트 열기
2. src/client/ClientMain.java 실행
3. 로그인 화면에서
  - 호스트: localhost
  - 포트: 5000
  - 이름: 원하는 닉네임
클라이언트를 여러 개 실행하면 한 PC에서 다중 접속 테스트 가능.
