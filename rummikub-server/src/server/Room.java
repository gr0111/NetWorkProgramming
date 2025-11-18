package server;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Room {
    private List<ClientSession> players = new CopyOnWriteArrayList<>();
    private int currentTurn = 0;
    private boolean gameStarted = false;

    public void addPlayer(ClientSession session) {
        players.add(session);
        broadcast("👥 현재 참가자 수: " + players.size());

        if (players.size() >= 2 && !gameStarted) {
            startGame();
        }
    }

    public void removePlayer(ClientSession session) {
        players.remove(session);
        broadcast("❌ " + session.getPlayerName() + " 님이 나갔습니다.");
    }

    public void broadcast(String msg) {
        for (ClientSession p : players) {
            p.send(msg);
        }
    }

    public void handleMessage(String sender, String msg) {
        // 기본 메시지는 모두 브로드캐스트
        broadcast("[" + sender + "] " + msg);

        // 나중에 Protocol.java 기반으로 구체적인 명령 처리 (예: /turn, /play 등)
        if (msg.equalsIgnoreCase("/next")) {
            nextTurn();
        }
    }

    private void startGame() {
        gameStarted = true;
        broadcast("🚀 게임을 시작합니다!");
        notifyTurn();
    }

    private void notifyTurn() {
        if (players.isEmpty()) return;
        ClientSession current = players.get(currentTurn);
        broadcast("🎯 현재 턴: " + current.getPlayerName());
    }

    public void nextTurn() {
        if (players.isEmpty()) return;
        currentTurn = (currentTurn + 1) % players.size();
        notifyTurn();
    }
}
