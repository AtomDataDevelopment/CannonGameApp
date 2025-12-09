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

    public boolean addScore(int score, int level) {
        if (score <= 0) {
            return false;
        }
        List<HighScore> scores = getHighScores();

        boolean isNewHighScore;
        if (scores.size() < MAX_SCORES) {
            isNewHighScore = true;
        } else {
            Collections.sort(scores, (s1, s2) -> Integer.compare(s2.getScore(), s1.getScore()));
            isNewHighScore = score > scores.get(scores.size() - 1).getScore();
        }

        if (isNewHighScore) {
            scores.add(new HighScore(score, level));
            Collections.sort(scores, (s1, s2) -> Integer.compare(s2.getScore(), s1.getScore()));
            while (scores.size() > MAX_SCORES) {
                scores.remove(scores.size() - 1);
            }
            saveScores(scores);
        }
        return isNewHighScore;
    }

    public List<HighScore> getHighScores() {
        List<HighScore> scores = new ArrayList<>();
        String savedString = preferences.getString(SCORES_KEY, null);

        if (savedString == null) {
            return scores;
        }

        String[] items = savedString.split(",");
        for (String item : items) {
            try {
                String trimmedItem = item.trim();
                if (trimmedItem.isEmpty()) {
                    continue;
                }

                if (trimmedItem.contains(":")) {
                    String[] parts = trimmedItem.split(":");
                    if (parts.length == 2) {
                        int scoreValue = Integer.parseInt(parts[0].trim());
                        int levelValue = Integer.parseInt(parts[1].trim());
                        scores.add(new HighScore(scoreValue, levelValue));
                    }
                } else {
                    int scoreValue = Integer.parseInt(trimmedItem);
                    scores.add(new HighScore(scoreValue, 1));
                }
            } catch (Exception e) {
            }
        }
        return scores;
    }

    private void saveScores(List<HighScore> scores) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < scores.size(); i++) {
            HighScore hs = scores.get(i);
            sb.append(hs.getScore()).append(":").append(hs.getLevel());
            if (i < scores.size() - 1) {
                sb.append(",");
            }
        }
        preferences.edit().putString(SCORES_KEY, sb.toString()).commit();
    }

    public static class HighScore {
        private final int score;
        private final int level;

        public HighScore(int score, int level) {
            this.score = score;
            this.level = level;
        }

        public int getScore() { return score; }
        public int getLevel() { return level; }
    }
}
