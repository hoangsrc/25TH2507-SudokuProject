package ntu.minhhoangg.duanketthucmonhoc.data;

import java.util.Random;

public class PuzzleRepository {

    // Biến lưu lại vị trí đề vừa chơi để ván sau không bốc trùng lại
    private static int lastEasyIndex = -1;
    private static int lastHardIndex = -1;

    private static final String[][] EASY_PUZZLES = {
            {
                    "530070000600195000098000060800060003400803001700020006060000280000419005000080079",
                    "534678912672195348198342567859761423426853791713924856961537284287419635345286179"
            },
            {
                    "000260701680070090190004500820100040004602900050003028009300074040050036703018000",
                    "435269781682571493197834562826195347374682915951743628519326874248957136763418259"
            },
            {
                    "000000907000420180000705026100904000050000040000507009920108000034059000507000000",
                    "245816937369427185817735426172984563658273841493567219926148753734659821587392694"
            },
            // Đề #7244 (Aug 14, 2026)
            {
                    "600007900200605007003000108056240000400000005090000084040030020005100300060400070",
                    "614387952289651437573924168856249713437816295192573684941735826725168349368492571"
            },
            // Đề #7237 (Aug 07, 2026)
            {
                    "900008005000903060068070000402003700800700004057040600000801020010007580085000100",
                    "924168375571934862368572419492683751836715294157249638743851926619427583285396147"
            }
    };

    private static final String[][] HARD_PUZZLES = {
            {
                    "000000012000000003002300400001800005060070800000009000008500000900040500470006000",
                    "694785312185294673732361458924183795563972841817659234328517964956843527471236189"
            },
            {
                    "800000000003600000070090200050007000000045700000100030001000068008500010090000400",
                    "812753649943682175675491283154237896369845721287169534521974368438526917796318452"
            }
    };

    public static int[][][] getRandomPuzzle(String difficulty) {
        String[][] targetCategory;
        int lastIndex;
        boolean isHard = difficulty.equals("Hard") || difficulty.equals("Expert");

        if (isHard) {
            targetCategory = HARD_PUZZLES;
            lastIndex = lastHardIndex;
        } else {
            targetCategory = EASY_PUZZLES;
            lastIndex = lastEasyIndex;
        }

        Random rand = new Random();
        int index;

        // THUẬT TOÁN CHỐNG TRÙNG ĐỀ: Bốc liên tục cho đến khi nào ra số khác ván trước thì thôi
        if (targetCategory.length > 1) {
            do {
                index = rand.nextInt(targetCategory.length);
            } while (index == lastIndex);
        } else {
            index = 0;
        }

        // Lưu lại vị trí vừa bốc để dành kiểm tra cho ván tiếp theo
        if (isHard) {
            lastHardIndex = index;
        } else {
            lastEasyIndex = index;
        }

        String initialStr = targetCategory[index][0];
        String solutionStr = targetCategory[index][1];

        return new int[][][]{ parseStringToArray(initialStr), parseStringToArray(solutionStr) };
    }

    private static int[][] parseStringToArray(String data) {
        int[][] grid = new int[9][9];
        for (int i = 0; i < 81; i++) {
            grid[i / 9][i % 9] = data.charAt(i) - '0';
        }
        return grid;
    }
}