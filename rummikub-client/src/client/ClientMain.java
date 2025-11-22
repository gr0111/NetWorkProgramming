package client;

import javax.swing.SwingUtilities;

public class ClientMain {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ClientApp app = new ClientApp();
            new LoginView(app).setVisible(true);   // 로그인 화면부터 시작
        });
    }
}
