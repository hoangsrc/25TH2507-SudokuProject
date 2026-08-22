package ntu.minhhoangg.duanketthucmonhoc.util;

import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import java.util.Locale;

/**
 * Đếm thời gian dạng mm:ss cho ván chơi.
 */
public class TimerHelper {
    private long secondsElapsed = 0;
    private boolean isRunning = false;
    private Handler handler = new Handler(Looper.getMainLooper());
    private TextView timerTextView;

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning) {
                secondsElapsed++;
                updateDisplay();
                handler.postDelayed(this, 1000);
            }
        }
    };

    public TimerHelper(TextView timerTextView) {
        this.timerTextView = timerTextView;
    }

    public void start() {
        if (!isRunning) {
            isRunning = true;
            handler.post(runnable);
        }
    }

    public void pause() {
        isRunning = false;
        handler.removeCallbacks(runnable);
    }

    public void reset() {
        pause();
        secondsElapsed = 0;
        updateDisplay();
    }

    public long getSecondsElapsed() { return secondsElapsed; }

    public static String formatTime(long seconds) {
        if (seconds < 0) return "--:--";
        long m = seconds / 60;
        long s = seconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", m, s);
    }

    private void updateDisplay() {
        if (timerTextView != null) {
            timerTextView.setText(formatTime(secondsElapsed));
        }
    }
}