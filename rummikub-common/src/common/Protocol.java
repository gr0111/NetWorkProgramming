package common;

/**
 * 서버-클라이언트 간 통신 규약 정의
 */
public class Protocol {
    // 연결 관련
    public static final String JOIN = "JOIN";      // 클라이언트 입장
    public static final String EXIT = "EXIT";      // 게임 종료 / 나가기

    // 게임 진행 관련
    public static final String START = "START";    // 게임 시작
    public static final String TURN = "TURN";      // 턴 전환
    public static final String PLAY = "PLAY";      // 타일 배치/턴 수행
    public static final String RESULT = "RESULT";  // 게임 결과 전송

    // 기타
    public static final String CHAT = "CHAT";      // 일반 채팅 메시지
    public static final String INFO = "INFO";      // 서버 정보/알림

     // --- 새로 추가된 프로토콜 신호 ---
    public static final String OWNER         = "OWNER";         // 방장 알림 (예: OWNER|true)
    public static final String PLAYER_COUNT  = "PLAYER_COUNT"; // 현재 인원수 알림 (예: PLAYER_COUNT|3)
    
    private Protocol() {} // 인스턴스 생성 방지
}
