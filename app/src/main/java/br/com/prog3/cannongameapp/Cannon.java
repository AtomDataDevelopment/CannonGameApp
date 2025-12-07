package br.com.prog3.cannongameapp;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;

public class Cannon {
    private int baseRadius;
    private int barrelLength;
    private Point barrelEnd = new Point();
    private double barrelAngle;
    private Cannonball cannonball;
    private Paint paint = new Paint();
    private CannonView view;

    public Cannon(CannonView view, int baseRadius, int barrelLength, int barrelWidth) {
        this.view = view;
        this.baseRadius = baseRadius;
        this.barrelLength = barrelLength;
        paint.setStrokeWidth(barrelWidth);

        // MUDANÇA: Define a cor do Canhão usando o tema
        int cannonColor = CannonView.getThemeColor(view.getContext(), R.color.cor_canhao_principal_id);
        paint.setColor(cannonColor);

        align(Math.PI / 2); // Aponta para a direita
    }

    public void align(double barrelAngle) {
        this.barrelAngle = barrelAngle;
        barrelEnd.x = (int) (barrelLength * Math.sin(barrelAngle));
        barrelEnd.y = (int) (-barrelLength * Math.cos(barrelAngle)) + view.getScreenHeight() / 2;
    }

    public void fireCannonball() {
        int velocityX = (int) (CannonView.CANNONBALL_SPEED_PERCENT * view.getScreenWidth() * Math.sin(barrelAngle));
        int velocityY = (int) (CannonView.CANNONBALL_SPEED_PERCENT * view.getScreenWidth() * -Math.cos(barrelAngle));
        int radius = (int) (view.getScreenHeight() * CannonView.CANNONBALL_RADIUS_PERCENT);

        // MUDANÇA: Define a cor da Cannonball (Partículas) usando o tema
        int cannonballColor = CannonView.getThemeColor(view.getContext(), R.color.cor_particula_id);

        cannonball = new Cannonball(view, cannonballColor, CannonView.CANNON_SOUND_ID, 0,
                view.getScreenHeight() / 2 - radius, radius, velocityX, velocityY);
        cannonball.playSound();
    }

    public void draw(Canvas canvas) {
        canvas.drawLine(0, view.getScreenHeight() / 2, barrelEnd.x, barrelEnd.y, paint);
        canvas.drawCircle(0, (int) view.getScreenHeight() / 2, (int) baseRadius, paint);
    }

    public Cannonball getCannonball() { return cannonball; }
    public void removeCannonball() { cannonball = null; }
}
