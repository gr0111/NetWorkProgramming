package client;

import javax.swing.SwingUtilities;

public class ClientMain {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                ClientApp app = new ClientApp();
                new LoginView(app).setVisible(true);
            }
        });
    }
}
