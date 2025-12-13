package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class TileView extends JComponent {

    private String tileId;
    private Image img;

    private boolean isJoker;
    private String color;
    private int number; 

    public boolean isJoker() { return isJoker; }
    public String getColor() { return color; }
    public int getNumber() { return number; }

    private boolean dragging = false;
    private int offsetX, offsetY;
    public int getOffsetX() { return offsetX; }
    public int getOffsetY() { return offsetY; }

    private boolean draggable = true;

    private boolean fromHand = true;

    public boolean isFromHand() { return fromHand; }
    public void setFromHand(boolean v) { this.fromHand = v; }

    public TileView(String tileId, Image img) {
        this.tileId = tileId;
        this.img = img;

        // 타일 문자열 파싱
        parseTileId(tileId);

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

    // 타일 문자열 파싱 (조커 포함)
    private void parseTileId(String tile) {

        isJoker = tile.contains("Joker");

        String pure = tile;
        if (tile.contains("(")) {
            pure = tile.substring(0, tile.indexOf("("));
        }

        if (isJoker) {
            // 색 추출
            color = pure.replace("Joker", "");  

            // 숫자 추출 
            String numStr = tile.replaceAll("[^0-9]", "");  

            number = numStr.isEmpty() ? -1 : Integer.parseInt(numStr);

        } else {
            // 일반 타일 처리
            color = pure.replaceAll("[0-9]", ""); 
            number = Integer.parseInt(pure.replaceAll("[^0-9]", "")); 
        }
    }


    // 마우스 리스너
    private final MouseListener mouseListener = new MouseAdapter() {

        @Override
        public void mousePressed(MouseEvent e) {
            if (!draggable) return;

            dragging = true;

            offsetX = e.getX();
            offsetY = e.getY();

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


    // 드래그 이동 이벤트 전달
    private final MouseMotionListener mouseMotionListener = new MouseMotionAdapter() {

        @Override
        public void mouseDragged(MouseEvent e) {
            if (!draggable) return;

            firePropertyChange(
                    "tileDragging",
                    null,
                    new Point(e.getX(), e.getY())
            );

            if (getParent() != null)
                getParent().repaint();
        }
    };
}