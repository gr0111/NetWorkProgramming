package server;

import java.io.*;
import java.net.Socket;

public class ClientSession extends Thread {

    private Socket socket;
    private GameServer server;
    private DataInputStream in;
    private DataOutputStream out;

    private String playerName;
    private Room currentRoom; // null이면 로비 상태

    public ClientSession(Socket socket, GameServer server) {
        this.socket = socket;
        this.server = server;
        try {
            in  = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            // 첫 메시지를 닉네임으로 가정하거나,
            // "LOGIN|닉네임" 형식으로 받을 수도 있음.
            String first = in.readUTF();
            if (first.startsWith("LOGIN|")) {
                playerName = first.substring("LOGIN|".length());
            } else {
                playerName = first; // 그냥 닉네임만 보낸 경우
            }
            System.out.println("👤 Player connected: " + playerName);
            send("INFO|로비에 입장했습니다.");

            // 메인 루프
            while (true) {
                String msg = in.readUTF();
                if (msg == null) break;
                handleMessage(msg);
            }

        } catch (IOException e) {
            System.out.println("⚠️ 연결 종료: " + playerName);
        } finally {
            // 방에 있었으면 제거
            if (currentRoom != null) {
                currentRoom.removePlayer(this);
            }
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    /** 클라이언트에서 온 문자열 명령 처리 */
    private void handleMessage(String msg) throws IOException {
        // 기본 포맷: TYPE|DATA
        String type;
        String data = "";

        int sep = msg.indexOf('|');
        if (sep >= 0) {
            type = msg.substring(0, sep);
            data = msg.substring(sep + 1);
        } else {
            type = msg;
        }

        switch (type) {
            case "LIST":      // 방 리스트 요청
                handleListRooms();
                break;

            case "CREATE":    // CREATE|방이름
                handleCreateRoom(data);
                break;

            case "JOIN":      // JOIN|방ID
                handleJoinRoom(data);
                break;

            case "LEAVE":     // 방 나가기(나가기 버튼)
                handleLeaveRoom();
                break;

            case "CHAT":      // 방 안에서의 채팅
                handleChat(data);
                break;

            case "PLAY":      // PLAY|...  (나중에 GameCore와 연동)
                handlePlay(data);
                break;

            case "NO_TILE":   // 낼 타일 없어서 한 장 뽑기 (나중에 GameCore와 연동)
                handleNoTile();
                break;

            case "EXIT":      // 전체 종료 (프로그램 종료 버튼)
                handleExit();
                break;

            default:
                send("ERROR|알 수 없는 명령: " + type);
        }
    }

    private void handleListRooms() throws IOException {
        String roomListMsg = server.buildRoomListMessage();
        send(roomListMsg); // "ROOM_LIST|..." 형식
    }

    private void handleCreateRoom(String roomName) throws IOException {
        if (roomName == null || roomName.isBlank()) {
            roomName = playerName + "의 방";
        }
        Room room = server.createRoom(roomName);
        // 기존 방에서 빼고 새 방에 입장
        if (currentRoom != null) {
            currentRoom.removePlayer(this);
        }
        currentRoom = room;
        room.addPlayer(this);
        send("JOIN_OK|" + room.getId());
    }

    private void handleJoinRoom(String roomIdStr) throws IOException {
        try {
            int roomId = Integer.parseInt(roomIdStr.trim());
            Room room = server.findRoomById(roomId);
            if (room == null) {
                send("ERROR|존재하지 않는 방 ID 입니다.");
                return;
            }
            if (currentRoom != null) {
                currentRoom.removePlayer(this);
            }
            currentRoom = room;
            room.addPlayer(this);   // 안에서 방송도 함
            send("JOIN_OK|" + room.getId());
        } catch (NumberFormatException e) {
            send("ERROR|방 번호 형식이 올바르지 않습니다.");
        }
    }

    private void handleLeaveRoom() throws IOException {
        if (currentRoom != null) {
            currentRoom.removePlayer(this);
            currentRoom = null;
            send("INFO|로비로 이동했습니다.");
        }
    }

    private void handleChat(String message) {
        if (currentRoom != null) {
            currentRoom.broadcast("CHAT|" + playerName + ": " + message);
        }
    }

    private void handlePlay(String moveData) {
        if (currentRoom != null) {
            currentRoom.handlePlay(playerName, moveData);
        }
    }

    private void handleNoTile() {
        if (currentRoom != null) {
            currentRoom.handleNoTile(playerName);
        }
    }

    private void handleExit() throws IOException {
        if (currentRoom != null) {
            currentRoom.removePlayer(this);
        }
        send("INFO|서버에서 연결 종료");
        socket.close(); // run()의 finally로 감
    }

    // ===== 클라이언트로 메시지 보내는 헬퍼 =====
    public void send(String msg) throws IOException {
        synchronized (out) {
            out.writeUTF(msg);
            out.flush();
        }
    }

    public String getPlayerName() {
        return playerName;
    }
}
