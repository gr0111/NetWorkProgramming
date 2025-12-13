package client;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;

// 방 목록 조회 / 방 만들기 / 방 참가 (배경 포함) 
public class LobbyView extends JFrame {
    private final ClientApp app;

    private final DefaultListModel<RoomItem> model = new DefaultListModel<>();
    private final JList<RoomItem> list = new JList<>(model);
    private final JButton btnRefresh = new JButton("새로고침");
    private final JButton btnCreate  = new JButton("방 만들기");
    private final JButton btnJoin    = new JButton("참가");
    private final JLabel  status     = new JLabel(" ", SwingConstants.LEFT);

    public LobbyView(ClientApp app) {
        this.app = app;
        setTitle("Rummikub - Lobby");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(560, 460);
        setLocationRelativeTo(null);

        // 배경 패널
        BackgroundPanel bg = new BackgroundPanel(loadImage("assets/images/login_bg.png"));
        bg.setLayout(new BorderLayout(12,12));
        bg.setBorder(new EmptyBorder(10,10,10,10));
        setContentPane(bg);

        // 상단 타이틀
        JPanel north = translucent(new BorderLayout());
        JLabel title = new JLabel("Lobby", SwingConstants.LEFT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        title.setForeground(new Color(255,255,255,235));
        north.add(title, BorderLayout.WEST);
        bg.add(wrapCard(north), BorderLayout.NORTH);

        // 방 리스트
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane sp = new JScrollPane(list);
        makeScrollTranslucent(sp);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setViewportBorder(null);

        // 방 리스트를 카드처럼 보이게 설정
        list.setCellRenderer(new RoomCardRenderer());
        list.setFixedCellHeight(80);              
        list.setBorder(new EmptyBorder(4, 4, 4, 4));
        list.setOpaque(false);                     
        list.setBackground(new Color(0, 0, 0, 0));

        JPanel center = translucent(new BorderLayout());
        center.add(sp, BorderLayout.CENTER);
        bg.add(wrapCard(center), BorderLayout.CENTER);

        // 하단 버튼 및 상태
        JPanel south = translucent(new BorderLayout(8,8));
        JPanel btns  = translucent(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.add(btnRefresh);
        btns.add(btnCreate);
        btns.add(btnJoin);
        status.setForeground(new Color(255,255,255,220));
        south.add(status, BorderLayout.WEST);
        south.add(btns, BorderLayout.EAST);
        bg.add(wrapCard(south), BorderLayout.SOUTH);

        // 방 목록 새고로침
        btnRefresh.addActionListener(e -> app.requestRoomList());

        // 방 생성 
        btnCreate.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(this, "방 제목을 입력하세요", app.myName() + "의 방");
            if (name != null && !name.isBlank()) app.requestCreateRoom(name.trim());
        });
        // 방 참가
        btnJoin.addActionListener(e -> {
            RoomItem it = list.getSelectedValue();
            if (it == null) {
                JOptionPane.showMessageDialog(this, "참가할 방을 선택하세요.");
                return;
            }
            app.requestJoinRoom(it.id);
        });
        list.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    RoomItem it = list.getSelectedValue();
                    if (it != null) app.requestJoinRoom(it.id);
                }
            }
        });
    }

    // 서버에서 온 ROOM_LIST 데이터를 파싱하여 리스트 반영
    public void updateRoomList(String data) {
        List<RoomItem> items = parse(data);
        SwingUtilities.invokeLater(() -> {
            model.clear();
            for (RoomItem it : items) model.addElement(it);
            status.setText("방 수: " + items.size());
        });
    }

    // 하단 상태 메시지 업데이트
    public void showInfo(String msg) {
        SwingUtilities.invokeLater(() -> status.setText(msg));
    }

    private static List<RoomItem> parse(String data) {
        List<RoomItem> out = new ArrayList<>();
        if (data == null || data.isBlank()) return out;
        String[] parts = data.split(";");
        for (String p : parts) {
            String[] cols = p.split(",", 3); // id,name,count
            if (cols.length >= 3) {
                try {
                    int id = Integer.parseInt(cols[0].trim());
                    String name = cols[1].trim();
                    int cnt = Integer.parseInt(cols[2].trim());
                    out.add(new RoomItem(id, name, cnt));
                } catch (NumberFormatException ignore) {}
            }
        }
        return out;
    }

    // 방 정보 구조
    static class RoomItem {
        final int id; final String name; final int count;
        RoomItem(int id, String name, int count){ this.id=id; this.name=name; this.count=count; }
        @Override public String toString() { return String.format("#%d  %s  (%d명)", id, name, count); }
    }

    // 방 정보를 카드 형태로 렌더링
    private static class RoomCardRenderer extends JPanel implements ListCellRenderer<RoomItem> {

        private final JLabel lbId    = new JLabel();
        private final JLabel lbName  = new JLabel();
        private final JLabel lbInfo  = new JLabel();

        private Color bgTop  = new Color(30, 60, 110, 200);
        private Color bgBottom = new Color(10, 25, 60, 200);

        RoomCardRenderer() {
            setLayout(new BorderLayout(8, 4));
            setBorder(new EmptyBorder(6, 8, 6, 8));
            setOpaque(false);

            lbId.setFont(lbId.getFont().deriveFont(Font.BOLD, 12f));
            lbId.setForeground(new Color(210, 230, 255));

            lbName.setFont(lbName.getFont().deriveFont(Font.BOLD, 14f));
            lbName.setForeground(Color.WHITE);

            lbInfo.setFont(lbInfo.getFont().deriveFont(11f));
            lbInfo.setForeground(new Color(220, 220, 220));

            JPanel text = new JPanel(new BorderLayout(0, 4));
            text.setOpaque(false);
            text.add(lbName, BorderLayout.CENTER);
            text.add(lbInfo, BorderLayout.SOUTH);

            add(lbId, BorderLayout.WEST);
            add(text, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(
                JList<? extends RoomItem> list,
                RoomItem value,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {

            if (value != null) {
                lbId.setText("#" + value.id);
                lbName.setText(value.name);
                lbInfo.setText(String.format("인원: %d / 4명", value.count));
            }

            if (isSelected) {
                bgTop = new Color(90, 150, 230, 230);
                bgBottom = new Color(40, 90, 180, 230);
            } else {
                bgTop = new Color(30, 60, 110, 200);
                bgBottom = new Color(10, 25, 60, 200);
            }

            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // 카드 배경
            g2.setPaint(new GradientPaint(0, 0, bgTop, 0, h, bgBottom));
            g2.fillRoundRect(0, 0, w - 1, h - 1, 18, 18);

            // 테두리
            g2.setColor(new Color(255, 255, 255, 80));
            g2.drawRoundRect(0, 0, w - 1, h - 1, 18, 18);

            g2.dispose();
            // 자식 컴포넌트(라벨들) 그리기
            super.paintComponent(g);
        }
    }


    private static JPanel translucent(LayoutManager lm){
        return new JPanel(lm){ @Override public boolean isOpaque(){ return false; } };
    }
    private static JComponent wrapCard(JComponent c){
        JPanel card = translucent(new BorderLayout());
        card.setBorder(new CompoundBorder(
                new LineBorder(new Color(255,255,255,150), 1, true),
                new EmptyBorder(10,12,10,12)
        ));
        card.add(c, BorderLayout.CENTER);
        return card;
    }
    private static void makeScrollTranslucent(JScrollPane sp){
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
    }

    private static BufferedImage loadImage(String path){
        try {
            var url = LobbyView.class.getClassLoader().getResource(path);
            if (url != null) return ImageIO.read(url);
            return ImageIO.read(new File(path));
        } catch (Exception e) { return null; }
    }
    static class BackgroundPanel extends JPanel {
        private final BufferedImage img;
        BackgroundPanel(BufferedImage img){ this.img = img; }
        @Override protected void paintComponent(Graphics g){
            super.paintComponent(g);
            if (img == null) return;
            int w = getWidth(), h = getHeight();
            double s = Math.max(w/(double)img.getWidth(), h/(double)img.getHeight());
            int dw = (int)(img.getWidth()*s), dh = (int)(img.getHeight()*s);
            int dx = (w - dw)/2, dy = (h - dh)/2;
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(img, dx, dy, dw, dh, null);
            g2.setColor(new Color(0,0,0,60));
            g2.fillRect(0,0,w,h);
        }
    }
}
