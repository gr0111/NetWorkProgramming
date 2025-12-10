package server;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import server.GameCore;

public class Room {

    private final int id;
    private final String name;
    private final GameServer server;

    private final List<ClientSession> players = new CopyOnWriteArrayList<>();
    private final GameCore gameCore = new GameCore();

    private String ownerName = null;
    private boolean gameStarted = false;

    private static final int MIN_PLAYER_TO_START = 2;
    private static final int MAX_PLAYERS = 4;

    public Room(int id, String name, GameServer server) {
        this.id = id;
        this.name = name;
        this.server = server;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public int getPlayerCount() { return players.size(); }


    // ============================================================
    // PLAYER JOIN
    public void addPlayer(ClientSession session) {

        players.add(session);
        String pn = session.getPlayerName();
        gameCore.onPlayerJoin(pn);

        if (ownerName == null) {
            ownerName = pn;
            sendTo(ownerName, "OWNER|true");
            sendTo(ownerName, "INFO|당신은 방장입니다.");
        }

        broadcast("INFO|" + pn + "님이 입장했습니다.");
        broadcast("PLAYER_COUNT|" + players.size());

        if (!gameStarted && players.size() >= MAX_PLAYERS) {
            try { startGame(); }
            catch (IOException e) {
                System.err.println("자동 게임 시작 실패");
            }
        }
    }


    // ============================================================
    // PLAYER LEAVE
    public void removePlayer(ClientSession session) {

        players.remove(session);
        String pn = session.getPlayerName();

        if (players.size() == 1 && gameStarted) {
            String winner = players.get(0).getPlayerName();
            broadcast("GAME_END|" + winner);
            resetRoomState();
            return;
        }

        gameCore.onPlayerLeave(pn);

        broadcast("INFO|" + pn + "님이 나갔습니다.");
        broadcast("PLAYER_COUNT|" + players.size());

        if (pn.equals(ownerName)) {
            if (!players.isEmpty()) {
                ownerName = players.get(0).getPlayerName();
                sendTo(ownerName, "OWNER|true");
                broadcast("INFO|새 방장은 " + ownerName + "님입니다.");
            } else {
                ownerName = null;
            }
        }
    }


    // ============================================================
    // GAME START
    public void requestStartGame(String requester) {

        if (!requester.equals(ownerName)) {
            sendTo(requester, "ERROR|방장만 시작할 수 있습니다.");
            return;
        }
        if (gameStarted) {
            sendTo(requester, "ERROR|이미 시작됨");
            return;
        }
        if (players.size() < MIN_PLAYER_TO_START) {
            sendTo(requester, "ERROR|" + MIN_PLAYER_TO_START + "명 이상 필요합니다.");
            return;
        }

        try { startGame(); }
        catch (IOException e) {
            sendTo(requester, "ERROR|게임 시작 오류 발생");
        }
    }

    private void startGame() throws IOException {

        gameStarted = true;
        broadcast("GAME_START|" + players.size());

        for (ClientSession s : players) {
            String pn = s.getPlayerName();
            s.send("INITIAL_TILES|" + String.join(",", gameCore.getHand(pn)));
        }

        broadcast("TURN|" + gameCore.getCurrentTurnPlayer());
    }


    // ============================================================
    // PLAY SUBMISSION
    // ============================================================
    public void handlePlay(String playerName, String meldData) {

        if (!playerName.equals(gameCore.getCurrentTurnPlayer())) {
            sendTo(playerName, "ERROR|당신의 턴이 아닙니다.");
            return;
        }

        if (meldData == null || meldData.isBlank()) {
            sendTo(playerName, "PLAY_FAIL|제출된 타일이 없습니다.");
            return;
        }

        boolean ok = gameCore.handlePlay(playerName, meldData);

        if (!ok) {
            sendTo(playerName, "PLAY_FAIL|규칙 위반 또는 초기 30 부족");
            return;
        }

        // 서버 안전 장치: play 성공 보정
        gameCore.setPlayedThisTurn(playerName, true);

                broadcast("PLAY_OK|" + playerName + "|" + gameCore.encodeBoard());

        // ✅ 승리 체크 + 라운드 점수 정산
        if (gameCore.hasWon(playerName)) {
            // 라운드 점수/순위 계산
            gameCore.onRoundWin(playerName);

            // (선택) 점수 브로드캐스트 하고 싶으면 이런 식으로:
            // Map<String, Integer> scores = gameCore.getTotalScoresSnapshot();
            // for (var entry : scores.entrySet()) {
            //     broadcast("SCORE|" + entry.getKey() + "|" + entry.getValue());
            // }

            broadcast("GAME_END|" + playerName);
            resetRoomState();
            return;
        }

        String next = gameCore.nextTurnAndGetPlayer();
        broadcast("TURN|" + next);
    }

    // ============================================================
    // DRAW TILE / NEXT 턴
    public void handleNoTile(String playerName) {

        if (!playerName.equals(gameCore.getCurrentTurnPlayer())) {
            sendTo(playerName, "ERROR|당신의 턴이 아닙니다.");
            return;
        }

        boolean ok = gameCore.forceDrawIfNeeded(playerName);

        if (!ok) {
            sendTo(playerName, "DRAW_FAIL|낼 수 있는 상태입니다.");
            return;
        }

        // 드로우 가능한 경우(준강제 포함)
        if (!gameCore.playedThisTurn(playerName)) {

            String tile = gameCore.drawRandomTileFor(playerName);

            if (tile != null)
                sendTo(playerName, "NEW_TILE|" + tile);
        }

        String next = gameCore.nextTurnAndGetPlayer();
        broadcast("TURN|" + next);
    }


    // ============================================================
    // MESSAGE SENDING
    public void broadcast(String msg) {
        for (ClientSession s : players) s.send(msg);
    }

    public void sendTo(String name, String msg) {
        for (ClientSession s : players)
            if (s.getPlayerName().equals(name)) {
                s.send(msg);
                return;
            }
    }

    private void resetRoomState() {
        gameStarted = false;
    }
}
