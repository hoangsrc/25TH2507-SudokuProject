package ntu.minhhoangg.duanketthucmonhoc.data;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Quản lý lưu/đọc kỷ lục thời gian qua SharedPreferences.
 */
public class ScoreManager {
    private static final String PREF_NAME = "SudokuScores";
    private static final String KEY_BEST_TIME_PREFIX = "best_time_";

    private SharedPreferences preferences;

    public ScoreManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean saveBestTime(String difficulty, long timeInSeconds) {
        long currentBest = getBestTime(difficulty);
        if (currentBest == -1 || timeInSeconds < currentBest) {
            preferences.edit().putLong(KEY_BEST_TIME_PREFIX + difficulty.toLowerCase(), timeInSeconds).apply();
            return true;
        }
        return false;
    }

    public long getBestTime(String difficulty) {
        return preferences.getLong(KEY_BEST_TIME_PREFIX + difficulty.toLowerCase(), -1);
    }
}