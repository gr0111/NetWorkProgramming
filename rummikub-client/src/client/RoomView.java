package client;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class RoomView extends JFrame {
    private final ClientApp app;
    private final String roomId;

    private final JTextArea taChat = new JTextArea();
    private final JTextField tfChat = new JTextField();
    private final JLabel lbTurn = new JLabel("TURN: -", SwingConstants.CENTER);
    private final JTextArea taLog = new JTextArea(5,24);

    private final JButton btnStart = new JButton("게임 시작");
    private final JButton btnNext  = new JButton("다음 턴");
    private final JButton btnPlay  = new JButton("수 제출");

    public RoomView(ClientApp app, String roomId){
        this.app = app; this.roomId = roomId;
        setTitle("Room #" + roomId);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(760, 520);
        setLocationRelativeTo(null);

        BackgroundPanel bg = new BackgroundPanel(loadImage("assets/images/login_bg.png"));
        bg.setLayout(new BorderLayout(12,12));
        bg.setBorder(new EmptyBorder(8, 8, 8, 8));
        setContentPane(bg);

        JPanel north = translucentPanel(new BorderLayout());
        lbTurn.setForeground(new Color(255,255,255,230));
        lbTurn.setFont(lbTurn.getFont().deriveFont(Font.BOLD, 14f));
        north.add(lbTurn, BorderLayout.CENTER);
        bg.add(north, BorderLayout.NORTH);

        // 좌(보드)
        JPanel board = translucentPanel(new GridBagLayout());
        JLabel placeholder = new JLabel("Board / Hand (추가 예정)");
        placeholder.setForeground(new Color(255,255,255,220));
        board.add(placeholder, new GridBagConstraints());
        JComponent boardCard = wrapCard(board);

        // 우(채팅) — 투명 배경 + 흰색 글씨
        JPanel chat = translucentPanel(new BorderLayout());

        // === 여기부터 채팅창 스타일 ===
        taChat.setEditable(false);
        taChat.setLineWrap(true);
        taChat.setWrapStyleWord(true);
        taChat.setOpaque(false);                                   // 텍스트 영역 자체 투명
        taChat.setBackground(new Color(0,0,0,0));                  // 완전 투명
        taChat.setForeground(new Color(255,255,255,230));          // 흰색 글씨(약간 투명)
        taChat.setCaretColor(new Color(255,255,255,230));          // (필요시) 캐럿 색

        JScrollPane spChat = new JScrollPane(taChat);
        spChat.setOpaque(false);                                   // 스크롤팬 투명
        spChat.getViewport().setOpaque(false);                     // 뷰포트도 투명
        spChat.setBorder(new LineBorder(new Color(255,255,255,80))); // 얇은 테두리(원하면 제거)

        chat.add(spChat, BorderLayout.CENTER);

        // 입력창은 기본 스타일 유지(원하면 아래 두 줄로 반투명)
        // tfChat.setOpaque(false);
        // tfChat.setBackground(new Color(255,255,255,40));
        chat.add(tfChat, BorderLayout.SOUTH);
        // === 채팅창 스타일 끝 ===

        JComponent chatCard = wrapCard(chat);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, boardCard, chatCard);
        split.setResizeWeight(0.72);
        split.setDividerSize(6);
        split.setBorder(null);
        split.setOpaque(false);
        split.setContinuousLayout(true);

        addComponentListener(new ComponentAdapter() {
            @Override public void componentShown(ComponentEvent e)  { split.setDividerLocation(0.72); }
            @Override public void componentResized(ComponentEvent e){ split.setDividerLocation(0.72); }
        });

        bg.add(split, BorderLayout.CENTER);

        JPanel south = translucentPanel(new BorderLayout(8,8));
        JPanel btns = translucentPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnStart.setEnabled(false);
        btns.add(btnStart);
        btns.add(btnNext);
        btns.add(btnPlay);
        south.add(wrapCard(btns), BorderLayout.NORTH);

        taLog.setEditable(false);
        taLog.setLineWrap(true);
        taLog.setWrapStyleWord(true);
        JScrollPane spLog = new JScrollPane(taLog);
        makeScrollTranslucent(spLog);
        south.add(wrapCard(spLog), BorderLayout.CENTER);

        bg.add(south, BorderLayout.SOUTH);

        tfChat.addActionListener(e -> {
            String msg = tfChat.getText().trim();
            if (!msg.isEmpty()) app.send("CHAT|" + msg);
            tfChat.setText("");
        });
        btnNext.addActionListener(e -> app.send("/next"));
        btnPlay.addActionListener(e -> {
            String encodedMove = "DUMMY_MOVE";
            app.send(encodedMove);
        });
        btnStart.addActionListener(e -> app.send("START_GAME"));
    }

    public void appendLog(final String line){
        SwingUtilities.invokeLater(() -> {
            taChat.append(line + "\n");
            taChat.setCaretPosition(taChat.getDocument().getLength());
        });
    }
    public void showTurn(final String player){
        SwingUtilities.invokeLater(() -> lbTurn.setText("TURN: " + player));
    }
    public void setStartEnabled(final boolean on){
        SwingUtilities.invokeLater(() -> btnStart.setEnabled(on));
    }

    private static JPanel translucentPanel(LayoutManager lm){
        return new JPanel(lm){ @Override public boolean isOpaque(){ return false; } };
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
            g2.setColor(new Color(0,0,0,60));
            g2.fillRect(0,0,w,h);
        }
    }
}
