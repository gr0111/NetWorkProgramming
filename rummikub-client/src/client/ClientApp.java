package client;

import javax.swing.*;

public class ClientApp implements NetIO.MessageHandler {

    private final NetIO net = new NetIO();

    private LoginView login;
    private LobbyView lobby;
    private RoomView room;

    private String myName;

    private boolean isOwner = false;
    private int playerCount = 0;

    // ⭐ INITIAL_TILES 패킷이 너무 일찍 도착할 때를 대비한 버퍼
    private String pendingInitialTiles = null;

    public ClientApp() {
        net.setHandler(this);
    }

    public void setLogin(LoginView login) { this.login = login; }
    public String myName() { return myName; }

    // ===========================================================
    // 로그인 & 로비 이동
    // ===========================================================
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

    // ===========================================================
    // 로비 화면으로 돌아가기
    // ===========================================================
    public void showLobby() {
        SwingUtilities.invokeLater(() -> {

            if (room != null) {
                room.dispose();
                room = null;
            }

            if (lobby == null) {
                lobby = new LobbyView(this);
            }

            lobby.setVisible(true);
            lobby.toFront();
            requestRoomList();
        });
    }


    public void requestRoomList() { net.send("LIST"); }
    public void requestCreateRoom(String roomName) { net.send("CREATE|" + roomName); }
    public void requestJoinRoom(int roomId) { net.send("JOIN|" + roomId); }

    public void send(String line) { net.send(line); }

    // ===========================================================
    // 서버 메시지 처리
    // ===========================================================
    @Override
    public void onMessage(String line) {

        String type = line;
        String data = "";
        int idx = line.indexOf('|');
        if (idx >= 0) { type = line.substring(0, idx); data = line.substring(idx + 1); }

        switch (type) {

            case "ROOM_LIST":
                if (lobby != null) lobby.updateRoomList(data);
                break;

            case "JOIN_OK": {
                final String roomId = data;

                SwingUtilities.invokeLater(() -> {
                    room = new RoomView(this, roomId);
                    room.setVisible(true);

                    // ⭐ 대기 중이던 INITIAL_TILES 적용
                    if (pendingInitialTiles != null) {
                        room.setInitialHand(pendingInitialTiles);
                        room.appendLog("내 손패: " + pendingInitialTiles);
                        pendingInitialTiles = null;
                    }

                    if (lobby != null) lobby.dispose();
                    refreshStartButton();
                });
                break;
            }

            case "OWNER":
                isOwner = "true".equalsIgnoreCase(data.trim());
                refreshStartButton();
                if (room != null)
                    room.appendLog(isOwner ? "당신은 방장입니다." : "방장 권한 없음");
                break;

            case "PLAYER_COUNT":
                try { playerCount = Integer.parseInt(data.trim()); } catch (Exception ignore) {}
                refreshStartButton();
                if (room != null) room.appendLog("현재 인원: " + playerCount);
                break;

            case "INFO":
                if (room != null) room.appendLog(data);
                else if (lobby != null) lobby.showInfo(data);
                break;

            case "CHAT":
                if (room != null) room.appendLog(data);
                break;

            case "TURN":
                if (room != null) room.updateTurn(data);
                break;

            case "GAME_END": {
                String winner = data.trim();
                if (room != null) room.showGameEndPopup(winner);
                break;
            }

            case "GAME_START":
                if (room != null) {
                    room.appendLog("게임 시작 (" + data + ")");
                    room.setStartEnabled(false);
                }
                break;

            case "INITIAL_TILES":
                if (room != null) {
                    room.setInitialHand(data);
                    room.appendLog("내 손패: " + data);
                } else {
                    // ⭐ RoomView 생성 전 → 일단 저장해둔다
                    pendingInitialTiles = data;
                }
                break;

            case "PLAY_OK": {
                String[] p = data.split("\\|");
                if (room != null) room.applyPlayOk(p[0], p[1]);
                break;
            }

            case "PLAY_FAIL":
                if (room != null) {
                    room.appendLog("⛔ 규칙 위반! 제출이 취소되었습니다.");
                    room.restoreJustPlayedTiles();
                }
                break;

            case "NEW_TILE":
                if (room != null) {
                    room.addHandTile(data.trim());
                    room.appendLog("새 타일: " + data);
                }
                break;

            case "SCORE": {
                String[] p = data.split("\\|");
                if (p.length >= 2 && room != null) {
                    String player = p[0];
                    int score = 0;
                    try { score = Integer.parseInt(p[1].trim()); } catch (NumberFormatException ignored) {}
                    room.updateScore(player, score);
                }
                break;
            }

            default:
                if (room != null) room.appendLog(line);
                else if (lobby != null) lobby.showInfo(line);
        }
    }

    public int getPlayerCount() {
        return playerCount;
    }

    private void refreshStartButton() {
        if (room != null) room.setStartEnabled(isOwner && playerCount >= 2);
    }
}
