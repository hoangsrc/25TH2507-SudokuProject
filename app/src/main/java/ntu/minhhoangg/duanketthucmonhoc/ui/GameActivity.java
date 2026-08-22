package ntu.minhhoangg.duanketthucmonhoc.ui;

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
import android.content.SharedPreferences;

import ntu.minhhoangg.duanketthucmonhoc.R;
import ntu.minhhoangg.duanketthucmonhoc.data.PuzzleRepository;
import ntu.minhhoangg.duanketthucmonhoc.data.ScoreManager;
import ntu.minhhoangg.duanketthucmonhoc.logic.SudokuValidator;
import ntu.minhhoangg.duanketthucmonhoc.model.MoveHistory;
import ntu.minhhoangg.duanketthucmonhoc.model.SudokuCell;
import ntu.minhhoangg.duanketthucmonhoc.util.TimerHelper;

public class GameActivity extends AppCompatActivity {

    private String difficulty;
    private SudokuCell[][] board = new SudokuCell[9][9];
    private TextView[][] cellViews = new TextView[9][9];

    private int selectedRow = -1;
    private int selectedCol = -1;
    private boolean isNoteMode = false;

    // Khai báo biến đếm lỗi
    private int mistakesCount = 0;
    private TextView tvMistakes;

    private Stack<MoveHistory> moveStack = new Stack<>();
    private TimerHelper timerHelper;
    private ScoreManager scoreManager;

    private GridLayout gridBoard;
    private TextView tvTimer, tvCurrentDifficulty;
    private Button btnNotes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        difficulty = getIntent().getStringExtra("DIFFICULTY");
        if (difficulty == null) difficulty = "Easy";

        scoreManager = new ScoreManager(this);
        initViews();
        setupNumPad();
        setupToolBar();
        SharedPreferences prefs = getSharedPreferences("SudokuData", MODE_PRIVATE);
        boolean hasSavedGame = prefs.getBoolean(difficulty + "_hasSavedGame", false);

