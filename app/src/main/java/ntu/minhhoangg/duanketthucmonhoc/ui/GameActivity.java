package ntu.minhhoangg.duanketthucmonhoc.ui;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Stack;

import ntu.minhhoangg.duanketthucmonhoc.R;
import ntu.minhhoangg.duanketthucmonhoc.data.PuzzleRepository;
import ntu.minhhoangg.duanketthucmonhoc.data.ScoreManager;
import ntu.minhhoangg.duanketthucmonhoc.model.MoveHistory;
import ntu.minhhoangg.duanketthucmonhoc.model.SudokuCell;
import ntu.minhhoangg.duanketthucmonhoc.util.TimerHelper;

public class GameActivity extends AppCompatActivity {

    private static final String PREF_NAME = "SudokuData";

    private int maxMistakes;
    private int initialHints;

    private String difficulty;
    private SudokuCell[][] board = new SudokuCell[9][9];
    private TextView[][] cellViews = new TextView[9][9];

    private int selectedRow = -1;
    private int selectedCol = -1;
    private boolean isNoteMode = false;

    private int mistakesCount = 0;
    private int hintsRemaining;
    private int hintedRow = -1;
    private int hintedCol = -1;

    private TextView tvMistakes, tvTimer, tvCurrentDifficulty;
    private Button btnNotes;
    private GridLayout gridBoard;

    private Stack<MoveHistory> moveStack = new Stack<>();
    private TimerHelper timerHelper;
    private ScoreManager scoreManager;
    private Button[] numButtons = new Button[9];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        difficulty = getIntent().getStringExtra("DIFFICULTY");
        if (difficulty == null) difficulty = "Easy";

        // Khởi tạo độ khó hệ thống
        maxMistakes = getMaxMistakesByDifficulty();
        initialHints = getInitialHintsByDifficulty();

        scoreManager = new ScoreManager(this);
        initViews();
        setupNumPad();
        setupToolBar();

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        boolean hasSavedGame = prefs.getBoolean(difficulty + "_hasSavedGame", false);

