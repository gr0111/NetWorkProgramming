package server;

import java.util.*;

public class MeldRearranger {

    // ================================
    // 예외
    // ================================
    public static class RearrangeFailedException extends Exception {
        public RearrangeFailedException(String msg) {
            super(msg);
        }
    }

    // ================================
    // 외부 호출 진입점
    // oldBoard + tilesFromPlayer → 새로운 완전한 멜드 구조
    // ================================
    public static List<List<String>> rearrange(
            List<List<String>> oldBoard,
            List<String> tilesFromPlayer
    ) throws RearrangeFailedException {

        // 1) 풀(pool) 만들기
        List<String> pool = buildTilePool(oldBoard, tilesFromPlayer);

        // 2) 풀을 이용해 멜드 생성
        List<List<String>> newMelds = buildMeldsGreedy(pool);

        // 3) 모든 타일 사용했는지 확인
        int used = newMelds.stream().mapToInt(List::size).sum();
        if (used != pool.size()) {
            throw new RearrangeFailedException("남는 타일 있음 → 재조합 실패");
        }

        return newMelds;
    }

    // ================================
    // 풀 생성
    // ================================
    private static List<String> buildTilePool(List<List<String>> oldBoard, List<String> tilesFromPlayer) {
        List<String> pool = new ArrayList<>();

        // 기존 보드 타일 전부 해제
        for (List<String> meld : oldBoard)
            pool.addAll(meld);

        // 이번 턴에 플레이어가 낸 타일 추가
        pool.addAll(tilesFromPlayer);

        return pool;
    }

    // ================================
    // 그리디 방식 멜드 생성 (Run 우선 → Set)
    // ================================
    private static List<List<String>> buildMeldsGreedy(List<String> pool)
            throws RearrangeFailedException {

        List<String> remain = new ArrayList<>(pool);
        sortTiles(remain);

        List<List<String>> output = new ArrayList<>();
        boolean changed = true;

        while (changed) {
            changed = false;

            // 1) Run 탐색
            List<String> run = tryExtractRun(remain);
            if (run != null) {
                output.add(run);
                remain.removeAll(run);
                changed = true;
                continue;
            }

            // 2) Set 탐색
            List<String> set = tryExtractSet(remain);
            if (set != null) {
                output.add(set);
                remain.removeAll(set);
                changed = true;
            }
        }

        return output;
    }

    // ================================
    // 타일 정렬: 숫자 오름차순
    // ================================
    private static void sortTiles(List<String> tiles) {
        tiles.sort((a, b) -> {
            int na = extractNum(a);
            int nb = extractNum(b);
            return Integer.compare(na, nb);
        });
    }

    // ================================
    // Run 추출
    // ================================
    private static List<String> tryExtractRun(List<String> tiles) {

        // 색깔별 그룹화
        Map<String, List<String>> byColor = new HashMap<>();

        for (String t : tiles) {
            String c = extractColor(t);
            if (c.equals("J")) continue;
            byColor.computeIfAbsent(c, k -> new ArrayList<>()).add(t);
        }

        for (String color : byColor.keySet()) {

            List<String> sameColor = new ArrayList<>(byColor.get(color));
            sameColor.sort(Comparator.comparingInt(MeldRearranger::extractNum));

            // 숫자 기반 연속 run 찾기
            List<String> run = new ArrayList<>();
            int prev = -999;

            for (String t : sameColor) {
                int num = extractNum(t);

                if (run.isEmpty()) {
                    run.add(t);
                    prev = num;
                } else if (num == prev + 1) {
                    run.add(t);
                    prev = num;
                }
            }

            // 조커 보완
            if (run.size() >= 2) {
                int expected = extractNum(run.get(0)) + 1;

                for (String t : tiles) {
                    if (isJoker(t) && run.size() < 13) {
                        // 조커를 중간에 넣는 것은 서버 검증 시 validateAndFixJoker()가 처리해줌
                        run.add(t);
                        break;
                    }
                }
            }

            if (run.size() >= 3)
                return run;
        }

        return null;
    }

    // ================================
    // Set 추출
    // ================================
    private static List<String> tryExtractSet(List<String> tiles) {

        Map<Integer, List<String>> byNum = new HashMap<>();

        for (String t : tiles) {
            if (isJoker(t)) continue;

            int n = extractNum(t);
            byNum.computeIfAbsent(n, k -> new ArrayList<>()).add(t);
        }

        for (int num : byNum.keySet()) {

            List<String> sameNum = new ArrayList<>(byNum.get(num));

            // 색 중복 제거 위해
            Set<String> colors = new HashSet<>();
            for (String t : sameNum)
                colors.add(extractColor(t));

            List<String> set = new ArrayList<>(sameNum);

            // 조커 보완
            for (String t : tiles) {
                if (isJoker(t) && set.size() < 4) {
                    set.add(t);
                }
            }

            if (set.size() >= 3)
                return set;
        }

        return null;
    }

    // ================================
    // 유틸 함수
    // ================================
    private static boolean isJoker(String t) {
        return t.contains("Joker");
    }

    private static int extractNum(String t) {
        if (isJoker(t)) return 0;
        String num = t.replaceAll("[^0-9]", "");
        return Integer.parseInt(num);
    }

    private static String extractColor(String tile) {

        if (tile.contains("Joker"))
            return "J";

        // 2글자 색상 BL
        if (tile.startsWith("BL"))
            return "BL";

        // 1글자 색상
        if (tile.startsWith("B"))
            return "B";
        if (tile.startsWith("R"))
            return "R";
        if (tile.startsWith("Y"))
            return "Y";

        return "";
    }
}
