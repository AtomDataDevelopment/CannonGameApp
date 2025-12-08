package br.com.prog3.cannongameapp;

public class Target extends GameElement {

    private int hitReward;

    public Target(CannonView view, int color, int hitReward,
                  int x, int y, int width, int height, float velocityY) {

        super(view, color, CannonView.SOUND_HIT, x, y, width, height, velocityY);
        this.hitReward = hitReward;
    }

    public int getHitReward() {
        return hitReward;
    }
}
