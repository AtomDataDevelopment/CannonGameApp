package br.com.prog3.cannongameapp;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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

import java.util.ArrayList;
import java.util.Random;

public class CannonView extends SurfaceView implements SurfaceHolder.Callback {

    // ===========================
    //   CONSTANTES DO JOGO
    // ===========================

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
    public static final double TARGET_MIN_SPEED_PERCENT = 3.0 / 4;
    public static final double TARGET_MAX_SPEED_PERCENT = 6.0 / 4;

    public static final double BLOCKER_WIDTH_PERCENT = 1.0 / 40;
    public static final double BLOCKER_LENGTH_PERCENT = 1.0 / 4;
    public static final double BLOCKER_X_PERCENT = 1.0 / 2;
    public static final double BLOCKER_SPEED_PERCENT = 1.0;

    public static final double TEXT_SIZE_PERCENT = 1.0 / 18;

    // ===========================
    //       SONS NOVOS
    // ===========================

    public static final int SOUND_FIRE = 1;   // tiro.wav
    public static final int SOUND_HIT = 2;    // tei.wav
    public static final int SOUND_BLOCK = 3;  // faustao_errou.wav

    private SoundPool soundPool;
    private SparseIntArray soundMap;
    private MediaPlayer bgMusic;

    // ===========================
    //    OBJETOS DO JOGO
    // ===========================

    private Cannon cannon;
    private Blocker blocker;
    private ArrayList<Target> targets;

    private CannonThread cannonThread;
    private Activity activity;

    private boolean gameStarted = false;
    private boolean gameOver = false;

    private int screenWidth;
    private int screenHeight;

    private double timeLeft;
    private int shotsFired;
    private double totalElapsedTime;

    private long lastFireTime = 0;
    private static final int RELOAD_DELAY = 500;
    private boolean canFire = true;

    // ===========================
    //      DESENHO
    // ===========================

    private Paint textPaint;
    private Paint backgroundPaint;
    private Paint overlayPaint;
    private Paint playButtonPaint;
    private Paint playTextPaint;

    // ===========================
    //     CONSTRUTOR
    // ===========================

    public CannonView(Context context, AttributeSet attrs) {
        super(context, attrs);

        activity = (Activity) context;
        getHolder().addCallback(this);

        setupSound(context);
        setupPaints();
    }

    // ===========================
    //   CONFIGURAÇÃO DE SOM
    // ===========================

    private void setupSound(Context context) {

        AudioAttributes attr = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(attr)
                .build();

        soundMap = new SparseIntArray();

        soundMap.put(SOUND_FIRE, soundPool.load(context, R.raw.tiro, 1));
        soundMap.put(SOUND_HIT, soundPool.load(context, R.raw.tei, 1));
        soundMap.put(SOUND_BLOCK, soundPool.load(context, R.raw.faustao_errou, 1));

        bgMusic = MediaPlayer.create(context, R.raw.background_music);
        bgMusic.setLooping(true);
        bgMusic.setVolume(0.10f, 0.10f);
    }

    // ===========================
    //   CONFIGURAÇÃO GRÁFICA
    // ===========================

    private void setupPaints() {

        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setAntiAlias(true);

        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.WHITE);

        overlayPaint = new Paint();
        overlayPaint.setColor(Color.argb(180, 0, 0, 0));

        playButtonPaint = new Paint();
        playButtonPaint.setColor(Color.GREEN);

        playTextPaint = new Paint();
        playTextPaint.setColor(Color.WHITE);
        playTextPaint.setTextSize(80);
        playTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    // ===========================
    //      GETTERS NOVOS
    // ===========================

    public int getScreenWidth() { return screenWidth; }
    public int getScreenHeight() { return screenHeight; }

    // ===========================
    //   CONTROLE DO JOGO
    // ===========================

