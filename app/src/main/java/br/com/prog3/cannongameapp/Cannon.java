package br.com.prog3.cannongameapp;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;

public class Cannon {
    private int baseRadius; // Cannon base's radius
    private int barrelLength; // Cannon barrel's length
    private Point barrelEnd = new Point(); // endpoint of Cannon's barrel
    private double barrelAngle; // angle of the Cannon's barrel
    private Cannonball cannonball; // the Cannon's Cannonball
    private Paint paint = new Paint(); // Paint used to draw the cannon
    private CannonView view; // view containing the Cannon

    // constructor
    public Cannon(CannonView view, int baseRadius, int barrelLength,
                  int barrelWidth) {
        this.view = view;
        this.baseRadius = baseRadius;
        this.barrelLength = barrelLength;
        paint.setStrokeWidth(barrelWidth); // set width of barrel
        paint.setColor(Color.BLACK); // Cannon's color is Black
        align(Math.PI / 2); // Cannon barrel facing straight right
    }
}
