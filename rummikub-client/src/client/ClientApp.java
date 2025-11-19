package client;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class ClientApp implements NetIO.MessageHandler {
    private final NetIO net = new NetIO();
    private JFrame current;
    private String myName;

    public ClientApp() { net.setHandler(this); }

    public void connect(String host, int port, String name, JFrame loginView){
        this.myName = name;
        net.connect(host, port);
        net.send(name);                 // 서버 ClientSession.readLine() 규약과 동일: 첫 줄에 닉네임
        this.current = loginView;
        openRoom("1");                  // 단일 Room 구조라 바로 방 화면 띄움
    }

    public void send(String line){ net.send(line); }
    public String myName(){ return myName; }

    @Override
    public void onMessage(String line) {
        // 서버는 태그 없이 브로드캐스트/알림을 보내므로 그대로 표시
        if (current instanceof RoomView) {
            ((RoomView) current).appendLog(line);

            // "🎯 현재 턴: XXX" 형식이 오면 턴 라벨 갱신(가벼운 UX 보강)
            int idx = line.indexOf("현재 턴:");
            if (idx >= 0) {
                String who = line.substring(idx + "현재 턴:".length()).trim();
                ((RoomView) current).showTurn(who);
            }
        }
    }

    private void openRoom(final String roomId){
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                if (current != null) current.dispose();
                current = new RoomView(ClientApp.this, roomId);
                current.setVisible(true);
            }
        });
    }
}
