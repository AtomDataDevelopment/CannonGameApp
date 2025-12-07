package br.com.prog3.cannongameapp;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

public class GameElement {
    protected CannonView view; // A view que contém este elemento
    protected Paint paint = new Paint(); // Objeto para desenhar
    protected Rect shape; // Limites retangulares do elemento
    private float velocityY; // Velocidade vertical
    private int soundId; // ID do som associado

    public GameElement(CannonView view, int color, int soundId, int x,
                       int y, int width, int length, float velocityY) {
        this.view = view;
        paint.setColor(color);
        shape = new Rect(x, y, x + width, y + length); // Define limites
        this.soundId = soundId;
        this.velocityY = velocityY;
    }

    public void update(double interval) {
        // Atualiza posição vertical
        shape.offset(0, (int) (velocityY * interval));

        // Colisão com paredes (topo e fundo)
        if (shape.top < 0 && velocityY < 0 ||
                shape.bottom > view.getScreenHeight() && velocityY > 0) {
            velocityY *= -1; // Inverte velocidade
        }
    }

    public void draw(Canvas canvas) {
        canvas.drawRect(shape, paint);
    }

    public void playSound() {
        view.playSound(soundId);
    }

    // NOVO MÉTODO: Permite que a cor do elemento seja atualizada
    public void setColor(int newColor) {
        paint.setColor(newColor);
    }
}
