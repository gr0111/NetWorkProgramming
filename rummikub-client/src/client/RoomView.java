package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.border.LineBorder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RoomView extends JFrame {

    private final ClientApp app;
    private final String roomId;

    private final JTextArea taChat = new JTextArea();
    private final JTextField tfChat = new JTextField();
    private final JLabel lbTurn = new JLabel("TURN: -", SwingConstants.CENTER);

    private JLayeredPane layeredPane;
    private final int DRAG_LAYER = JLayeredPane.DRAG_LAYER;

    private final JButton btnStart = new JButton("게임 시작");
    private final JButton btnNext  = new JButton("다음 턴");
    private final JButton btnPlay  = new JButton("수 제출");
    private final JButton btnDraw  = new JButton("한 장 뽑기");
    private final JButton btnSortColor = new JButton("색상정렬");
    private final JButton btnSortNumber = new JButton("숫자정렬");

    private final TwoRowHandPanel handPanel = new TwoRowHandPanel();
    private final BoardPanel boardPanel = new BoardPanel();

    private boolean myTurn = false;
    private final JScrollPane spBoard;

    // 이번 턴에 내려놓은 타일 기록
    private final List<TileView> justPlayedTiles = new ArrayList<>();

    public RoomView(ClientApp app, String roomId) {

        this.app = app;
        this.roomId = roomId;

        setTitle("Room #" + roomId);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1024, 720);
        setLocationRelativeTo(null);

        BackgroundPanel bg = new BackgroundPanel(loadImage("assets/images/login_bg.png"));
        bg.setLayout(new BorderLayout(12,12));
        setContentPane(bg);
        layeredPane = getLayeredPane();

        JPanel north = translucentPanel(new BorderLayout());
        lbTurn.setForeground(Color.WHITE);
        lbTurn.setFont(lbTurn.getFont().deriveFont(Font.BOLD, 16f));
        north.add(lbTurn, BorderLayout.CENTER);
        bg.add(north, BorderLayout.NORTH);

        // ===== 중앙 =====
        // ▶ 1) BoardPanel을 JScrollPane으로 감싼다
        spBoard = new JScrollPane(boardPanel);
        spBoard.setOpaque(false);
        spBoard.getViewport().setOpaque(false);
        spBoard.setBorder(new LineBorder(Color.WHITE, 1));
        spBoard.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        spBoard.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        // ▶ 2) boardContainer에 JScrollPane을 넣는다
        JPanel boardContainer = translucentPanel(new BorderLayout());
        boardContainer.add(spBoard, BorderLayout.CENTER);


        JPanel chat = translucentPanel(new BorderLayout());
        taChat.setEditable(false);
        taChat.setOpaque(false);
        taChat.setForeground(Color.WHITE);
        taChat.setLineWrap(true);

        JScrollPane spChat = new JScrollPane(taChat);
        spChat.setOpaque(false);
        spChat.getViewport().setOpaque(false);
        spChat.setBorder(new LineBorder(Color.WHITE, 1));
        chat.add(spChat, BorderLayout.CENTER);
        chat.add(tfChat, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT, boardContainer, wrapCard(chat));
        split.setResizeWeight(0.72);
        split.setDividerSize(6);
        split.setOpaque(false);
        split.setBorder(null);
        bg.add(split, BorderLayout.CENTER);

        // ===== 하단 =====
        JPanel south = translucentPanel(new BorderLayout(8,8));
        JPanel btns = translucentPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnStart.setEnabled(false);

        btns.add(btnStart);
        btns.add(btnNext);
        btns.add(btnPlay);
        btns.add(btnDraw);
        btns.add(btnSortColor);
        btns.add(btnSortNumber);

        south.add(wrapCard(btns), BorderLayout.NORTH);

        JPanel handWrap = translucentPanel(new BorderLayout());
        handWrap.add(handPanel, BorderLayout.CENTER);
        south.add(handWrap, BorderLayout.CENTER);
        bg.add(south, BorderLayout.SOUTH);

        // ===== 리스너 =====
        tfChat.addActionListener(e -> {
            String msg = tfChat.getText().trim();
            if (!msg.isEmpty()) app.send("CHAT|" + msg);
            tfChat.setText("");
        });

        btnNext.addActionListener(e -> app.send("/next"));

        btnPlay.addActionListener(e -> {
        if (!myTurn) return;

        // 🔥 BoardPanel 전체 보드 상태를 서버로 제출
        String data = boardPanel.encodeMeldsForServer();

        if (data.isBlank()) {
            appendLog("❌ 제출할 타일이 없습니다.");
            return;
        }

        app.send("PLAY|" + data);
    });


        btnStart.addActionListener(e -> app.send("START_GAME"));
        btnDraw.addActionListener(e -> app.send("NO_TILE"));
        btnSortColor.addActionListener(e -> handPanel.sortByColor());
        btnSortNumber.addActionListener(e -> handPanel.sortByNumber());
    }

    // ===========================================================
    // 드래그 → Drop 처리 (핵심 수정)
    // ===========================================================
    private void handleDrop(TileView tv) {
        if (!myTurn) return;

        layeredPane.remove(tv);
        layeredPane.repaint();

        // 화면 기준 → boardPanel 기준 좌표 변환
        Point dropPoint = MouseInfo.getPointerInfo().getLocation();
        SwingUtilities.convertPointFromScreen(dropPoint, boardPanel);

        // 🔥 스크롤에서 '보이는 영역'만 드롭 가능하도록 처리
        JViewport vp = spBoard.getViewport();
        Rectangle visible = vp.getViewRect();

        // 🔥 좌표계를 viewport 기준으로 변환해야 정확한 판정 가능
Point vpPoint = SwingUtilities.convertPoint(boardPanel, dropPoint, vp);
        
        if (visible.contains(vpPoint)) {

            boardPanel.addTileAt(tv, dropPoint);

            if (!justPlayedTiles.contains(tv))
                justPlayedTiles.add(tv);

        } else {
            // 손패 복귀
            handPanel.addTile(tv);
            handPanel.restoreTile(tv);
            justPlayedTiles.remove(tv);
        }
    }

    // ===========================================================
    // 드래그 중 타일 위치를 layeredPane 기준으로 정확히 이동
    // ===========================================================
    private void handleDragging(TileView tv, Point localPoint) {

        if (!myTurn) return;

        // ① 드래그 시작 → layeredPane으로 부모 변경
        if (tv.getParent() != layeredPane) {

            // 현재 tv의 화면 좌표를 얻어 layeredPane로 변환
            Point screenPos = tv.getLocationOnScreen();
            SwingUtilities.convertPointFromScreen(screenPos, layeredPane);

            layeredPane.add(tv, DRAG_LAYER);
            tv.setLocation(screenPos);

            layeredPane.revalidate();
            layeredPane.repaint();
        }

        // ② localPoint = 타일 내부 좌표
        //  타일의 offsetX, offsetY 반영 필요
        int offsetX = tv.getOffsetX();
        int offsetY = tv.getOffsetY();

        // ③ 현재 마우스 화면 위치 가져오기
        Point mouseScreen = MouseInfo.getPointerInfo().getLocation();

        // ④ layeredPane 좌표계로 변환
        SwingUtilities.convertPointFromScreen(mouseScreen, layeredPane);

        // ⑤ 타일 위치 조정
        int tileX = mouseScreen.x - offsetX;
        int tileY = mouseScreen.y - offsetY;

        tv.setLocation(tileX, tileY);

        layeredPane.repaint();
    }


    // ===========================================================
    // 손패/턴 처리 (삭제 없음)
    // ===========================================================

    public void setInitialHand(String csv) {

        handPanel.clearTiles();
        if (csv == null || csv.isBlank()) return;

        for (String s : csv.split(",")) {

            String id = s.trim();
            Image img = loadTileImage(id);

            TileView tv = new TileView(id, img);

            // 중요: drag 이벤트 연결
            tv.addPropertyChangeListener("tileDropped", evt -> handleDrop(tv));
            tv.addPropertyChangeListener("tileDragging", evt -> handleDragging(tv, (Point) evt.getNewValue()));
            tv.addPropertyChangeListener("tileReturn", evt -> handleTileReturn(tv));

            handPanel.addTile(tv);
        }
    }

    public void addHandTile(String id) {

        Image img = loadTileImage(id);
        TileView tv = new TileView(id, img);

        tv.addPropertyChangeListener("tileDropped", evt -> handleDrop(tv));
        tv.addPropertyChangeListener("tileDragging", evt -> handleDragging(tv, (Point) evt.getNewValue()));
        tv.addPropertyChangeListener("tileReturn", evt -> handleTileReturn(tv));

        handPanel.addTile(tv);
    }

    private void handleTileReturn(TileView tv) {

        layeredPane.remove(tv);
        boardPanel.removeTile(tv);

        handPanel.restoreTile(tv);
        handPanel.sortDefault();

        repaint();
    }

    // ===========================================================
    // 턴 처리
    // ===========================================================
    public void updateTurn(String player) {

        lbTurn.setText("TURN: " + player);
        myTurn = player.equals(app.myName());

        btnPlay.setEnabled(myTurn);
        btnDraw.setEnabled(myTurn);
        btnNext.setEnabled(myTurn);

        for (TileView t : handPanel.getTileViews())
            t.setDraggable(myTurn);

        appendLog(myTurn ? "⭐ 내 턴입니다." : "⏳ 상대 턴입니다.");

        justPlayedTiles.clear();
    }

    // ===========================================================
    // 제출 성공
    // ===========================================================
    public void applyPlayOk(String who, String boardEncoded) {
        appendLog("✔ " + who + " 수 성공");
        justPlayedTiles.clear();
        boardPanel.loadBoardFromServer(boardEncoded);
    }

    // ===========================================================
    // 규칙 위반 → 이번 턴에 낸 타일만 복구
    // ===========================================================
    public void restoreJustPlayedTiles() {

        appendLog("⛔ 규칙 위반! 수가 취소되어 타일을 복구합니다.");

        List<TileView> list = new ArrayList<>(justPlayedTiles);
        justPlayedTiles.clear();

        for (TileView tv : list) {

            boardPanel.removeTile(tv);

            handPanel.add(tv);
            handPanel.restoreTile(tv);
        }

        handPanel.sortDefault();
        handPanel.repaint();
    }

    public void setStartEnabled(boolean on) {
        btnStart.setEnabled(on);
    }


    // ===========================================================
    // 유틸
    // ===========================================================
    public void appendLog(String line) {
        SwingUtilities.invokeLater(() -> {
            taChat.append(line + "\n");
            taChat.setCaretPosition(taChat.getDocument().getLength());
        });
    }

    private Image loadTileImage(String id) {
        return loadTileImageStatic(id);
    }

    public static Image loadTileImageStatic(String id) {
        try {
            var url = RoomView.class.getClassLoader()
                    .getResource("assets/images/" + id + ".png");
            if (url != null) return ImageIO.read(url);

            File f = new File("assets/images/" + id + ".png");
            if (f.exists()) return ImageIO.read(f);

        } catch (Exception ignored) {}
        return null;
    }

    private static BufferedImage loadImage(String path) {
        try {
            var url = RoomView.class.getClassLoader().getResource(path);
            if (url != null) return ImageIO.read(url);
            File f = new File(path);
            if (f.exists()) return ImageIO.read(f);
        } catch (Exception ignored) {}
        return null;
    }

    private static JPanel translucentPanel(LayoutManager lm){
        return new JPanel(lm){
            @Override public boolean isOpaque(){ return false; }
        };
    }

    private static JComponent wrapCard(JComponent c){
        JPanel card = translucentPanel(new BorderLayout());
        card.setBorder(new LineBorder(new Color(255,255,255,150), 1, true));
        card.add(c);
        return card;
    }

    static class BackgroundPanel extends JPanel {

        private final BufferedImage img;

        BackgroundPanel(BufferedImage img){ this.img = img; }

        @Override protected void paintComponent(Graphics g){

            super.paintComponent(g);
            if (img == null) return;

            int w = getWidth(), h = getHeight();
            double s = Math.max(
                    w / (double) img.getWidth(),
                    h / (double) img.getHeight());

            int dw = (int)(img.getWidth()*s);
            int dh = (int)(img.getHeight()*s);

            int dx = (w - dw)/2;
            int dy = (h - dh)/2;

            g.drawImage(img, dx, dy, dw, dh, null);

            g.setColor(new Color(0,0,0,60));
            g.fillRect(0,0,w,h);
        }
    }

    private int playersInRoom() {
            return app.getPlayerCount();
        }

        public void showGameEndPopup(String winner) {

        boolean iAmWinner = winner.equals(app.myName());
        boolean aloneWin = (app.getPlayerCount() == 1);

        // -------------------------------
        // 팝업 기본 설정
        // -------------------------------
        JDialog dialog = new JDialog(this, "게임 끝", true);
        dialog.setSize(500, 360);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(new Color(245, 245, 245));

        // -------------------------------
        // 제목 (WIN / LOSE)
        // -------------------------------
        JLabel title = new JLabel(
                iAmWinner ? "🏆 WIN!" : "😢 LOSE...",
                SwingConstants.CENTER
        );

        title.setFont(new Font("Dialog", Font.BOLD, 36));
        title.setBorder(BorderFactory.createEmptyBorder(40, 0, 20, 0));
        dialog.add(title, BorderLayout.NORTH);

        // -------------------------------
        // 중앙 여백
        // -------------------------------
        JPanel center = new JPanel();
        center.setOpaque(false);
        dialog.add(center, BorderLayout.CENTER);

        // ================================
        // 버튼 영역
        // ================================
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        btns.setOpaque(false);

        // 루미큐브 스타일 버튼
        Color greenStart = new Color(70, 200, 120);
        Color greenEnd   = new Color(40, 150, 90);

        Color redStart   = new Color(240, 120, 120);
        Color redEnd     = new Color(200, 60, 60);

        Color grayStart  = new Color(180, 180, 180);
        Color grayEnd    = new Color(130, 130, 130);

        FancyButton btnRetry     = new FancyButton("다시 게임하기", greenStart, greenEnd);
        FancyButton btnLobby     = new FancyButton("로비로 돌아가기", redStart, redEnd);
        FancyButton btnLobbyGray = new FancyButton("로비로 돌아가기", grayStart, grayEnd);

        // ------------------------------
        // 버튼 배치 로직
        // ------------------------------
        if (iAmWinner) {

            if (aloneWin) {
                // 혼자 남아 승리 → Gray 로비 버튼만
                btnLobbyGray.addActionListener(e -> {
                    dialog.dispose();
                    this.dispose();
                });
                btns.add(btnLobbyGray);

            } else {
                // 일반 승리
                btnRetry.addActionListener(e -> {
                    dialog.dispose();
                    app.send("START_GAME");
                });

                btnLobby.addActionListener(e -> {
                    dialog.dispose();
                    this.dispose();
                });

                btns.add(btnRetry);
                btns.add(btnLobby);
            }

        } else {
            // 패자
            btnLobbyGray.addActionListener(e -> {
                dialog.dispose();
                this.dispose();
            });
            btns.add(btnLobbyGray);
        }

        dialog.add(btns, BorderLayout.SOUTH);


        dialog.setVisible(true);
    }

    // ================================================
    // 🎨 루미큐브 스타일 커스텀 버튼 클래스
    // ================================================
    class FancyButton extends JButton {

        private Color startColor;
        private Color endColor;

        public FancyButton(String text, Color start, Color end) {
            super(text);
            this.startColor = start;
            this.endColor = end;

            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setForeground(Color.WHITE);
            setFont(new Font("Dialog", Font.BOLD, 16));
            setPreferredSize(new Dimension(170, 48));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            // Hover 효과
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                    startColor = startColor.brighter();
                    endColor = endColor.brighter();
                    repaint();
                }

                @Override public void mouseExited(java.awt.event.MouseEvent e) {
                    startColor = start;
                    endColor = end;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            // 그라데이션
            GradientPaint gp = new GradientPaint(
                    0, 0, startColor,
                    0, getHeight(), endColor
            );

            g2.setPaint(gp);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 25, 25);

            // 테두리
            g2.setColor(new Color(255, 255, 255, 180));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(0, 0, getWidth(), getHeight(), 25, 25);

            g2.dispose();
            super.paintComponent(g);
        }
    }
}
