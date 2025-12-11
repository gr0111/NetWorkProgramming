package client;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

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

        // 최소 높이를 600 유지
        int newHeight = Math.max(600, rows * rowHeight);

        // 폭은 2000 그대로 유지
        preferred = new Dimension(2000, newHeight);
    }

    // ============================================================
    // 🔥 멜드에서 tv 제거 + 자동 쪼개기
    // ============================================================
    private void removeFromMelds(TileView tv) {

        for (int i = 0; i < melds.size(); i++) {
            List<TileView> m = melds.get(i);

            if (m.contains(tv)) {
                int idx = m.indexOf(tv);
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
// 🔥 보드에서 타일 제거 (멜드 구조 대응)
// ============================================================
    public void removeTile(TileView tv) {

        // 1) 모든 멜드에서 tv 제거
        for (int i = 0; i < melds.size(); i++) {
            List<TileView> meld = melds.get(i);

            if (meld.remove(tv)) {

                // 제거 후 멜드가 비면 삭제
                if (meld.isEmpty()) {
                    melds.remove(i);
                }
                break;
        }
    }

    // 2) 화면에서도 제거
    remove(tv);

    // 3) 스크롤 높이 갱신
    updatePreferredSizeByMeldCount();

    revalidate();
    repaint();
}

    // ============================================================
    // 🔥 새로운 위치에 타일 추가
    // ============================================================
    public void addTileAt(TileView tv, Point p) {

        removeFromMelds(tv);

        // 1) 라인 번호 결정
        int lineHeight = 120, baseY = 20;
        int meldIndex = (p.y - baseY + lineHeight / 2) / lineHeight;
        if (meldIndex < 0) meldIndex = 0;
        if (meldIndex > melds.size()) meldIndex = melds.size();

        // 2) 필요 시 새 멜드 생성
        while (meldIndex >= melds.size()) {
            melds.add(new ArrayList<>());
        }

        List<TileView> meld = melds.get(meldIndex);

        // 3) 삽입 위치 계산
        int insertPos = 0;
        for (TileView t : meld) {
            if (p.x > t.getX()) insertPos++;
        }

        meld.add(insertPos, tv);

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
    // ⭐ 서버 문자열 인코딩
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
    // 서버에서 받은 보드 로딩
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

    // ============================================================
    // TileView 생성기
    // ============================================================
    private TileView createTile(String id) {

        Image img = RoomView.loadTileImageStatic(id);
        TileView tv = new TileView(id, img);

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
