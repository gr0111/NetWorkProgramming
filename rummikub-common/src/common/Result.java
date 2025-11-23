package common;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 게임 종료 후 결과 정보 저장
 */
public class Result implements Serializable {
    private static final long serialVersionUID = 1L;

    private Map<String, Integer> scores = new LinkedHashMap<>();

    public void addScore(String playerName, int score) {
        scores.put(playerName, score);
    }

    public Map<String, Integer> getScores() {
        return scores;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("📊 최종 결과\n");
        scores.forEach((name, score) -> sb.append(name).append(" : ").append(score).append("\n"));
        return sb.toString();
    }
}
