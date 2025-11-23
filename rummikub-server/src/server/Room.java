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

    // 방장(방 만든 사람) + 게임 시작 여부
    private String ownerName = null;
    private boolean gameStarted = false;

    // 게임 시작 최소 인원 (버튼 활성화 기준에도 쓰일 수 있음)
    private static final int MIN_PLAYER_TO_START = 2;

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

        // ✅ 첫 입장자 → 방장 지정 + OWNER 알림
        if (ownerName == null) {
            ownerName = session.getPlayerName();
            // 클라이언트 팀원이 만든 Protocol.OWNER ("OWNER") 형식에 맞춰 전송
            sendTo(ownerName, "OWNER|true");
            sendTo(ownerName, "INFO|당신은 방장입니다. [게임 시작] 버튼으로 게임을 시작할 수 있습니다.");
        }

        // 입장 브로드캐스트
        broadcast("INFO|" + session.getPlayerName() + " 님이 방에 입장했습니다. (" + name + ")");

        // ✅ 인원 수 브로드캐스트 (PLAYER_COUNT)
        // 예: "PLAYER_COUNT|3"
        broadcast("PLAYER_COUNT|" + players.size());

        // ❌ 자동 시작은 넣지 않음 (방장이 START_GAME 눌렀을 때만 시작)
        // 만약 자동 시작도 하고 싶으면 여기서:
        // if (!gameStarted && players.size() >= MIN_PLAYER_TO_START) { startGame(); }
        // 를 추가하면 됨.
    }

    public void removePlayer(ClientSession session) {
        players.remove(session);
        broadcast("INFO|" + session.getPlayerName() + " 님이 방에서 나갔습니다.");
        gameCore.onPlayerLeave(session.getPlayerName());

        // ✅ 인원 수 브로드캐스트 (퇴장 시에도)
        broadcast("PLAYER_COUNT|" + players.size());

        // ✅ 방장이 나갔으면 새 방장 승계 + OWNER 알림
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

    /** 방 전체에 메시지 브로드캐스트 */
    public void broadcast(String msg) {
        for (ClientSession s : players) {
            try {
                s.send(msg);
            } catch (Exception ignored) {}
        }
    }

    /** 특정 플레이어에게만 전송 */
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

    /** 방장이 [게임 시작] 눌렀을 때 호출 (클라에서 START_GAME 전송) */
    public void requestStartGame(String requesterName) {
        // 방장인지 확인
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** 실제 게임 시작 처리 */
    private void startGame() throws IOException {
        gameStarted = true;

        // 1) 게임 시작 알림
        // 클라에서 GAME_START 수신 → 게임 화면 전환 등에 사용
        broadcast("GAME_START|" + players.size());

        // 2) 각 플레이어에게 초기 손패(14장) 전송
        for (ClientSession s : players) {
            String pn = s.getPlayerName();
            List<String> hand = gameCore.getHand(pn); // ["R1","BL3",...]
            String tilesCsv = String.join(",", hand); // "R1,BL3,..."
            s.send("INITIAL_TILES|" + tilesCsv);
        }

        // 3) 첫 턴 알림
        String first = gameCore.getCurrentTurnPlayer();
        if (first != null) {
            broadcast("TURN|" + first);
        }
    }

    // ===== 게임 관련 =====

    public void handlePlay(String playerName, String moveData) {
        boolean ok = gameCore.handlePlay(playerName, moveData);
        if (ok) {
            broadcast("PLAY_OK|" + playerName + "|" + moveData);
            String nextPlayer = gameCore.nextTurnAndGetPlayer();
            if (nextPlayer != null) {
                broadcast("TURN|" + nextPlayer);
            }
        } else {
            sendTo(playerName, "PLAY_FAIL|규칙에 맞지 않는 수입니다.");
        }
    }

    public void handleNoTile(String playerName) {
        String tileId = gameCore.drawRandomTileFor(playerName); // "R5", "BJoker" 등
        sendTo(playerName, "NEW_TILE|" + tileId);

        String nextPlayer = gameCore.nextTurnAndGetPlayer();
        if (nextPlayer != null) {
            broadcast("TURN|" + nextPlayer);
        }
    }
}
