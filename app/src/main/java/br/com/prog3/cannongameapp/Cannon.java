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

        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(barrelWidth);

        align(Math.PI / 2); // apontado para a direita
    }

    public void align(double angle) {
        this.barrelAngle = angle;

        barrelEnd.x = (int) (barrelLength * Math.sin(angle));
        barrelEnd.y = (int) (-barrelLength * Math.cos(angle))
                + view.getScreenHeight() / 2;
    }

    public void fireCannonball() {

        int velocityX = (int) (CannonView.CANNONBALL_SPEED_PERCENT *
                view.getScreenWidth() * Math.sin(barrelAngle));

        int velocityY = (int) (CannonView.CANNONBALL_SPEED_PERCENT *
                view.getScreenWidth() * -Math.cos(barrelAngle));

        int radius = (int) (view.getScreenHeight() *
                CannonView.CANNONBALL_RADIUS_PERCENT);

        cannonball = new Cannonball(
                view,
                Color.BLACK,
                CannonView.SOUND_FIRE,
                0,
                view.getScreenHeight() / 2 - radius,
                radius,
                velocityX,
                velocityY
        );
    }

    public void draw(Canvas canvas) {
        canvas.drawLine(
                0, view.getScreenHeight() / 2,
                barrelEnd.x, barrelEnd.y,
                paint
        );

        canvas.drawCircle(
                0,
                view.getScreenHeight() / 2,
                baseRadius,
                paint
        );
    }

    public Cannonball getCannonball() { return cannonball; }
    public void removeCannonball() { cannonball = null; }
}
