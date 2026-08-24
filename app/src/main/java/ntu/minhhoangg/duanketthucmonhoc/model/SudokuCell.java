package ntu.minhhoangg.duanketthucmonhoc.model;

import java.util.HashSet;
import java.util.Set;

/**
 * Đại diện cho 1 ô vuông trên bàn cờ Sudoku.
 */
public class SudokuCell {
    private int row;
    private int col;
    private int value;
    private int solutionValue; // Đáp án đúng của ô
    private boolean isFixed;
    private boolean isValid = true; // false: bị trùng hàng/cột/khối (hiển thị màu đỏ)
    private Set<Integer> notes = new HashSet<>();

    public SudokuCell(int row, int col, int value, int solutionValue, boolean isFixed) {
        this.row = row;
        this.col = col;
        this.value = value;
        this.solutionValue = solutionValue;
        this.isFixed = isFixed;
    }

    public int getRow() { return row; }
    public int getCol() { return col; }
    public int getValue() { return value; }
    public void setValue(int value) { this.value = value; }
    public int getSolutionValue() { return solutionValue; }
    public boolean isFixed() { return isFixed; }
    public boolean isValid() { return isValid; }
    public void setValid(boolean valid) { isValid = valid; }
    public Set<Integer> getNotes() { return notes; }

    public void toggleNote(int number) {
        if (notes.contains(number)) {
            notes.remove(number);
        } else {
            notes.add(number);
        }
    }

    public void clearNotes() {
        notes.clear();
    }

    public void setNotes(Set<Integer> newNotes) {
        if (this.notes == null) {
            this.notes = new HashSet<>();
        } else {
            this.notes.clear();
        }

        if (newNotes != null) {
            this.notes.addAll(newNotes);
        }
    }
}