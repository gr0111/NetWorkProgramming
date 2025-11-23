package server;

import java.util.*;

public class GameCore {

    private Map<String, List<String>> hands = new HashMap<>();
    private List<String> turnOrder = new ArrayList<>();
    private int turnIndex = 0;

    private List<String> tilePool = new ArrayList<>();
    private Random random = new Random();
    private List<List<String>> tableMelds = new ArrayList<>();  // 테이블 위에 깔려있는 타일들
    private boolean gameStarted = false;

    public GameCore() {
        initTilePool();
    }

    // 타일 풀 초기화 (106개 타일, 두 세트)
    private void initTilePool() {
        String[] colors = {"R", "BL", "Y", "B"};
        for (int set = 0; set < 2; set++) {
            for (String c : colors) {
                for (int n = 1; n <= 13; n++) {
                    tilePool.add(c + n);
                }
            }
        }
        tilePool.add("RJoker");
        tilePool.add("BJoker");
        Collections.shuffle(tilePool, random);
    }

    // 플레이어가 낸 타일들을 처리하는 메서드
    public boolean handlePlay(String playerName, String moveData) {
        List<List<String>> newMelds = parseMoveData(moveData);  // 예: "R1,R2,R3;BL5,BL6,BL7"

        // 각 멜드가 유효한지 확인
        for (List<String> meld : newMelds) {
            if (!isValidMeld(meld)) {
                return false;  // 규칙에 맞지 않으면 실패
            }
        }

        // 손패에서 낸 타일들 제거
        for (List<String> meld : newMelds) {
            for (String tile : meld) {
                if (!hands.get(playerName).remove(tile)) {
                    return false; // 손패에 타일이 없으면 실패
                }
            }
        }

        // 테이블에 낸 타일들 추가
        tableMelds.addAll(newMelds);

        // 승리 여부 체크
        if (hasWon(playerName)) {
            broadcast("WIN|" + playerName);
            broadcast("GAME_OVER|" + playerName);
            gameStarted = false; // 게임 종료
            return true;
        }

        // 턴 넘기기
        String nextPlayer = nextTurnAndGetPlayer();
        broadcast("TURN|" + nextPlayer);

        return true;  // 성공
    }

    // 세트/런 검증
    private boolean isValidMeld(List<String> meld) {
        if (meld.size() < 3) return false;  // 최소 3개 타일이어야 함

        String firstTile = meld.get(0);
        String color = firstTile.substring(0, 1);  // 색상 (예: R, BL, Y, B)
        int number = Integer.parseInt(firstTile.substring(1));  // 숫자 (예: 1, 2, 3, ...)

        boolean isRun = true;  // 런인지 체크
        boolean isSet = true;  // 세트인지 체크

        for (int i = 1; i < meld.size(); i++) {
            String tile = meld.get(i);
            String tileColor = tile.substring(0, 1);
            int tileNumber = Integer.parseInt(tile.substring(1));

            // 런: 같은 색, 연속된 숫자
            if (!tileColor.equals(color) || tileNumber != number + i) {
                isRun = false;
            }

            // 세트: 같은 숫자, 다른 색
            if (tileNumber != number || tileColor.equals(color)) {
                isSet = false;
            }
        }

        return isRun || isSet;  // 세트나 런이 맞으면 통과
    }

    // 멜드 데이터를 파싱하여 List<List<String>>로 변환
    public List<List<String>> parseMoveData(String moveData) {
        List<List<String>> newMelds = new ArrayList<>();
        String[] meldStrings = moveData.split(";");
        for (String meldStr : meldStrings) {
            newMelds.add(Arrays.asList(meldStr.split(",")));
        }
        return newMelds;
    }

    // 게임 승리 여부 체크 (손패가 비어 있으면 승리)
    public boolean hasWon(String playerName) {
        List<String> hand = hands.get(playerName);
        return hand != null && hand.isEmpty();  // 손패가 비어 있으면 승리
    }

    // 게임 종료 알림
    public void broadcast(String message) {
        // 서버에서 클라이언트에게 메시지 보내는 방법 (예시)
        // 이 메서드는 서버에서 각 클라이언트에게 메시지를 보내는 방식에 맞춰 구현
    }

    // 다음 턴 플레이어 반환
    public String nextTurnAndGetPlayer() {
        if (turnOrder.isEmpty()) return null;
        turnIndex = (turnIndex + 1) % turnOrder.size();
        return turnOrder.get(turnIndex);
    }

    // 현재 턴 플레이어 반환
    public String getCurrentTurnPlayer() {
        if (turnOrder.isEmpty()) return null;
        return turnOrder.get(turnIndex);
    }

    // 타일을 랜덤으로 뽑아주는 메서드
    public String drawRandomTileFor(String playerName) {
        String tileId = drawFromPool();
        if (tileId != null) {
            hands.get(playerName).add(tileId);
        }
        return tileId;
    }

    // 중앙 타일 더미에서 한 장 뽑기
    private String drawFromPool() {
        if (tilePool.isEmpty()) return null;
        return tilePool.remove(0);
    }

    // 플레이어가 입장했을 때 초기화
    public void onPlayerJoin(String playerName) {
        if (!hands.containsKey(playerName)) {
            hands.put(playerName, new ArrayList<>());
        }
        if (!turnOrder.contains(playerName)) {
            turnOrder.add(playerName);
        }

        // 손패가 비어 있으면 14장 지급
        if (hands.get(playerName).isEmpty()) {
            for (int i = 0; i < 14; i++) {
                String tileId = drawFromPool();
                if (tileId != null) hands.get(playerName).add(tileId);
            }
        }
    }

    // 플레이어가 나갔을 때 처리
    public void onPlayerLeave(String playerName) {
        turnOrder.remove(playerName);
        hands.remove(playerName);
        if (turnOrder.isEmpty()) {
            turnIndex = 0;
        } else if (turnIndex >= turnOrder.size()) {
            turnIndex = 0;
        }
    }

    // 플레이어의 손패 가져오기
    public List<String> getHand(String playerName) {
        List<String> hand = hands.get(playerName);
        if (hand == null) return Collections.emptyList();
        return new ArrayList<>(hand);
    }

    // 테이블 상태를 문자열로 변환 (클라이언트에게 전송용)
    public String encodeBoard() {
        StringBuilder sb = new StringBuilder();
        for (List<String> meld : tableMelds) {
            if (sb.length() > 0) sb.append(";");
            sb.append(String.join(",", meld));
        }
        return sb.toString();
    }
}
