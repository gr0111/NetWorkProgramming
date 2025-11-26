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
                final String payload = data;
                if (lobby != null) lobby.updateRoomList(payload);
                break;
            }
            case "JOIN_OK": {
    final String roomId = data;
    SwingUtilities.invokeLater(() -> {
        room = new RoomView(this, roomId);
        room.setVisible(true);
        if (lobby != null) lobby.dispose();


        refreshStartButton(); // 현재 플래그로 즉시 버튼 상태 반영
    });
    break;
}


            // ====== 버튼 활성화 신호들 ======
            case "OWNER": { // 예: OWNER|true (방장만 수신)
                isOwner = "true".equalsIgnoreCase(data.trim());
                refreshStartButton();
                if (room != null) room.appendLog(isOwner ? "당신은 방장입니다." : "방장 권한이 없습니다.");
                break;
            }
            case "PLAYER_COUNT": { // 예: PLAYER_COUNT|3
                try { playerCount = Integer.parseInt(data.trim()); }
                catch (NumberFormatException ignore) {}
                refreshStartButton();
                if (room != null) room.appendLog("현재 인원: " + playerCount + "명");
                break;
            }

            // ====== 일반 안내/채팅/턴 ======
            case "INFO": {
                if (room != null) room.appendLog(data);
                else if (lobby != null) lobby.showInfo(data);
                break;
            }
            case "CHAT": {
                if (room != null) room.appendLog(data);
                break;
            }
            case "TURN": {
                if (room != null) room.showTurn(data);
                break;
            }

            // ====== 게임 시작 & 초기 타일 ======
            case "GAME_START": {
                // 서버에서 랜덤 배분이 이미 수행됨. 클라에선 안내 + 버튼 off
                if (room != null) {
                    room.appendLog("게임 시작 (" + data + "명)");
                    room.setStartEnabled(false);
                }
                break;
            }
            case "INITIAL_TILES": {
                // 서버가 내려준 14장(초기 손패) 표시
                if (room != null) {
                    room.appendLog("내 손패: " + data);
                    // 필요하면 여기서 문자열 파싱해 보드 UI로 반영 가능
                }
                break;
            }

            // ====== 그 외 미정 신호는 로그로만 ======
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
