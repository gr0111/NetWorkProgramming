package client;

import javax.swing.*;

public class ClientApp implements NetIO.MessageHandler {
    private final NetIO net = new NetIO();

    private LoginView login;
    private LobbyView lobby;
    private RoomView  room;

    private String myName;

    private boolean isOwner = false;
    private int playerCount = 0;

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

    public void requestRoomList()                  { net.send("LIST"); }
    public void requestCreateRoom(String roomName) { net.send("CREATE|" + roomName); }
    public void requestJoinRoom(int roomId)        { net.send("JOIN|" + roomId); }

    public void send(String line) { net.send(line); }

    @Override
    public void onMessage(String line) {
        String type = line, data = "";
        int idx = line.indexOf('|');
        if (idx >= 0) { type = line.substring(0, idx); data = line.substring(idx + 1); }

        switch (type) {
            case "ROOM_LIST": {
                if (lobby != null) lobby.updateRoomList(data); break;
            }
            case "JOIN_OK": {
                final String roomId = data;
                SwingUtilities.invokeLater(() -> {
                    room = new RoomView(this, roomId);
                    room.setVisible(true);
                    if (lobby != null) lobby.dispose();
                    refreshStartButton();
                });
                break;
            }

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

            case "INFO":  { if (room != null) room.appendLog(data); else if (lobby != null) lobby.showInfo(data); break; }
            case "CHAT":  { if (room != null) room.appendLog(data); break; }
            case "TURN":  { if (room != null) room.showTurn(data); break; }

            case "GAME_START": {
                if (room != null) { room.appendLog("게임 시작 (" + data + "명)"); room.setStartEnabled(false); }
                break;
            }
            case "INITIAL_TILES": {
                if (room != null) {
                    room.appendLog("내 손패: " + data);
                    room.setInitialHand(data);         // 하단 보드 초기 14장 반영
                }
                break;
            }
            case "NEW_TILE": {                         // ★ 서버가 보내는 새 타일 1장
                if (room != null) {
                    room.appendLog("새 타일: " + data);
                    room.addHandTile(data.trim());     // 하단 보드에 1장 추가 + 자동 재정렬
                }
                break;
            }

            default: {
                if (room != null) room.appendLog(line);
                else if (lobby != null) lobby.showInfo(line);
            }
        }
    }

    private void refreshStartButton() {
        if (room != null) room.setStartEnabled(isOwner && playerCount >= 2);
    }
}
