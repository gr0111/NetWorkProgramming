package client;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

/** 서버와 라인(문자열) 단위 송수신을 담당하는 경량 I/O 유틸 */
public class NetIO {

    /** 수신 콜백 인터페이스 */
    public interface MessageHandler {
        void onMessage(String line);
        default void onClosed() {}           // 연결 종료 시 알림(선택)
    }

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Thread listener;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile MessageHandler handler;

    public NetIO() {}

    /** 기존 사용 패턴 지원: new NetIO(host, port, this::onMessage) */
    public NetIO(String host, int port, MessageHandler h) throws IOException {
        this.handler = h;
        connect(host, port);
    }

    public void setHandler(MessageHandler h) { this.handler = h; }

    /** 서버에 접속하고 수신 스레드를 시작한다. */
    public void connect(String host, int port) throws IOException {
        close(); // 혹시 열려 있던 자원 정리
        socket = new Socket(host, port);
        in  = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true); // auto-flush

        running.set(true);
        listener = new Thread(this::listenLoop, "NetIO-Listener");
        listener.setDaemon(true);
        listener.start();
    }

    /** 수신 루프: 한 줄씩 읽어 handler에 전달 */
    private void listenLoop() {
        try {
            String line;
            while (running.get() && (line = in.readLine()) != null) {
                MessageHandler h = handler;
                if (h != null) h.onMessage(line);
            }
        } catch (IOException ignore) {
            // 소켓 종료/네트워크 오류는 종료 처리로 이어짐
        } finally {
            running.set(false);
            safeClose();
            MessageHandler h = handler;
            if (h != null) h.onClosed();
        }
    }

    /** 한 줄 전송(서버는 readLine으로 받음) */
    public void send(String msg) {
        PrintWriter o = out;
        if (o != null) {
            o.println(msg);
        }
    }

    /** 외부에서 명시적으로 종료할 때 호출 */
    public void close() {
        running.set(false);
        safeClose();
        Thread t = listener;
        if (t != null) t.interrupt();
    }

    /** 내부 자원 정리 */
    private void safeClose() {
        try { if (socket != null && !socket.isClosed()) socket.close(); } catch (IOException ignore) {}
        try { if (in != null) in.close(); } catch (IOException ignore) {}
        if (out != null) out.close();
        socket = null; in = null; out = null;
    }
}
