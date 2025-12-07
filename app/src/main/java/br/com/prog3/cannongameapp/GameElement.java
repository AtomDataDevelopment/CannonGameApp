package br.com.prog3.cannongameapp;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

public class GameElement {
    protected CannonView view;
    protected Paint paint = new Paint();
    protected Rect shape;
    protected float velocityY;
    protected float velocityX;
    private int soundId;

    private static final long APPEAR_ANIMATION_TIME = 500L;
    private static final long HIT_ANIMATION_TIME = 300L;
    private long animationStartTime;
    private boolean hit = false;
    private float scale = 0.0f;
    private int alpha = 0;


    public GameElement(CannonView view, int color, int soundId, int x,
                       int y, int width, int length, float velocityY, float velocityX) {
        this.view = view;
        paint.setColor(color);
        shape = new Rect(x, y, x + width, y + length);
        this.soundId = soundId;
        this.velocityY = velocityY;
        this.velocityX = velocityX;
        this.animationStartTime = System.currentTimeMillis();
    }

    public void update(double interval) {
        shape.offset(0, (int) (velocityY * interval));
        shape.offset((int) (velocityX * interval), 0);

        if (shape.top < 0 && velocityY < 0 ||
                shape.bottom > view.getScreenHeight() && velocityY > 0) {
            velocityY *= -1;
        }

        if (shape.left < 0 && velocityX < 0 ||
                shape.right > view.getScreenWidth() && velocityX > 0) {
            velocityX *= -1;
        }

        if (shape.top < 0) {
            shape.offsetTo(shape.left, 0);
        }
        if (shape.bottom > view.getScreenHeight()) {
            shape.offsetTo(shape.left, view.getScreenHeight() - shape.height());
        }

        if (shape.left < 0) {
            shape.offsetTo(0, shape.top);
        }
        if (shape.right > view.getScreenWidth()) {
            shape.offsetTo(view.getScreenWidth() - shape.width(), shape.top);
        }
        alpha = Math.max(0, Math.min(255, alpha));
    }

    public void draw(Canvas canvas) {
        paint.setAlpha(alpha);
        canvas.save();
        canvas.scale(scale, scale, shape.centerX(), shape.centerY());
        canvas.drawRect(shape, paint);
        canvas.restore();
    }

    public void playSound() {
        view.playSound(soundId);
    }

    public boolean collidesWith(GameElement other) {
        return Rect.intersects(shape, other.shape);
    }

    public void onHit() {
        if (!hit) {
            hit = true;
            animationStartTime = System.currentTimeMillis();
            velocityY = 0;
        }
    }

    public boolean isAnimationFinished() {
        return hit && (System.currentTimeMillis() - animationStartTime > HIT_ANIMATION_TIME);
    }

    public boolean isHit() { return hit; }

    public void setColor(int newColor) {
        paint.setColor(newColor);
    }
}
