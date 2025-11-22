package client;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * 배경: 루미큐브 로고가 포함된 이미지(전체화면 스케일)
 * 레이아웃: 배경 위에 수직 스택(위는 여백, 아래는 로그인 카드)
 * → 로고가 배경에 있으므로, 카드가 로고 "바로 아래"에 오도록 오프셋을 줌.
 */
public class LoginView extends JFrame {
    private final ClientApp app;

    // ⬇ 폼을 살짝 더 위로 (기존 220 → 190)
    private static final int LOGO_BOTTOM_OFFSET = 190;

    public LoginView(ClientApp app) {
        this.app = app;
        setTitle("Rummikub - Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(720, 420);
        setLocationRelativeTo(null);

        // 1) 배경 패널
        BufferedImage bgImg = loadImage(
                "assets/images/rummikub_logo.jpg",
                "/mnt/data/rummikub_logo.jpg"
        );
        BackgroundPanel bg = new BackgroundPanel(bgImg);
        bg.setLayout(new BoxLayout(bg, BoxLayout.Y_AXIS));
        setContentPane(bg);

        // 2) 위쪽 여백(로고 아래 위치 조정)
        bg.add(Box.createVerticalStrut(LOGO_BOTTOM_OFFSET));

        // 3) 가운데 정렬 래퍼
        JPanel centerWrap = new JPanel();
        centerWrap.setOpaque(false);
        centerWrap.setLayout(new BoxLayout(centerWrap, BoxLayout.X_AXIS));
        centerWrap.add(Box.createHorizontalGlue());

        // 4) 로그인 카드
        JPanel card = new JPanel(new GridBagLayout()){
            @Override public boolean isOpaque(){ return false; }
        };
        // ⬇ 흰 선 제거: LineBorder 삭제, 내부 패딩만 유지
        card.setBorder(new EmptyBorder(16,20,16,20));

        JPanel form = new JPanel(new GridLayout(4, 2, 10, 10)){
            @Override public boolean isOpaque(){ return false; }
        };
        JLabel lbName = label("Name");
        JLabel lbHost = label("Host");
        JLabel lbPort = label("Port");

        JTextField tfName = new JTextField("player", 12);
        JTextField tfHost = new JTextField("127.0.0.1", 12);
        JTextField tfPort = new JTextField("9999", 6);

        JButton btn = new JButton("Connect");
        btn.setFocusable(false);

        form.add(lbName); form.add(tfName);
        form.add(lbHost); form.add(tfHost);
        form.add(lbPort); form.add(tfPort);
        form.add(new JLabel()); form.add(btn);

        card.add(form);
        centerWrap.add(card);
        centerWrap.add(Box.createHorizontalGlue());

        // 5) 중앙 카드 추가
        bg.add(centerWrap);

        // 아래 여백
        bg.add(Box.createVerticalGlue());

        // 6) 버튼 액션
        btn.addActionListener((ActionEvent e) -> {
            String host = tfHost.getText().trim();
            String name = tfName.getText().trim();
            int port;
            try {
                port = Integer.parseInt(tfPort.getText().trim());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Port는 숫자로 입력해 주세요.", "입력 오류", JOptionPane.WARNING_MESSAGE);
                tfPort.requestFocus();
                return;
            }
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "이름을 입력해 주세요.", "입력 오류", JOptionPane.WARNING_MESSAGE);
                tfName.requestFocus();
                return;
            }
            app.setLogin(this);
            app.connectAndLogin(host, port, name);
        });
        getRootPane().setDefaultButton(btn);
    }

    private static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(new Color(255,255,255,220));
        l.setFont(l.getFont().deriveFont(Font.BOLD));
        return l;
    }

    private static BufferedImage loadImage(String... paths) {
        for (String path : paths) {
            try {
                var url = LoginView.class.getClassLoader().getResource(path);
                if (url != null) return ImageIO.read(url);
                File f = new File(path);
                if (f.exists()) return ImageIO.read(f);
            } catch (Exception ignore) {}
        }
        return null;
    }

    /** 프레임 크기에 맞춰 이미지를 부드럽게 스케일링해 그리는 배경 패널 */
    static class BackgroundPanel extends JPanel {
        private final BufferedImage img;
        BackgroundPanel(BufferedImage img){ this.img = img; }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (img == null) return;
            int w = getWidth(), h = getHeight();
            double s = Math.max(w / (double)img.getWidth(), h / (double)img.getHeight());
            int dw = (int)(img.getWidth() * s);
            int dh = (int)(img.getHeight() * s);
            int dx = (w - dw) / 2;
            int dy = (h - dh) / 2;
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(img, dx, dy, dw, dh, null);
            // 살짝 어둡게(가독성)
            g2.setColor(new Color(0,0,0,60));
            g2.fillRect(0,0,w,h);
        }
    }
}
