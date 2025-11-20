package common;

import java.io.Serializable;

public class Tile implements Serializable {
    private String color;
    private int number;
    private boolean isJoker;

    public Tile(String color, int number, boolean isJoker) {
        this.color = color;
        this.number = number;
        this.isJoker = isJoker;
    }

    public String getColor() { return color; }
    public int getNumber() { return number; }
    public boolean isJoker() { return isJoker; }

    public String getImageName() {
        return isJoker ? color + "Joker" : color + number;
    }

    @Override
    public String toString() {
        return getImageName();
    }
}