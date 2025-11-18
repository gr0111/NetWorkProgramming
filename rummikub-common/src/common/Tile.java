package common;

import java.io.Serializable;

/**
 * 루미큐브 타일 데이터 구조
 */
public class Tile implements Serializable {
    private static final long serialVersionUID = 1L;

    private String color;   // 색상 ("R", "B", "Y", "BL" 등)
    private int number;     // 타일 숫자 (1~13)
    private boolean isJoker;
    private String imageName; // 이미지 파일명 (예: "R1", "BJoker")

    public Tile(String color, int number, boolean isJoker) {
        this.color = color;
        this.number = number;
        this.isJoker = isJoker;

        if (isJoker) {
            this.imageName = color + "Joker";
        } else {
            this.imageName = color + number;
        }
    }

    public String getColor() { return color; }
    public int getNumber() { return number; }
    public boolean isJoker() { return isJoker; }
    public String getImageName() { return imageName; }

    @Override
    public String toString() {
        return isJoker ? color + "Joker" : color + number;
    }
}