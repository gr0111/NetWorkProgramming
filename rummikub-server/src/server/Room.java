package server;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Room {

    private final int id;
    private final String name;
    private final GameServer server;

    private final List<ClientSession> players = new CopyOnWriteArrayList<>();
    private final GameCore gameCore = new GameCore(); // 나중에 게임 규칙 연동용

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
        gameCore.onPlayerJoin(session.getPlayerName()); // 나중에 손패/점수 관리에 활용
        broadcast("INFO|" + session.getPlayerName() + " 님이 방에 입장했습니다. (" + name + ")");
        // 필요하면 게임 시작 조건 체크 (예: 2명 이상이면 startGame())
    }

    public void removePlayer(ClientSession session) {
        players.remove(session);
        broadcast("INFO|" + session.getPlayerName() + " 님이 방에서 나갔습니다.");
        gameCore.onPlayerLeave(session.getPlayerName());
    }

    /** 방 전체에 메시지 전송 */
    public void broadcast(String msg) {
        for (ClientSession s : players) {
            try {
                s.send(msg);
            } catch (IOException ignored) {}
        }
    }

    /** 특정 플레이어에게만 보내고 싶으면 사용 */
    public void sendTo(String playerName, String msg) {
        for (ClientSession s : players) {
            if (s.getPlayerName().equals(playerName)) {
                try {
                    s.send(msg);
                } catch (IOException ignored) {}
                break;
            }
        }
    }

    // ===== 게임 관련 메서드 (GameCore와 연동) =====

    /** 플레이어가 타일을 낸 경우 */
    public void handlePlay(String playerName, String moveData) {
        // moveData: 클라이언트가 보낸 타일 정보(문자열, 예: "R1,R2,R3")라고 가정
        boolean ok = gameCore.handlePlay(playerName, moveData);
        if (ok) {
            broadcast("PLAY_OK|" + playerName + "|" + moveData);
            // 턴 전환 등도 GameCore에서 관리하고, 여기서 알림 보낼 수 있음
            String nextPlayer = gameCore.getCurrentTurnPlayer();
            broadcast("TURN|" + nextPlayer);
        } else {
            sendTo(playerName, "PLAY_FAIL|규칙에 맞지 않는 수입니다.");
        }
    }

    /** 낼 타일이 없어서 한 장 뽑는 경우 */
    public void handleNoTile(String playerName) {
        String tileId = gameCore.drawRandomTileFor(playerName); // 예: "R5", "BJoker"
        sendTo(playerName, "NEW_TILE|" + tileId);
        // 턴 넘기기
        String nextPlayer = gameCore.nextTurnAndGetPlayer();
        broadcast("TURN|" + nextPlayer);
    }
}
