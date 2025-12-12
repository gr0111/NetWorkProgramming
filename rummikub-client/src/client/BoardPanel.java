package client;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class BoardPanel extends JPanel {

    // ⭐ 진짜 멜드 구조
    private final List<List<TileView>> melds = new ArrayList<>();

    private static final int TILE_W = 60;
    private static final int TILE_H = 80;

    private RoomView room;

    public void setRoom(RoomView r) { this.room = r; }

    private Dimension preferred = new Dimension(2000, 600);

    @Override
    public Dimension getPreferredSize() {
        return preferred;
    }

    public BoardPanel() {
        setLayout(null);
        setOpaque(false);
    }

    private void updatePreferredSizeByMeldCount() {

        int rows = melds.size();           // 멜드(줄) 개수
        int rowHeight = TILE_H + 40;       // 한 줄 높이 + 간격
        int newHeight = Math.max(600, rows * rowHeight);

        preferred = new Dimension(2000, newHeight);
    }

    // ============================================
    // ⭐ FAIL 복구용 좌표 저장 구조체 + 저장소
    // ============================================
    public static class Pos {
    public final int meldIndex;
    public final int tileIndex;

    public Pos(int m, int t) {
        this.meldIndex = m;
        this.tileIndex = t;
    }
}


    // 원래 멜드 위치 저장 (FAIL 복구용)
    private final java.util.Map<TileView, Pos> boardBackup = new java.util.HashMap<>();



    // ============================================================
    // 🔥 멜드에서 tv 제거 + 자동 쪼개기 + 원위치 백업 저장
    // ============================================================
    private void removeFromMelds(TileView tv) {

        for (int i = 0; i < melds.size(); i++) {
            List<TileView> m = melds.get(i);

            if (m.contains(tv)) {

                int idx = m.indexOf(tv);

                // ⭐ FAIL 복구를 위한 백업 위치 기록
                boardBackup.put(tv, new Pos(i, idx));

                m.remove(tv);

                // 🔥 멜드 쪼개기
                List<TileView> left = new ArrayList<>(m.subList(0, idx));
                List<TileView> right = new ArrayList<>(m.subList(idx, m.size()));

                melds.remove(i);

                if (!left.isEmpty()) melds.add(i++, left);
                if (!right.isEmpty()) melds.add(i, right);

                return;
            }
        }
    }


    // ============================================================
    // 🔥 보드에서 타일 제거
    // ============================================================
    public void removeTile(TileView tv) {

        for (int i = 0; i < melds.size(); i++) {
            List<TileView> meld = melds.get(i);

            if (meld.remove(tv)) {

                if (meld.isEmpty()) {
                    melds.remove(i);
                }
                break;
            }
        }

        remove(tv);

        updatePreferredSizeByMeldCount();
        revalidate();
        repaint();
    }


    // ============================================================
    // 🔥 새로운 위치에 타일 추가
    // ============================================================
    public void addTileAt(TileView tv, Point p) {

        removeFromMelds(tv);

        int lineHeight = 120, baseY = 20;
        int meldIndex = (p.y - baseY + lineHeight / 2) / lineHeight;

        if (meldIndex < 0) meldIndex = 0;
        if (meldIndex > melds.size()) meldIndex = melds.size();

        while (meldIndex >= melds.size()) {
            melds.add(new ArrayList<>());
        }

        List<TileView> meld = melds.get(meldIndex);

        int insertPos = 0;
        for (TileView t : meld) {
            if (p.x > t.getX()) insertPos++;
        }

        meld.add(insertPos, tv);

        layoutMelds();
    }


    // ============================================================
    // ⭐ FAIL 복구: 타일을 원래 멜드 위치로 되돌리기
    // ============================================================
    public void restoreTileToOriginalPosition(TileView tv, int meldIndex, int tileIndex) {

        if (meldIndex < 0 || meldIndex >= melds.size()) return;

        List<TileView> meld = melds.get(meldIndex);

        if (tileIndex < 0) tileIndex = 0;
        if (tileIndex > meld.size()) tileIndex = meld.size();

        removeFromMelds(tv);

        meld.add(tileIndex, tv);

        layoutMelds();
    }



    // ============================================================
    // 🔥 멜드 배치(화면 표시)
    // ============================================================
    private void layoutMelds() {

        removeAll();

        int y = 20;

        for (List<TileView> meld : melds) {
            int x = 20;

            for (TileView tv : meld) {
                tv.setBounds(x, y, TILE_W, TILE_H);
                add(tv);
                x += TILE_W + 10;
            }

            y += TILE_H + 40;
        }

        updatePreferredSizeByMeldCount();
        revalidate();
        repaint();
    }


    // ============================================================
    // ⭐ 서버 전송용 문자열 인코딩
    // ============================================================
    public String encodeMeldsForServer() {

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < melds.size(); i++) {

            if (i > 0) sb.append(";");

            List<TileView> meld = melds.get(i);

            for (int j = 0; j < meld.size(); j++) {

                if (j > 0) sb.append(",");

                sb.append(meld.get(j).getTileId());
            }
        }

        return sb.toString();
    }


    // ============================================================
    // 서버 보드 로딩
    // ============================================================
    public void loadBoardFromServer(String encoded) {

        removeAll();
        melds.clear();

        if (encoded == null || encoded.isBlank()) {
            repaint();
            return;
        }

        String[] mstrs = encoded.split(";");

        for (String m : mstrs) {

            String[] ids = m.split(",");

            List<TileView> meld = new ArrayList<>();

            for (String id : ids) {
                TileView tv = createTile(id);
                meld.add(tv);
            }

            melds.add(meld);
        }

        layoutMelds();
    }

    public Pos getBackupPosition(TileView tv) {
        return boardBackup.get(tv);
    }

    // ============================================================
    // TileView 생성기 (서버 보드용)
    // ============================================================
    private TileView createTile(String id) {

        Image img = RoomView.loadTileImageStatic(id);
        TileView tv = new TileView(id, img);

        tv.setFromHand(false);
        tv.setDraggable(true);

        tv.addPropertyChangeListener("tileDragging",
                evt -> room.handleDragging(tv, (Point) evt.getNewValue()));

        tv.addPropertyChangeListener("tileDropped",
                evt -> room.handleDrop(tv));

        tv.addPropertyChangeListener("tileReturn",
                evt -> room.handleTileReturn(tv));

        return tv;
    }
}
