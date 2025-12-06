package server;

import java.util.*;

public class GameCore {

    private Map<String, List<String>> hands = new HashMap<>();
    private List<String> turnOrder = new ArrayList<>();
    private int turnIndex = 0;

    private List<String> tilePool = new ArrayList<>();
    private Random random = new Random();

    private List<List<String>> tableMelds = new ArrayList<>();

    // ⭐ 초기 30 규칙 충족 여부 저장
    private Map<String, Boolean> initialMeldDone = new HashMap<>();

    public GameCore() {
        initTilePool();
    }

    // ----------------------------------------------------
    // 타일 풀 초기화
    // ----------------------------------------------------
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

    // ----------------------------------------------------
    // 플레이어가 제출한 수 처리 (턴 이동 없음)
    // ----------------------------------------------------
    public boolean handlePlay(String playerName, String moveData) {

        // 턴 보호
        if (!playerName.equals(getCurrentTurnPlayer())) {
            return false;
        }

        // 🔥 기존 보드 상태 저장
        List<List<String>> oldBoard = deepCopy(tableMelds);

        // 🔥 새 보드 파싱
        List<List<String>> newBoard = parseMoveData(moveData);

        // 🔥 새 보드 전체 멜드 유효성 검사
        for (List<String> meld : newBoard) {
            if (!isValidMeld(meld)) {
                System.out.println("[RULE] Invalid meld: " + meld);
                return false;
            }
        }

        // 🔥 이번 턴 실제로 새로 내려놓은 타일(diff 계산)
        List<String> justPlayed = calcJustPlayedTilesCorrect(oldBoard, newBoard);

        // ----------------------------------------------------
        // ⭐ 초기 30 규칙 — justPlayed가 포함된 멜드 점수만 계산
        // ----------------------------------------------------
        if (!initialMeldDone.getOrDefault(playerName, false)) {

            int sum = 0;

            // 새 보드(newBoard)의 멜드 중, justPlayed 타일이 속한 것만 점수 계산
            for (List<String> meld : newBoard) {

                boolean related = false;
                for (String t : meld) {
                    if (justPlayed.contains(t)) {
                        related = true;
                        break;
                    }
                }

                if (related) {
                    sum += computeMeldScore(meld);
                }
            }

            if (sum < 30) {
                System.out.println("[RULE] Initial 30 failed: " + sum);
                return false;
            }

            initialMeldDone.put(playerName, true);
        }


        // 이번 턴 새로 내려놓은 타일 제거
        List<String> hand = hands.get(playerName);
        for (String t : justPlayed) {
            hand.remove(t);
        }

        // 보드를 완전히 새로 제출된 모습(newBoard)로 덮어쓴다
        tableMelds = deepCopy(newBoard);
        return true;
    }

    // ----------------------------------------------------
    // 기존 보드와 새 보드 비교하여 이번 턴 새 타일 구하기
    // ----------------------------------------------------
    private List<String> calcJustPlayedTilesCorrect(List<List<String>> oldBoard,
                                                    List<List<String>> newBoard) {
        List<String> oldFlat = new ArrayList<>();
        oldBoard.forEach(oldFlat::addAll);

        List<String> newFlat = new ArrayList<>();
        newBoard.forEach(newFlat::addAll);

        List<String> diff = new ArrayList<>(newFlat);

        for (String t : oldFlat) {
            diff.remove(t);   // oldBoard에 있던 타일 제외
        }

        return diff; // 이번 턴에 새로 낸 타일만 반환
    }

    // ----------------------------------------------------
    // 2차원 배열 깊은 복사
    // ----------------------------------------------------
    private List<List<String>> deepCopy(List<List<String>> src) {
        List<List<String>> out = new ArrayList<>();
        for (List<String> m : src) out.add(new ArrayList<>(m));
        return out;
    }

    // ----------------------------------------------------
    // 세트/런 검증
    // ----------------------------------------------------
    private int getTileValue(String tile) {
        if (tile.contains("Joker")) return 0;
        return Integer.parseInt(tile.replaceAll("[^0-9]", ""));
    }

    private boolean isValidMeld(List<String> meld) {
        if (meld.size() < 3) return false;

        List<Integer> nums = new ArrayList<>();
        List<String> colors = new ArrayList<>();
        boolean containsJoker = false;

        for (String t : meld) {
            if (t.contains("Joker")) {
                containsJoker = true;
                nums.add(0);
                colors.add("J");
                continue;
            }
            colors.add(t.replaceAll("[0-9]", ""));
            nums.add(Integer.parseInt(t.replaceAll("[^0-9]", "")));
        }

        return isValidSet(nums, colors, containsJoker) ||
                isValidRun(nums, colors, containsJoker);
    }

    private boolean isValidSet(List<Integer> nums, List<String> colors, boolean joker) {
        Set<Integer> ns = new HashSet<>();
        Set<String> cs = new HashSet<>();

        for (int n : nums) if (n != 0) ns.add(n);
        for (String c : colors) if (!c.equals("J")) cs.add(c);

        if (ns.size() > 1) return false;

        return cs.size() + (joker ? 1 : 0) >= 3;
    }

