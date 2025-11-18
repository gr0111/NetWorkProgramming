package server;

import java.util.*;

public class GameCore {
    private Map<String, Integer> playerScores = new HashMap<>();

    public void initPlayers(List<ClientSession> players) {
        for (ClientSession p : players) {
            playerScores.put(p.getPlayerName(), 0);
        }
    }

    public boolean validateMove(String player, String moveData) {
        // TODO: 루미큐브 타일 조합 검증 로직
        return true;
    }

    public void updateScore(String player, int delta) {
        playerScores.put(player, playerScores.get(player) + delta);
    }

    public String getResultSummary() {
        StringBuilder sb = new StringBuilder("📊 최종 점수표\n");
        playerScores.entrySet().stream()
            .sorted((a, b) -> b.getValue() - a.getValue())
            .forEach(e -> sb.append(e.getKey()).append(": ").append(e.getValue()).append("\n"));
        return sb.toString();
    }
}
