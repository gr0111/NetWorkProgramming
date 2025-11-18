package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class GameServer {
    private ServerSocket serverSocket;
    private List<ClientSession> sessions = new ArrayList<>();
    private Room gameRoom;

    public GameServer(int port) {
        try {
            serverSocket = new ServerSocket(port);
            gameRoom = new Room();
            System.out.println("🎮 Rummikub Server started on port " + port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("✅ New client connected: " + clientSocket);

                ClientSession session = new ClientSession(clientSocket, gameRoom);
                sessions.add(session);
                gameRoom.addPlayer(session);

                session.start(); // 스레드 실행
            } catch (IOException e) {
                System.out.println("❌ Connection error: " + e.getMessage());
            }
        }
    }
}
