package client;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class NetIO {
    public static interface MessageHandler { void onMessage(String line); }

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Thread listener;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile MessageHandler handler;

    public void setHandler(MessageHandler h){ this.handler = h; }

    public void connect(String host, int port) {
        try {
            socket = new Socket(host, port);
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true); // auto-flush
            running.set(true);
            listener = new Thread(new Runnable() {
                @Override public void run() { listenLoop(); }
            }, "NetIO-Listener");
            listener.start();
        } catch (IOException e) {
            throw new RuntimeException("서버 연결 실패: " + e.getMessage(), e);
        }
    }

    private void listenLoop() {
        try {
            String line;
            while (running.get() && (line = in.readLine()) != null) {
                MessageHandler h = handler;
                if (h != null) h.onMessage(line);
            }
        } catch (IOException ignore) {
        } finally {
            close();
        }
    }

    public void send(String msg) { if (out != null) out.println(msg); }

    public void close() {
        running.set(false);
        try { if (socket != null) socket.close(); } catch (IOException ignore) {}
        try { if (in != null) in.close(); } catch (IOException ignore) {}
        if (out != null) out.close();
        if (listener != null) listener.interrupt();
    }
}
