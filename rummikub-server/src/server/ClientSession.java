package server;

import java.io.*;
import java.net.Socket;

public class ClientSession extends Thread {

    private Socket socket;
    private GameServer server;

    private BufferedReader in;
    private PrintWriter out;

    private String playerName;
    private Room currentRoom; // null이면 로비

    public ClientSession(Socket socket, GameServer server) {
        this.socket = socket;
        this.server = server;
        try {
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true); // auto-flush
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            // 첫 메시지는 닉네임 (LOGIN|닉네임, 또는 닉네임만)
            String first = in.readLine();
            if (first == null) return;

            if (first.startsWith("LOGIN|")) {
                playerName = first.substring("LOGIN|".length());
            } else {
                playerName = first;
            }
            System.out.println("👤 Player connected: " + playerName);
            send("INFO|로비에 입장했습니다.");

            String line;
            while ((line = in.readLine()) != null) {
                handleMessage(line);
            }

        } catch (IOException e) {
            System.out.println("⚠️ 연결 종료: " + playerName);
        } finally {
            if (currentRoom != null) {
                currentRoom.removePlayer(this);
            }
            try { socket.close(); } catch (IOException ignore) {}
        }
    }

    private void handleMessage(String msg) {
        String type;
        String data = "";

        int sep = msg.indexOf('|');
        if (sep >= 0) {
            type = msg.substring(0, sep);
            data = msg.substring(sep + 1);
        } else {
            type = msg;
        }

        try {
            switch (type) {
                case "LIST":
                    handleListRooms();
                    break;

                case "CREATE": // CREATE|방이름
                    handleCreateRoom(data);
                    break;

                case "JOIN":   // JOIN|방ID
                    handleJoinRoom(data);
                    break;

                case "LEAVE":  // 방 나가기
                    handleLeaveRoom();
                    break;

                case "CHAT":   // CHAT|메시지
                    handleChat(data);
                    break;

                case "PLAY":   // PLAY|타일데이터 ("R1,R2,R3" 등)
                    handlePlay(data);
                    break;

                case "NO_TILE":
                    handleNoTile();
                    break;

                case "START_GAME": // 방장이 [게임 시작] 눌렀을 때
                    handleStartGame();
                    break;

                case "EXIT":   // 프로그램 종료
                    handleExit();
                    break;

                default:
                    send("ERROR|알 수 없는 명령: " + type);
            }
        } catch (IOException e) {
            System.out.println("메시지 처리 중 오류: " + e.getMessage());
        }
    }

    private void handleListRooms() throws IOException {
        String roomListMsg = server.buildRoomListMessage();
        send(roomListMsg);
    }

    private void handleCreateRoom(String roomName) throws IOException {
        if (roomName == null || roomName.isBlank()) {
            roomName = playerName + "의 방";
        }
        Room room = server.createRoom(roomName);
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
            room.addPlayer(this);
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

    private void handleStartGame() {
        if (currentRoom != null) {
            currentRoom.requestStartGame(playerName);
        } else {
            send("ERROR|방 안에 있을 때만 게임을 시작할 수 있습니다.");
        }
    }

    private void handleExit() throws IOException {
        if (currentRoom != null) {
            currentRoom.removePlayer(this);
        }
        send("INFO|서버에서 연결 종료");
        socket.close();
    }

    public void send(String msg) {
        synchronized (out) {
            out.println(msg); // NetIO 쪽의 readLine()과 짝 맞음
        }
    }

    public String getPlayerName() {
        return playerName;
    }
}
