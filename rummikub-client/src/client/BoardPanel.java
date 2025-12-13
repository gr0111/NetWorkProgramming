package client;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class BoardPanel extends JPanel {

    // 보드 위의 멜드(줄) 구조
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

    // 멜드 개수에 따라 스크롤 높이를 조정
    private void updatePreferredSizeByMeldCount() {

        int rows = melds.size();           
        int rowHeight = TILE_H + 40;       
        int newHeight = Math.max(600, rows * rowHeight);

        preferred = new Dimension(2000, newHeight);
    }

    // 제출 실패 시 복원하기 위한 위치 정보 저장 구조
    public static class Pos {
    public final int meldIndex;
    public final int tileIndex;

    public Pos(int m, int t) {
        this.meldIndex = m;
        this.tileIndex = t;
    }
}


    // 타일의 원래 위치 백업
    private final java.util.Map<TileView, Pos> boardBackup = new java.util.HashMap<>();



    // 멜드에서 특정 타일을 제거하고, 필요하면 멜드를 분할
    // 제거되기 전 위치는 복구용으로 저장
    private void removeFromMelds(TileView tv) {

        for (int i = 0; i < melds.size(); i++) {
            List<TileView> m = melds.get(i);

            if (m.contains(tv)) {

                int idx = m.indexOf(tv);

                // 복구를 위해 기존 위치 저장
                boardBackup.put(tv, new Pos(i, idx));

                m.remove(tv);

                // 멜드가 중간에서 잘릴 경우 앞/뒤로 분리
                List<TileView> left = new ArrayList<>(m.subList(0, idx));
                List<TileView> right = new ArrayList<>(m.subList(idx, m.size()));

                melds.remove(i);

                if (!left.isEmpty()) melds.add(i++, left);
                if (!right.isEmpty()) melds.add(i, right);

                return;
            }
        }
    }


    // 보드에서 타일을 제거
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


    // 타일을 지정된 좌표 기준으로 보드에 배치
    public void addTileAt(TileView tv, Point p) {

        // 기존 위치에서 제거
        removeFromMelds(tv);

        int lineHeight = 120, baseY = 20;
        int meldIndex = (p.y - baseY + lineHeight / 2) / lineHeight;

        if (meldIndex < 0) meldIndex = 0;
        if (meldIndex > melds.size()) meldIndex = melds.size();

        // 필요한 경우 새로운 멜드 생성
        while (meldIndex >= melds.size()) {
            melds.add(new ArrayList<>());
        }

        List<TileView> meld = melds.get(meldIndex);

        // X좌표를 기준으로 삽입할 위치 계산
        int insertPos = 0;
        for (TileView t : meld) {
            if (p.x > t.getX()) insertPos++;
        }

        meld.add(insertPos, tv);

        layoutMelds();
    }


    // 제출 실패 시, 타일을 원래 위치로 복원
    public void restoreTileToOriginalPosition(TileView tv, int meldIndex, int tileIndex) {

        if (meldIndex < 0 || meldIndex >= melds.size()) return;

        List<TileView> meld = melds.get(meldIndex);

        if (tileIndex < 0) tileIndex = 0;
        if (tileIndex > meld.size()) tileIndex = meld.size();

        removeFromMelds(tv);

        meld.add(tileIndex, tv);

        layoutMelds();
    }



    // 멜드를 화면에 다시 배치
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


    // 서버 전송용 문자열로 변환
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


    // 서버에서 전달된 보드 상태를 로딩
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

    // 서버로부터 받아 보드에 생성하는 타일
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
