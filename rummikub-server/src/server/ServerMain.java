package server;

public class ServerMain {
    public static void main(String[] args) {
        int port = 9999; // 필요하면 포트 변경
        GameServer server = new GameServer(port);
        server.start();
    }
}

