package br.com.prog3.cannongameapp;

public class Target extends GameElement {
    private int hitReward;

    public Target(CannonView view, int color, int hitReward, int x, int y,
                  int width, int length, float velocityY, float velocityX) {
        super(view, color, CannonView.TARGET_SOUND_ID, x, y, width, length, velocityY, velocityX);
        this.hitReward = hitReward;
    }

    public int getHitReward() {
        return hitReward;
    }

    public void increaseVelocity(float factor) {
        velocityX *= factor;
        velocityY *= factor;
    }
}
