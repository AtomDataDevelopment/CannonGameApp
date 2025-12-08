package br.com.prog3.cannongameapp;

import java.util.Random;

public class Target extends GameElement {
    private int hitReward;
    private int color;

    public Target(CannonView view, int color, int hitReward, int x, int y,
                  int width, int length, float velocityY, float velocityX) {
        super(view, color, CannonView.TARGET_SOUND_ID, x, y, width, length, velocityY, velocityX);
        this.hitReward = hitReward;
        this.color = color;
    }

    public int getHitReward() {
        return hitReward;
    }

    public int getColor() {
        return color;
    }

    public void increaseVelocity(float factor) {
        this.velocityY *= factor;
        this.velocityX *= factor;
    }

    @Override
    public void update(double interval) {
        super.update(interval);
    }
}
