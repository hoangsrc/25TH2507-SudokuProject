package ntu.minhhoangg.duanketthucmonhoc.logic;

/**
 * Kiểm tra tính hợp lệ của ô số trên bàn cờ Sudoku.
 */
public class SudokuValidator {

    public static boolean isValidPlacement(int[][] board, int row, int col, int value) {
        if (value == 0) return true;

        // 1. Kiểm tra hàng
        for (int c = 0; c < 9; c++) {
            if (c != col && board[row][c] == value) return false;
        }

        // 2. Kiểm tra cột
        for (int r = 0; r < 9; r++) {
            if (r != row && board[r][col] == value) return false;
        }

        // 3. Kiểm tra khối 3x3
        int startRow = (row / 3) * 3;
        int startCol = (col / 3) * 3;
        for (int r = startRow; r < startRow + 3; r++) {
            for (int c = startCol; c < startCol + 3; c++) {
                if ((r != row || c != col) && board[r][c] == value) return false;
            }
        }

        return true;
    }
}