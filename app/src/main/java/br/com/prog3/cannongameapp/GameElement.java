package br.com.prog3.cannongameapp;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

public class GameElement {
    protected CannonView view;
    protected Paint paint = new Paint();
    public Rect shape;
    public float velocityY;
    public float velocityX;
    private int soundId;

    public GameElement(CannonView view, int color, int soundId, int x, int y, 
                       int width, int length, float velocityY, float velocityX) {
        this.view = view;
        paint.setColor(color);
        shape = new Rect(x, y, x + width, y + length);
        this.soundId = soundId;
        this.velocityY = velocityY;
        this.velocityX = velocityX;
    }

    public void update(double interval) {
        shape.offset((int) (velocityX * interval), (int) (velocityY * interval));

        if ((shape.top < 0 && velocityY < 0) || (shape.bottom > view.getScreenHeight() && velocityY > 0)) {
            velocityY *= -1;
        }

        if ((shape.left < 0 && velocityX < 0) || (shape.right > view.getScreenWidth() && velocityX > 0)) {
            velocityX *= -1;
        }
    }

    public void draw(Canvas canvas) {
        canvas.drawRect(shape, paint);
    }

    public boolean collidesWith(GameElement element) {
        return Rect.intersects(shape, element.shape);
    }

    public void playSound() {
        view.playSound(soundId);
    }
}
