package br.com.prog3.cannongameapp;

public class Blocker extends GameElement {
    private int missPenalty; // Penalidade de tempo

    public Blocker(CannonView view, int color, int missPenalty, int x,
                   int y, int width, int length, float velocityY) {
        // Corrected: Pass velocityY for the y-axis and 0.0f for the x-axis
        super(view, color, CannonView.BLOCKER_SOUND_ID, x, y, width, length, velocityY, 0.0f);
        this.missPenalty = missPenalty;
    }

    public int getMissPenalty() {
        return missPenalty;
    }
}