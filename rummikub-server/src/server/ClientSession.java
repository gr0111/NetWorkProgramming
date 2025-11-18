package server;

import java.io.*;
import java.net.Socket;

public class ClientSession extends Thread {
    private Socket socket;
    private Room room;
    private BufferedReader in;
    private BufferedWriter out;
    private String playerName;

    public ClientSession(Socket socket, Room room) {
        this.socket = socket;
        this.room = room;
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            // 첫 메시지는 플레이어 닉네임
            playerName = in.readLine();
            System.out.println("👤 Player joined: " + playerName);
            room.broadcast("[" + playerName + "] 님이 입장했습니다.");

            String msg;
            while ((msg = in.readLine()) != null) {
                if (msg.equalsIgnoreCase("/exit")) break;
                room.handleMessage(playerName, msg);
            }

        } catch (IOException e) {
            System.out.println("⚠️ Connection lost: " + playerName);
        } finally {
            room.removePlayer(this);
            try {
                socket.close();
            } catch (IOException e) {}
        }
    }

    public void send(String msg) {
        try {
            out.write(msg + "\n");
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getPlayerName() {
        return playerName;
    }
}
