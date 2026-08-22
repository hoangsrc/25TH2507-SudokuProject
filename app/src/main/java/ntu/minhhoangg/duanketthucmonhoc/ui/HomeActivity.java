package ntu.minhhoangg.duanketthucmonhoc.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import ntu.minhhoangg.duanketthucmonhoc.R;
import ntu.minhhoangg.duanketthucmonhoc.data.ScoreManager;
import ntu.minhhoangg.duanketthucmonhoc.util.TimerHelper;

public class HomeActivity extends AppCompatActivity {

    private ScoreManager scoreManager;
    private TextView tvBestEasy, tvBestMedium, tvBestHard, tvBestExpert;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        scoreManager = new ScoreManager(this);
        initViews();
        setupClickListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateBestScores();
    }

    private void initViews() {
        tvBestEasy = findViewById(R.id.tvBestEasy);
        tvBestMedium = findViewById(R.id.tvBestMedium);
        tvBestHard = findViewById(R.id.tvBestHard);
        tvBestExpert = findViewById(R.id.tvBestExpert);
    }

    private void updateBestScores() {
        tvBestEasy.setText("Kỷ lục: " + TimerHelper.formatTime(scoreManager.getBestTime("Easy")));
        tvBestMedium.setText("Kỷ lục: " + TimerHelper.formatTime(scoreManager.getBestTime("Medium")));
        tvBestHard.setText("Kỷ lục: " + TimerHelper.formatTime(scoreManager.getBestTime("Hard")));
        tvBestExpert.setText("Kỷ lục: " + TimerHelper.formatTime(scoreManager.getBestTime("Expert")));
    }

    private void setupClickListeners() {
        findViewById(R.id.cardEasy).setOnClickListener(v -> startGame("Easy"));
        findViewById(R.id.cardMedium).setOnClickListener(v -> startGame("Medium"));
        findViewById(R.id.cardHard).setOnClickListener(v -> startGame("Hard"));
        findViewById(R.id.cardExpert).setOnClickListener(v -> startGame("Expert"));
    }

    private void startGame(String difficulty) {
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("DIFFICULTY", difficulty);
        startActivity(intent);
    }
}