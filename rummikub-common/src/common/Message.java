package common;

import java.io.Serializable;

/**
 * 서버-클라이언트 간 주고받는 기본 메시지 구조
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    private String type;     // 메시지 타입 (Protocol 상수 중 하나)
    private String sender;   // 전송자 이름
    private String data;     // 실제 데이터 내용

    public Message(String type, String sender, String data) {
        this.type = type;
        this.sender = sender;
        this.data = data;
    }

    public String getType() {
        return type;
    }

    public String getSender() {
        return sender;
    }

    public String getData() {
        return data;
    }

    @Override
    public String toString() {
        return "[" + type + "] " + sender + ": " + data;
    }
}