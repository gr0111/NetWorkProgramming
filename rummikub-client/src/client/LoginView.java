package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class LoginView extends JFrame {
    private final ClientApp app;

    public LoginView(ClientApp app) {
        this.app = app;
        setTitle("Rummikub - Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(320, 180);
        setLocationRelativeTo(null);

        final JTextField tfName = new JTextField("player", 10);
        final JTextField tfHost = new JTextField("127.0.0.1", 10);
        final JTextField tfPort = new JTextField("9999", 5);
        JButton btn = new JButton("Connect");

        JPanel p = new JPanel(new GridLayout(4,2,8,8));
        p.add(new JLabel("Name")); p.add(tfName);
        p.add(new JLabel("Host")); p.add(tfHost);
        p.add(new JLabel("Port")); p.add(tfPort);
        p.add(new JLabel());       p.add(btn);
        add(p);

        btn.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                String host = tfHost.getText().trim();
                int port = Integer.parseInt(tfPort.getText().trim());
                String name = tfName.getText().trim();
                app.connect(host, port, name, LoginView.this);
            }
        });
        getRootPane().setDefaultButton(btn);
    }
}
