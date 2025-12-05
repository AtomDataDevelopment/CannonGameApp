package br.com.prog3.cannongameapp;

import android.graphics.Rect;

public class Target extends GameElement {
    private int hitReward; // Recompensa de tempo

    public Target(CannonView view, int color, int hitReward, int x, int y,
                  int width, int length, float velocityY) {
        super(view, color, CannonView.TARGET_SOUND_ID, x, y, width, length, velocityY);
        this.hitReward = hitReward;
    }

    public int getHitReward() {
        return hitReward;
    }

    public Rect getRect() {
        return shape;
    }

    // --- ADICIONE ESTE MÉTODO NOVO ---
    public int getColor() {
        return paint.getColor(); // Pega a cor do objeto Paint herdado de GameElement
    }
}
