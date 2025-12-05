package br.com.prog3.cannongameapp;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

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

    private CannonThread cannonThread;
    private final Activity activity;
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
    private int score;

    // NOVAS VARIÁVEIS
    private int comboMultiplier = 1; 
    private final ArrayList<FloatingText> floatingTexts = new ArrayList<>(); 
    private final ArrayList<Particle> particles = new ArrayList<>();
    private final Paint hudPaint; 
    private final Paint comboPaint;
    
    private final HighScoredManager highScoreManager;

    public static final int TARGET_SOUND_ID = 0;
    public static final int CANNON_SOUND_ID = 1;
    public static final int BLOCKER_SOUND_ID = 2;
    private SoundPool soundPool;
    private final SparseIntArray soundMap;

    private final Paint textPaint;
    private final Paint backgroundPaint;
    private final Paint timerBarPaint;
    private final Paint timerBarBackgroundPaint;

    public CannonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        activity = (Activity) context;
        highScoreManager = new HighScoredManager(context);
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

        timerBarPaint = new Paint();
        timerBarPaint.setColor(Color.GREEN);
        timerBarBackgroundPaint = new Paint();
        timerBarBackgroundPaint.setColor(Color.LTGRAY);

        // Configuração do fundo do HUD (Barra superior)
        hudPaint = new Paint();
        hudPaint.setColor(Color.parseColor("#CC222222")); // Cinza escuro semi-transparente
        hudPaint.setStyle(Paint.Style.FILL);

        // Configuração do texto de Combo (Amarelo e grande)
        comboPaint = new Paint();
        comboPaint.setColor(Color.parseColor("#FFD700")); // Dourado
        comboPaint.setTextAlign(Paint.Align.CENTER);
        comboPaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        comboPaint.setShadowLayer(5, 2, 2, Color.BLACK); // Sombra para destaque
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
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        if (!dialogIsDisplayed) {
            newGame();
            cannonThread = new CannonThread(holder);
            cannonThread.setRunning(true);
            cannonThread.start();
        }
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) { }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
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
    public boolean performClick() {
        super.performClick();
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int action = e.getAction();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
            alignAndFireCannonball(e);
        }
        if (action == MotionEvent.ACTION_UP) {
             performClick();
        }
        return true;
    }

    private class CannonThread extends Thread {
        private final SurfaceHolder surfaceHolder;
        private volatile boolean threadIsRunning = true;

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

        int targetX = (int) (TARGET_FIRST_X_PERCENT * screenWidth);
        int targetY = (int) ((0.5 - TARGET_LENGTH_PERCENT / 2) * screenHeight);

        for (int n = 0; n < TARGET_PIECES; n++) {
            double velocity = screenHeight * (random.nextDouble() * (TARGET_MAX_SPEED_PERCENT - TARGET_MIN_SPEED_PERCENT) + TARGET_MIN_SPEED_PERCENT);
            int color = (n % 2 == 0) ? getResources().getColor(R.color.dark, getContext().getTheme())
                    : getResources().getColor(R.color.light, getContext().getTheme());
            velocity *= -1;

            targets.add(new Target(this, color, HIT_REWARD, targetX, targetY,
                    (int) (TARGET_WIDTH_PERCENT * screenWidth),
                    (int) (TARGET_LENGTH_PERCENT * screenHeight),
                    (int) velocity));

            targetX += (int) ((TARGET_WIDTH_PERCENT + TARGET_SPACING_PERCENT) * screenWidth);
        }

        blocker = new Blocker(this, Color.BLACK, MISS_PENALTY,
                (int) (BLOCKER_X_PERCENT * screenWidth),
                (int) ((0.5 - BLOCKER_LENGTH_PERCENT / 2) * screenHeight),
                (int) (BLOCKER_WIDTH_PERCENT * screenWidth),
                (int) (BLOCKER_LENGTH_PERCENT * screenHeight),
                (float) (BLOCKER_SPEED_PERCENT * screenHeight));

        timeLeft = 10;
        shotsFired = 0;
        score = 0;
        totalElapsedTime = 0.0;

        comboMultiplier = 1;
        floatingTexts.clear();
        particles.clear();

        if (gameOver) {
            gameOver = false;
            cannonThread = new CannonThread(getHolder());
            cannonThread.start();
        }

        hideSystemBars();
    }

    private void createExplosion(float x, float y, int color) {
        int numberOfParticles = 20; 
        for (int i = 0; i < numberOfParticles; i++) {
            particles.add(new Particle(x, y, color));
        }
    }

    private void updatePositions(double elapsedTimeMS) {
        double interval = elapsedTimeMS; 
        double intervalSeconds = interval / 1000.0;

        if (cannon.getCannonball() != null)
            cannon.getCannonball().update(intervalSeconds);
        
        blocker.update(intervalSeconds);
        
        for (Target target : targets)
            target.update(intervalSeconds);

        for (int i = 0; i < floatingTexts.size(); i++) {
            boolean isAlive = floatingTexts.get(i).update(interval);
            if (!isAlive) {
                floatingTexts.remove(i);
                i--;
            }
        }

        for (int i = 0; i < particles.size(); i++) {
            boolean isAlive = particles.get(i).update(interval); 
            if (!isAlive) {
                particles.remove(i);
                i--;
            }
        }

        timeLeft -= intervalSeconds;
        if (timeLeft <= 0) {
            timeLeft = 0.0;
            gameOver = true;
            cannonThread.setRunning(false);
            showGameOverDialog(R.string.lose);
        }

        if (targets.isEmpty()) {
            cannonThread.setRunning(false);
            score += (int) (timeLeft * 100);
            showGameOverDialog(R.string.win);
            gameOver = true;
        }
    }

    public void alignAndFireCannonball(MotionEvent event) {
        Point touchPoint = new Point((int) event.getX(), (int) event.getY());
        double centerMinusY = (screenHeight / 2.0 - touchPoint.y);
        double angle = 0;
        if (centerMinusY != 0)
            angle = Math.atan2(touchPoint.x, centerMinusY);

        cannon.align(angle);

        if (cannon.getCannonball() == null || !cannon.getCannonball().isOnScreen()) {
            cannon.fireCannonball();
            ++shotsFired;
        }
        
        if (cannon.getCannonball() != null && !cannon.getCannonball().isOnScreen()) {
             comboMultiplier = 1;
             cannon.removeCannonball(); 
        }
    }

    private void showGameOverDialog(final int messageId) {
        // Salva o score antes de mostrar o dialog
        highScoreManager.addScore(score);

        // Busca o ranking atualizado
        List<Integer> highScores = highScoreManager.getHighScores();
        StringBuilder rankingText = new StringBuilder("🏆 RANKING TOP 5 🏆\n");

        for (int i = 0; i < highScores.size(); i++) {
            rankingText.append(i + 1).append(".  ").append(highScores.get(i)).append(" pts");
            if (highScores.get(i) == score && score > 0) {
                rankingText.append(" (NOVO!)");
            }
            rankingText.append("\n");
        }

        activity.runOnUiThread(() -> {
            showSystemBars(); // Mostra as barras para o usuário conseguir sair se quiser

            final Dialog dialog = new Dialog(activity);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_game_over);
            dialog.setCancelable(false);

            // Fundo transparente para o CardView arredondado funcionar visualmente
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }

            // Vincula os elementos do layout
            TextView tvTitle = dialog.findViewById(R.id.tvTitle);
            TextView tvMessage = dialog.findViewById(R.id.tvMessage);
            TextView tvHighScores = dialog.findViewById(R.id.tvHighScores);
            Button btnRestart = dialog.findViewById(R.id.btnRestart);

            // Define os textos
            tvTitle.setText(getResources().getString(messageId));

            // IMPORTANTE: Aqui passamos 3 argumentos (tiros, tempo, score)
            // Certifique-se que seu strings.xml aceita %1$d, %2$.1f e %3$d
            tvMessage.setText(getResources().getString(R.string.results_format, shotsFired, totalElapsedTime, score));

            tvHighScores.setText(rankingText.toString());

            // Ação do botão
            btnRestart.setOnClickListener(v -> {
                dialog.dismiss();
                dialogIsDisplayed = false;
                hideSystemBars();
                newGame();
            });

            dialog.show();
        });
    }


    public void drawGameElements(Canvas canvas) {
        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), backgroundPaint);

        cannon.draw(canvas);
        if (cannon.getCannonball() != null && cannon.getCannonball().isOnScreen())
            cannon.getCannonball().draw(canvas);
        blocker.draw(canvas);
        for (Target target : targets)
            target.draw(canvas);

        Paint particlePaint = new Paint();
        particlePaint.setAntiAlias(true);
        particlePaint.setStyle(Paint.Style.FILL);

        for (Particle p : particles) {
            p.draw(canvas, particlePaint);
        }

        Paint floatPaint = new Paint(textPaint); 
        floatPaint.setShadowLayer(3, 1, 1, Color.BLACK); 
        floatPaint.setTextSize(textPaint.getTextSize() * 0.8f); 
        
        for (FloatingText ft : floatingTexts) {
            ft.draw(canvas, floatPaint);
        }

        float hudHeight = screenHeight * 0.12f;
        canvas.drawRect(0, 0, screenWidth, hudHeight, hudPaint);

        float barMargin = 30;
        float barWidth = screenWidth * 0.3f;
        float barHeight = 20;
        float barX = barMargin;
        float barY = hudHeight / 2 + 10;
        float timePercentage = (float) (timeLeft / 10.0);

        timerBarBackgroundPaint.setColor(Color.DKGRAY);
        canvas.drawRect(barX, barY, barX + barWidth, barY + barHeight, timerBarBackgroundPaint);

        int timerColor = timePercentage > 0.5 ? Color.GREEN : (timePercentage > 0.2 ? Color.YELLOW : Color.RED);
        timerBarPaint.setColor(timerColor);
        canvas.drawRect(barX, barY, barX + (barWidth * timePercentage), barY + barHeight, timerBarPaint);
        
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("TEMPO: " + String.format("%.1f", timeLeft), barX, barY - 10, textPaint);

        textPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("SCORE: " + score, screenWidth - barMargin, hudHeight / 2 + 10, textPaint);

        if (comboMultiplier > 1) {
            canvas.drawText("COMBO x" + comboMultiplier + "!", screenWidth / 2f, hudHeight / 2 + 15, comboPaint);
        }
    }

    public void testForCollisions() {
        if (cannon.getCannonball() != null && cannon.getCannonball().isOnScreen()) {
            for (int n = 0; n < targets.size(); n++) {
                if (cannon.getCannonball().collidesWith(targets.get(n))) {
                    targets.get(n).playSound();
                    
                    int basePoints = 100;
                    int pointsEarned = basePoints * comboMultiplier;
                    
                    timeLeft += targets.get(n).getHitReward();
                    score += pointsEarned;

                    float centerX = targets.get(n).getRect().centerX();
                    float centerY = targets.get(n).getRect().centerY();
                    createExplosion(centerX, centerY, targets.get(n).getColor()); 

                    float textX = (targets.get(n).getRect().left + targets.get(n).getRect().right) / 2f;
                    float textY = targets.get(n).getRect().top;
                    String msg = "+" + pointsEarned + (comboMultiplier > 1 ? " (x" + comboMultiplier + ")" : "");
                    
                    floatingTexts.add(new FloatingText(msg, textX, textY, Color.GREEN, true));

                    comboMultiplier++; 

                    cannon.removeCannonball();
                    targets.remove(n);
                    --n;
                    break;
                }
            }
        } else { return; }

        if (cannon.getCannonball() != null && cannon.getCannonball().collidesWith(blocker)) {
            blocker.playSound();
            cannon.getCannonball().reverseVelocityX();
            
            timeLeft -= blocker.getMissPenalty();
            score -= 15;
            comboMultiplier = 1; 

            if (score < 0) score = 0;

            float textX = (blocker.getRect().left + blocker.getRect().right) / 2f;
            float textY = blocker.getRect().bottom;
            floatingTexts.add(new FloatingText("-15 Hit!", textX, textY, Color.RED, false));
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
        comboPaint.setTextSize(h * 0.08f); 
    }

    private void hideSystemBars() {
        setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    private void showSystemBars() {
        setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    private class FloatingText {
        String text;
        float x, y;
        int color;
        int alpha = 255; 
        float velocityY; 

        public FloatingText(String text, float x, float y, int color, boolean isGood) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.color = color;
            this.velocityY = isGood ? -screenHeight * 0.001f : screenHeight * 0.001f; 
        }

        public boolean update(double dt) {
            y += velocityY * dt;
            alpha -= 0.15 * dt;
            
            if (alpha < 0) alpha = 0;
            return alpha > 0;
        }

        public void draw(Canvas canvas, Paint paint) {
            paint.setColor(color);
            paint.setAlpha(alpha);
            canvas.drawText(text, x, y, paint);
        }
    }

    private class Particle {
        float x, y;
        float velocityX, velocityY;
        int color;
        int alpha = 255; 
        float radius;

        public Particle(float startX, float startY, int color) {
            this.x = startX;
            this.y = startY;
            this.color = color;
            
            this.radius = (float) (screenHeight * (0.005 + Math.random() * 0.01));

            double angle = Math.random() * 2 * Math.PI; 
            double speed = screenHeight * (Math.random() * 0.0005 + 0.0005); 
            
            this.velocityX = (float) (Math.cos(angle) * speed);
            this.velocityY = (float) (Math.sin(angle) * speed);
        }

        public boolean update(double dt) {
            x += velocityX * dt;
            y += velocityY * dt;
            
            alpha -= (int)(0.4 * dt); 
            
            radius *= 0.95; 

            if (alpha < 0) alpha = 0;
            return alpha > 0; 
        }

        public void draw(Canvas canvas, Paint paint) {
            paint.setColor(color);
            paint.setAlpha(alpha);
            canvas.drawCircle(x, y, radius, paint);
        }
    }
}