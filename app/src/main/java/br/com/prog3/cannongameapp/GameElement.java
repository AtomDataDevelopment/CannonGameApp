package br.com.prog3.cannongameapp;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

public class GameElement {
    protected CannonView view;
    protected Paint paint = new Paint();
    protected Rect shape;
    private float velocityY;
    private int soundId;

    private static final long APPEAR_ANIMATION_TIME = 500L;
    private static final long HIT_ANIMATION_TIME = 300L;
    private long animationStartTime;
    private boolean hit = false;
    private float scale = 0.0f;
    private int alpha = 0;


    public GameElement(CannonView view, int color, int soundId, int x,
                       int y, int width, int length, float velocityY) {
        this.view = view;
        paint.setColor(color);
        shape = new Rect(x, y, x + width, y + length);
        this.soundId = soundId;
        this.velocityY = velocityY;
        this.animationStartTime = System.currentTimeMillis();
    }

    public void update(double interval) {
        long elapsed = System.currentTimeMillis() - animationStartTime;

        if (hit) {
            if (elapsed < HIT_ANIMATION_TIME) {
                float fraction = (float) elapsed / HIT_ANIMATION_TIME;
                scale = 1.0f + fraction * 0.5f;
                alpha = (int) ((1.0f - fraction) * 255);
            } else {
                alpha = 0;
            }
        } else {
            if (elapsed < APPEAR_ANIMATION_TIME) {
                float fraction = (float) elapsed / APPEAR_ANIMATION_TIME;
                scale = fraction;
                alpha = (int) (fraction * 255);
            } else {
                scale = 1.0f;
                alpha = 255;
            }

            shape.offset(0, (int) (velocityY * interval));

            if (shape.top < 0 && velocityY < 0 ||
                    shape.bottom > view.getScreenHeight() && velocityY > 0) {
                velocityY *= -1;
            }
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
