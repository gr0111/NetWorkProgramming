package server;

import java.util.*;

/**
 * GameState + Validator + Scoring 통합
 * - 실제 루미큐브 규칙은 차근차근 채워가면 됨
 */
public class GameCore {

    // 플레이어 이름 -> 손패(타일 ID 문자열 리스트, 예: "R1", "BL3", "BJoker")
    private Map<String, List<String>> hands = new HashMap<>();

    // 턴 관리용
    private List<String> turnOrder = new ArrayList<>();
    private int turnIndex = 0;

    // 중앙 타일 더미 (타일 ID로 관리, 예: "R1", "BL5")
    private List<String> tilePool = new ArrayList<>();
    private Random random = new Random();

    public GameCore() {
        initTilePool();
    }

    /** 106장 타일 초기화 (문자열 기준) */
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

    /** 플레이어가 방에 들어왔을 때 호출 */
    public void onPlayerJoin(String playerName) {
        if (!hands.containsKey(playerName)) {
            hands.put(playerName, new ArrayList<>());
        }
        if (!turnOrder.contains(playerName)) {
            turnOrder.add(playerName);
        }
        // 필요하면 입장 시 14장 지급도 여기서 처리 가능
        if (hands.get(playerName).isEmpty()) {
            for (int i = 0; i < 14; i++) {
                String tileId = drawFromPool();
                if (tileId != null) hands.get(playerName).add(tileId);
            }
        }
        if (turnOrder.size() == 1) {
            turnIndex = 0;
        }
    }

    public void onPlayerLeave(String playerName) {
        turnOrder.remove(playerName);
        hands.remove(playerName);
        if (turnOrder.isEmpty()) {
            turnIndex = 0;
        } else if (turnIndex >= turnOrder.size()) {
            turnIndex = 0;
        }
    }

    /** 현재 턴의 플레이어 이름 */
    public String getCurrentTurnPlayer() {
        if (turnOrder.isEmpty()) return null;
        return turnOrder.get(turnIndex);
    }

    /** 턴 넘기고, 넘겨진 플레이어 반환 */
    public String nextTurnAndGetPlayer() {
        if (turnOrder.isEmpty()) return null;
        turnIndex = (turnIndex + 1) % turnOrder.size();
        return turnOrder.get(turnIndex);
    }

    /** 타일 더미에서 한 장 뽑기 */
    private String drawFromPool() {
        if (tilePool.isEmpty()) return null;
        return tilePool.remove(0);
    }

    /** 낼 타일 없어서 한 장 랜덤 지급 */
    public String drawRandomTileFor(String playerName) {
        String tileId = drawFromPool();
        if (tileId != null) {
            hands.get(playerName).add(tileId);
        }
        return tileId;
    }

    /**
     * 플레이어가 타일을 낸 경우
     * moveData 예: "R1,R2,R3" (클라이언트가 보드에 놓은 타일 ID 리스트)
     * 여기서 실제 루미큐브 규칙에 맞는지 확인하면 됨.
     */
    public boolean handlePlay(String playerName, String moveData) {
        // TODO: 1) moveData 파싱
        //       2) hands에서 타일 제거
        //       3) 규칙 검증 (같은 숫자/다른 색, 연속 숫자/같은 색, 세트 3장 이상 등)
        //       지금은 일단 true만 반환해서 "규칙 검증 성공"처럼 동작시키자.
        return true;
    }
}
