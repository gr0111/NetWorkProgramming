package client;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BoardPanel extends JPanel {

    // ⭐ 서버 + 클라이언트 공용 보드 타일 리스트
    private final List<TileView> tileViews = new ArrayList<>();

    private static final int TILE_W = 60;
    private static final int TILE_H = 80;
    private static final int TILE_GAP = 10;

    // 🔥 동적 preferredSize 저장 변수
    private Dimension preferred = new Dimension(2000, 600);

    private RoomView room;
    public void setRoom(RoomView r) { this.room = r; } 


    public BoardPanel() {
        setLayout(null);
        setOpaque(false);
    }

    // ============================================================
    // 🔥 보드에 타일 추가 (드래그 Drop 포함)
    // ============================================================
    public void addTileAt(TileView tv, Point p) {

        if (tv.getParent() != this) {
            if (tv.getParent() != null)
                tv.getParent().remove(tv);
            add(tv);
        }

        tv.setSize(TILE_W, TILE_H);

        // 🔥 Y를 라인 번호에 맞게 스냅
        int lineHeight = 120;
        int baseY = 20;
        int line = (p.y - baseY + lineHeight / 2) / lineHeight;

        if (line < 0) line = 0;
        if (line > 2) line = 2;

        int snapY = baseY + line * lineHeight;

        // 🔥 X 위치도 살짝 보정 (스크롤 영역 벗어나지 않도록)
        int px = Math.max(0, Math.min(p.x, preferred.width - TILE_W));

        tv.setLocation(px, snapY);

        if (!tileViews.contains(tv))
            tileViews.add(tv);

        updatePreferredSize();
        revalidate();
        repaint();
    }

    // ============================================================
    // 🔥 동적으로 preferredSize 계산
    // ============================================================
    private void updatePreferredSize() {

        if (tileViews.isEmpty()) {
            preferred = new Dimension(2000, 600);
            return;
        }

        int maxY = 0;

        for (TileView tv : tileViews) {
            int bottom = tv.getY() + TILE_H;
            if (bottom > maxY) maxY = bottom;
        }

        int newHeight = Math.max(600, maxY + 100);
        preferred = new Dimension(2000, newHeight);
    }

    @Override
    public Dimension getPreferredSize() {
        return preferred;
    }

    // ============================================================
    // ⭐ 서버 문자열 인코딩
    // ============================================================
    public String encodeMeldsForServer() {

        if (tileViews.isEmpty()) return "";

        List<List<TileView>> groups = extractMeldGroups();
        StringBuilder sb = new StringBuilder();

        for (List<TileView> g : groups) {
            if (sb.length() > 0) sb.append(";");

            for (int i = 0; i < g.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(g.get(i).getTileId());  // 🔥 tileId는 TileView에서 조커 포함 파싱됨
            }
        }
        return sb.toString();
    }

    // ============================================================
    // 서버 보드용 TileView 생성기
    // ============================================================
    private TileView createTile(String id) {

    Image img = RoomView.loadTileImageStatic(id);
    TileView tv = new TileView(id, img);

    tv.setDraggable(true);  // 보드 위 타일도 드래그 가능하게 설정

    // 🔥 RoomView의 드래그 처리 연결
    tv.addPropertyChangeListener("tileDragging",
            evt -> room.handleDragging(tv, (Point) evt.getNewValue()));

    tv.addPropertyChangeListener("tileDropped",
            evt -> room.handleDrop(tv));

    tv.addPropertyChangeListener("tileReturn",
            evt -> room.handleTileReturn(tv));

    return tv;
}


    // ============================================================
    // 서버에서 받은 보드 로딩
    // ============================================================
    public void loadBoardFromServer(String encoded) {

        removeAll();
        tileViews.clear();

        if (encoded == null || encoded.isBlank()) {
            preferred = new Dimension(2000, 600);
            repaint();
            return;
        }

        String[] melds = encoded.split(";");
        int y = 20;

        for (String meld : melds) {
            String[] tiles = meld.split(",");

            int x = 20;

            for (String id : tiles) {

                // 🔥 TileView 내부에서 조커 파싱 자동 처리
                TileView tv = createTile(id);

                tv.setBounds(x, y, TILE_W, TILE_H);

                add(tv);
                tileViews.add(tv);

                x += TILE_W + 8;
            }

            y += TILE_H + 20;
        }

        updatePreferredSize();
        revalidate();
        repaint();
    }

    // ============================================================
    // 타일 제거
    // ============================================================
    public void removeTile(TileView tv) {
        remove(tv);
        tileViews.remove(tv);

        updatePreferredSize();
        revalidate();
        repaint();
    }

    // ============================================================
    // 타일들을 그룹으로 분리 (가로로 가까운 타일 묶기)
    // ============================================================
    public List<List<TileView>> extractMeldGroups() {

        List<TileView> sorted = new ArrayList<>(tileViews);

        // Y → X 순으로 정렬
        sorted.sort(Comparator.comparingInt(TileView::getY)
                            .thenComparingInt(TileView::getX));

        List<List<TileView>> result = new ArrayList<>();
        List<TileView> cur = new ArrayList<>();

        int prevYGroup = -9999;
        int prevX = -9999;

        for (TileView tv : sorted) {

            int yGroup = tv.getY() / 120;

            boolean newLine = (yGroup != prevYGroup);
            boolean farX = Math.abs(tv.getX() - prevX) > 80;

            if (cur.isEmpty() || (!newLine && !farX)) {
                cur.add(tv);
            } else {
                result.add(cur);
                cur = new ArrayList<>();
                cur.add(tv);
            }

            prevYGroup = yGroup;
            prevX = tv.getX();
        }

        if (!cur.isEmpty()) {
            result.add(cur);
        }

        return result;
    }

    // ============================================================
    // 자동 레이아웃 (서버 로딩 후 정렬)
    // ============================================================
    public void autoLayout() {

        List<List<TileView>> groups = extractMeldGroups();

        removeAll();
        int y = 20;

        for (List<TileView> g : groups) {
            int x = 20;

            for (TileView tv : g) {
                tv.setBounds(x, y, TILE_W, TILE_H);
                add(tv);
                x += TILE_W + 8;
            }
            y += TILE_H + 20;

            if (y > 260) {
                y = 260;
            }
        }

        updatePreferredSize();
        revalidate();
        repaint();
    }

    // ============================================================
    // 라인 표시
    // ============================================================
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.setColor(new Color(255,255,255,40));
        g.drawLine(20, 110, getWidth() - 20, 110);
        g.drawLine(20, 230, getWidth() - 20, 230);
        g.drawLine(20, 350, getWidth() - 20, 350);
    }
}
