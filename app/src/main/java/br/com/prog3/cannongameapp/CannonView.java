package br.com.prog3.cannongameapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
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
    public static final double TARGET_MIN_SPEED_PERCENT = 3.0 / 8;
    public static final double TARGET_MAX_SPEED_PERCENT = 6.0 / 8;
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
    private ArrayList<Target> targetsToRemove = new ArrayList<>();
    private ArrayList<Explosion> explosions;

    private int screenWidth;
    private int screenHeight;

    private boolean gameOver;
    private double timeLeft;
    private int shotsFired;
    private double totalElapsedTime;
    private int score;
    private int level;

    private int comboMultiplier;
    private Paint hudPaint;

    private float power = 0.0f;
    private long fireTouchStartTime = 0;
    private boolean isCharging = false;
    private int firingPointerId = -1;

    public static final int TARGET_SOUND_ID = 0;
    public static final int CANNON_SOUND_ID = 1;
    public static final int BLOCKER_SOUND_ID = 2;

    private SoundPool soundPool;
    private SparseIntArray soundMap;

    private MediaPlayer backgroundPlayer;  // <<<<<< SOM DE FUNDO

    private Paint textPaint;
    private Paint backgroundPaint;

    private int colorBackground;
    private int colorText;

    private long lastFireTime = 0;
    private static final int RELOAD_DELAY = 500;
    private boolean canFire = true;

    private final HighScoreManager highScoreManager;

    public CannonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        activity = (Activity) context;
        highScoreManager = new HighScoreManager(context);
        getHolder().addCallback(this);

        // ---------- SOUNDPOOL ----------
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

        // ---------- SOM DE FUNDO ----------
        backgroundPlayer = MediaPlayer.create(context, R.raw.background_sound);
        backgroundPlayer.setLooping(true);
        backgroundPlayer.setVolume(0.15f, 0.15f);

        textPaint = new Paint();
        backgroundPaint = new Paint();

        hudPaint = new Paint();
        hudPaint.setColor(Color.WHITE);
        hudPaint.setAntiAlias(true);
        hudPaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        hudPaint.setShadowLayer(10.0f, 2.0f, 2.0f, Color.BLACK);

        loadThemeColors(context);
    }

    public static int getThemeColor(Context context, int colorResourceId) {
        return ContextCompat.getColor(context, colorResourceId);
    }

    private void loadThemeColors(Context context) {
        colorBackground = getThemeColor(context, R.color.cor_cenario_fundo_id);
        backgroundPaint.setColor(colorBackground);

        colorText = getThemeColor(context, R.color.cor_texto_id);
        textPaint.setColor(colorText);

        if (hudPaint != null) {
            hudPaint.setColor(colorText);
        }
    }

    public void playSound(int soundId) {
        if (soundPool != null)
            soundPool.play(soundMap.get(soundId), 1, 1, 1, 0, 1f);
    }

    public int getScreenWidth() { return screenWidth; }
    public int getScreenHeight() { return screenHeight; }

    public void releaseResources() {

        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }

        if (backgroundPlayer != null) {
            backgroundPlayer.release();
            backgroundPlayer = null;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!dialogIsDisplayed) {
            showMainMenu();
        }
    }

    private void showMainMenu() {
        activity.runOnUiThread(() -> {
            showSystemBars();

            final Dialog dialog = new Dialog(activity);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_main_menu);
            dialog.setCancelable(false);

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }

            Button btnPlay = dialog.findViewById(R.id.btnPlay);
            Button btnSelectTheme = dialog.findViewById(R.id.btnSelectTheme);

            btnPlay.setOnClickListener(v -> {
                dialog.dismiss();
                dialogIsDisplayed = false;
                hideSystemBars();
                newGame(1);
            });

            btnSelectTheme.setOnClickListener(v -> {
                showThemeSelectionDialog();
            });

            dialog.show();

            if (dialog.getWindow() != null) {
                int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.8);
                dialog.getWindow().setLayout(width, android.view.WindowManager.LayoutParams.WRAP_CONTENT);
            }
        });
    }

    private void showThemeSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View view = activity.getLayoutInflater().inflate(R.layout.dialog_select_theme, null);
        builder.setView(view);

        RadioGroup themeRadioGroup = view.findViewById(R.id.themeRadioGroup);
        RadioButton lightThemeRadioButton = view.findViewById(R.id.lightThemeRadioButton);
        RadioButton darkThemeRadioButton = view.findViewById(R.id.darkThemeRadioButton);

        SharedPreferences prefs = activity.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE);
        String currentTheme = prefs.getString("theme", "light");

        if (currentTheme.equals("dark")) {
            darkThemeRadioButton.setChecked(true);
        } else {
            lightThemeRadioButton.setChecked(true);
        }

        AlertDialog dialog = builder.create();

        themeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.lightThemeRadioButton) {
                saveTheme("light");
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            } else {
                saveTheme("dark");
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private void saveTheme(String theme) {
        SharedPreferences prefs = activity.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("theme", theme);
        editor.apply();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        if (cannonThread != null) {
            cannonThread.setRunning(false);
            while (retry) {
                try {
                    cannonThread.join();
                    retry = false;
                } catch (InterruptedException e) {}
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
                if (e.getX(pointerIndex) >= screenWidth / 2) {
                    if (firingPointerId == -1) {
                        firingPointerId = pointerId;
                        isCharging = true;
                        fireTouchStartTime = System.currentTimeMillis();
                        power = 0.0f;
                    }
                } else {
                    alignCannon(e.getX(pointerIndex), e.getY(pointerIndex));
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                if (pointerId == firingPointerId) {
                    fireCannon();
                    firingPointerId = -1;
                    isCharging = false;
                    power = 0.0f;
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
                    if (canvas == null) continue;
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

    public void newGame(int level) {
        this.level = level;
        loadThemeColors(getContext());

        cannon = new Cannon(this,
                (int) (CANNON_BASE_RADIUS_PERCENT * screenHeight),
                (int) (CANNON_BARREL_LENGTH_PERCENT * screenWidth),
                (int) (CANNON_BARREL_WIDTH_PERCENT * screenHeight));

        Random random = new Random();
        targets = new ArrayList<>();
        targetsToRemove = new ArrayList<>();
        explosions = new ArrayList<>();

        int targetWidth = (int) (TARGET_WIDTH_PERCENT * screenWidth);
        int targetLength = (int) (TARGET_LENGTH_PERCENT * screenHeight);
        int numTargets = 5 + (level - 1) * 2;

        for (int n = 0; n < numTargets; n++) {
            int targetX = random.nextInt(screenWidth / 2 - targetWidth) + screenWidth / 2;
            int targetY = random.nextInt(screenHeight - targetLength);

            double velocityY = screenHeight * (random.nextDouble() * (TARGET_MAX_SPEED_PERCENT - TARGET_MIN_SPEED_PERCENT) + TARGET_MIN_SPEED_PERCENT);
            double velocityX = screenHeight * (random.nextDouble() * (TARGET_MAX_SPEED_PERCENT - TARGET_MIN_SPEED_PERCENT) + TARGET_MIN_SPEED_PERCENT);
            int color = (n % 2 == 0) ? getThemeColor(getContext(), R.color.cor_elemento_padrao_id)
                    : getThemeColor(getContext(), R.color.cor_elemento_alternativo_id);
            velocityY *= -1;
            if (random.nextBoolean()) {
                velocityX *= -1;
            }

            targets.add(new Target(this, color, HIT_REWARD, targetX, targetY,
                    targetWidth, targetLength, (float) velocityY, (float) velocityX));
        }

        blocker = new Blocker(this, getThemeColor(getContext(), R.color.cor_elemento_bloco_id), MISS_PENALTY,
                (int) (BLOCKER_X_PERCENT * screenWidth),
                (int) ((0.5 - BLOCKER_LENGTH_PERCENT / 2) * screenHeight),
                (int) (BLOCKER_WIDTH_PERCENT * screenWidth),
                (int) (BLOCKER_LENGTH_PERCENT * screenHeight),
                (float) (BLOCKER_SPEED_PERCENT * screenHeight));

        timeLeft = 30;
        shotsFired = 0;
        score = 0;
        comboMultiplier = 1;
        totalElapsedTime = 0.0;
        canFire = true;
        lastFireTime = 0;
        power = 0.0f;

        if (gameOver) {
            gameOver = false;
        }
        if (cannonThread != null) {
            cannonThread.setRunning(false);
        }
        cannonThread = new CannonThread(getHolder());
        cannonThread.start();

        // <<< INICIAR SOM DE FUNDO AQUI >>>
        if (backgroundPlayer != null && !backgroundPlayer.isPlaying()) {
            backgroundPlayer.start();
        }

        hideSystemBars();
    }

    private void updatePositions(double elapsedTimeMS) {
        if (gameOver) return;
        double interval = elapsedTimeMS / 1000.0;

        if (isCharging) {
            long elapsedTime = System.currentTimeMillis() - fireTouchStartTime;
            float chargeRatio = Math.min(1.0f, elapsedTime / 2500.0f);
            power = chargeRatio;
        }

        if (cannon.getCannonball() != null) {
            cannon.getCannonball().update(interval);

            if (!cannon.getCannonball().isOnScreen()) {
                comboMultiplier = 1;
                cannon.removeCannonball();
            }
        }

        blocker.update(interval);
        for (int i = 0; i < targets.size(); i++) {
            Target target = targets.get(i);
            target.update(interval);
            if (target.isHit()) {
                targetsToRemove.add(target);
            }
        }
        targets.removeAll(targetsToRemove);
        targetsToRemove.clear();

        ArrayList<Explosion> finishedExplosions = new ArrayList<>();
        for (Explosion explosion : explosions) {
            explosion.update(interval);
            if (explosion.finished) {
                finishedExplosions.add(explosion);
            }
        }
        explosions.removeAll(finishedExplosions);

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
            for (int j = i + 1; j < targets.size(); j++) {
                Target target1 = targets.get(i);
                Target target2 = targets.get(j);
                if (target1.collidesWith(target2)) {
                    int overlapX = Math.min(target1.shape.right, target2.shape.right) - Math.max(target1.shape.left, target2.shape.left);
                    int overlapY = Math.min(target1.shape.bottom, target2.shape.bottom) - Math.max(target1.shape.top, target2.shape.top);

                    if (overlapX > 0 && overlapY > 0) {
                        if (overlapX < overlapY) {
                            float tempVx = target1.velocityX;
                            target1.velocityX = target2.velocityX;
                            target2.velocityX = tempVx;
                        } else {
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
            level++;
            newGame(level);
        }

        if (!canFire && (System.currentTimeMillis() - lastFireTime) >= RELOAD_DELAY) {
            canFire = true;
        }
    }

    public void testForCollisions() {
        if (gameOver) return;

        if (cannon.getCannonball() != null && cannon.getCannonball().isOnScreen()) {
            for (int n = 0; n < targets.size(); n++) {
                if (cannon.getCannonball().collidesWith(targets.get(n))) {
                    Target hitTarget = targets.get(n);

                    hitTarget.playSound();
                    hitTarget.onHit();

                    explosions.add(new Explosion(
                            hitTarget.shape.centerX(),
                            hitTarget.shape.centerY(),
                            hitTarget.getColor()
                    ));

                    int points = 100 * comboMultiplier;
                    score += points;
                    comboMultiplier++;

                    timeLeft += targets.get(n).getHitReward();
                    cannon.removeCannonball();

                    for (Target remainingTarget : targets) {
                        remainingTarget.increaseVelocity(1.2f);
                    }
                    break;
                }
            }
        }

        if (cannon.getCannonball() != null && cannon.getCannonball().collidesWith(blocker)) {
            blocker.playSound();
            cannon.removeCannonball();

            timeLeft -= blocker.getMissPenalty();

            score -= 50;
            if (score < 0) score = 0;
            comboMultiplier = 1;
        }
    }

    public void drawGameElements(Canvas canvas) {
        if (canvas == null) return;
        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), backgroundPaint);

        if (gameOver) return;

        canvas.drawText(getResources().getString(R.string.time_remaining_format, timeLeft), 30, 50, textPaint);
        canvas.drawText(getResources().getString(R.string.level_format, level), 30, 100, textPaint);

        hudPaint.setTextAlign(Paint.Align.RIGHT);
        hudPaint.setColor(colorText);
        hudPaint.setTextSize(screenHeight * 0.05f);
        canvas.drawText("Score: " + score, screenWidth - 50, 80, hudPaint);

        if (comboMultiplier > 1) {
            hudPaint.setTextAlign(Paint.Align.CENTER);
            hudPaint.setColor(Color.YELLOW);
            hudPaint.setTextSize(screenHeight * 0.08f);
            canvas.drawText("COMBO x" + comboMultiplier + "!", screenWidth / 2f, screenHeight * 0.2f, hudPaint);
            hudPaint.setColor(colorText);
        }

        if (cannon.getCannonball() != null && cannon.getCannonball().isOnScreen())
            cannon.getCannonball().draw(canvas);

        cannon.draw(canvas);
        blocker.draw(canvas);

        for (Target target : targets)
            target.draw(canvas);

        if (explosions != null) {
            for (Explosion explosion : explosions) {
                explosion.draw(canvas);
            }
        }

        if (isCharging) {
            Paint powerPaint = new Paint();
            powerPaint.setColor(Color.RED);
            float barWidth = screenWidth * 0.2f;
            float barHeight = 20f;
            float left = (screenWidth - barWidth) / 2;
            float top = screenHeight - 150;
            canvas.drawRect(left, top, left + (barWidth * power), top + barHeight, powerPaint);
        }
    }

    private void showGameOverDialog(final int messageId) {
        highScoreManager.addScore(score, level);
        List<HighScoreManager.HighScore> highScores = highScoreManager.getHighScores();

        StringBuilder rankingText = new StringBuilder();
        if (highScores.isEmpty()) {
            rankingText.append("Sem recordes ainda!");
        } else {
            for (int i = 0; i < highScores.size(); i++) {
                HighScoreManager.HighScore hs = highScores.get(i);
                rankingText.append(i + 1).append(". ").append(hs.getScore()).append(" pts (Level ").append(hs.getLevel()).append(")");
                if (hs.getScore() == score && hs.getLevel() == level && score > 0) {
                    rankingText.append(" (SEU!)");
                }
                rankingText.append("\n");
            }
        }

        activity.runOnUiThread(() -> {
            showSystemBars();

            final Dialog dialog = new Dialog(activity);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_game_over);
            dialog.setCancelable(false);

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }

            TextView tvTitle = dialog.findViewById(R.id.tvTitle);
            TextView tvMessage = dialog.findViewById(R.id.tvMessage);
            TextView tvHighScores = dialog.findViewById(R.id.tvHighScores);
            Button btnRestart = dialog.findViewById(R.id.btnRestart);
            Button btnReturnToMenu = dialog.findViewById(R.id.btnReturnToMenu);

            tvTitle.setText(getResources().getString(messageId));
            tvMessage.setText(getResources().getString(R.string.results_format, shotsFired, totalElapsedTime, score, level));
            tvHighScores.setText(rankingText.toString());

            btnRestart.setOnClickListener(v -> {
                dialog.dismiss();
                dialogIsDisplayed = false;
                hideSystemBars();
                newGame(1);
            });

            btnReturnToMenu.setOnClickListener(v -> {
                dialog.dismiss();
                dialogIsDisplayed = false;
                showMainMenu();
            });

            dialog.show();

            if (dialog.getWindow() != null) {
                int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.95);
                int height = (int) (getResources().getDisplayMetrics().heightPixels * 0.85);
                dialog.getWindow().setLayout(width, height);
            }
        });
    }

    public boolean checkCircleRectangleCollision(Cannonball cannonball, Blocker blocker) {
        float circleX = cannonball.shape.centerX();
        float circleY = cannonball.shape.centerY();
        float radius = cannonball.getRadius();

        float closestX = Math.max(blocker.shape.left, Math.min(circleX, blocker.shape.right));
        float closestY = Math.max(blocker.shape.top, Math.min(circleY, blocker.shape.bottom));

        float distanceX = circleX - closestX;
        float distanceY = circleY - closestY;

        float distanceSquared = (distanceX * distanceX) + (distanceY * distanceY);

        return distanceSquared < (radius * radius);
    }

    private void hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    private void showSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    public void stopGame() {
        if (cannonThread != null)
            cannonThread.setRunning(false);

        if (backgroundPlayer != null && backgroundPlayer.isPlaying()) {
            backgroundPlayer.pause();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        screenWidth = w;
        screenHeight = h;
        textPaint.setTextSize((int) (TEXT_SIZE_PERCENT * screenHeight));
        textPaint.setAntiAlias(true);
        textPaint.setColor(colorText);

        if (hudPaint != null)
            hudPaint.setTextSize(screenHeight * 0.05f);
    }

    private class Particle {
        float x, y;
        float velocityX, velocityY;
        int color;
        float alpha = 255f;
        Paint paint;
        float radius;
        private Random random = new Random();

        public Particle(float x, float y, int color) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.paint = new Paint();
            this.paint.setColor(color);
            this.velocityX = (random.nextFloat() * 2f - 1f) * screenWidth * 0.1f;
            this.velocityY = (random.nextFloat() * 2f - 1f) * screenHeight * 0.1f;
            this.radius = random.nextFloat() * 6f + 2f;
        }

        public void update(double interval) {
            x += velocityX * interval;
            y += velocityY * interval;
            alpha -= (float) (255 * interval / 1.2);
            if (alpha < 0) alpha = 0;
        }

        public void draw(Canvas canvas) {
            paint.setAlpha((int) alpha);
            canvas.drawCircle(x, y, radius, paint);
        }

        public boolean isAlive() {
            return alpha > 0;
        }
    }

    private class Explosion {
        private ArrayList<Particle> particles = new ArrayList<>();
        public boolean finished = false;

        public Explosion(float x, float y, int color) {
            for (int i = 0; i < 25; i++) {
                particles.add(new Particle(x, y, color));
            }
        }

        public void update(double interval) {
            particles.removeIf(p -> !p.isAlive());
            for (Particle p : particles) {
                p.update(interval);
            }
            if (particles.isEmpty()) {
                finished = true;
            }
        }

        public void draw(Canvas canvas) {
            for (Particle p : particles) {
                p.draw(canvas);
            }
        }
    }
}
