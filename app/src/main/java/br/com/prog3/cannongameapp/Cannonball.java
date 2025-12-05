package br.com.prog3.cannongameapp;

import android.graphics.Canvas;
import android.graphics.Rect;

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

    public boolean collidesWith(GameElement element) {
        return Rect.intersects(shape, element.shape);
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

        // Verifica colisão com as paredes e conta os quiques
        if ((shape.left < 0 && velocityX < 0) || (shape.right > view.getScreenWidth() && velocityX > 0)) {
            velocityX *= -1; // Quica nas paredes laterais
            registerBounce();
        }
        if ((shape.top < 0 && velocityY < 0) || (shape.bottom > view.getScreenHeight() && velocityY > 0)) {
            velocityY *= -1; // Quica no teto e no chão
            registerBounce();
        }

        timeToLive -= interval;

        // Remove a bola de canhão se exceder os quiques ou o tempo de vida
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