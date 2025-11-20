package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameServer {

    private ServerSocket serverSocket;
    // 여러 개의 방 관리
    private List<Room> rooms = Collections.synchronizedList(new ArrayList<>());
    private int nextRoomId = 0;

    public GameServer(int port) {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("🎮 Rummikub Server started on port " + port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 메인 accept 루프
    public void start() {
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                System.out.println("✅ New client: " + socket);

                ClientSession session = new ClientSession(socket, this);
                session.start();

            } catch (IOException e) {
                System.out.println("❌ Connection error: " + e.getMessage());
            }
        }
    }

    // ===== 방 관리 메서드들 =====

    /** 방 생성 */
    public synchronized Room createRoom(String roomName) {
        Room room = new Room(nextRoomId++, roomName, this);
        rooms.add(room);
        System.out.println("🆕 Room created: " + roomName + " (id=" + room.getId() + ")");
        return room;
    }

    /** 전체 방 리스트 반환 (복사본) */
    public synchronized List<Room> getRooms() {
        return new ArrayList<>(rooms);
    }

    /** 방 ID로 방 찾기 */
    public synchronized Room findRoomById(int id) {
        for (Room r : rooms) {
            if (r.getId() == id) return r;
        }
        return null;
    }

    /** 클라이언트에게 전달할 방 리스트 문자열 생성 */
    public synchronized String buildRoomListMessage() {
        // 예: "ROOM_LIST|0,방1,2;1,방2,1"
        StringBuilder sb = new StringBuilder();
        sb.append("ROOM_LIST|");
        for (int i = 0; i < rooms.size(); i++) {
            Room r = rooms.get(i);
            if (i > 0) sb.append(";");
            sb.append(r.getId())
            .append(",")
            .append(r.getName())
            .append(",")
            .append(r.getPlayerCount());
        }
        return sb.toString();
    }
}