    private boolean isValidRun(List<Integer> nums, List<String> colors, boolean joker) {
        String c = null;
        int jokerCount = 0;

        // 색상 체크 + 조커 카운트
        List<Integer> realNums = new ArrayList<>();
        for (String t : colors) {
            if (t.equals("J")) {
                jokerCount++;
            }
        }

        // 숫자 리스트 재구성
        for (int n : nums) {
            if (n != 0) realNums.add(n);
        }

        // 모두 빈칸이면 불가
        if (realNums.isEmpty()) return false;

        // 색상 통일 검사
        for (String col : colors) {
            if (!col.equals("J")) {
                if (c == null) c = col;
                else if (!c.equals(col)) return false;
            }
        }

        Collections.sort(realNums);

        // gap 카운트
        int gaps = 0;
        for (int i = 1; i < realNums.size(); i++) {
            gaps += (realNums.get(i) - realNums.get(i - 1) - 1);
        }

        // gap보다 조커가 많아야만 run 가능
        return jokerCount >= gaps;
    }


    // ----------------------------------------------------
    // Move 파싱
    // ----------------------------------------------------
    public List<List<String>> parseMoveData(String moveData) {
        List<List<String>> res = new ArrayList<>();
        if (moveData == null || moveData.isBlank()) return res;

        String[] meldStrings = moveData.split(";");
        for (String m : meldStrings) {
            List<String> tiles = new ArrayList<>();
            for (String t : m.split(",")) {
                tiles.add(t.trim());
            }
            res.add(tiles);
        }

        return res;
    }

    // ----------------------------------------------------
    // 승리
    // ----------------------------------------------------
    public boolean hasWon(String playerName) {
        return hands.get(playerName) != null && hands.get(playerName).isEmpty();
    }

    // ----------------------------------------------------
    // 턴 관리
    // ----------------------------------------------------
    public String getCurrentTurnPlayer() {
        if (turnOrder.isEmpty()) return null;
        return turnOrder.get(turnIndex);
    }

    public String nextTurnAndGetPlayer() {
        if (turnOrder.isEmpty()) return null;
        turnIndex = (turnIndex + 1) % turnOrder.size();
        return turnOrder.get(turnIndex);
    }

    // ----------------------------------------------------
    // 손패 조회
    // ----------------------------------------------------
    public List<String> getHand(String playerName) {
        List<String> h = hands.get(playerName);
        if (h == null) return new ArrayList<>();
        return new ArrayList<>(h);
    }

    // ----------------------------------------------------
    // 타일 뽑기
    // ----------------------------------------------------
    public String drawRandomTileFor(String playerName) {
        if (!playerName.equals(getCurrentTurnPlayer())) return null;

        String tile = drawFromPool();
        if (tile != null) hands.get(playerName).add(tile);
        return tile;
    }

    private String drawFromPool() {
        if (tilePool.isEmpty()) return null;
        return tilePool.remove(0);
    }

    // ----------------------------------------------------
    // 플레이어 입장/퇴장
    // ----------------------------------------------------
    public void onPlayerJoin(String name) {
        hands.putIfAbsent(name, new ArrayList<>());
        initialMeldDone.putIfAbsent(name, false);

        if (!turnOrder.contains(name)) turnOrder.add(name);

        if (hands.get(name).isEmpty()) {
            for (int i = 0; i < 14; i++) {
                String tile = drawFromPool();
                if (tile != null) hands.get(name).add(tile);
            }
        }
    }

    public void onPlayerLeave(String name) {
        turnOrder.remove(name);
        hands.remove(name);
        initialMeldDone.remove(name);

        if (turnIndex >= turnOrder.size()) turnIndex = 0;
    }

    // ----------------------------------------------------
    // 보드 인코딩
    // ----------------------------------------------------
    public String encodeBoard() {
        StringBuilder sb = new StringBuilder();

        for (List<String> meld : tableMelds) {
            if (sb.length() > 0) sb.append(";");
            sb.append(String.join(",", meld));
        }

        return sb.toString();
    }

    // ============================================================
    // 🎯 멜드 점수 계산 함수 (조커 포함)
    // ============================================================
    private int computeMeldScore(List<String> meld) {

        List<Integer> nums = new ArrayList<>();
        List<String> colors = new ArrayList<>();
        int jokerCount = 0;

        for (String t : meld) {
            if (t.contains("Joker")) {
                jokerCount++;
                nums.add(0);
                colors.add("J");
            } else {
                nums.add(Integer.parseInt(t.replaceAll("[^0-9]", "")));
                colors.add(t.replaceAll("[0-9]", ""));
            }
        }

        // === SET 점수 ===
        if (isValidSet(nums, colors, jokerCount > 0)) {
            int base = 0;
            for (int n : nums) if (n != 0) base = n;
            return base * meld.size();
        }

        // === RUN 점수 ===
        if (isValidRun(nums, colors, jokerCount > 0)) {

            List<Integer> real = new ArrayList<>();
            for (int n : nums) if (n != 0) real.add(n);

            Collections.sort(real);

            int length = real.size() + jokerCount;  // 전체 길이
            int min = real.get(0);                 // 최소값 기준

            // 연속합 공식: (첫 + 끝) * 개수 / 2
            return (min + (min + length - 1)) * length / 2;
        }

        return 0;
    }
}