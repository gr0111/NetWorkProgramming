package client;

import javax.swing.*;
import java.util.*;

public class ClientApp implements NetIO.MessageHandler {
    private final NetIO net = new NetIO();

    private LoginView login;
    private LobbyView lobby;
    private RoomView  room;

    private String myName;

    // ▶ 서버 수정 없이 버튼 활성화를 위해 방 상태 최소 추적
    private boolean isOwner = false;
    private int playerCount = 0;   // 현재 방 인원수(추정)

    public ClientApp() { net.setHandler(this); }

    public void setLogin(LoginView login) { this.login = login; }
    public String myName() { return myName; }

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

    public void requestRoomList()                 { net.send("LIST"); }
    public void requestCreateRoom(String roomName){ net.send("CREATE|" + roomName); }
    public void requestJoinRoom(int roomId)       { net.send("JOIN|" + roomId); }
    public void send(String line)                 { net.send(line); }

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
                    // 내가 입장했으니 최소 1명
                    playerCount = 1;
                    isOwner = false;
                    refreshStartButton();
                });
                break;
            }

            case "INFO": {
                final String msg = data;

                // 로그는 우측 채팅창에만
                if (room != null) room.appendLog(msg);
                else if (lobby != null) lobby.showInfo(msg);

                // 방장 안내(초기/승계 모두 케이스 커버)
                if (msg.contains("당신은 방장"))         { isOwner = true;  refreshStartButton(); }
                if (msg.contains("새로운 방장입니다"))   { isOwner = true;  refreshStartButton(); }

                // 입/퇴장 문구 기반으로 인원수 추적
                if (msg.contains("입장했습니다")) { playerCount = Math.max(1, playerCount + 1); refreshStartButton(); }
                if (msg.contains("나갔습니다"))   { playerCount = Math.max(0, playerCount - 1); refreshStartButton(); }

                // 예전 버전 메시지: "현재 참가자 수: N"
                if (msg.contains("현재 참가자 수")) {
                    int n = tryParseCount(msg);
                    if (n >= 0) { playerCount = n; refreshStartButton(); }
                }
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

            case "GAME_START": {
                if (room != null) {
                    room.appendLog("게임 시작 (" + data + "명)");
                    room.setStartEnabled(false); // 시작 후엔 잠시 비활성
                }
                break;
            }

            case "INITIAL_TILES": {
                if (room != null) room.appendLog("내 손패: " + data);
                break;
            }

            default: {
                if (room != null) room.appendLog(line);
                else if (lobby != null) lobby.showInfo(line);
            }
        }
    }

    private void refreshStartButton() {
        if (room == null) return;
        // 방장 + 2인 이상이면 활성화
        room.setStartEnabled(isOwner && playerCount >= 2);
    }

    private static int tryParseCount(String msg) {
        // "현재 참가자 수: 2" 형태에서 숫자만 뽑기
        try {
            int p = msg.indexOf(':');
            if (p >= 0) return Integer.parseInt(msg.substring(p + 1).trim());
        } catch (Exception ignore) {}
        return -1;
    }
}
