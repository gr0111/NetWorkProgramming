package client;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/*
배경: 루미큐브 로고가 포함된 이미지(전체화면 스케일)
레이아웃: 배경 위에 수직 스택(위는 여백, 아래는 로그인 카드)
로고가 배경에 있으므로, 카드가 로고 "바로 아래"에 오도록 오프셋을 줌.
 */
public class LoginView extends JFrame {
    private final ClientApp app;

    // 로그인 패널이 배경 로고 아래에 적절하게 위치하도록 간격을 조정
    private static final int LOGO_BOTTOM_OFFSET = 190;

    public LoginView(ClientApp app) {
        this.app = app;
        setTitle("Rummikub - Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(720, 420);
        setLocationRelativeTo(null);

        // 배경 구성
        BufferedImage bgImg = loadImage(
                "assets/images/rummikub_logo.jpg",
                "/mnt/data/rummikub_logo.jpg"
        );
        BackgroundPanel bg = new BackgroundPanel(bgImg);
        bg.setLayout(new BoxLayout(bg, BoxLayout.Y_AXIS));
        setContentPane(bg);

        // 로고 아래쪽에 로그인 카드가 위치하도록 여백 추가
        bg.add(Box.createVerticalStrut(LOGO_BOTTOM_OFFSET));

        // 중앙 로그인
        JPanel centerWrap = new JPanel();
        centerWrap.setOpaque(false);
        centerWrap.setLayout(new BoxLayout(centerWrap, BoxLayout.X_AXIS));
        centerWrap.add(Box.createHorizontalGlue());

        JPanel card = new JPanel(new GridBagLayout()){
            @Override public boolean isOpaque(){ return false; }
        };
        // 카드 외곽선 제거, 내부 여백만 사용
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

        bg.add(centerWrap);
        bg.add(Box.createVerticalGlue());

        // 버튼 이벤트
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
        // Enter 키 입력 시 Connect 실행
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

    // 창 크기에 맞춰 배경 이미지 그리기
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
            // 살짝 어둡게
            g2.setColor(new Color(0,0,0,60));
            g2.fillRect(0,0,w,h);
        }
    }
}
