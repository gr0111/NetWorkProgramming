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
import javax.swing.border.EmptyBorder;
import java.util.List;

public class RoomView extends JFrame {

    private final ClientApp app;
    private final String roomId;

    private final JTextArea taChat = new JTextArea();
    private final JTextField tfChat = new JTextField();
    private final JLabel lbTurn = new JLabel("TURN: -", SwingConstants.CENTER);
    private final JLabel lbScore = new JLabel("점수: 0");
    private JLayeredPane layeredPane;
    private final int DRAG_LAYER = JLayeredPane.DRAG_LAYER;

    private final JButton btnStart = new JButton("게임 시작");
    //private final JButton btnNext  = new JButton("다음 턴");
    private final JButton btnPlay  = new JButton("수 제출");
    private final JButton btnDraw  = new JButton("한 장 뽑기");
    private final JButton btnSortColor = new JButton("색상정렬");
    private final JButton btnSortNumber = new JButton("숫자정렬");

    private final TwoRowHandPanel handPanel = new TwoRowHandPanel();
    private final BoardPanel boardPanel = new BoardPanel();

    private boolean myTurn = false;
    private final JScrollPane spBoard;
    private int myScore = 0;

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
        lbScore.setForeground(Color.WHITE);
        lbScore.setFont(lbScore.getFont().deriveFont(Font.BOLD, 14f));
        north.add(lbScore, BorderLayout.EAST);
        bg.add(north, BorderLayout.NORTH);

        spBoard = new JScrollPane(boardPanel);
        spBoard.setOpaque(false);
        spBoard.getViewport().setOpaque(false);
        spBoard.setBorder(new LineBorder(Color.WHITE, 1));
        spBoard.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        spBoard.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

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

        JPanel south = translucentPanel(new BorderLayout(8,8));
        JPanel btns = translucentPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnStart.setEnabled(false);

        btns.add(btnStart);
        //btns.add(btnNext);
        btns.add(btnPlay);
        btns.add(btnDraw);
        btns.add(btnSortColor);
        btns.add(btnSortNumber);

        south.add(wrapCard(btns), BorderLayout.NORTH);

        JPanel handWrap = translucentPanel(new BorderLayout());
        handWrap.add(handPanel, BorderLayout.CENTER);
        south.add(handWrap, BorderLayout.CENTER);
        bg.add(south, BorderLayout.SOUTH);

        tfChat.addActionListener(e -> {
            String msg = tfChat.getText().trim();
            if (!msg.isEmpty()) app.send("CHAT|" + msg);
            tfChat.setText("");
        });

        //btnNext.addActionListener(e -> app.send("/next"));

