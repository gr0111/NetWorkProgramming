package server;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Room {

    private final int id;
    private final String name;
    private final GameServer server;

    private final List<ClientSession> players = new CopyOnWriteArrayList<>();
    private final GameCore gameCore = new GameCore();

    private String ownerName = null;
    private boolean gameStarted = false;

    private static final int MIN_PLAYER_TO_START = 2;
    private static final int MAX_PLAYERS = 4;  // ★ 자동 시작 인원 기준 추가

    public Room(int id, String name, GameServer server) {
        this.id = id;
        this.name = name;
        this.server = server;
    }

    public int getId()   { return id; }
    public String getName() { return name; }
    public int getPlayerCount() { return players.size(); }

    public void addPlayer(ClientSession session) throws IOException {
        players.add(session);
        gameCore.onPlayerJoin(session.getPlayerName());

        // 첫 입장자 → 방장 지정
        if (ownerName == null) {
            ownerName = session.getPlayerName();
            sendTo(ownerName, "OWNER|true");
            sendTo(ownerName, "INFO|당신은 방장입니다. [게임 시작] 버튼으로 게임을 시작할 수 있습니다.");
        }

        // 입장 알림
        broadcast("INFO|" + session.getPlayerName() + " 님이 방에 입장했습니다.");
        broadcast("PLAYER_COUNT|" + players.size());

        // -------------------------------
        // ★ 자동 게임 시작 기능 추가
        // -------------------------------
        if (!gameStarted && players.size() >= MAX_PLAYERS) {
            try { 
                startGame();
            } catch (IOException e) { 
                e.printStackTrace();
            }
        }
    }

    public void removePlayer(ClientSession session) {
        players.remove(session);
        broadcast("INFO|" + session.getPlayerName() + " 님이 방에서 나갔습니다.");
        gameCore.onPlayerLeave(session.getPlayerName());
        broadcast("PLAYER_COUNT|" + players.size());

        // 방장이 나갔으면 새 방장 승계
        if (session.getPlayerName().equals(ownerName)) {
            if (!players.isEmpty()) {
                ownerName = players.get(0).getPlayerName();
                sendTo(ownerName, "OWNER|true");
                sendTo(ownerName, "INFO|이제 당신이 새로운 방장입니다.");
            } else {
                ownerName = null;
            }
        }
    }

    public void broadcast(String msg) {
        for (ClientSession s : players) {
            try {
                s.send(msg);
            } catch (Exception ignored) {}
        }
    }

    public void sendTo(String playerName, String msg) {
        for (ClientSession s : players) {
            if (s.getPlayerName().equals(playerName)) {
                try {
                    s.send(msg);
                } catch (Exception ignored) {}
                break;
            }
        }
    }

    public void requestStartGame(String requesterName) {
        if (ownerName == null || !ownerName.equals(requesterName)) {
            sendTo(requesterName, "ERROR|방장만 게임을 시작할 수 있습니다.");
            return;
        }
        if (gameStarted) {
            sendTo(requesterName, "ERROR|이미 게임이 시작되었습니다.");
            return;
        }
        if (players.size() < MIN_PLAYER_TO_START) {
            sendTo(requesterName, "ERROR|최소 " + MIN_PLAYER_TO_START + "명 이상 있어야 게임을 시작할 수 있습니다.");
            return;
        }

        try {
            startGame();
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void startGame() throws IOException {
        gameStarted = true;
        broadcast("GAME_START|" + players.size());

        for (ClientSession s : players) {
            String pn = s.getPlayerName();
            List<String> hand = gameCore.getHand(pn);
            String tilesCsv = String.join(",", hand);
            s.send("INITIAL_TILES|" + tilesCsv);
        }

        String first = gameCore.getCurrentTurnPlayer();
        if (first != null) {
            broadcast("TURN|" + first);
        }
    }

    public void handlePlay(String playerName, String moveData) {
        boolean ok = gameCore.handlePlay(playerName, moveData);
        if (ok) {
            String boardStr = gameCore.encodeBoard();
            broadcast("PLAY_OK|" + playerName + "|" + boardStr);
            String nextPlayer = gameCore.nextTurnAndGetPlayer();
            broadcast("TURN|" + nextPlayer);
        } else {
            sendTo(playerName, "PLAY_FAIL|규칙에 맞지 않는 수입니다.");
        }
    }

    public void handleNoTile(String playerName) {
        String tileId = gameCore.drawRandomTileFor(playerName);
        sendTo(playerName, "NEW_TILE|" + tileId);

        String nextPlayer = gameCore.nextTurnAndGetPlayer();
        if (nextPlayer != null) {
            broadcast("TURN|" + nextPlayer);
        }
    }
}
