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

    public GameElement(CannonView view, int color, int soundId, int x,
                       int y, int width, int length, float velocityY, float velocityX) {
        this.view = view;
        paint.setColor(color);
        shape = new Rect(x, y, x + width, y + length); // Define limites
        this.soundId = soundId;
        this.velocityY = velocityY;
        this.velocityX = velocityX;
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
    }

    public void draw(Canvas canvas) {
        canvas.drawRect(shape, paint);
    }

    public void playSound() {
        view.playSound(soundId);
    }

    public boolean collidesWith(GameElement other) {
        return Rect.intersects(shape, other.shape);
    }
}