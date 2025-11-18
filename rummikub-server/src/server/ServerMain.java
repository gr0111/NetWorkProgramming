package server;

public class ServerMain {
    public static void main(String[] args) {
        int port = 9999; // 서버 포트
        GameServer server = new GameServer(port);
        server.start();
    }
}
