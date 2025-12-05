package br.com.prog3.cannongameapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import java.util.ArrayList;
import java.util.Random;

public class CannonView extends SurfaceView implements SurfaceHolder.Callback {
    public static final int MISS_PENALTY = 2;
    public static final int HIT_REWARD = 3;
    public static final double CANNON_BASE_RADIUS_PERCENT = 3.0 / 40;
    public static final double CANNON_BARREL_WIDTH_PERCENT = 3.0 / 40;
    public static final double CANNON_BARREL_LENGTH_PERCENT = 1.0 / 10;
    public static final double CANNONBALL_RADIUS_PERCENT = 3.0 / 80;
    public static final double CANNONBALL_SPEED_PERCENT = 3.0 / 2;
    public static final double TARGET_WIDTH_PERCENT = 1.0 / 40;
    public static final double TARGET_LENGTH_PERCENT = 3.0 / 20;
    public static final double TARGET_FIRST_X_PERCENT = 3.0 / 5;
    public static final double TARGET_SPACING_PERCENT = 1.0 / 60;
    public static final double TARGET_PIECES = 9;
    public static final double TARGET_MIN_SPEED_PERCENT = 3.0 / 8; // Reduced speed
    public static final double TARGET_MAX_SPEED_PERCENT = 6.0 / 8; // Reduced speed
    public static final double BLOCKER_WIDTH_PERCENT = 1.0 / 40;
    public static final double BLOCKER_LENGTH_PERCENT = 1.0 / 4;
    public static final double BLOCKER_X_PERCENT = 1.0 / 2;
    public static final double BLOCKER_SPEED_PERCENT = 1.0;
    public static final double TEXT_SIZE_PERCENT = 1.0 / 18;

    private CannonThread cannonThread;
    private Activity activity;
    private boolean dialogIsDisplayed = false;

    private Cannon cannon;
    private Blocker blocker;
    private ArrayList<Target> targets;

    private int screenWidth;
    private int screenHeight;

    private boolean gameOver;
    private double timeLeft;
    private int shotsFired;
    private double totalElapsedTime;

    private float power = 0.0f;
    private long fireTouchStartTime = 0;
    private boolean isCharging = false;

    public static final int TARGET_SOUND_ID = 0;
    public static final int CANNON_SOUND_ID = 1;
    public static final int BLOCKER_SOUND_ID = 2;
    private SoundPool soundPool;
    private SparseIntArray soundMap;

    private Paint textPaint;
    private Paint backgroundPaint;

    private long lastFireTime = 0;
    private static final int RELOAD_DELAY = 500;
    private boolean canFire = true;

    public CannonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        activity = (Activity) context;
        getHolder().addCallback(this);

        AudioAttributes.Builder attrBuilder = new AudioAttributes.Builder();
        attrBuilder.setUsage(AudioAttributes.USAGE_GAME);

        SoundPool.Builder builder = new SoundPool.Builder();
        builder.setMaxStreams(1);
        builder.setAudioAttributes(attrBuilder.build());
        soundPool = builder.build();

        soundMap = new SparseIntArray(3);
        soundMap.put(TARGET_SOUND_ID, soundPool.load(context, R.raw.target_hit, 1));
        soundMap.put(CANNON_SOUND_ID, soundPool.load(context, R.raw.cannon_fire, 1));
        soundMap.put(BLOCKER_SOUND_ID, soundPool.load(context, R.raw.blocker_hit, 1));

