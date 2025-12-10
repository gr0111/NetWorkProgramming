package client;

import javax.swing.BorderFactory;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatIntelliJLaf;  

public class ClientMain {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatIntelliJLaf());
        } catch (Exception ex) {
            System.err.println("Failed to initialize FlatLaf");
            ex.printStackTrace();
        }

        UIManager.put("List.focusCellHighlightBorder",
                    BorderFactory.createEmptyBorder());

        SwingUtilities.invokeLater(() -> {
            ClientApp app = new ClientApp();
            new LoginView(app).setVisible(true);   // 로그인 화면부터 시작
        });
    }
}
