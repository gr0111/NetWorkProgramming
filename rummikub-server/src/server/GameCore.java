package server;

import java.util.*;

public class GameCore {

    private Map<String, List<String>> hands = new HashMap<>();
    private List<String> turnOrder = new ArrayList<>();
    private int turnIndex = 0;

    // 플레이어 누적 점수: 이름 → 점수
    private Map<String, Integer> totalScores = new HashMap<>();


    private Map<String, Boolean> playedThisTurn = new HashMap<>();

    private List<String> tilePool = new ArrayList<>();
    private Random random = new Random();

    private List<List<String>> tableMelds = new ArrayList<>();

    private Map<String, Boolean> initialMeldDone = new HashMap<>();

    // 조커 값 저장: "meldIndex:tileIndex" → value
    private Map<String, Integer> jokerValueMap = new HashMap<>();


    public GameCore() {
        initTilePool();
    }

    private void initTilePool() {
        String[] colors = { "R", "BL", "Y", "B" };

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

    // ============================================================
    // 서버 재조합 진입점
    public List<List<String>> rearrangeServerSide(List<List<String>> oldBoard, List<String> tilesFromPlayer) {
        try {
            return MeldRearranger.rearrange(oldBoard, tilesFromPlayer);
        } catch (MeldRearranger.RearrangeFailedException e) {
            System.out.println("[REARRANGE] 실패: " + e.getMessage());
            return null;
        }
    }

    // ============================================================
    // PLAY HANDLING
    public boolean handlePlay(String playerName, String moveData) {
        playedThisTurn.putIfAbsent(playerName, false);

        // 0) 턴 체크
        if (!playerName.equals(getCurrentTurnPlayer()))
            return false;

        // 1) 기존 보드 백업
        List<List<String>> oldBoard = deepCopy(tableMelds);

        // 2) 클라이언트가 보낸 보드 파싱
        List<List<String>> clientBoard = parseMoveData(moveData);

        // 3) 기존 보드 타일 삭제 여부 검사 (A-1: 멜드 분해 허용 + 타일 미삭제 보장)
        if (!validateBoardConsistency(oldBoard, clientBoard)) {
            System.out.println("[RULE] Board tile removed illegally.");
            return false;
        }

        // 4) 플레이어 손패에 없는 타일 사용 방지
        for (List<String> meld : clientBoard) {
            for (String t : meld) {

                if (oldBoardContains(oldBoard, t))
                    continue;

                if (!hands.get(playerName).contains(t)) {
                    System.out.println("[RULE] Illegal tile usage: " + t);
                    return false;
                }
            }
        }

        // 5) 이번 턴 실제로 낸 타일 계산 (oldBoard 기준)
        List<String> justPlayed = calcJustPlayedTilesCorrect(oldBoard, clientBoard);

        // 6) 타일을 실제로 1개도 안 냈으면 제출 실패
        if (justPlayed.isEmpty()) {
            System.out.println("[RULE] No tiles played.");
            return false;
        }

        // 7) 서버 측 자동 재조합 시도
        List<List<String>> finalBoard = rearrangeServerSide(oldBoard, justPlayed);
        if (finalBoard != null) {
            System.out.println("[REARRANGE] 서버 자동 재조합 성공, 보드 갱신됨.");
        } else {
            System.out.println("[REARRANGE] 재조합 불가, 클라이언트 보드 사용.");
            finalBoard = clientBoard;
        }

        // 8) 최종 보드 기준으로 전체 멜드 유효성 + 조커 값 확정 (A-2)
        if (!validateRemainingMeldsAfterRearrange(oldBoard, finalBoard)) {
            // 내부에서 이미 로그를 찍었으므로 여기서는 false만 리턴
            return false;
        }

        // 9) 초기 30 검사 (최종 보드 기준 + justPlayed 사용)
        if (!initialMeldDone.getOrDefault(playerName, false)) {

            int sum = 0;

            for (int mi = 0; mi < finalBoard.size(); mi++) {

                List<String> meld = finalBoard.get(mi);
                boolean used = false;

                // 이 멜드에 플레이어가 이번 턴에 낸 타일이 1개라도 포함되어 있으면
                for (String t : meld) {
                    if (justPlayed.contains(t)) {
                        used = true;
                        break;
                    }
                }

                if (used)
                    sum += computeMeldScore(finalBoard, mi);
            }

            if (sum < 30) {
                System.out.println("[RULE] Initial 30 failed: " + sum);
                return false;
            }

            initialMeldDone.put(playerName, true);
        }

        // 10) 손패에서 제거
        List<String> hand = hands.get(playerName);
        for (String t : justPlayed)
            hand.remove(t);

        // 11) 서버 보드 교체 (최종 보드로)
        tableMelds = deepCopy(finalBoard);

        playedThisTurn.put(playerName, true);
        return true;
    }

    // ============================================================
    // VALIDATION
    private boolean validateAndFixJoker(List<String> meld, int meldIndex) {

        List<Integer> nums = new ArrayList<>();
        List<String> colors = new ArrayList<>();
        int jokerCount = 0;

        for (String t : meld) {
            if (t.contains("Joker")) {
                nums.add(0);
                colors.add("J");
                jokerCount++;
            } else {
                nums.add(Integer.parseInt(t.replaceAll("[^0-9]", "")));
                colors.add(extractColor(t));
            }
        }

        // SET
        if (isValidSet(nums, colors, jokerCount)) {

            int base = 0;
            for (int n : nums)
                if (n != 0)
                    base = n;

            for (int ti = 0; ti < meld.size(); ti++) {
                if (meld.get(ti).contains("Joker"))
                    jokerValueMap.put(meldIndex + ":" + ti, base);
            }

            return true;
        }

        // RUN
        if (isValidRun(nums, colors, jokerCount)) {

            List<Integer> real = new ArrayList<>();
            for (int n : nums)
                if (n != 0)
                    real.add(n);

            Collections.sort(real);

            List<Integer> full = inferJokerValues(real, jokerCount);

            int fullIdx = 0;

            for (int ti = 0; ti < meld.size(); ti++) {

                if (!meld.get(ti).contains("Joker")) {
                    fullIdx++;
                } else {
                    jokerValueMap.put(meldIndex + ":" + ti, full.get(fullIdx));
                }
            }
            return true;
        }

        return false;
    }

    private List<Integer> inferJokerValues(List<Integer> nums, int jokerCount) {

        List<Integer> full = new ArrayList<>();
        if (nums.isEmpty()) return full;

        int expected = nums.get(0);

        for (int n : nums) {
            while (expected < n && jokerCount > 0) {
                full.add(expected);
                expected++;
                jokerCount--;
            }
            full.add(n);
            expected = n + 1;
        }

        while (jokerCount-- > 0)
            full.add(expected++);

        return full;
    }

    // ============================================================
    // SET / RUN VALIDATION
    private boolean isValidSet(List<Integer> nums, List<String> colors, int jokerCount) {

        Set<Integer> ns = new HashSet<>();
        for (int n : nums)
            if (n != 0)
                ns.add(n);

        if (ns.size() != 1)
            return false;

        List<String> realColors = new ArrayList<>();
        for (String c : colors)
            if (!c.equals("J"))
                realColors.add(c);

        Set<String> unique = new HashSet<>(realColors);
        if (unique.size() != realColors.size())
            return false;

        return unique.size() + jokerCount >= 3;
    }

    private boolean isValidRun(List<Integer> nums, List<String> colors, int jokerCount) {
        String col = null;

        for (String c : colors) {
            if (!c.equals("J")) {
                if (col == null)
                    col = c;
                else if (!col.equals(c))
                    return false;
            }
        }

        List<Integer> real = new ArrayList<>();
        for (int n : nums)
            if (n != 0)
                real.add(n);

        if (real.isEmpty())
            return false;

        Collections.sort(real);

        int gaps = 0;
        for (int i = 1; i < real.size(); i++)
            gaps += (real.get(i) - real.get(i - 1) - 1);

        return gaps <= jokerCount;
    }

    // ============================================================
    // BOARD CONSISTENCY CHECK
    private boolean validateBoardConsistency(List<List<String>> oldBoard, List<List<String>> newBoard) {
        // 1. oldBoard flatten 후 타일 개수 카운트
        Map<String, Integer> oldCount = new HashMap<>();
        for (List<String> meld : oldBoard) {
            for (String t : meld) {
                oldCount.put(t, oldCount.getOrDefault(t, 0) + 1);
            }
        }

        // 2. newBoard flatten 후 타일 개수 카운트
        Map<String, Integer> newCount = new HashMap<>();
        for (List<String> meld : newBoard) {
            for (String t : meld) {
                newCount.put(t, newCount.getOrDefault(t, 0) + 1);
            }
        }

        // 3. oldBoard 에 있던 모든 타일이
        //    newBoard 에 "같은 개수 이상" 존재하는지 확인
        for (Map.Entry<String, Integer> e : oldCount.entrySet()) {
            String tile = e.getKey();
            int cntOld = e.getValue();
            int cntNew = newCount.getOrDefault(tile, 0);

            if (cntNew < cntOld) {
                // 기존 보드에 있던 타일이 사라졌거나 개수가 줄었음 → 불법
                System.out.println("[RULE] Board tile removed illegally: " + tile
                        + " (old=" + cntOld + ", new=" + cntNew + ")");
                return false;
            }
        }
        return true;
    }

    // ============================================================
    // 남은 조각 멜드 유효성 검사
    private boolean validateRemainingMeldsAfterRearrange(List<List<String>> oldBoard,
                                                        List<List<String>> newBoard) {
        jokerValueMap.clear();

        for (int mi = 0; mi < newBoard.size(); mi++) {

            List<String> meld = newBoard.get(mi);

            // 길이 3 미만이면 무조건 불가 (SET/RUN 최소 길이)
            if (meld.size() < 3) {
                System.out.println("[RULE] Invalid meld length (<3) at index " + mi + ": " + meld);
                return false;
            }

            // 기존에 사용하던 조커 포함 검증 + 조커 값 확정 로직 재사용
            if (!validateAndFixJoker(meld, mi)) {
                System.out.println("[RULE] Invalid meld after rearrange at index " + mi + ": " + meld);
                return false;
            }
        }
        return true;
    }

    private boolean oldBoardContains(List<List<String>> oldBoard, String tile) {
        for (List<String> meld : oldBoard)
            if (meld.contains(tile))
                return true;
        return false;
    }

    // ============================================================
    // JUST PLAYED TILES
    private List<String> calcJustPlayedTilesCorrect(List<List<String>> oldBoard,
                                                    List<List<String>> newBoard) {

        List<String> oldFlat = new ArrayList<>();
        for (List<String> m : oldBoard)
            oldFlat.addAll(m);

        List<String> newFlat = new ArrayList<>();
        for (List<String> m : newBoard)
            newFlat.addAll(m);

        List<String> diff = new ArrayList<>(newFlat);
        diff.removeAll(oldFlat);

        return diff;
    }

    private List<List<String>> deepCopy(List<List<String>> src) {
        List<List<String>> out = new ArrayList<>();
        for (List<String> m : src)
            out.add(new ArrayList<>(m));
        return out;
    }

    // ============================================================
    // SCORE
    // 최종 보드 기준으로 점수 계산 (조커 값은 jokerValueMap 에서 읽음)
    private int computeMeldScore(List<List<String>> board, int meldIndex) {

        List<String> meld = board.get(meldIndex);
        int sum = 0;

        for (int ti = 0; ti < meld.size(); ti++) {

            String t = meld.get(ti);

            if (t.contains("Joker")) {
                sum += jokerValueMap.getOrDefault(meldIndex + ":" + ti, 0);
            } else {
                sum += Integer.parseInt(t.replaceAll("[^0-9]", ""));
            }
        }

        return sum;
    }

    // ============================================================
    // PARSE (decode board)
    public List<List<String>> parseMoveData(String moveData) {

        jokerValueMap.clear();

        List<List<String>> out = new ArrayList<>();
        if (moveData == null || moveData.isBlank())
            return out;

        String[] melds = moveData.split(";");

        for (int mi = 0; mi < melds.length; mi++) {

            String[] tiles = melds[mi].split(",");
            List<String> meld = new ArrayList<>();

            for (int ti = 0; ti < tiles.length; ti++) {

                String raw = tiles[ti].trim();

                if (raw.contains("(")) {
                    String id = raw.substring(0, raw.indexOf("("));
                    int val = Integer.parseInt(raw.substring(raw.indexOf("(") + 1, raw.indexOf(")")));

                    jokerValueMap.put(mi + ":" + ti, val);
                    meld.add(id);
                } else {
                    meld.add(raw);
                }
            }
            out.add(meld);
        }
        return out;
    }

    // ============================================================
    // BASIC SYSTEM
    public boolean hasWon(String playerName) {
        return hands.get(playerName) != null && hands.get(playerName).isEmpty();
    }

    public String getCurrentTurnPlayer() {
        if (turnOrder.isEmpty())
            return null;
        return turnOrder.get(turnIndex);
    }

    public String nextTurnAndGetPlayer() {
        if (turnOrder.isEmpty())
            return null;

        playedThisTurn.put(getCurrentTurnPlayer(), false);

        turnIndex = (turnIndex + 1) % turnOrder.size();
        return turnOrder.get(turnIndex);
    }

    public List<String> getHand(String playerName) {
        List<String> original = hands.get(playerName);
        return original == null ? new ArrayList<>() : new ArrayList<>(original);
    }

    public String drawRandomTileFor(String player) {
        if (!player.equals(getCurrentTurnPlayer()))
            return null;

        String tile = drawFromPool();
        if (tile != null)
            hands.get(player).add(tile);

        return tile;
    }

    private String drawFromPool() {
        if (tilePool.isEmpty())
            return null;
        return tilePool.remove(0);
    }

    public void onPlayerJoin(String name) {

        hands.putIfAbsent(name, new ArrayList<>());
        initialMeldDone.putIfAbsent(name, false);

        if (!turnOrder.contains(name))
            turnOrder.add(name);

        totalScores.putIfAbsent(name, 0);

        if (hands.get(name).isEmpty()) {
            for (int i = 0; i < 14; i++) {
                String t = drawFromPool();
                if (t != null)
                    hands.get(name).add(t);
            }
        }
    }

    public void onPlayerLeave(String name) {

        turnOrder.remove(name);
        hands.remove(name);
        initialMeldDone.remove(name);
        totalScores.remove(name);

        if (turnIndex >= turnOrder.size())
            turnIndex = 0;
    }

    // ============================================================
    // ROUND END & SCORE
    // 라운드가 끝났을 때(누군가 이겼을 때) 호출
    public void onRoundWin(String winnerName) {
        endRoundAndUpdateScores(winnerName);
    }

    // 승자/패자 점수 계산
    private void endRoundAndUpdateScores(String winner) {

        int winnerDelta = 0;

        for (String player : turnOrder) {

            List<String> hand = hands.getOrDefault(player, new ArrayList<>());
            int remainScore = computeHandScore(hand);

            if (player.equals(winner)) {
                // 승자는 일반적으로 손패가 0장이므로 별도 처리 X
                continue;
            } else {
                // 패자: 자기 손패 점수만큼 -점수
                int cur = totalScores.getOrDefault(player, 0);
                totalScores.put(player, cur - remainScore);

                // 승자에게 더해줄 점수 누적
                winnerDelta += remainScore;
            }
        }

        // 승자: (모든 패자 손패 점수 합)만큼 +
        int curWinScore = totalScores.getOrDefault(winner, 0);
        totalScores.put(winner, curWinScore + winnerDelta);

        System.out.println("[SCORE] Round end. Winner = " + winner);
        for (String p : turnOrder) {
            System.out.println("   - " + p + " : " + totalScores.getOrDefault(p, 0));
        }
    }

    // 손에 남은 타일 점수 계산
    private int computeHandScore(List<String> hand) {
        int sum = 0;
        for (String t : hand) {
            if (t.contains("Joker")) {
                // 조커는 페널티 크게
                sum += 30;
            } else {
                try {
                    sum += Integer.parseInt(t.replaceAll("[^0-9]", ""));
                } catch (NumberFormatException ignored) {
                    // 이상한 문자열이면 0으로
                }
            }
        }
        return sum;
    }

    // Room 쪽에서 SCORE 브로드캐스트할 때 쓸 스냅샷
    public Map<String, Integer> getTotalScoresSnapshot() {
        return new HashMap<>(totalScores);
    }

    // ============================================================
    // COLOR PARSING (정확한 색상 추출)
    private String extractColor(String tile) {

        // 조커는 별도 처리
        if (tile.contains("Joker"))
            return "J";

        // 2글자 색상 먼저 체크 (BL = Blue)
        if (tile.startsWith("BL"))
            return "BL";

        // 1글자 색상
        if (tile.startsWith("B"))
            return "B";   // Black
        if (tile.startsWith("R"))
            return "R";
        if (tile.startsWith("Y"))
            return "Y";

        return ""; // 못 읽을 일이 없지만 안정성 위해
    }

    // ============================================================
    // ENCODE BOARD WITH JOKER VALUES
    public String encodeBoard() {

        StringBuilder sb = new StringBuilder();

        for (int mi = 0; mi < tableMelds.size(); mi++) {

            if (mi > 0)
                sb.append(";");

            List<String> meld = tableMelds.get(mi);

            for (int ti = 0; ti < meld.size(); ti++) {

                if (ti > 0)
                    sb.append(",");

                String t = meld.get(ti);

                if (t.contains("Joker")) {
                    int v = jokerValueMap.getOrDefault(mi + ":" + ti, 0);
                    sb.append(t).append("(").append(v).append(")");
                } else {
                    sb.append(t);
                }
            }
        }

        return sb.toString();
    }

    public boolean playedThisTurn(String p) {
        return playedThisTurn.getOrDefault(p, false);
    }

    public void setPlayedThisTurn(String player, boolean value) {
        playedThisTurn.put(player, value);
    }
}
