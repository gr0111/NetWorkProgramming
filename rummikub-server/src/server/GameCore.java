package server;

import java.util.*;

/**
 * GameState + Validator + Scoring 통합
 * - 지금은: 106장 타일, 손패(14장), 턴 관리, 랜덤 타일 지급까지 구현
 * - 나중에: handlePlay()에 실제 루미큐브 규칙 검증 추가하면 됨
 */
public class GameCore {

    // 플레이어 이름 → 손패(타일 ID 리스트: "R1", "BL3", "BJoker" ...)
    private Map<String, List<String>> hands = new HashMap<>();

    // 턴 순서 관리
    private List<String> turnOrder = new ArrayList<>();
    private int turnIndex = 0;

    // 중앙 타일 더미
    private List<String> tilePool = new ArrayList<>();
    private Random random = new Random();

    public GameCore() {
        initTilePool();
    }

    /** 106장 타일 초기화 */
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

    /** 플레이어가 방에 들어왔을 때 (Room.addPlayer에서 호출) */
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

    /** 현재 턴 플레이어 */
    public String getCurrentTurnPlayer() {
        if (turnOrder.isEmpty()) return null;
        return turnOrder.get(turnIndex);
    }

    /** 다음 턴으로 넘기고, 그 플레이어 이름 반환 */
    public String nextTurnAndGetPlayer() {
        if (turnOrder.isEmpty()) return null;
        turnIndex = (turnIndex + 1) % turnOrder.size();
        return turnOrder.get(turnIndex);
    }

    /** 중앙 타일 더미에서 한 장 뽑기 */
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

    /** 특정 플레이어의 손패 (복사본 반환) */
    public List<String> getHand(String playerName) {
        List<String> hand = hands.get(playerName);
        if (hand == null) return Collections.emptyList();
        return new ArrayList<>(hand);
    }

    /** 플레이어가 타일을 낸 경우 (지금은 규칙 검증 생략) */
    public boolean handlePlay(String playerName, String moveData) {
        // TODO: moveData 파싱 + hands에서 타일 제거 + 규칙 검증
        return true; // 우선은 항상 성공 처리
    }
}
