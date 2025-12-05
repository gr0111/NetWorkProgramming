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
    private final JButton btnDraw  = new JButton("한 장 뽑기");

    // ✅ 하단 손패 보드(항상 2줄로 렌더링)
    private final TwoRowHandPanel handPanel = new TwoRowHandPanel();

    public RoomView(ClientApp app, String roomId){
        this.app = app; this.roomId = roomId;
        setTitle("Room #" + roomId);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1024, 720);                // 화면을 조금 키워 두 줄 보기 좋게
        setLocationRelativeTo(null);

        BackgroundPanel bg = new BackgroundPanel(loadImage("assets/images/login_bg.png"));
        bg.setLayout(new BorderLayout(12,12));
        bg.setBorder(new EmptyBorder(8, 8, 8, 8));
        setContentPane(bg);

        // 상단: 턴 라벨
        JPanel north = translucentPanel(new BorderLayout());
        lbTurn.setForeground(new Color(255,255,255,230));
        lbTurn.setFont(lbTurn.getFont().deriveFont(Font.BOLD, 16f));
        north.add(lbTurn, BorderLayout.CENTER);
        bg.add(north, BorderLayout.NORTH);

        // 좌(보드) 자리
        JPanel board = translucentPanel(new GridBagLayout());
        JLabel placeholder = new JLabel("Board / Hand (추가 예정)");
        placeholder.setForeground(new Color(255,255,255,220));
        board.add(placeholder, new GridBagConstraints());
        JComponent boardCard = wrapCard(board);

        // 우(채팅) — 투명 배경 + 흰색 글씨
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

        // 하단: 버튼 + 손패 보드(두 줄)
        JPanel south = translucentPanel(new BorderLayout(8,8));
        JPanel btns = translucentPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnStart.setEnabled(false);
        btns.add(btnStart);
        btns.add(btnNext);
        btns.add(btnPlay);
        btns.add(btnDraw); // 한 장 뽑기 버튼(기능은 기존 로직 유지)
        south.add(wrapCard(btns), BorderLayout.NORTH);

        // 손패 보드 영역 – 스크롤 없이 두 줄 꽉 채워서 그림
        JPanel handWrap = translucentPanel(new BorderLayout());
        handWrap.add(handPanel, BorderLayout.CENTER);
        south.add(wrapCard(handWrap), BorderLayout.CENTER);

        bg.add(south, BorderLayout.SOUTH);

        // 리스너
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
        btnDraw.addActionListener(e -> app.send("NO_TILE")); // 서버가 NEW_TILE|<id> 로 응답
    }

    // ========== ClientApp 에서 호출되는 API (이름/시그니처 변경 없음) ==========
    public void setInitialHand(String csv){
        handPanel.setTilesFromCsv(csv);
    }
    public void addHandTile(String tileId){
        handPanel.addTile(tileId);
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

    // ===================== 두 줄 손패 패널 =====================
    private static class TwoRowHandPanel extends JPanel {
        private java.util.List<String> tiles = new ArrayList<>();
        private final Map<String, Image> cache = new HashMap<>();
        private int baseTileH = 84;    // 기준 높이
        private int gap = 10;          // 타일 간격
        private int pad = 12;          // 좌우 상하 패딩

        TwoRowHandPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(100, baseTileH * 2 + gap + pad*2));
        }

        void setTilesFromCsv(String csv){
            java.util.List<String> list = new ArrayList<>();
            if (csv != null && !csv.isBlank()) {
                for (String s : csv.split(",")) {
                    s = s.trim();
                    if (!s.isEmpty()) list.add(s);
                }
            }
            this.tiles = list;
            revalidate();
            repaint();
        }

        void addTile(String id){
            if (id != null && !id.isBlank()) {
                tiles.add(id.trim());
                revalidate();
                repaint();
            }
        }

        @Override
        public Dimension getPreferredSize() {
            // 높이는 항상 2줄(타일H*2 + gap + padding)
            return new Dimension(super.getPreferredSize().width, baseTileH * 2 + gap + pad*2);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (tiles.isEmpty()) return;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int innerW = w - pad*2;

            // 두 줄로 분배 (윗줄에 ceil(n/2), 아랫줄에 나머지)
            int n = tiles.size();
            int topCount = (n + 1) / 2;
            int bottomCount = n - topCount;

            // 이미지 원본 비율(가로/세로) — 타일 PNG가 대략 0.72~0.78 사이,
            // 안전하게 0.75 비율로 가정 (이미지 읽어올 때 실제비율 사용해 다시 계산)
            double aspect = 0.75;

            // 한 줄이 화면 폭을 넘기지 않도록 스케일 계산(두 줄 중 더 빡빡한 줄을 기준)
            double needWTop = calcRowWidth(innerW, topCount, baseTileH, gap, aspect);
            double needWBottom = calcRowWidth(innerW, bottomCount, baseTileH, gap, aspect);
            double scale = Math.min(1.0, Math.min(needWTop, needWBottom));

            int tileH = (int)Math.max(56, Math.round(baseTileH * scale)); // 너무 작아지지 않게 하한선
            // 실제 이미지 비율로 W 계산(첫 장 로드해서 비율 얻음; 없으면 aspect 가정)
            double trueAspect = getAspectForFirst();
            int tileW = (int)Math.round(tileH * trueAspect);

            // 각 줄의 시작 X(가운데 정렬)
            int topRowWidth = topCount > 0 ? (topCount * tileW + (topCount - 1) * gap) : 0;
            int botRowWidth = bottomCount > 0 ? (bottomCount * tileW + (bottomCount - 1) * gap) : 0;
            int topStartX = pad + (innerW - topRowWidth) / 2;
            int botStartX = pad + (innerW - botRowWidth) / 2;

            int topY = pad;
            int botY = pad + tileH + gap;

            // 그리기
            for (int i=0;i<n;i++){
                String id = tiles.get(i);
                int row = (i < topCount) ? 0 : 1;
                int idxInRow = (row == 0) ? i : (i - topCount);

                int x = (row==0 ? topStartX : botStartX) + idxInRow * (tileW + gap);
                int y = (row==0 ? topY : botY);

                Image img = getTileImage(id, tileW, tileH);
                if (img != null) {
                    g2.drawImage(img, x, y, tileW, tileH, null);
                } else {
                    // 대체 렌더링(이미지 없을 때)
                    g2.setColor(new Color(255,255,255,180));
                    g2.fillRoundRect(x, y, tileW, tileH, 12, 12);
                    g2.setColor(Color.DARK_GRAY);
                    g2.drawRoundRect(x, y, tileW, tileH, 12, 12);
                    g2.drawString(id, x + 8, y + tileH/2);
                }
            }
            g2.dispose();
        }

        private double calcRowWidth(int innerW, int count, int h, int gap, double aspect){
            if (count <= 0) return 1.0; // 여유
            double need = count * (h*aspect) + (count-1)*gap;
            return innerW / need; // 이 값이 1보다 작으면 스케일 필요
        }

        private double getAspectForFirst() {
            for (String id : tiles) {
                Image raw = loadRaw(id);
                if (raw != null) {
                    int iw = raw.getWidth(null), ih = raw.getHeight(null);
                    if (iw > 0 && ih > 0) return iw / (double) ih;
                }
            }
            return 0.75;
        }

        private Image getTileImage(String id, int w, int h){
            String key = id + "@" + w + "x" + h;
            Image cached = cache.get(key);
            if (cached != null) return cached;

            Image raw = loadRaw(id);
            if (raw == null) return null;
            Image scaled = raw.getScaledInstance(w, h, Image.SCALE_SMOOTH);
            cache.put(key, scaled);
            return scaled;
        }

        private Image loadRaw(String id){
            // 파일명 규칙: assets/images/<ID>.png  (예: R5, BL10, Y3, BJoker, RJoker)
            String[] candidates = {
                    "assets/images/" + id + ".png",
                    id + ".png"
            };
            for (String p : candidates) {
                try {
                    var url = RoomView.class.getClassLoader().getResource(p);
                    if (url != null) return ImageIO.read(url);
                    File f = new File(p);
                    if (f.exists()) return ImageIO.read(f);
                } catch (Exception ignore) {}
            }
            return null;
        }
    }

    // ===== 유틸 =====
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
