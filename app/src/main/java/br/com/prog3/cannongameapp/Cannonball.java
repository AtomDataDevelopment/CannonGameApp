package br.com.prog3.cannongameapp;

import android.graphics.Canvas;

public class Cannonball extends GameElement {
    private boolean onScreen;
    private double timeToLive = 5.0;
    private int bounces = 0;
    private static final int MAX_BOUNCES = 3;

    public Cannonball(CannonView view, int color, int soundId, int x,
                      int y, int radius, float velocityX, float velocityY) {
        super(view, color, soundId, x, y, 2 * radius, 2 * radius, velocityY, velocityX);
        onScreen = true;
    }

    private int getRadius() {
        return (shape.right - shape.left) / 2;
    }

    public boolean isOnScreen() {
        return onScreen;
    }

    public void reverseVelocityX() {
        velocityX *= -1;
    }

    public void registerBounce() {
        bounces++;
    }

    @Override
    public void update(double interval) {
        shape.offset((int) (velocityX * interval), (int) (velocityY * interval));

        if ((shape.left < 0 && velocityX < 0) || (shape.right > view.getScreenWidth() && velocityX > 0)) {
            velocityX *= -1;
            registerBounce();
        }
        if ((shape.top < 0 && velocityY < 0) || (shape.bottom > view.getScreenHeight() && velocityY > 0)) {
            velocityY *= -1;
            registerBounce();
        }

        timeToLive -= interval;

        if (bounces >= MAX_BOUNCES || timeToLive <= 0) {
            onScreen = false;
        }
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawCircle(shape.left + getRadius(),
                shape.top + getRadius(), getRadius(), paint);
    }
}