        if (hasSavedGame) {
            restoreSavedGame();
        } else {
            loadNewGame();
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> {
            saveCurrentProgress();
            finish();
        });
    }

    private int getMaxMistakesByDifficulty() {
        switch (difficulty.toUpperCase()) {
            case "EASY": return 5;
            case "MEDIUM": return 4;
            case "HARD": return 3;
            case "EXPERT": return 2;
            case "MASTER": return 1;
            default: return 3;
        }
    }

    private int getInitialHintsByDifficulty() {
        switch (difficulty.toUpperCase()) {
            case "EASY": return 5;
            case "MEDIUM": return 3;
            case "HARD": return 2;
            case "EXPERT": return 1;
            case "MASTER": return 0;
            default: return 3;
        }
    }

    private void initViews() {
        gridBoard = findViewById(R.id.gridBoard);
        tvTimer = findViewById(R.id.tvTimer);
        tvCurrentDifficulty = findViewById(R.id.tvCurrentDifficulty);
        tvMistakes = findViewById(R.id.tvMistakes);
        btnNotes = findViewById(R.id.btnNotes);

        tvCurrentDifficulty.setText("Chế độ: " + difficulty);
        timerHelper = new TimerHelper(tvTimer);
    }

    private void updateMistakesUI() {
        if (tvMistakes != null) {
            tvMistakes.setText("Lỗi: " + mistakesCount + "/" + maxMistakes);
        }
    }

    private void updateHintUI() {
        TextView btnHint = findViewById(R.id.btnHint);
        if (btnHint != null) {
            btnHint.setText("Gợi ý (" + hintsRemaining + ")");
        }
    }

    private void saveCurrentProgress() {
        if (timerHelper != null) {
            timerHelper.pause();
        }

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putInt(difficulty + "_mistakes", mistakesCount);
        editor.putLong(difficulty + "_time", timerHelper.getSecondsElapsed());
        editor.putInt(difficulty + "_hints", hintsRemaining);

        StringBuilder currentBoardStr = new StringBuilder();
        StringBuilder initialBoardStr = new StringBuilder();
        StringBuilder solutionBoardStr = new StringBuilder();

        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                currentBoardStr.append(board[r][c].getValue());
                initialBoardStr.append(board[r][c].isFixed() ? board[r][c].getValue() : "0");
                solutionBoardStr.append(board[r][c].getSolutionValue());
            }
        }

        editor.putString(difficulty + "_currentBoard", currentBoardStr.toString());
        editor.putString(difficulty + "_initialBoard", initialBoardStr.toString());
        editor.putString(difficulty + "_solutionBoard", solutionBoardStr.toString());
        editor.putBoolean(difficulty + "_hasSavedGame", true);

        editor.apply();
    }

    private void restoreSavedGame() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        mistakesCount = prefs.getInt(difficulty + "_mistakes", 0);
        updateMistakesUI();

        long savedTime = prefs.getLong(difficulty + "_time", 0);
        String currentStr = prefs.getString(difficulty + "_currentBoard", "");
        String initialStr = prefs.getString(difficulty + "_initialBoard", "");
        String solutionStr = prefs.getString(difficulty + "_solutionBoard", "");

        if (currentStr.length() != 81) {
            loadNewGame();
            return;
        }

        gridBoard.removeAllViews();
        moveStack.clear();
        selectedRow = -1;
        selectedCol = -1;
        hintedRow = -1;
        hintedCol = -1;
        hintsRemaining = prefs.getInt(difficulty + "_hints", initialHints);
        updateHintUI();

        int paddingPx = (int) (32 * getResources().getDisplayMetrics().density);
        int displayWidth = getResources().getDisplayMetrics().widthPixels - paddingPx;
        int cellSize = (displayWidth - 40) / 9;

        for (int i = 0; i < 81; i++) {
            int r = i / 9;
            int c = i % 9;

            int currentVal = currentStr.charAt(i) - '0';
            boolean isFixed = initialStr.charAt(i) != '0';
            int solutionVal = solutionStr.charAt(i) - '0';

            board[r][c] = new SudokuCell(r, c, currentVal, solutionVal, isFixed);

            TextView cellView = new TextView(this);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = cellSize;
            params.height = cellSize;
            params.rowSpec = GridLayout.spec(r, GridLayout.CENTER);
            params.columnSpec = GridLayout.spec(c, GridLayout.CENTER);

            int leftMargin = (c % 3 == 0) ? 4 : 1;
            int topMargin = (r % 3 == 0) ? 4 : 1;
            int rightMargin = (c == 8) ? 4 : 1;
            int bottomMargin = (r == 8) ? 4 : 1;
            params.setMargins(leftMargin, topMargin, rightMargin, bottomMargin);

            cellView.setLayoutParams(params);
            cellView.setGravity(Gravity.CENTER);

            final int finalR = r;
            final int finalC = c;
            cellView.setOnClickListener(v -> selectCell(finalR, finalC));

            cellViews[r][c] = cellView;
            gridBoard.addView(cellView);
        }

        validateBoard();
        updateBoardUI();

        timerHelper.reset();
        timerHelper.setSecondsElapsed(savedTime);
        timerHelper.start();
    }

    private void clearSavedProgress() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(difficulty + "_hasSavedGame", false).apply();
    }

    private void loadNewGame() {
        gridBoard.removeAllViews();
        gridBoard.setAlignmentMode(GridLayout.ALIGN_BOUNDS);

        moveStack.clear();
        selectedRow = -1;
        selectedCol = -1;
        hintedRow = -1;
        hintedCol = -1;
        mistakesCount = 0;
        hintsRemaining = initialHints;

        updateMistakesUI();
        updateHintUI();

        int[][][] puzzleData = PuzzleRepository.getRandomPuzzle(difficulty);
        int[][] initialGrid = puzzleData[0];
        int[][] solutionGrid = puzzleData[1];

        int paddingPx = (int) (32 * getResources().getDisplayMetrics().density);
        int displayWidth = getResources().getDisplayMetrics().widthPixels - paddingPx;
        int cellSize = (displayWidth - 40) / 9;

        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                boolean isFixed = initialGrid[r][c] != 0;
                board[r][c] = new SudokuCell(r, c, initialGrid[r][c], solutionGrid[r][c], isFixed);

                TextView cellView = new TextView(this);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = cellSize;
                params.height = cellSize;
                params.rowSpec = GridLayout.spec(r, GridLayout.CENTER);
                params.columnSpec = GridLayout.spec(c, GridLayout.CENTER);

                int leftMargin = (c % 3 == 0) ? 4 : 1;
                int topMargin = (r % 3 == 0) ? 4 : 1;
                int rightMargin = (c == 8) ? 4 : 1;
                int bottomMargin = (r == 8) ? 4 : 1;
                params.setMargins(leftMargin, topMargin, rightMargin, bottomMargin);

                cellView.setLayoutParams(params);
                cellView.setGravity(Gravity.CENTER);

                final int finalR = r;
                final int finalC = c;
                cellView.setOnClickListener(v -> selectCell(finalR, finalC));

                cellViews[r][c] = cellView;
                gridBoard.addView(cellView);
            }
        }
        validateBoard();
        updateBoardUI();
        timerHelper.reset();
        timerHelper.start();
    }

    private void updateBoardUI() {
        int selectedValue = 0;
        if (selectedRow != -1 && selectedCol != -1) {
            selectedValue = board[selectedRow][selectedCol].getValue();
        }

        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                SudokuCell cell = board[r][c];
                TextView view = cellViews[r][c];
                view.setMaxLines(2);

                if (cell.getValue() != 0) {
                    view.setText(String.valueOf(cell.getValue()));
                    view.setTextSize(20);
                } else if (!cell.getNotes().isEmpty()) {
                    String noteStr = cell.getNotes().toString().replaceAll("[\\[\\],]", "");
                    view.setText(noteStr);
                    view.setTextSize(10);
                } else {
                    view.setText("");
                    view.setTextSize(20);
                }

                if (cell.isFixed()) {
                    view.setTextColor(Color.BLACK);
                    view.setTypeface(null, Typeface.BOLD);
                } else {
                    view.setTextColor(Color.parseColor("#1976D2"));
                    view.setTypeface(null, Typeface.NORMAL);
                }

                boolean isSelected = (r == selectedRow && c == selectedCol);
                boolean isHinted = (r == hintedRow && c == hintedCol);
                boolean isSameRow = (selectedRow != -1 && r == selectedRow);
                boolean isSameCol = (selectedCol != -1 && c == selectedCol);
                boolean isSameBlock = (selectedRow != -1 && selectedCol != -1 && (r / 3 == selectedRow / 3) && (c / 3 == selectedCol / 3));
                boolean isSameValue = (selectedValue != 0 && cell.getValue() == selectedValue);

                if (!cell.isValid()) {
                    view.setBackgroundColor(Color.parseColor("#FFCDD2"));
                    view.setTextColor(Color.parseColor("#D32F2F"));
                } else if (isHinted) {
                    view.setBackgroundColor(Color.parseColor("#FFF59D"));
                } else if (isSelected) {
                    view.setBackgroundColor(Color.parseColor("#90CAF9"));
                } else if (isSameValue) {
                    view.setBackgroundColor(Color.parseColor("#BBDEFB"));
                } else if (isSameRow || isSameCol || isSameBlock) {
                    view.setBackgroundColor(Color.parseColor("#E3F2FD"));
                } else {
                    if ((r / 3 + c / 3) % 2 == 0) {
                        view.setBackgroundColor(Color.parseColor("#F5F5F5"));
                    } else {
                        view.setBackgroundColor(Color.WHITE);
                    }
                }
            }
        }
        updateNumPadUI();
    }

    private void selectCell(int r, int c) {
        selectedRow = r;
        selectedCol = c;
        hintedRow = -1;
        hintedCol = -1;
        updateBoardUI();
        updateNumPadUI();
    }

    private boolean isValidPlacement(int r, int c, int num) {
        for (int i = 0; i < 9; i++) {
            if (board[r][i].getValue() == num) return false;
            if (board[i][c].getValue() == num) return false;
        }

        int startR = (r / 3) * 3;
        int startC = (c / 3) * 3;
        for (int i = startR; i < startR + 3; i++) {
            for (int j = startC; j < startC + 3; j++) {
                if (board[i][j].getValue() == num) return false;
            }
        }
        return true;
    }

    private void setupNumPad() {
        LinearLayout layoutNumPad = findViewById(R.id.layoutNumPad);
        layoutNumPad.removeAllViews();

        for (int i = 1; i <= 9; i++) {
            Button btnNum = new Button(this);
            btnNum.setText(String.valueOf(i));
            btnNum.setTextSize(18);
            btnNum.setPadding(0, 0, 0, 0);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            params.setMargins(2, 4, 2, 4);
            btnNum.setLayoutParams(params);
            btnNum.setBackgroundColor(Color.parseColor("#E2E8F0"));
            btnNum.setTextColor(Color.BLACK);

            final int number = i;
            btnNum.setOnClickListener(v -> onNumberClick(number));
            layoutNumPad.addView(btnNum);

            numButtons[i - 1] = btnNum;
        }
    }

    private void updateNumPadUI() {
        int[] numberCounts = new int[10];

        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                SudokuCell cell = board[r][c];
                if (cell != null && cell.getValue() != 0 && cell.isValid()) {
                    numberCounts[cell.getValue()]++;
                }
            }
        }

        for (int i = 0; i < 9; i++) {
            if (numButtons[i] != null) {
                if (numberCounts[i + 1] == 9) {
                    numButtons[i].setVisibility(android.view.View.INVISIBLE);
                } else {
                    numButtons[i].setVisibility(android.view.View.VISIBLE);
                }
            }
        }
    }

    private void onNumberClick(int number) {
        if (selectedRow == -1 || selectedCol == -1) return;
        SudokuCell cell = board[selectedRow][selectedCol];

        if (cell.isFixed()) return;
        if (!isNoteMode && cell.getValue() != 0) return;

        if (isNoteMode) {
            moveStack.push(new MoveHistory(selectedRow, selectedCol, cell.getValue(), cell.getValue(), cell.getNotes(), cell.getNotes(), true));
            cell.toggleNote(number);
        } else {
            int oldValue = cell.getValue();
            moveStack.push(new MoveHistory(selectedRow, selectedCol, oldValue, number, cell.getNotes(), cell.getNotes(), false));

            cell.setValue(number);
            cell.clearNotes();
            validateBoard();

            if (number != cell.getSolutionValue()) {
                mistakesCount++;
                updateMistakesUI();

                if (mistakesCount >= maxMistakes) {
                    updateBoardUI();
                    showGameOverDialog();
                    return;
                }
            }

            checkWinCondition();
        }
        updateBoardUI();
    }

    private void validateBoard() {
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (board[r][c].getValue() != 0) {
                    boolean isValid = (board[r][c].getValue() == board[r][c].getSolutionValue());
                    board[r][c].setValid(isValid);
                } else {
                    board[r][c].setValid(true);
                }
            }
        }
    }

    private void setupToolBar() {
        findViewById(R.id.btnUndo).setOnClickListener(v -> {
            if (!moveStack.isEmpty()) {
                MoveHistory lastMove = moveStack.pop();
                SudokuCell cell = board[lastMove.getRow()][lastMove.getCol()];
                cell.setValue(lastMove.getOldValue());
                validateBoard();
                updateBoardUI();
            }
        });

        findViewById(R.id.btnErase).setOnClickListener(v -> {
            if (selectedRow != -1 && selectedCol != -1) {
                SudokuCell cell = board[selectedRow][selectedCol];
                if (!cell.isFixed()) {
                    moveStack.push(new MoveHistory(selectedRow, selectedCol, cell.getValue(), 0, cell.getNotes(), cell.getNotes(), false));
                    cell.setValue(0);
                    cell.clearNotes();
                    validateBoard();
                    updateBoardUI();
                }
            }
        });

        btnNotes.setOnClickListener(v -> {
            isNoteMode = !isNoteMode;
            btnNotes.setBackgroundColor(isNoteMode ? Color.LTGRAY : Color.TRANSPARENT);
        });

        findViewById(R.id.btnHint).setOnClickListener(v -> {
            if (hintsRemaining <= 0) return;

            for (int r = 0; r < 9; r++) {
                for (int c = 0; c < 9; c++) {
                    if (board[r][c].getValue() == 0) {
                        int possibleCount = 0;
                        int lastValidNum = 0;

                        for (int num = 1; num <= 9; num++) {
                            if (isValidPlacement(r, c, num)) {
                                possibleCount++;
                                lastValidNum = num;
                            }
                        }

                        if (possibleCount == 1 && lastValidNum == board[r][c].getSolutionValue()) {
                            applyHint(r, c, lastValidNum);
                            return;
                        }
                    }
                }
            }

            for (int r = 0; r < 9; r++) {
                for (int c = 0; c < 9; c++) {
                    if (board[r][c].getValue() == 0) {
                        applyHint(r, c, board[r][c].getSolutionValue());
                        return;
                    }
                }
            }
        });

        findViewById(R.id.btnNewGame).setOnClickListener(v -> loadNewGame());

        findViewById(R.id.btnRestart).setOnClickListener(v -> {
            for (int r = 0; r < 9; r++) {
                for (int c = 0; c < 9; c++) {
                    if (!board[r][c].isFixed()) {
                        board[r][c].setValue(0);
                        board[r][c].clearNotes();
                    }
                }
            }
            moveStack.clear();
            mistakesCount = 0;
            hintsRemaining = initialHints;
            hintedRow = -1;
            hintedCol = -1;

            updateMistakesUI();
            updateHintUI();
            validateBoard();
            updateBoardUI();

            timerHelper.reset();
            timerHelper.start();
        });
    }

    private void applyHint(int r, int c, int value) {
        hintsRemaining--;
        updateHintUI();

        board[r][c].setValue(value);
        board[r][c].clearNotes();

        selectedRow = r;
        selectedCol = c;
        hintedRow = r;
        hintedCol = c;

        validateBoard();
        updateBoardUI();
        checkWinCondition();
    }

    private void showGameOverDialog() {
        clearSavedProgress();
        timerHelper.pause();

        new AlertDialog.Builder(this)
                .setTitle("Game Over! 😢")
                .setMessage("Bạn đã sai quá " + maxMistakes + " lần. Đừng nản chí, hãy thử lại nhé!")
                .setPositiveButton("Chơi lại ván này", (dialog, which) -> findViewById(R.id.btnRestart).performClick())
                .setNegativeButton("Tạo ván mới", (dialog, which) -> loadNewGame())
                .setCancelable(false)
                .show();
    }

    private void checkWinCondition() {
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (board[r][c].getValue() == 0 || !board[r][c].isValid()) {
                    return;
                }
            }
        }

        clearSavedProgress();
        timerHelper.pause();

        long timeTaken = timerHelper.getSecondsElapsed();
        boolean isNewBest = scoreManager.saveBestTime(difficulty, timeTaken);

        new AlertDialog.Builder(this)
                .setTitle("Chúc mừng! 🎉")
                .setMessage("Bạn đã hoàn thành ván Sudoku trong " + TimerHelper.formatTime(timeTaken) +
                        (isNewBest ? "\n🌟 Kỷ lục mới!" : ""))
                .setPositiveButton("Ván mới", (dialog, which) -> loadNewGame())
                .setNegativeButton("Về trang chủ", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (timerHelper != null) {
            timerHelper.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (timerHelper != null) {
            timerHelper.start();
        }
    }
}