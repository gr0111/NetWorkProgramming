package server;

import java.util.*;

public class GameCore {

    private Map<String, List<String>> hands = new HashMap<>();
    private List<String> turnOrder = new ArrayList<>();
    private int turnIndex = 0;

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
    // PLAY HANDLING
    // ============================================================
    public boolean handlePlay(String playerName, String moveData) {
        playedThisTurn.putIfAbsent(playerName, false);

        if (!playerName.equals(getCurrentTurnPlayer()))
            return false;

        List<List<String>> oldBoard = deepCopy(tableMelds);
        List<List<String>> newBoard = parseMoveData(moveData);

        // 1) 기존 보드 타일 삭제 여부 검사
        if (!validateBoardConsistency(oldBoard, newBoard)) {
            System.out.println("[RULE] Board tile removed illegally.");
            return false;
        }

        // 2) 플레이어 손패에 없는 타일 사용 방지
        for (List<String> meld : newBoard) {
            for (String t : meld) {

                if (oldBoardContains(oldBoard, t))
                    continue;

                if (!hands.get(playerName).contains(t)) {
                    System.out.println("[RULE] Illegal tile usage: " + t);
                    return false;
                }
            }
        }

        // 3) 멜드 유효성 + 조커 값 확정
        for (int mi = 0; mi < newBoard.size(); mi++) {
            if (!validateAndFixJoker(newBoard.get(mi), mi)) {
                System.out.println("[RULE] Invalid meld at index " + mi);
                return false;
            }
        }

        // 4) 이번 턴 실제로 낸 타일 계산
        List<String> justPlayed = calcJustPlayedTilesCorrect(oldBoard, newBoard);

        // 5) 타일을 실제로 1개도 안 냈으면 제출 실패
        if (justPlayed.isEmpty()) {
            System.out.println("[RULE] No tiles played.");
            return false;
        }

        // 6) 초기 30 검사
        if (!initialMeldDone.getOrDefault(playerName, false)) {

            int sum = 0;

            for (List<String> meld : newBoard) {
                boolean used = false;

                for (String t : meld)
                    if (justPlayed.contains(t)) used = true;

                if (used)
                    sum += computeMeldScore(meld);
            }

            if (sum < 30) {
                System.out.println("[RULE] Initial 30 failed: " + sum);
                return false;
            }

            initialMeldDone.put(playerName, true);
        }

        // 7) 손패에서 제거
        List<String> hand = hands.get(playerName);
        for (String t : justPlayed)
            hand.remove(t);

        // 8) 서버 보드 교체
        tableMelds = deepCopy(newBoard);

        playedThisTurn.put(playerName, true);
        return true;
    }

    // ============================================================
    // VALIDATION (조커 포함)
    // ============================================================
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
    // ============================================================
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
    // ============================================================
    private boolean validateBoardConsistency(List<List<String>> oldBoard, List<List<String>> newBoard) {

        List<String> oldFlat = new ArrayList<>();
        for (List<String> m : oldBoard)
            oldFlat.addAll(m);

        List<String> newFlat = new ArrayList<>();
        for (List<String> m : newBoard)
            newFlat.addAll(m);

        for (String t : oldFlat) {
            if (!newFlat.remove(t)) {
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
    // ============================================================
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
    // ============================================================
    private int computeMeldScore(List<String> meld) {

        int meldIndex = tableMelds.indexOf(meld);
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
    // ============================================================
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
    // ============================================================
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

        if (turnIndex >= turnOrder.size())
            turnIndex = 0;
    }

    // ============================================================
    // MELD POSSIBILITY (FORCED DRAW)
    // ============================================================
    public boolean canPlayAnyMeld(String player) {
        return false;   // 항상 false → "낼 수 있는 상태입니다" 절대 발생 안함
    }


    private int computeRawScore(List<String> meld) {
        int sum = 0;
        for (String t : meld)
            sum += Integer.parseInt(t.replaceAll("[^0-9]", ""));
        return sum;
    }

    private boolean isValidMeld(List<String> meld) {

        if (meld.size() < 3)
            return false;

        List<Integer> nums = new ArrayList<>();
        List<String> colors = new ArrayList<>();
        int jc = 0;

        for (String t : meld) {
            if (t.contains("Joker")) {
                nums.add(0);
                colors.add("J");
                jc++;
            } else {
                nums.add(Integer.parseInt(t.replaceAll("[^0-9]", "")));
                colors.add(extractColor(t));  
            }
        }

        return isValidSet(nums, colors, jc) || isValidRun(nums, colors, jc);
    }

    // ============================================================
    // COLOR PARSING (정확한 색상 추출)
    // ============================================================
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
    // ============================================================
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

    public boolean forceDrawIfNeeded(String player) {

        if (playedThisTurn(player))
            return true;

        String tile = drawRandomTileFor(player);
        System.out.println("[AUTO DRAW] " + player + " → " + tile);

        playedThisTurn.put(player, false);

        return true;
    }
}
