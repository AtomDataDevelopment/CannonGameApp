package br.com.prog3.cannongameapp;

public class Blocker extends GameElement {

    private int missPenalty;

    public Blocker(CannonView view, int color, int missPenalty,
                   int x, int y, int width, int height, float velocityY) {

        super(view, color, CannonView.SOUND_BLOCK, x, y, width, height, velocityY);
        this.missPenalty = missPenalty;
    }

    public int getMissPenalty() {
        return missPenalty;
    }
}
