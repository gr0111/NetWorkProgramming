package client;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class RoomView extends JFrame {
    private final ClientApp app;
    private final String roomId;

    private final JTextArea taChat = new JTextArea();
    private final JTextField tfChat = new JTextField();
    private final JLabel lbTurn = new JLabel("TURN: -", SwingConstants.CENTER);
    private final JTextArea taLog = new JTextArea(5,24); // 하단 로그 패널(필요시 사용)

    private final JButton btnStart = new JButton("게임 시작");
    private final JButton btnNext  = new JButton("다음 턴");
    private final JButton btnPlay  = new JButton("수 제출");

    public RoomView(ClientApp app, String roomId){
        this.app = app; this.roomId = roomId;
        setTitle("Room #" + roomId);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(760, 520);
        setLocationRelativeTo(null);

        // 배경 패널
        BackgroundPanel bg = new BackgroundPanel(loadImage("assets/images/login_bg.png"));
        bg.setLayout(new BorderLayout(12,12));
        bg.setBorder(new EmptyBorder(8, 8, 8, 8));
        setContentPane(bg);

        // 상단: 턴 라벨
        JPanel north = translucentPanel(new BorderLayout());
        lbTurn.setForeground(new Color(255,255,255,230));
        lbTurn.setFont(lbTurn.getFont().deriveFont(Font.BOLD, 14f));
        north.add(lbTurn, BorderLayout.CENTER);
        bg.add(north, BorderLayout.NORTH);

        // 중앙: 좌(보드) / 우(채팅)
        JPanel center = translucentPanel(new GridLayout(1,2,12,12));

        // 좌: 보드/손패 자리
        JPanel board = translucentPanel(new GridBagLayout());
        JLabel placeholder = new JLabel("Board / Hand (추가 예정)");
        placeholder.setForeground(new Color(255,255,255,220));
        board.add(placeholder, new GridBagConstraints());
        center.add(wrapCard(board));

        // 우: 채팅
        JPanel chat = translucentPanel(new BorderLayout());
        taChat.setEditable(false);
        taChat.setLineWrap(true);
        taChat.setWrapStyleWord(true);
        JScrollPane spChat = new JScrollPane(taChat);
        makeScrollTranslucent(spChat);
        chat.add(spChat, BorderLayout.CENTER);
        chat.add(tfChat, BorderLayout.SOUTH);
        center.add(wrapCard(chat));

        bg.add(center, BorderLayout.CENTER);

        // 하단: 버튼 + (선택) 로그영역
        JPanel south = translucentPanel(new BorderLayout(8,8));

        JPanel btns = translucentPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnStart.setEnabled(false); // 초기 비활성
        btns.add(btnStart);
        btns.add(btnNext);
        btns.add(btnPlay);
        south.add(wrapCard(btns), BorderLayout.NORTH);

        taLog.setEditable(false);
        taLog.setLineWrap(true);
        taLog.setWrapStyleWord(true);
        JScrollPane spLog = new JScrollPane(taLog);
        makeScrollTranslucent(spLog);
        south.add(wrapCard(spLog), BorderLayout.CENTER); // 필요 없으면 이 줄을 제거해도 됨

        bg.add(south, BorderLayout.SOUTH);

        // 리스너
        tfChat.addActionListener(e -> {
            String msg = tfChat.getText().trim();
            if (!msg.isEmpty()) app.send("CHAT|" + msg);
            tfChat.setText("");
        });
        btnNext.addActionListener(e -> app.send("/next"));
        btnPlay.addActionListener(e -> {
            String encodedMove = "DUMMY_MOVE"; // TODO: 실제 수 인코딩
            app.send(encodedMove);
        });
        btnStart.addActionListener(e -> app.send("START_GAME"));
    }

    /** 오른쪽 채팅창에만 표시 (중복 출력 방지) */
    public void appendLog(final String line){
        SwingUtilities.invokeLater(() -> {
            taChat.append(line + "\n");
            taChat.setCaretPosition(taChat.getDocument().getLength());
        });
    }

    public void showTurn(final String player){
        SwingUtilities.invokeLater(() -> lbTurn.setText("TURN: " + player));
    }

    /** 외부(ClientApp)에서 인원수/방장여부 계산 후 호출: rv.setStartEnabled(isOwner && count >= 2) */
    public void setStartEnabled(final boolean on){
        SwingUtilities.invokeLater(() -> btnStart.setEnabled(on));
    }

    /* ---------- 유틸: 반투명 카드/배경/이미지 로딩 ---------- */

    private static JPanel translucentPanel(LayoutManager lm){
        return new JPanel(lm){
            @Override public boolean isOpaque(){ return false; }
        };
    }
    private static JComponent wrapCard(JComponent c){
        JPanel card = translucentPanel(new BorderLayout());
        card.setBorder(new CompoundBorder(
                new LineBorder(new Color(255,255,255,150), 1, true),
                new EmptyBorder(10,10,10,10)
        ));
        card.add(c, BorderLayout.CENTER);
        return card;
    }
    private static void makeScrollTranslucent(JScrollPane sp){
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
    }

    private static BufferedImage loadImage(String path){
        try {
            var url = RoomView.class.getClassLoader().getResource(path);
            if (url != null) return ImageIO.read(url);
            return ImageIO.read(new File(path));
        } catch (Exception e) { return null; }
    }
    static class BackgroundPanel extends JPanel {
        private final BufferedImage img;
        BackgroundPanel(BufferedImage img){ this.img = img; }
        @Override protected void paintComponent(Graphics g){
            super.paintComponent(g);
            if (img == null) return;
            int w = getWidth(), h = getHeight();
            double s = Math.max(w/(double)img.getWidth(), h/(double)img.getHeight());
            int dw = (int)(img.getWidth()*s), dh = (int)(img.getHeight()*s);
            int dx = (w - dw)/2, dy = (h - dh)/2;
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(img, dx, dy, dw, dh, null);
            g2.setColor(new Color(0,0,0,60)); // 살짝 어둡게
            g2.fillRect(0,0,w,h);
        }
    }
}