        textPaint = new Paint();
        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.WHITE);
    }

    public void playSound(int soundId) {
        if(soundPool != null)
            soundPool.play(soundMap.get(soundId), 1, 1, 1, 0, 1f);
    }

    public int getScreenWidth() { return screenWidth; }
    public int getScreenHeight() { return screenHeight; }

    public void releaseResources() {
        if(soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!dialogIsDisplayed) {
            newGame();
            cannonThread = new CannonThread(holder);
            cannonThread.setRunning(true);
            cannonThread.start();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) { }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        cannonThread.setRunning(false);
        while (retry) {
            try {
                cannonThread.join();
                retry = false;
            } catch (InterruptedException e) {
                // log exception
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int pointerIndex = e.getActionIndex();
        int pointerId = e.getPointerId(pointerIndex);
        int maskedAction = e.getActionMasked();

        switch (maskedAction) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                // Verifica se o toque foi na área de mira ou de tiro
                if (e.getX(pointerIndex) >= screenWidth / 2) {
                    isCharging = true;
                    fireTouchStartTime = System.currentTimeMillis();
                    power = 0.0f;
                } else {
                    alignCannon(e.getX(pointerIndex), e.getY(pointerIndex));
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                if (isCharging) {
                    fireCannon();
                    isCharging = false;
                    fireTouchStartTime = 0;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < e.getPointerCount(); i++) {
                    if (e.getX(i) < screenWidth / 2) {
                        alignCannon(e.getX(i), e.getY(i));
                    }
                }
                break;
        }
        return true;
    }

    private void alignCannon(float touchX, float touchY) {
        Point touchPoint = new Point((int) touchX, (int) touchY);
        double centerMinusY = (screenHeight / 2 - touchPoint.y);
        double angle = 0;
        if (centerMinusY != 0)
            angle = Math.atan2(touchPoint.x, centerMinusY);
        cannon.align(angle);
    }

    private void fireCannon() {
        if (canFire && (cannon.getCannonball() == null || !cannon.getCannonball().isOnScreen())) {
            float finalPower = (power > 0.1f) ? power : 0.2f;
            cannon.fireCannonball(finalPower);
            playSound(CANNON_SOUND_ID);
            ++shotsFired;
            canFire = false;
            lastFireTime = System.currentTimeMillis();
            power = 0.0f;
        }
    }

    private class CannonThread extends Thread {
        private SurfaceHolder surfaceHolder;
        private boolean threadIsRunning = true;

        public CannonThread(SurfaceHolder holder) {
            surfaceHolder = holder;
            setName("CannonThread");
        }

        public void setRunning(boolean running) {
            threadIsRunning = running;
        }

        @Override
        public void run() {
            Canvas canvas = null;
            long previousFrameTime = System.currentTimeMillis();

            while (threadIsRunning) {
                try {
                    canvas = surfaceHolder.lockCanvas(null);
                    synchronized (surfaceHolder) {
                        long currentTime = System.currentTimeMillis();
                        double elapsedTimeMS = currentTime - previousFrameTime;
                        totalElapsedTime += elapsedTimeMS / 1000.0;
                        updatePositions(elapsedTimeMS);
                        testForCollisions();
                        drawGameElements(canvas);
                        previousFrameTime = currentTime;
                    }
                } finally {
                    if (canvas != null)
                        surfaceHolder.unlockCanvasAndPost(canvas);
                }
            }
        }
    }

    public void newGame() {
        cannon = new Cannon(this,
                (int) (CANNON_BASE_RADIUS_PERCENT * screenHeight),
                (int) (CANNON_BARREL_LENGTH_PERCENT * screenWidth),
                (int) (CANNON_BARREL_WIDTH_PERCENT * screenHeight));

        Random random = new Random();
        targets = new ArrayList<>();

        int targetWidth = (int) (TARGET_WIDTH_PERCENT * screenWidth);
        int targetLength = (int) (TARGET_LENGTH_PERCENT * screenHeight);

        for (int n = 0; n < TARGET_PIECES; n++) {
            int targetX = random.nextInt(screenWidth / 2 - targetWidth) + screenWidth / 2;
            int targetY = random.nextInt(screenHeight - targetLength);

            double velocityY = screenHeight * (random.nextDouble() * (TARGET_MAX_SPEED_PERCENT - TARGET_MIN_SPEED_PERCENT) + TARGET_MIN_SPEED_PERCENT);
            double velocityX = screenHeight * (random.nextDouble() * (TARGET_MAX_SPEED_PERCENT - TARGET_MIN_SPEED_PERCENT) + TARGET_MIN_SPEED_PERCENT);
            int color = (n % 2 == 0) ? getResources().getColor(R.color.dark, getContext().getTheme())
                    : getResources().getColor(R.color.light, getContext().getTheme());
            velocityY *= -1;
            if (random.nextBoolean()) {
                velocityX *= -1;
            }

            targets.add(new Target(this, color, HIT_REWARD, targetX, targetY,
                    targetWidth, targetLength, (float) velocityY, (float) velocityX));
        }

        blocker = new Blocker(this, Color.BLACK, MISS_PENALTY,
                (int) (BLOCKER_X_PERCENT * screenWidth),
                (int) ((0.5 - BLOCKER_LENGTH_PERCENT / 2) * screenHeight),
                (int) (BLOCKER_WIDTH_PERCENT * screenWidth),
                (int) (BLOCKER_LENGTH_PERCENT * screenHeight),
                (float) (BLOCKER_SPEED_PERCENT * screenHeight));

        timeLeft = 60;
        shotsFired = 0;
        totalElapsedTime = 0.0;
        canFire = true;
        lastFireTime = 0;
        power = 0.0f; // Reseta a potência no novo jogo

        if (gameOver) {
            gameOver = false;
            cannonThread = new CannonThread(getHolder());
            cannonThread.start();
        }

        hideSystemBars();
    }

    private void updatePositions(double elapsedTimeMS) {
        double interval = elapsedTimeMS / 1000.0;

        // Lógica para carregar a potência do tiro
        if (isCharging) {
            long elapsedTime = System.currentTimeMillis() - fireTouchStartTime;
            float chargeRatio = Math.min(1.0f, elapsedTime / 2500.0f); // Leva 2.5s para carregar
            power = chargeRatio; // Potência de 0% a 100%
        }

        if (cannon.getCannonball() != null)
            cannon.getCannonball().update(interval);
        blocker.update(interval);
        for (Target target : targets)
            target.update(interval);

        int midScreenX = (int) (screenWidth * 0.6);
        int ceilingY = (int) (screenHeight * 0.05);

        for (Target target : targets) {
            if (target.collidesWith(blocker)) {
                target.velocityX *= -1;
            }

            if (target.shape.left < midScreenX) {
                target.shape.offsetTo(midScreenX, target.shape.top);
                target.velocityX *= -1;
            }

            if (target.shape.top < ceilingY) {
                target.shape.offsetTo(target.shape.left, ceilingY);
                target.velocityY *= -1;
            }
        }

        for (int i = 0; i < targets.size(); i++) {
            Target target1 = targets.get(i);
            for (int j = i + 1; j < targets.size(); j++) {
                Target target2 = targets.get(j);
                if (target1.collidesWith(target2)) {
                    int overlapX = Math.min(target1.shape.right, target2.shape.right) - Math.max(target1.shape.left, target2.shape.left);
                    int overlapY = Math.min(target1.shape.bottom, target2.shape.bottom) - Math.max(target1.shape.top, target2.shape.top);

                    if (overlapX > 0 && overlapY > 0) {
                        if (overlapX < overlapY) {
                            if (target1.shape.centerX() < target2.shape.centerX()) {
                                target1.shape.offset(-overlapX / 2, 0);
                                target2.shape.offset(overlapX / 2, 0);
                            } else {
                                target1.shape.offset(overlapX / 2, 0);
                                target2.shape.offset(-overlapX / 2, 0);
                            }
                            float tempVx = target1.velocityX;
                            target1.velocityX = target2.velocityX;
                            target2.velocityX = tempVx;
                        } else {
                            if (target1.shape.centerY() < target2.shape.centerY()) {
                                target1.shape.offset(0, -overlapY / 2);
                                target2.shape.offset(0, overlapY / 2);
                            } else {
                                target1.shape.offset(0, overlapY / 2);
                                target2.shape.offset(0, -overlapY / 2);
                            }
                            float tempVy = target1.velocityY;
                            target1.velocityY = target2.velocityY;
                            target2.velocityY = tempVy;
                        }
                    }
                }
            }
        }

        timeLeft -= interval;
        if (timeLeft <= 0) {
            timeLeft = 0.0;
            gameOver = true;
            cannonThread.setRunning(false);
            showGameOverDialog(R.string.lose);
        }

        if (targets.isEmpty()) {
            cannonThread.setRunning(false);
            showGameOverDialog(R.string.win);
            gameOver = true;
        }

        if (!canFire && (System.currentTimeMillis() - lastFireTime) >= RELOAD_DELAY) {
            canFire = true;
        }
    }

    private void showGameOverDialog(final int messageId) {
        activity.runOnUiThread(new Runnable() {
            public void run() {
                showSystemBars();
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle(getResources().getString(messageId));
                builder.setMessage(getResources().getString(
                        R.string.results_format, shotsFired, totalElapsedTime));
                builder.setPositiveButton(R.string.reset_game,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialogIsDisplayed = false;
                                newGame();
                            }
                        }
                );
                builder.setCancelable(false);
                builder.show();
            }
        });
    }

    public void drawGameElements(Canvas canvas) {
        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), backgroundPaint);
        canvas.drawText(getResources().getString(R.string.time_remaining_format, timeLeft),
                30, 50, textPaint);

        // Exibe a potência do tiro na tela
        String powerString = (isCharging) ? String.format("Power: %.0f%%", power * 100) : "Power: 0%";
        canvas.drawText(powerString, 30, 100, textPaint);

        cannon.draw(canvas);
        if (cannon.getCannonball() != null && cannon.getCannonball().isOnScreen())
            cannon.getCannonball().draw(canvas);
        blocker.draw(canvas);
        for (Target target : targets)
            target.draw(canvas);
    }

    public void testForCollisions() {
        if (cannon.getCannonball() != null && cannon.getCannonball().isOnScreen()) {
            for (int n = 0; n < targets.size(); n++) {
                if (cannon.getCannonball().collidesWith(targets.get(n))) {
                    targets.get(n).playSound();
                    timeLeft += targets.get(n).getHitReward();
                    cannon.removeCannonball();
                    targets.remove(n);
                    
                    for (Target remainingTarget : targets) {
                        remainingTarget.increaseVelocity(1.3f);
                    }

                    --n;
                    break;
                }
            }
        } else { return; }

        if (cannon.getCannonball() != null && cannon.getCannonball().collidesWith(blocker)) {
            blocker.playSound();
            cannon.getCannonball().reverseVelocityX();
            cannon.getCannonball().registerBounce();
            timeLeft -= blocker.getMissPenalty();
        }
    }

    public void stopGame() {
        if (cannonThread != null)
            cannonThread.setRunning(false);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        screenWidth = w;
        screenHeight = h;
        textPaint.setTextSize((int) (TEXT_SIZE_PERCENT * screenHeight));
        textPaint.setAntiAlias(true);
    }

    private void hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_IMMERSIVE);
        }
    }

    private void showSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }
}