        btnPlay.addActionListener(e -> {
            if (!myTurn) return;

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

        boardPanel.setRoom(this);
    }

    public void handleDrop(TileView tv) {
    if (!myTurn) return;

    // 드래그 레이어에서 제거
    layeredPane.remove(tv);
    layeredPane.repaint();

    // ============================================================
    // ⭐ 1) 스크롤을 고려한 실제 보드 좌표 계산
    // ============================================================
    Point mouse = MouseInfo.getPointerInfo().getLocation();

    // 스크린 → viewport 좌표로 변환
    SwingUtilities.convertPointFromScreen(mouse, spBoard.getViewport());

    // viewport 좌표 + 스크롤 오프셋 → 실제 boardPanel 기준 좌표
    int realX = mouse.x + spBoard.getHorizontalScrollBar().getValue();
    int realY = mouse.y + spBoard.getVerticalScrollBar().getValue();
    Point dropPoint = new Point(realX, realY);

    // ============================================================
    // 2) 보이는 영역 판정
    // ============================================================
    Rectangle visible = spBoard.getViewport().getViewRect();

    if (visible.contains(realX, realY)) {

        // ============================================================
        // ⭐ 3) 손패에서 드래그했다면 무조건 공백 제거
        // ============================================================
        if (tv.isFromHand()) {
            handPanel.removeTile(tv);   // 👈 공백 제거 핵심
        }

        // ============================================================
        // ⭐ 4) 보드에 타일 추가
        // ============================================================
        boardPanel.addTileAt(tv, dropPoint);

        // 최초 제출 타일 목록에 기록
        if (!justPlayedTiles.contains(tv))
            justPlayedTiles.add(tv);

    } else {

        // ============================================================
        // ⭐ 5) 손패로 돌아갈 수 있는 조건: fromHand == true
        // ============================================================
        if (tv.isFromHand()) {
            handPanel.addTile(tv);
            handPanel.restoreTile(tv);
            justPlayedTiles.remove(tv);
        }

        // 이미 제출된 타일(fromHand=false)은 절대 복귀 불가
    }
}

    public void handleDragging(TileView tv, Point localPoint) {

        if (!myTurn) return;

        if (tv.getParent() != layeredPane) {

            Point screenPos = tv.getLocationOnScreen();
            SwingUtilities.convertPointFromScreen(screenPos, layeredPane);

            layeredPane.add(tv, DRAG_LAYER);
            tv.setLocation(screenPos);

            layeredPane.revalidate();
            layeredPane.repaint();
        }

        int offsetX = tv.getOffsetX();
        int offsetY = tv.getOffsetY();

        Point mouseScreen = MouseInfo.getPointerInfo().getLocation();

        SwingUtilities.convertPointFromScreen(mouseScreen, layeredPane);

        int tileX = mouseScreen.x - offsetX;
        int tileY = mouseScreen.y - offsetY;

        tv.setLocation(tileX, tileY);

        layeredPane.repaint();
    }

    public void setInitialHand(String csv) {

        handPanel.clearTiles();
        if (csv == null || csv.isBlank()) return;

        for (String s : csv.split(",")) {

            String id = s.trim();
            Image img = loadTileImage(id);

            TileView tv = new TileView(id, img);

            // 🔥 손패에서 받은 타일임을 명확히 표시
            tv.setFromHand(true);

            // 리스너 연결
            tv.addPropertyChangeListener("tileDropped", evt -> handleDrop(tv));
            tv.addPropertyChangeListener("tileDragging", evt -> handleDragging(tv, (Point) evt.getNewValue()));
            tv.addPropertyChangeListener("tileReturn", evt -> handleTileReturn(tv));

            // 손패에 추가
            handPanel.addTile(tv);
        }
    }


    public void updateScore(String player, int score) {

        if (player.equals(app.myName())) {
            myScore = score;
            lbScore.setText("점수: " + score);
        }

        appendLog("점수 ▶ " + player + " : " + score);
    }

    public void addHandTile(String id) {

        Image img = loadTileImage(id);
        TileView tv = new TileView(id, img);

        // ⭐ 손패에서 생성된 타일
        tv.setFromHand(true);

        // 드래그 이벤트 연결
        tv.addPropertyChangeListener("tileDropped", 
            evt -> handleDrop(tv));
        tv.addPropertyChangeListener("tileDragging",
            evt -> handleDragging(tv, (Point) evt.getNewValue()));
        tv.addPropertyChangeListener("tileReturn",
            evt -> handleTileReturn(tv));

        // 손패에 추가
        handPanel.addTile(tv);
    }


    public void handleTileReturn(TileView tv) {

        layeredPane.remove(tv);
        boardPanel.removeTile(tv);

        handPanel.restoreTile(tv);
        handPanel.sortDefault();

        repaint();
    }

    public void updateTurn(String player) {

        lbTurn.setText("TURN: " + player);
        myTurn = player.equals(app.myName());

        btnPlay.setEnabled(myTurn);
        btnDraw.setEnabled(myTurn);
        //btnNext.setEnabled(myTurn);

        for (TileView t : handPanel.getTileViews())
            t.setDraggable(myTurn);

        appendLog(myTurn ? "⭐ 내 턴입니다." : "⏳ 상대 턴입니다.");

        justPlayedTiles.clear();
    }

    public void applyPlayOk(String who, String boardEncoded) {
        appendLog("✔ " + who + " 수 성공");
        justPlayedTiles.clear();
        boardPanel.loadBoardFromServer(boardEncoded);
    }

    public void restoreJustPlayedTiles() {

    appendLog("⛔ 규칙 위반! 수가 취소되어 타일을 복구합니다.");

    List<TileView> list = new ArrayList<>(justPlayedTiles);
    justPlayedTiles.clear();

    for (TileView tv : list) {

        // 보드에서 삭제
        boardPanel.removeTile(tv);

        // 2) fromHand 여부로 복구 분기
        if (tv.isFromHand()) {

            // 손패에서 온 타일 → 손패로 복귀
            handPanel.addTile(tv);
            handPanel.restoreTile(tv);

        } else {

            // 보드 원래 위치로 되돌리기 위해 백업 위치 읽기
            BoardPanel.Pos pos = boardPanel.getBackupPosition(tv);
        if (pos != null) {
            boardPanel.restoreTileToOriginalPosition(tv, pos.meldIndex, pos.tileIndex);
        } else {
                // 혹시라도 백업 실패했을 경우: 마지막 줄 끝에 붙이기 (안전장치)
                boardPanel.addTileAt(tv, new Point(20, 20));
            }
        }
    }

    handPanel.sortDefault();
    handPanel.repaint();
}



    public void setStartEnabled(boolean on) {
        btnStart.setEnabled(on);
    }

    public void appendLog(String line) {
        SwingUtilities.invokeLater(() -> {
            taChat.append(line + "\n");
            taChat.setCaretPosition(taChat.getDocument().getLength());
        });
    }

    private Image loadTileImage(String id) {
        return loadTileImageStatic(id);
    }

    
    // ===========================================================
    // 🔥 조커 이미지 로딩 문제 해결 — 수정된 부분
    // ===========================================================
    public static Image loadTileImageStatic(String id) {
        try {
            String fixedId = id;

            // 조커일 경우: "RJoker(7)" → "RJoker"
            if (id.contains("Joker")) {
                int idx = id.indexOf("(");
                if (idx > 0) {
                    fixedId = id.substring(0, idx);
                }
            }

            var url = RoomView.class.getClassLoader()
                    .getResource("assets/images/" + fixedId + ".png");
            if (url != null) return ImageIO.read(url);

            File f = new File("assets/images/" + fixedId + ".png");
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

        boolean iAmWinner = winner != null && winner.equals(app.myName());
        boolean canRetry  = iAmWinner && playersInRoom() > 1;

        int absScore = Math.abs(myScore);

        JDialog dialog = new JDialog(this, "게임 끝", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(320, 220);
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(this);

        Box box = Box.createVerticalBox();
        box.setBorder(new EmptyBorder(20, 20, 10, 20));

        JLabel titleLabel = new JLabel(iAmWinner ? "승리했습니다!" : "패배했습니다");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18f));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        String winnerText = (winner != null) ? "승자: " + winner : "";
        JLabel winnerLabel = new JLabel(winnerText);
        winnerLabel.setForeground(new Color(90, 90, 90));
        winnerLabel.setFont(winnerLabel.getFont().deriveFont(13f));
        winnerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel scoreLabel = new JLabel();
        scoreLabel.setFont(scoreLabel.getFont().deriveFont(12f));
        scoreLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        if (iAmWinner) {
            scoreLabel.setText("🏆 +" + absScore);
            scoreLabel.setForeground(new Color(0x2e7d32));
        } else {
            scoreLabel.setText("😭 -" + absScore);
            scoreLabel.setForeground(new Color(0xc62828));
        }

        box.add(titleLabel);
        box.add(Box.createVerticalStrut(6));
        box.add(winnerLabel);
        box.add(Box.createVerticalStrut(4));
        box.add(scoreLabel);

        dialog.add(box, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        if (canRetry) {
            JButton btnRetry = new JButton("다시 게임하기");
            JButton btnExit  = new JButton("게임 종료");

            btnRetry.addActionListener(e -> {
                dialog.dispose();
                app.send("START_GAME");
            });

            btnExit.addActionListener(e -> {
                dialog.dispose();
                app.showLobby();
            });

            btnPanel.add(btnRetry);
            btnPanel.add(btnExit);

        } else {
            JButton btnExit = new JButton("게임 종료");
            btnExit.addActionListener(e -> {
                dialog.dispose();
                app.showLobby();
            });
            btnPanel.add(btnExit);
        }

        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
}