package br.com.prog3.cannongameapp;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HighScoreManager {
    private static final String PREFS_NAME = "CannonGamePrefs";
    private static final String SCORES_KEY = "HighScores";
    private static final int MAX_SCORES = 10; // Guardar apenas o Top 5

    private final SharedPreferences preferences;

    public HighScoreManager(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // Salva uma nova pontuação se ela for alta o suficiente
    public void addScore(int score) {
        if (score <= 0) return;

        List<Integer> scores = getHighScores();
        scores.add(score);
        
        // Ordena do maior para o menor
        Collections.sort(scores, Collections.reverseOrder());

        // Mantém apenas o Top 5
        if (scores.size() > MAX_SCORES) {
            scores = scores.subList(0, MAX_SCORES);
        }

        saveScores(scores);
    }

    public List<Integer> getHighScores() {
        String savedString = preferences.getString(SCORES_KEY, "");
        List<Integer> scores = new ArrayList<>();

        if (!savedString.isEmpty()) {
            String[] items = savedString.split(",");
            for (String item : items) {
                try {
                    scores.add(Integer.parseInt(item));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }
        return scores;
    }

    private void saveScores(List<Integer> scores) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < scores.size(); i++) {
            sb.append(scores.get(i));
            if (i < scores.size() - 1) {
                sb.append(",");
            }
        }
        // Use commit() instead of apply() to ensure it's saved immediately, 
        // or check if SharedPreferences is working correctly. 
        // For high scores apply() is usually fine, but let's stick to the provided logic which used apply().
        // Wait, the user says "it is not saving". Maybe the context is wrong?
        // But the user thinks the issue is the CALL to save.
        // I'm just rewriting the file to be sure it exists as I got "file not found" previously?
        // No, "read_file" failed? Ah, I might have made a typo in the path or the previous write failed silently?
        // The previous turn write to HighScoreManager said "file was written".
        // The read_file failed? 
        // "D:/CannonGameApp/app/src/main/java/br/com/prog3/cannongameapp/HighScoreManager.java"
        // Why did read_file fail? "file not found"?
        // Maybe I didn't create the directory structure? No, other files are there.
        // Let's just write it again to be sure.
        preferences.edit().putString(SCORES_KEY, sb.toString()).apply();
    }
}