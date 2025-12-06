package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class TileView extends JComponent {

    private String tileId;
    private Image img;

    private boolean dragging = false;
    private int offsetX, offsetY;
    public int getOffsetX() { return offsetX; }
    public int getOffsetY() { return offsetY; }


    private boolean draggable = true;

    public TileView(String tileId, Image img) {
        this.tileId = tileId;
        this.img = img;

        setSize(60, 80);
        setPreferredSize(new Dimension(60, 80));

        addMouseListener(mouseListener);
        addMouseMotionListener(mouseMotionListener);
    }

    public void setDraggable(boolean on) {
        this.draggable = on;
    }

    public String getTileId() {
        return tileId;
    }

    @Override
    protected void paintComponent(Graphics g) {

        g.drawImage(img, 0, 0, getWidth(), getHeight(), null);

        if (dragging) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, 0.35f));
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    // ============================================================
    // 마우스 리스너
    // ============================================================
    private final MouseListener mouseListener = new MouseAdapter() {

        @Override
        public void mousePressed(MouseEvent e) {
            if (!draggable) return;

            dragging = true;

            offsetX = e.getX();
            offsetY = e.getY();

            // 드래그 시작 → RoomView가 layeredPane으로 이동시키기 위해 z-index 조정
            if (getParent() != null) {
                getParent().setComponentZOrder(TileView.this, 0);
                getParent().repaint();
            }

            repaint();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (!draggable) return;

            dragging = false;
            repaint();

            // 🔥 드롭 완료 통지
            firePropertyChange("tileDropped", false, true);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (!draggable) return;

            if (e.getClickCount() == 2) {
                firePropertyChange("tileReturn", false, true);
            }
        }
    };


    // ============================================================
    // 드래그 이동 → 위치 이동은 절대 여기서 하지 않는다
    // ============================================================
    private final MouseMotionListener mouseMotionListener = new MouseMotionAdapter() {

        @Override
        public void mouseDragged(MouseEvent e) {
            if (!draggable) return;

            // ❌ 기존 코드 → setLocation()을 TileView 스스로 처리 → 좌표 튐 문제 발생
            // int newX = getX() + e.getX() - offsetX;
            // int newY = getY() + e.getY() - offsetY;
            // setLocation(newX, newY);

            // 🔥 RoomView가 이동을 담당하도록 “dragging event”만 보냄
            firePropertyChange(
                    "tileDragging",
                    null,
                    new Point(e.getX(), e.getY()) // local 좌표 전달
            );

            // 페인트
            if (getParent() != null)
                getParent().repaint();
        }
    };
}
