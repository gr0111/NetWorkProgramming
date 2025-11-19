package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;


public class RoomView extends JFrame {
    private final ClientApp app;
    private final String roomId;

    private final JTextArea taChat = new JTextArea();
    private final JTextField tfChat = new JTextField();
    private final JLabel lbTurn = new JLabel("TURN: -", SwingConstants.CENTER);
    private final JTextArea taLog = new JTextArea(5,24);

    public RoomView(ClientApp app, String roomId){
        this.app = app; this.roomId = roomId;
        setTitle("Room #" + roomId);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(640, 420);
        setLocationRelativeTo(null);

        add(lbTurn, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(1,2,8,8));
        JPanel board = new JPanel(); // TODO: 보드/손패 UI 이후 추가
        board.add(new JLabel("Board / Hand (추가 예정)"));
        center.add(board);

        JPanel chat = new JPanel(new BorderLayout());
        taChat.setEditable(false);
        chat.add(new JScrollPane(taChat), BorderLayout.CENTER);
        chat.add(tfChat, BorderLayout.SOUTH);
        center.add(chat);
        add(center, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout());
        JPanel btns = new JPanel();
        JButton btnNext = new JButton("다음 턴");
        JButton btnPlay = new JButton("수 제출");
        btns.add(btnNext); btns.add(btnPlay);
        south.add(btns, BorderLayout.NORTH);
        taLog.setEditable(false);
        south.add(new JScrollPane(taLog), BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

        tfChat.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                String msg = tfChat.getText().trim();
                if (msg.length() > 0) app.send(msg); // plain text 전송
                tfChat.setText("");
            }
        });
        btnNext.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                app.send("/next"); // 서버 Room이 처리
            }
        });
        btnPlay.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                String encodedMove = "DUMMY_MOVE"; // TODO: 실제 수 인코딩
                app.send(encodedMove);             // 서버는 지금 일반 문자열만 처리
            }
        });
    }

    public void appendLog(final String line){
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                taLog.append(line + "\n");
                taChat.append(line + "\n"); // 채팅창에도 같이 보여주기(초기 단계 편의)
            }
        });
    }
    public void showTurn(final String player){
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() { lbTurn.setText("TURN: " + player); }
        });
    }
}
