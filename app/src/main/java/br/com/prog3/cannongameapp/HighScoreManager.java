package br.com.prog3.cannongameapp;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HighScoreManager {
    private static final String PREFS_NAME = "CannonGamePrefs";
    private static final String SCORES_KEY = "HighScores";
    private static final int MAX_SCORES = 10;

    private final SharedPreferences preferences;

    public HighScoreManager(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void addScore(int score) {
        if (score <= 0) return;
        List<Integer> scores = getHighScores();
        scores.add(score);
        Collections.sort(scores, Collections.reverseOrder());
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
                } catch (NumberFormatException e) {}
            }
        }
        return scores;
    }

    private void saveScores(List<Integer> scores) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < scores.size(); i++) {
            sb.append(scores.get(i));
            if (i < scores.size() - 1) sb.append(",");
        }
        preferences.edit().putString(SCORES_KEY, sb.toString()).apply();
    }
}