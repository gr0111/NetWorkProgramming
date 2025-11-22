package client;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class ClientApp implements NetIO.MessageHandler {
    private final NetIO net = new NetIO();

    private LoginView login;
    private LobbyView lobby;
    private RoomView  room;

    private String myName;
    private final List<String> pendingLogs = new ArrayList<>(); // (선택) 나중에 쓸 수 있음

    public ClientApp() { net.setHandler(this); }

    // LoginView에서 호출
    public void connectAndLogin(String host, int port, String name) {
        try {
            this.myName = name;
            net.connect(host, port);
            net.send("LOGIN|" + name);

            // 로비 띄우고 즉시 목록 요청
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

    public void setLogin(LoginView login) { this.login = login; }
    public String myName() { return myName; }

    // 로비에서 호출할 네트워크 명령
    public void requestRoomList()        { net.send("LIST"); }
    public void requestCreateRoom(String name) { net.send("CREATE|" + name); }
    public void requestJoinRoom(int id)  { net.send("JOIN|" + id); }

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
                });
                break;
            }
            case "INFO": {
                final String msg = data;
                if (room != null) {
                    room.appendLog(msg);
                    if (msg.contains("방장")) room.setOwner(true);
                } else if (lobby != null) {
                    lobby.showInfo(msg);
                }
                break;
            }
            case "CHAT": {
                final String chat = data;
                if (room != null) room.appendLog(chat);
                break;
            }
            case "TURN": {
                final String who = data;
                if (room != null) room.showTurn(who);
                break;
            }
            case "GAME_START": {
                final String cnt = data;
                if (room != null) {
                    room.appendLog("게임 시작 (" + cnt + "명)");
                    room.setOwner(false);
                }
                break;
            }
            case "INITIAL_TILES": {
                final String tiles = data;
                if (room != null) room.appendLog("내 손패: " + tiles);
                break;
            }
            default:
                if (room != null) room.appendLog(line);
                else if (lobby != null) lobby.showInfo(line);
        }
    }
}
