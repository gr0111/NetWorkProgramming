package client;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class TwoRowHandPanel extends JPanel {

    private final List<TileView> tileViews = new ArrayList<>();

    private static final int TILE_WIDTH = 60;
    private static final int TILE_HEIGHT = 80;
    private static final int TILE_GAP = 10;

    public TwoRowHandPanel() {
        setLayout(null);
        setOpaque(false);
        setBackground(new Color(0,0,0,0));
        setPreferredSize(new Dimension(1000, 180));
    }

    public void clearTiles() {
        tileViews.clear();
        removeAll();
        revalidate();
        repaint();
    }

    // ★ tileViews 수정은 여기서만!
    public void addTile(TileView tv) {
        if (!tileViews.contains(tv))
            tileViews.add(tv);

        if (tv.getParent() != this)
            add(tv);

        layoutTiles();
    }

    // ★ tileViews 수정은 여기서만!
    public void removeTile(TileView tv) {
        tileViews.remove(tv);
        remove(tv);

        layoutTiles(); 
    }

    public void layoutTiles() {
        int x = TILE_GAP;
        int y = TILE_GAP;

        for (int i = 0; i < tileViews.size(); i++) {
            TileView tv = tileViews.get(i);

            tv.setSize(TILE_WIDTH, TILE_HEIGHT);

            if (i == 14) {
                x = TILE_GAP;
                y = TILE_HEIGHT + 20;
            }

            tv.setLocation(x, y);
            x += TILE_WIDTH + TILE_GAP;
        }

        revalidate();
        repaint();
    }

    public void restoreTile(TileView tv) {
        if (!tileViews.contains(tv))
            tileViews.add(tv);

        if (tv.getParent() != this)
            add(tv);

        layoutTiles();
    }
    public void sortDefault() {
        sortByNumber();
    }

    // ============================================================
    // ⭐ 숫자 정렬 — 조커 완전 지원
    // ============================================================
    public void sortByNumber() {
        tileViews.sort((a, b) -> {

            boolean aj = isJoker(a);
            boolean bj = isJoker(b);

            // 1) 조커는 항상 맨 뒤로
            if (aj && !bj) return 1;
            if (!aj && bj) return -1;

            // 2) 일반 타일이면 숫자 비교
            int na = extractNumber(a.getTileId());
            int nb = extractNumber(b.getTileId());
            if (na != nb) return na - nb;

            // 3) 숫자가 같으면 색 비교
            return extractColor(a.getTileId()).compareTo(extractColor(b.getTileId()));
        });

        layoutTiles();
    }

    // ============================================================
    // ⭐ 색상 정렬 — 조커 완전 지원
    // ============================================================
    public void sortByColor() {
        tileViews.sort((a, b) -> {

            boolean aj = isJoker(a);
            boolean bj = isJoker(b);

            // 1) 조커는 항상 뒤로
            if (aj && !bj) return 1;
            if (!aj && bj) return -1;

            // 2) 일반 타일이면 색 비교
            String ca = extractColor(a.getTileId());
            String cb = extractColor(b.getTileId());
            if (!ca.equals(cb)) return ca.compareTo(cb);

            // 3) 색이 같으면 숫자 비교
            int na = extractNumber(a.getTileId());
            int nb = extractNumber(b.getTileId());
            return na - nb;
        });

        layoutTiles();
    }

    // ============================================================
    // ⭐ 조커 판정
    // ============================================================
    private boolean isJoker(TileView tv) {
        String id = tv.getTileId();
        return id.equals("BJoker") || id.equals("RJoker");
    }

    // ============================================================
    // ⭐ 숫자 추출 (조커는 최댓값으로 고정)
    // ============================================================
    private int extractNumber(String id) {
        if (id.equals("BJoker") || id.equals("RJoker"))
            return Integer.MAX_VALUE;

        return Integer.parseInt(id.replaceAll("[^0-9]", ""));
    }

    // ============================================================
    // ⭐ 색 추출 (조커는 'ZZ'로 보내 정렬에서 가장 뒤로)
    // ============================================================
    private String extractColor(String id) {
        if (id.equals("BJoker") || id.equals("RJoker"))
            return "ZZ";

        return id.replaceAll("[0-9]", "");
    }

    public List<TileView> getTileViews() {
        return tileViews;
    }
}