        if (hasSavedGame) {
            restoreSavedGame();
        } else {
            loadNewGame();
        }
        TextView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            saveCurrentProgress(); // Bấm là lưu ngay lập tức
            finish(); // Đóng màn hình Game, quay về Home
        });
    }

    private void initViews() {
        gridBoard = findViewById(R.id.gridBoard);
        tvTimer = findViewById(R.id.tvTimer);
        tvCurrentDifficulty = findViewById(R.id.tvCurrentDifficulty);
        tvMistakes = findViewById(R.id.tvMistakes); // Ánh xạ TextView lỗi
        btnNotes = findViewById(R.id.btnNotes);

        tvCurrentDifficulty.setText("Chế độ: " + difficulty);
        timerHelper = new TimerHelper(tvTimer);
    }

    private void updateMistakesUI() {
        if (tvMistakes != null) {
            tvMistakes.setText("Lỗi: " + mistakesCount + "/3");
        }
    }
    private void saveCurrentProgress() {
        if (timerHelper != null) {
            timerHelper.pause(); // Dừng đồng hồ trước khi lưu
        }

        SharedPreferences prefs = getSharedPreferences("SudokuData", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // Dùng tên chế độ (Easy, Medium...) làm tiền tố để lưu tách biệt
        String prefix = difficulty;

        // Lưu tiến trình: Lỗi và Thời gian
        editor.putInt(prefix + "_mistakes", mistakesCount);
        editor.putLong(prefix + "_time", timerHelper.getSecondsElapsed());

        // Chuyển mảng board thành chuỗi 81 ký tự để lưu
        StringBuilder currentBoardStr = new StringBuilder();
        StringBuilder initialBoardStr = new StringBuilder();
        StringBuilder solutionBoardStr = new StringBuilder();

        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                currentBoardStr.append(board[r][c].getValue());
                // Lưu lại ô nào là đề bài (cố định)
                initialBoardStr.append(board[r][c].isFixed() ? board[r][c].getValue() : "0");
                solutionBoardStr.append(board[r][c].getSolutionValue());
            }
        }

        editor.putString(prefix + "_currentBoard", currentBoardStr.toString());
        editor.putString(prefix + "_initialBoard", initialBoardStr.toString());
        editor.putString(prefix + "_solutionBoard", solutionBoardStr.toString());

        // Đánh dấu là chế độ này đang có một ván chơi dở
        editor.putBoolean(prefix + "_hasSavedGame", true);

        editor.apply(); // Lưu vào bộ nhớ máy
    }
    private void restoreSavedGame() {
        SharedPreferences prefs = getSharedPreferences("SudokuData", MODE_PRIVATE);
        String prefix = difficulty;

        // Khôi phục lỗi
        mistakesCount = prefs.getInt(prefix + "_mistakes", 0);
        updateMistakesUI();

        // Khôi phục thời gian
        long savedTime = prefs.getLong(prefix + "_time", 0);
        // Nếu TimerHelper của bạn chưa có hàm setSecondsElapsed(savedTime) thì thêm vào nhé!

        String currentStr = prefs.getString(prefix + "_currentBoard", "");
        String initialStr = prefs.getString(prefix + "_initialBoard", "");
        String solutionStr = prefs.getString(prefix + "_solutionBoard", "");

        if (currentStr.length() != 81) {
            loadNewGame(); // Dữ liệu lỗi thì tạo ván mới
            return;
        }

        gridBoard.removeAllViews();
        moveStack.clear();
        selectedRow = -1;
        selectedCol = -1;

        int paddingPx = (int) (32 * getResources().getDisplayMetrics().density);
        int displayWidth = getResources().getDisplayMetrics().widthPixels - paddingPx;
        int cellSize = (displayWidth - 40) / 9; // Trừ hao 40px viền

        for (int i = 0; i < 81; i++) {
            int r = i / 9;
            int c = i % 9;

            int currentVal = currentStr.charAt(i) - '0';
            boolean isFixed = initialStr.charAt(i) != '0';
            int solutionVal = solutionStr.charAt(i) - '0';

            board[r][c] = new SudokuCell(r, c, currentVal, solutionVal, isFixed);

            // Vẽ giao diện cho từng ô
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
        timerHelper.setSecondsElapsed(savedTime); // Gọi hàm để gán lại thời gian
        timerHelper.start();
    }
    private void clearSavedProgress() {
        SharedPreferences prefs = getSharedPreferences("SudokuData", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(difficulty + "_hasSavedGame", false);
        editor.apply();
    }
    private void loadNewGame() {
        gridBoard.removeAllViews();
        gridBoard.setAlignmentMode(GridLayout.ALIGN_BOUNDS); // Chống xô lệch chữ

        moveStack.clear();
        selectedRow = -1;
        selectedCol = -1;

        // Đặt lại lỗi = 0 khi bắt đầu ván mới
        mistakesCount = 0;
        updateMistakesUI();

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

        updateBoardUI();
        timerHelper.reset();
        timerHelper.start();
    }

    private void updateBoardUI() {
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

                // Cài đặt màu chữ mặc định
                if (cell.isFixed()) {
                    view.setTextColor(Color.BLACK);
                    view.setTypeface(null, Typeface.BOLD);
                } else {
                    view.setTextColor(Color.parseColor("#1976D2")); // Chữ màu xanh dương
                    view.setTypeface(null, Typeface.NORMAL);
                }

                // THỨ TỰ ƯU TIÊN MÀU NỀN MỚI
                if (!cell.isValid()) {
                    // Ưu tiên 1: Sai là nền Đỏ, chữ Đỏ luôn (đè lên cả màu ô đang chọn)
                    view.setBackgroundColor(Color.parseColor("#FFCDD2"));
                    view.setTextColor(Color.parseColor("#D32F2F"));
                } else if (r == selectedRow && c == selectedCol) {
                    // Ưu tiên 2: Ô đang được chọn (Nền Xanh nhạt)
                    view.setBackgroundColor(Color.parseColor("#BBDEFB"));
                } else {
                    // Ưu tiên 3: Màu bàn cờ so le mặc định
                    if ((r / 3 + c / 3) % 2 == 0) {
                        view.setBackgroundColor(Color.parseColor("#F5F5F5"));
                    } else {
                        view.setBackgroundColor(Color.WHITE);
                    }
                }
            }
        }
    }

    private void selectCell(int r, int c) {
        selectedRow = r;
        selectedCol = c;
        updateBoardUI();
    }

    private void setupNumPad() {
        LinearLayout layoutNumPad = findViewById(R.id.layoutNumPad);
        layoutNumPad.removeAllViews();

        for (int i = 1; i <= 9; i++) {
            Button btnNum = new Button(this);
            btnNum.setText(String.valueOf(i));
            btnNum.setTextSize(18);
            btnNum.setPadding(0,0,0,0);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            params.setMargins(2, 4, 2, 4);
            btnNum.setLayoutParams(params);

            btnNum.setBackgroundColor(Color.parseColor("#E2E8F0"));
            btnNum.setTextColor(Color.BLACK);

            final int number = i;
            btnNum.setOnClickListener(v -> onNumberClick(number));
            layoutNumPad.addView(btnNum);
        }
    }

    private void onNumberClick(int number) {
        if (selectedRow == -1 || selectedCol == -1) return;
        SudokuCell cell = board[selectedRow][selectedCol];

        // 1. Ô cố định (đề bài) -> Không cho sửa
        if (cell.isFixed()) return;

        // 2. Ô ĐÃ CÓ SỐ -> Muốn nhập số khác bắt buộc phải XÓA hoặc HOÀN TÁC trước
        if (!isNoteMode && cell.getValue() != 0) {
            return; // Chặn không cho nhập đè
        }

        if (isNoteMode) {
            moveStack.push(new MoveHistory(selectedRow, selectedCol, cell.getValue(), cell.getValue(), cell.getNotes(), cell.getNotes(), true));
            cell.toggleNote(number);
        } else {
            int oldValue = cell.getValue();
            moveStack.push(new MoveHistory(selectedRow, selectedCol, oldValue, number, cell.getNotes(), cell.getNotes(), false));

            cell.setValue(number);
            cell.clearNotes();

            validateBoard();

            // Kiểm tra lỗi nếu nhập sai đáp án
            if (number != cell.getSolutionValue()) {
                mistakesCount++;
                updateMistakesUI();

                // Nếu đủ 3 lỗi -> Game Over
                if (mistakesCount >= 3) {
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
                    // Chỉ cần khác với đáp án (SolutionValue) là tính là sai (không hợp lệ)
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
            if (selectedRow != -1 && selectedCol != -1) {
                SudokuCell cell = board[selectedRow][selectedCol];
                if (!cell.isFixed()) {
                    cell.setValue(cell.getSolutionValue());
                    cell.clearNotes();
                    validateBoard();
                    updateBoardUI();
                    checkWinCondition();
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
            mistakesCount = 0; // Đặt lại bộ đếm khi chơi lại
            updateMistakesUI();
            validateBoard();
            updateBoardUI();
            timerHelper.reset();
            timerHelper.start();
        });
    }

    private void showGameOverDialog() {
        clearSavedProgress();
        timerHelper.pause();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Game Over! 😢");
        builder.setMessage("Bạn đã sai quá 3 lần. Đừng nản chí, hãy thử lại nhé!");
        builder.setPositiveButton("Chơi lại ván này", (dialog, which) -> {
            findViewById(R.id.btnRestart).performClick();
        });
        builder.setNegativeButton("Tạo ván mới", (dialog, which) -> loadNewGame());
        builder.setCancelable(false);
        builder.show();
    }

    private void checkWinCondition() {
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                // Nếu còn ô trống hoặc có ô sai thì thoát luôn, không làm gì cả
                if (board[r][c].getValue() == 0 || !board[r][c].isValid()) {
                    return;
                }
            }
        }

        // ĐÃ VƯỢT QUA VÒNG LẶP = CHẮC CHẮN CHIẾN THẮNG!
        // Lúc này mới được xóa tiến trình lưu dở để lần sau chơi ván mới.
        clearSavedProgress();

        timerHelper.pause();
        long timeTaken = timerHelper.getSecondsElapsed();
        boolean isNewBest = scoreManager.saveBestTime(difficulty, timeTaken);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chúc mừng! 🎉");
        builder.setMessage("Bạn đã hoàn thành ván Sudoku trong " + TimerHelper.formatTime(timeTaken) +
                (isNewBest ? "\n🌟 Kỷ lục mới!" : ""));
        builder.setPositiveButton("Ván mới", (dialog, which) -> loadNewGame());
        builder.setNegativeButton("Về trang chủ", (dialog, which) -> finish());
        builder.setCancelable(false);
        builder.show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        timerHelper.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        timerHelper.start();
    }
}