package br.com.prog3.cannongameapp;

public class Blocker extends GameElement {
    private int missPenalty;

    public Blocker(CannonView view, int color, int missPenalty, int x,
                   int y, int width, int length, float velocityY) {
        super(view, color, CannonView.BLOCKER_SOUND_ID, x, y, width, length, velocityY, 0.0f);
        this.missPenalty = missPenalty;
    }

    public int getMissPenalty() {
        return missPenalty;
    }
}
