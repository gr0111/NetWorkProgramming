package client;

import javax.swing.*;

public class ClientApp implements NetIO.MessageHandler {
    private final NetIO net = new NetIO();

    private LoginView login;
    private LobbyView lobby;
    private RoomView  room;

    private String myName;

    // 시작버튼 활성화를 위한 최소 상태
    private boolean isOwner = false;
    private int playerCount = 0;

    // 🔹 JOIN_OK 전에 도착할 수 있는 초기 신호들을 임시 저장
    private String pendingGameStartCnt = null; // GAME_START payload (인원수)
    private String pendingInitialTiles  = null; // INITIAL_TILES payload (CSV)
    private String pendingTurn          = null; // TURN payload (플레이어명)

    public ClientApp() { net.setHandler(this); }

    public void setLogin(LoginView login) { this.login = login; }
    public String myName() { return myName; }

    /** 로그인 → 서버 접속 */
    public void connectAndLogin(String host, int port, String name) {
        try {
            this.myName = name;
            net.connect(host, port);
            net.send("LOGIN|" + name);

            SwingUtilities.invokeLater(() -> {
                lobby = new LobbyView(this);
                lobby.setVisible(true);
                if (login != null) login.dispose();
                requestRoomList();
            });
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "연결 실패: " + ex.getMessage());
        }
    }

    // === 로비 조작 ===
    public void requestRoomList()                  { net.send("LIST"); }
    public void requestCreateRoom(String roomName) { net.send("CREATE|" + roomName); }
    public void requestJoinRoom(int roomId)        { net.send("JOIN|" + roomId); }

    // === 공통 전송 ===
    public void send(String line) { net.send(line); }

    @Override
    public void onMessage(String line) {
        String type = line, data = "";
        int idx = line.indexOf('|');
        if (idx >= 0) { type = line.substring(0, idx); data = line.substring(idx + 1); }

        switch (type) {
            case "ROOM_LIST": {
                if (lobby != null) lobby.updateRoomList(data);
                break;
            }

            case "JOIN_OK": {
                final String roomId = data;
                SwingUtilities.invokeLater(() -> {
                    room = new RoomView(this, roomId);
                    room.setVisible(true);
                    if (lobby != null) lobby.dispose();

                    // 🔹 JOIN_OK 이후, 버퍼에 있던 초기 신호들을 즉시 반영
                    if (pendingGameStartCnt != null) {
                        room.appendLog("게임 시작 (" + pendingGameStartCnt + "명)");
                        room.setStartEnabled(false);
                        pendingGameStartCnt = null;
                    }
                    if (pendingInitialTiles != null) {
                        room.setInitialHand(pendingInitialTiles);   // 하단 보드 14장 이미지 반영
                        room.appendLog("내 손패: " + pendingInitialTiles);
                        pendingInitialTiles = null;
                    }
                    if (pendingTurn != null) {
                        room.showTurn(pendingTurn);
                        pendingTurn = null;
                    }

                    refreshStartButton();
                });
                break;
            }

            // ====== 버튼 활성화 신호들 ======
            case "OWNER": {
                isOwner = "true".equalsIgnoreCase(data.trim());
                refreshStartButton();
                if (room != null) room.appendLog(isOwner ? "당신은 방장입니다." : "방장 권한이 없습니다.");
                break;
            }
            case "PLAYER_COUNT": {
                try { playerCount = Integer.parseInt(data.trim()); } catch (Exception ignore) {}
                refreshStartButton();
                if (room != null) room.appendLog("현재 인원: " + playerCount + "명");
                break;
            }

            // ====== 일반 안내/채팅/턴 ======
            case "INFO":  {
                if (room != null) room.appendLog(data);
                else if (lobby != null) lobby.showInfo(data);
                break;
            }
            case "CHAT":  {
                if (room != null) room.appendLog(data);
                break;
            }
            case "TURN":  {
                if (room != null) room.showTurn(data);
                else pendingTurn = data; // 🔹 RoomView 생성 전이면 버퍼
                break;
            }

            // ====== 게임 시작 & 초기 타일 ======
            case "GAME_START": {
                if (room != null) {
                    room.appendLog("게임 시작 (" + data + "명)");
                    room.setStartEnabled(false);
                } else {
                    pendingGameStartCnt = data; // 🔹 JOIN_OK 전이면 버퍼
                }
                break;
            }
            case "INITIAL_TILES": {
                if (room != null) {
                    room.setInitialHand(data);         // 하단 보드 초기 14장 이미지 반영
                    room.appendLog("내 손패: " + data);
                } else {
                    pendingInitialTiles = data;        // 🔹 JOIN_OK 전이면 버퍼
                }
                break;
            }

            case "NEW_TILE": {                         // 서버가 보내는 새 타일 1장
                if (room != null) {
                    room.appendLog("새 타일: " + data);
                    room.addHandTile(data.trim());     // 하단 보드에 1장 추가 + 자동 재정렬
                }
                // (RoomView 생성 전 NEW_TILE은 정상 시나리오상 거의 없음)
                break;
            }

            default: {
                if (room != null) room.appendLog(line);
                else if (lobby != null) lobby.showInfo(line);
            }
        }
    }

    /** 방장 & 2인 이상일 때만 [게임 시작] 활성화 */
    private void refreshStartButton() {
        if (room != null) room.setStartEnabled(isOwner && playerCount >= 2);
    }
}
