package client;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
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
    private final JButton btnDraw  = new JButton("한 장 뽑기"); // ★ 추가

    // — 아래 HandPanel은 기존에 사용 중인 하단 보드 컴포넌트
    //   (없다면 기존 보드 대신 이 클래스를 그대로 사용하세요)
    private final HandPanel handPanel = new HandPanel();

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

        // 좌측 보드(자리), 우측 채팅(투명 스타일)
        JPanel board = translucentPanel(new GridBagLayout());
        JLabel placeholder = new JLabel("Board / Hand (추가 예정)");
        placeholder.setForeground(new Color(255,255,255,220));
        board.add(placeholder, new GridBagConstraints());
        JComponent boardCard = wrapCard(board);

        JPanel chat = translucentPanel(new BorderLayout());
        taChat.setEditable(false);
        taChat.setLineWrap(true);
        taChat.setWrapStyleWord(true);
        taChat.setOpaque(false);
        taChat.setBackground(new Color(0,0,0,0));
        taChat.setForeground(new Color(255,255,255,230));
        taChat.setCaretColor(new Color(255,255,255,230));
        JScrollPane spChat = new JScrollPane(taChat);
        spChat.setOpaque(false);
        spChat.getViewport().setOpaque(false);
        spChat.setBorder(new LineBorder(new Color(255,255,255,80)));
        chat.add(spChat, BorderLayout.CENTER);
        chat.add(tfChat, BorderLayout.SOUTH);
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

        // 하단: 버튼 + (기존) 로그/보드 — 레이아웃은 그대로
        JPanel south = translucentPanel(new BorderLayout(8,8));
        JPanel btns = translucentPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnStart.setEnabled(false);
        btns.add(btnStart);
        btns.add(btnNext);
        btns.add(btnPlay);
        btns.add(btnDraw); // ★ 버튼 배치만 추가
        south.add(wrapCard(btns), BorderLayout.NORTH);

        // 하단 보드: 기존처럼 카드로 감싸고 가로 스크롤 가능
        JScrollPane spHand = new JScrollPane(handPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        spHand.setOpaque(false);
        spHand.getViewport().setOpaque(false);
        spHand.setBorder(new LineBorder(new Color(255,255,255,80)));
        south.add(wrapCard(spHand), BorderLayout.CENTER);

        bg.add(south, BorderLayout.SOUTH);

        // 리스너
        tfChat.addActionListener(e -> {
            String msg = tfChat.getText().trim();
            if (!msg.isEmpty()) app.send("CHAT|" + msg);
            tfChat.setText("");
        });
        btnNext.addActionListener(e -> app.send("/next"));
        btnPlay.addActionListener(e -> app.send("DUMMY_MOVE")); // 그대로 유지
        btnStart.addActionListener(e -> app.send("START_GAME"));
        btnDraw.addActionListener(e -> app.send("NO_TILE"));    // ★ 한 장 뽑기 트리거
    }

    /* ===== 뷰 갱신용 메서드 ===== */
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

    /** 초기 손패 세팅 (콤마로 구분된 타일 ID들) */
    public void setInitialHand(String csvIds){
        java.util.List<String> ids = new ArrayList<>();
        if (csvIds != null && !csvIds.isBlank()) {
            for (String s : csvIds.split(",")) {
                String t = s.trim();
                if (!t.isEmpty()) ids.add(t);
            }
        }
        SwingUtilities.invokeLater(() -> handPanel.setTiles(ids));
    }

    /** 새 타일 1장 추가 */
    public void addHandTile(String id){
        SwingUtilities.invokeLater(() -> handPanel.addTile(id));
    }

    /* ===== 보조/스킨 ===== */
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

    /* ===== 하단 보드: 폭이 부족하면 자동 2줄로 줄바꿈 ===== */
    static class HandPanel extends JPanel {
        private static final int HAND_TILE_HEIGHT = 64; // 기존 보드 비율 유지
        private static final int GAP = 8;

        private final java.util.List<String> tiles = new ArrayList<>();
        private final Map<String, Image> cache = new HashMap<>();

        HandPanel() {
            setOpaque(false);
            setBorder(new EmptyBorder(10,10,10,10));
            // FlowLayout 은 폭이 부족하면 자동 줄바꿈 → 2줄 표시에 적합
            setLayout(new FlowLayout(FlowLayout.CENTER, GAP, 0));
        }

        void setTiles(java.util.List<String> ids){
            tiles.clear();
            tiles.addAll(ids);
            rebuild();
        }
        void addTile(String id){
            tiles.add(id);
            rebuild();
        }

        private void rebuild(){
            removeAll();
            for (String id : tiles) add(tileLabel(id));
            revalidate();
            repaint();
        }

        private JLabel tileLabel(String id){
            Image img = getScaledTile(id);
            JLabel l;
            if (img != null) {
                l = new JLabel(new ImageIcon(img));
            } else {
                l = new JLabel(id, SwingConstants.CENTER);
                l.setForeground(Color.WHITE);
                l.setBorder(new LineBorder(Color.LIGHT_GRAY));
                l.setPreferredSize(new Dimension(HAND_TILE_HEIGHT * 2/3, HAND_TILE_HEIGHT));
            }
            l.setBorder(new EmptyBorder(2,2,2,2));
            return l;
        }

        private Image getScaledTile(String id){
            try {
                String file = "assets/images/" + id + ".png"; // 예: R5.png, BL10.png, RJoker.png
                Image base = cache.get(file);
                if (base == null) {
                    BufferedImage src;
                    var url = RoomView.class.getClassLoader().getResource(file);
                    if (url != null) src = ImageIO.read(url);
                    else src = ImageIO.read(new File(file));
                    if (src == null) return null;
                    double s = HAND_TILE_HEIGHT / (double) src.getHeight();
                    int w = (int) Math.round(src.getWidth() * s);
                    base = src.getScaledInstance(w, HAND_TILE_HEIGHT, Image.SCALE_SMOOTH);
                    cache.put(file, base);
                }
                return base;
            } catch (Exception ignore) { return null; }
        }
    }
}
