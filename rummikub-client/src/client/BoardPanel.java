package client;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class BoardPanel extends JPanel {

    private final List<TileView> placedTiles = new ArrayList<>();

    private static final int TILE_W = 60;
    private static final int TILE_H = 80;
    private static final int TILE_GAP = 10;

    public BoardPanel() {
        setLayout(null);
        setOpaque(false);
    }

    // ============================================================
    // 타일 배치 (🔥 스냅 복구 → 자동 정렬)
    // ============================================================
    public void addTileAt(TileView tv, Point p) {

        if (tv.getParent() != this) {
            if (tv.getParent() != null)
                tv.getParent().remove(tv);
            add(tv);
        }

        tv.setSize(TILE_W, TILE_H);
        tv.setLocation(p.x, p.y);

        if (!placedTiles.contains(tv))
            placedTiles.add(tv);

        // ⭐ 자동 정렬 기능 복구됨
        snapPositions();

        revalidate();
        repaint();
    }


    // ============================================================
    // ⭐ 자동 스냅 — 실제 루미큐브처럼 라인별 정렬
    // ============================================================
    private void snapPositions() {

        if (placedTiles.isEmpty()) return;

        List<TileView> line1 = new ArrayList<>();
        List<TileView> line2 = new ArrayList<>();
        List<TileView> line3 = new ArrayList<>();

        for (TileView tv : placedTiles) {

            int y = tv.getY();

            if (y < 160)           line1.add(tv);
            else if (y < 280)     line2.add(tv);
            else                  line3.add(tv);
        }

        sortLine(line1, 30);
        sortLine(line2, 150);
        sortLine(line3, 270);
    }

    private void sortLine(List<TileView> line, int baseY) {
        if (line.isEmpty()) return;

        // 🌟 타일 숫자 기준으로 오름차순 정렬
        line.sort((a, b) -> {
            int na = Integer.parseInt(a.getTileId().replaceAll("[^0-9]", ""));
            int nb = Integer.parseInt(b.getTileId().replaceAll("[^0-9]", ""));
            return Integer.compare(na, nb);
        });

        int x = 30;

        for (TileView tv : line) {
            tv.setLocation(x, baseY);
            x += TILE_W + TILE_GAP;
        }
    }

    // ============================================================
    // 서버 전송용 인코딩 (UI와 무관, 기존 유지)
    // ============================================================
    public String encodeMeldsForServer() {

        if (placedTiles.isEmpty()) return "";

        placedTiles.sort((a, b) -> Integer.compare(a.getX(), b.getX()));

        List<List<TileView>> groups = new ArrayList<>();
        List<TileView> cur = new ArrayList<>();
        int prevX = -9999;

        for (TileView t : placedTiles) {

            if (Math.abs(t.getX() - prevX) > 30) {
                if (!cur.isEmpty()) groups.add(cur);
                cur = new ArrayList<>();
            }

            cur.add(t);
            prevX = t.getX();
        }

        if (!cur.isEmpty()) groups.add(cur);

        StringBuilder sb = new StringBuilder();

        for (List<TileView> g : groups) {
            if (sb.length() > 0) sb.append(",");
            for (int i = 0; i < g.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(g.get(i).getTileId());
            }
        }

        return sb.toString();
    }


    // ============================================================
    // 서버 보드 로딩 (기존 유지)
    // ============================================================
    public void loadBoardFromServer(String encoded) {
        removeAll();
        placedTiles.clear();

        if (encoded == null || encoded.isBlank()) {
            revalidate();
            repaint();
            return;
        }

        String[] melds = encoded.split(";");

        int x = 30;
        int y = 30;
        int maxWidth = getWidth() - 100;

        for (String meld : melds) {

            String[] ids = meld.split(",");
            int mw = ids.length * 70;

            if (x + mw > maxWidth) {
                x = 30;
                y += 120;
            }

            for (String id : ids) {
                Image img = RoomView.loadTileImageStatic(id);
                TileView tv = new TileView(id, img);

                tv.setSize(TILE_W, TILE_H);
                tv.setLocation(x, y);

                add(tv);
                placedTiles.add(tv);

                x += 70;
            }

            x += 50;
        }

        revalidate();
        repaint();
    }

    public void removeTile(TileView tv) {
        remove(tv);
        placedTiles.remove(tv);
        repaint();
    }


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.setColor(new Color(255,255,255,40));
        g.drawLine(20, 110, getWidth() - 20, 110);
        g.drawLine(20, 230, getWidth() - 20, 230);
        g.drawLine(20, 350, getWidth() - 20, 350);
    }
}