    public void playSound(int id) {
        if (soundPool != null) {
            soundPool.play(soundMap.get(id), 1, 1, 1, 0, 1f);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (cannonThread == null || !cannonThread.isAlive()) {
            cannonThread = new CannonThread(holder);
            cannonThread.setRunning(true);
            cannonThread.start();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        screenWidth = width;
        screenHeight = height;

        textPaint.setTextSize((int) (TEXT_SIZE_PERCENT * screenHeight));
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        if (cannonThread != null) {
            cannonThread.setRunning(false);
        }

        while (retry) {
            try {
                if (cannonThread != null) {
                    cannonThread.join();
                }
                retry = false;
            } catch (InterruptedException e) {
                // Tenta de novo para garantir que a thread pare
            }
        }
        cannonThread = null;
    }

    public void releaseResources() {
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
        if (bgMusic != null) {
            bgMusic.release();
            bgMusic = null;
        }
    }

    public void stopGame() {
        if (cannonThread != null) {
            cannonThread.setRunning(false);
        }
        if (bgMusic != null && bgMusic.isPlaying()) {
            bgMusic.pause();
        }
    }

    public void resume() {
        if (!gameOver && bgMusic != null && !bgMusic.isPlaying()) {
            bgMusic.start();
        }
        // Se o jogo estava pausado, cria e inicia uma nova thread
        if (cannonThread == null || !cannonThread.isAlive()) {
            cannonThread = new CannonThread(getHolder());
            cannonThread.setRunning(true);
            cannonThread.start();
        }
    }

    // ===========================
    //    TOQUE NA TELA
    // ===========================

    @Override
    public boolean onTouchEvent(MotionEvent e) {

        if (!gameStarted) {
            return handlePlayButton(e);
        }

        if (gameOver) {
            newGame();
            if (bgMusic != null) bgMusic.start();
            gameOver = false;
            return true;
        }

        handleGameTouch(e);
        return true;
    }

    private boolean handlePlayButton(MotionEvent e) {

        float x = e.getX();
        float y = e.getY();

        float left = screenWidth / 2f - 200;
        float top = screenHeight / 2f - 100;
        float right = screenWidth / 2f + 200;
        float bottom = screenHeight / 2f + 100;

        if (x > left && x < right && y > top && y < bottom) {
            gameStarted = true;
            newGame();
            if (bgMusic != null) bgMusic.start();
        }
        return true;
    }

    private void handleGameTouch(MotionEvent e) {

        int action = e.getActionMasked();
        int index = e.getActionIndex();

        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {

            if (e.getX(index) >= screenWidth / 2)
                fireCannon();
            else
                alignCannon(e.getX(index), e.getY(index));
        }

        if (action == MotionEvent.ACTION_MOVE) {

            for (int i = 0; i < e.getPointerCount(); i++) {

                if (e.getX(i) < screenWidth / 2)
                    alignCannon(e.getX(i), e.getY(i));
            }
        }
    }

    // ===========================
    //    CONTROLE DO CANHÃO
    // ===========================

    private void alignCannon(float x, float y) {

        double diffY = (screenHeight / 2 - y);
        double angle = (diffY != 0) ? Math.atan2(x, diffY) : 0;

        cannon.align(angle);
    }

    private void fireCannon() {

        if (canFire && (cannon.getCannonball() == null ||
                !cannon.getCannonball().isOnScreen())) {

            cannon.fireCannonball();

            playSound(SOUND_FIRE);

            shotsFired++;
            canFire = false;
            lastFireTime = System.currentTimeMillis();
        }
    }

    // ===========================
    //   THREAD PRINCIPAL
    // ===========================

    private class CannonThread extends Thread {

        private final SurfaceHolder holder;
        private boolean running = true;

        public CannonThread(SurfaceHolder h) {
            holder = h;
setName("CannonThread");
        }

        public void setRunning(boolean r) { running = r; }

        @Override
        public void run() {

            long previousTime = System.currentTimeMillis();

            while (running) {

                Canvas canvas = null;

                try {
                    canvas = holder.lockCanvas();

                    if (canvas != null) {

                        long now = System.currentTimeMillis();
                        double elapsed = now - previousTime;
                        totalElapsedTime += elapsed / 1000.0;

                        if (gameStarted && !gameOver) {
                            updatePositions(elapsed);
                            testForCollisions();
                        }

                        drawGameElements(canvas);
                        previousTime = now;
                    }

                } finally {

                    if (canvas != null)
                        holder.unlockCanvasAndPost(canvas);
                }
            }
        }
    }

    // ===========================
    //   LÓGICA DO JOGO
    // ===========================

    public void newGame() {

        gameOver = false;

        cannon = new Cannon(
                this,
                (int) (CANNON_BASE_RADIUS_PERCENT * screenHeight),
                (int) (CANNON_BARREL_LENGTH_PERCENT * screenWidth),
                (int) (CANNON_BARREL_WIDTH_PERCENT * screenHeight)
        );

        Random random = new Random();
        targets = new ArrayList<>();

        int targetX = (int) (TARGET_FIRST_X_PERCENT * screenWidth);
        int targetY = (int) ((0.5 - TARGET_LENGTH_PERCENT / 2) * screenHeight);

        for (int i = 0; i < TARGET_PIECES; i++) {

            double speed = screenHeight *
                    (random.nextDouble() *
                            (TARGET_MAX_SPEED_PERCENT - TARGET_MIN_SPEED_PERCENT)
                            + TARGET_MIN_SPEED_PERCENT);

            speed *= -1;

            int color = (i % 2 == 0)
                    ? Color.rgb(50, 50, 50)
                    : Color.rgb(200, 200, 200);

            targets.add(new Target(
                    this,
                    color,
                    HIT_REWARD,
                    targetX,
                    targetY,
                    (int) (TARGET_WIDTH_PERCENT * screenWidth),
                    (int) (TARGET_LENGTH_PERCENT * screenHeight),
                    (int) speed
            ));

            targetX += (TARGET_WIDTH_PERCENT + TARGET_SPACING_PERCENT) * screenWidth;
        }

        blocker = new Blocker(
                this,
                Color.BLACK,
                MISS_PENALTY,
                (int) (BLOCKER_X_PERCENT * screenWidth),
                (int) ((0.5 - BLOCKER_LENGTH_PERCENT / 2) * screenHeight),
                (int) (BLOCKER_WIDTH_PERCENT * screenWidth),
                (int) (BLOCKER_LENGTH_PERCENT * screenHeight),
                (float) (BLOCKER_SPEED_PERCENT * screenHeight)
        );

        timeLeft = 10;
        shotsFired = 0;
        totalElapsedTime = 0.0;
        canFire = true;
    }

    private void updatePositions(double elapsedMS) {

        double interval = elapsedMS / 1000.0;

        if (cannon.getCannonball() != null)
            cannon.getCannonball().update(interval);

        blocker.update(interval);

        for (Target t : targets)
            t.update(interval);

        timeLeft -= interval;

        if (timeLeft <= 0) {
            timeLeft = 0;
            gameOver = true;
            if(bgMusic != null && bgMusic.isPlaying())
                bgMusic.pause();
        }

        if (targets.isEmpty()) {
            gameOver = true;
            if(bgMusic != null && bgMusic.isPlaying())
                bgMusic.pause();
        }

        if (!canFire &&
                (System.currentTimeMillis() - lastFireTime) >= RELOAD_DELAY) {
            canFire = true;
        }
    }

    public void testForCollisions() {

        if (cannon.getCannonball() != null &&
                cannon.getCannonball().isOnScreen()) {

            for (int i = 0; i < targets.size(); i++) {

                if (cannon.getCannonball().collidesWith(targets.get(i))) {
                    playSound(SOUND_HIT);

                    timeLeft += targets.get(i).getHitReward();
                    cannon.removeCannonball();
                    targets.remove(i);

                    break;
                }
            }
        }

        if (cannon.getCannonball() != null &&
                cannon.getCannonball().collidesWith(blocker)) {

            playSound(SOUND_BLOCK);

            cannon.getCannonball().reverseVelocityX();
            timeLeft -= blocker.getMissPenalty();
        }
    }

    public void drawGameElements(Canvas canvas) {

        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), backgroundPaint);

        if (gameStarted) {
            canvas.drawText(
                    "Tempo: " + String.format("%.2f", timeLeft),
                    30, 60, textPaint
            );
        }

        if (gameStarted && !gameOver) {

            cannon.draw(canvas);

            if (cannon.getCannonball() != null &&
                    cannon.getCannonball().isOnScreen())
                cannon.getCannonball().draw(canvas);

            blocker.draw(canvas);

            for (Target t : targets)
                t.draw(canvas);

        } else if (!gameStarted) {

            canvas.drawRect(
                    screenWidth / 2f - 200,
                    screenHeight / 2f - 100,
                    screenWidth / 2f + 200,
                    screenHeight / 2f + 100,
                    playButtonPaint
            );

            canvas.drawText(
                    "PLAY",
                    screenWidth / 2f,
                    screenHeight / 2f + 25,
                    playTextPaint
            );

        } else if (gameOver) {

            canvas.drawRect(0, 0, screenWidth, screenHeight, overlayPaint);

            playTextPaint.setTextSize(80);
            canvas.drawText("Fim de Jogo!", screenWidth / 2f, screenHeight / 2f - 150, playTextPaint);

            canvas.drawText(
                    "Toque para Reiniciar",
                    screenWidth / 2f,
                    screenHeight / 2f,
                    playTextPaint
            );
        }
    }
}
