package ntu.minhhoangg.duanketthucmonhoc.model;

import java.util.HashSet;
import java.util.Set;

/**
 * Lưu lịch sử từng nước đi để phục vụ chức năng Undo.
 */
public class MoveHistory {
    private int row;
    private int col;
    private int oldValue;
    private int newValue;
    private Set<Integer> oldNotes;
    private Set<Integer> newNotes;
    private boolean isNoteChange;

    public MoveHistory(int row, int col, int oldValue, int newValue, Set<Integer> oldNotes, Set<Integer> newNotes, boolean isNoteChange) {
        this.row = row;
        this.col = col;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.oldNotes = new HashSet<>(oldNotes);
        this.newNotes = new HashSet<>(newNotes);
        this.isNoteChange = isNoteChange;
    }

    public int getRow() { return row; }
    public int getCol() { return col; }
    public int getOldValue() { return oldValue; }
    public int getNewValue() { return newValue; }
    public Set<Integer> getOldNotes() { return oldNotes; }
    public boolean isNoteChange() { return isNoteChange; }


}