package edu.sustech.xiangqi.search;

// info:基于Pikafish-HCE部分评估思路的静态局面评估函数
//  完整实现了:  1.基础子力加分
//             2.棋子站位加分
//             3.子力配合加分
//             4.阶段计算以及中/残局得分加权混合
//             5.rule60的和棋衰减接口
//             6.特殊的得分微调: 空头炮、炮镇窝心马、沉底炮、过河兵、士象全
//  由于时间复杂度而舍弃或简化了:
//             7.将原版的复杂机动性与牵制计算换成了简单计算伪合法落点, 并通过ChatGPT进行的大量实验结果重新规划了机动性得分表
//             8.舍弃了一些复杂的特殊微调：车占开放线、边马夹车、签手兵、多子归边
//             9.舍弃了特殊残局的复杂判断

import edu.sustech.xiangqi.model.Board;
import edu.sustech.xiangqi.model.Piece;

public class Evaluate {
    //车的站位加分
    private static final int[][][] che_pos = {
            {{-203, -131}, {46, -225}, {-147, -86}, {-17, 5}, {8, -13}, {-17, 5}, {-147, -86}, {46, -225}, {-203, -131}},
            {{-203, -52}, {58, -67}, {-89, -110}, {-78, 121}, {-106, 106}, {-78, 121}, {-89, -110}, {58, -67}, {-203, -52}},
            {{-138, -61}, {7, -96}, {-65, -9}, {-110, 7}, {-8, 45}, {-110, 7}, {-65, -9}, {7, -96}, {-138, -61}},
            {{-60, 77}, {-48, -33}, {-58, 61}, {88, 54}, {175, -4}, {88, 54}, {-58, 61}, {-48, -33}, {-60, 77}},
            {{-61, 32}, {42, -7}, {-112, 34}, {181, 13}, {-170, 14}, {181, 13}, {-112, 34}, {42, -7}, {-61, 32}},
            {{-69, -12}, {192, 24}, {88, -76}, {53, -74}, {110, 86}, {53, -74}, {88, -76}, {192, 24}, {-69, -12}},
            {{-199, 103}, {-8, -85}, {179, -39}, {48, 23}, {12, 79}, {48, 23}, {179, -39}, {-8, -85}, {-199, 103}},
            {{139, 130}, {20, -149}, {95, 113}, {92, 101}, {-20, -69}, {92, 101}, {95, 113}, {20, -149}, {139, 130}},
            {{-72, -15}, {163, -21}, {124, -79}, {32, 46}, {-78, -100}, {32, 46}, {124, -79}, {163, -21}, {-72, -15}},
            {{109, -97}, {66, -29}, {-86, -4}, {39, 55}, {22, 54}, {39, 55}, {-86, -4}, {66, -29}, {109, -97}}
    };
    //马的占位加分
    private static final int[][][] ma_pos = {
            {{-25, -48}, {-180, -201}, {-30, -32}, {-112, -133}, {-99, 56}, {-112, -133}, {-30, -32}, {-180, -201}, {-25, -48}},
            {{-126, -95}, {-93, 59}, {-142, -26}, {-37, -82}, {-64, -136}, {-37, -82}, {-142, -26}, {-93, 59}, {-126, -95}},
            {{-82, -88}, {43, -51}, {-35, -109}, {11, 54}, {-4, -16}, {11, 54}, {-35, -109}, {43, -51}, {-82, -88}},
            {{25, 7}, {-86, -111}, {82, -30}, {172, -90}, {-36, 101}, {172, -90}, {82, -30}, {-86, -111}, {25, 7}},
            {{-154, 35}, {-58, 68}, {5, 89}, {26, -50}, {103, 56}, {26, -50}, {5, 89}, {-58, 68}, {-154, 35}},
            {{117, -34}, {70, 66}, {43, -50}, {151, 74}, {-53, 110}, {151, 74}, {43, -50}, {70, 66}, {117, -34}},
            {{-62, -72}, {62, 47}, {170, 63}, {26, 34}, {-74, -2}, {26, 34}, {170, 63}, {62, 47}, {-62, -72}},
            {{122, -69}, {-69, -134}, {4, 25}, {78, 151}, {1, 198}, {78, 151}, {4, 25}, {-69, -134}, {122, -69}},
            {{-107, -19}, {-69, -57}, {-11, 100}, {-64, -74}, {187, 125}, {-64, -74}, {-11, 100}, {-69, -57}, {-107, -19}},
            {{-53, 20}, {12, 139}, {30, -12}, {-139, -79}, {-65, 25}, {-139, -79}, {30, -12}, {12, 139}, {-53, 20}}
    };
    //炮的站位加分
    private static final int[][][] pao_pos = {
            {{-4, 15}, {-42, -59}, {-44, -53}, {61, 124}, {4, 34}, {61, 124}, {-44, -53}, {-42, -59}, {-4, 15}},
            {{38, 6}, {141, -34}, {-63, 22}, {-1, 37}, {113, 41}, {-1, 37}, {-63, 22}, {141, -34}, {38, 6}},
            {{21, -24}, {72, -11}, {58, 82}, {104, 60}, {212, 42}, {104, 60}, {58, 82}, {72, -11}, {21, -24}},
            {{35, 106}, {-194, -36}, {112, 97}, {102, -151}, {-2, -43}, {102, -151}, {112, 97}, {-194, -36}, {35, 106}},
            {{-40, -30}, {-56, 78}, {-82, 32}, {-113, 136}, {246, 6}, {-113, 136}, {-82, 32}, {-56, 78}, {-40, -30}},
            {{-66, 13}, {66, -102}, {2, 40}, {-7, 34}, {79, 112}, {-7, 34}, {2, 40}, {66, -102}, {-66, 13}},
            {{51, -196}, {100, -46}, {20, -34}, {1, 52}, {48, 163}, {1, 52}, {20, -34}, {100, -46}, {51, -196}},
            {{149, -165}, {-13, 84}, {-2, 9}, {67, -107}, {180, 58}, {67, -107}, {-2, 9}, {-13, 84}, {149, -165}},
            {{-48, 100}, {55, -17}, {-2, 16}, {-42, -91}, {88, 51}, {-42, -91}, {-2, 16}, {55, -17}, {-48, 100}},
            {{135, -53}, {225, -12}, {-15, 26}, {189, 144}, {13, 12}, {189, 144}, {-15, 26}, {225, -12}, {135, -53}}
    };
    //兵卒的站位加分
    private static final int[][][] bing_pos = {
            {{0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}},
            {{0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}},
            {{0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}},
            {{-27, -19}, {0, 0}, {-19, -27}, {0, 0}, {56, 34}, {0, 0}, {-19, -27}, {0, 0}, {-27, -19}},
            {{-12, -18}, {0, 0}, {41, -2}, {0, 0}, {39, 61}, {0, 0}, {41, -2}, {0, 0}, {-12, -18}},
            {{-28, 70}, {-53, 93}, {138, 46}, {26, 197}, {73, 52}, {26, 197}, {138, 46}, {-53, 93}, {-28, 70}},
            {{-74, 69}, {-145, 80}, {53, 161}, {2, 210}, {-65, 22}, {2, 210}, {53, 161}, {-145, 80}, {-74, 69}},
            {{91, -92}, {-120, 104}, {18, 41}, {19, 46}, {-121, -67}, {19, 46}, {18, 41}, {-120, 104}, {91, -92}},
            {{-181, -148}, {50, 56}, {182, 34}, {11, 41}, {62, 17}, {11, 41}, {182, 34}, {50, 56}, {-181, -148}},
            {{116, -57}, {85, -85}, {-84, 3}, {35, -33}, {19, -119}, {35, -33}, {-84, 3}, {85, -85}, {116, -57}}
    };
    //士仕的站位加分
    private static final int[][][] shi_pos = {
            {{0, 0}, {0, 0}, {0, 0}, {41, 33}, {0, 0}, {41, 33}, {0, 0}, {0, 0}, {0, 0}},
            {{0, 0}, {0, 0}, {0, 0}, {0, 0}, {38, 113}, {0, 0}, {0, 0}, {0, 0}, {0, 0}},
            {{0, 0}, {0, 0}, {0, 0}, {-152, 47}, {0, 0}, {-152, 47}, {0, 0}, {0, 0}, {0, 0}},
            {{0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}},
            {{0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}},
            {{0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}},
            {{0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}},
            {{0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}},
            {{0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}},
            {{0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}}
    };
    //象相的站位加分
    private static int[][][] xiang_pos = {
            {{0, 0}, {0, 0}, {111, 129}, {0, 0}, {0, 0}, {0, 0}, {111, 129}, {0, 0}, {0, 0}},
            {{0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}},
            {{18, 105}, {0, 0}, {0, 0}, {0, 0}, {206, 148}, {0, 0}, {0, 0}, {0, 0}, {18, 105}},
            {{0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}},
            {{0, 0}, {0, 0}, {-4, 102}, {0, 0}, {0, 0}, {0, 0}, {-4, 102}, {0, 0}, {0, 0}},
            {{0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}},
            {{0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}},
            {{0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}},
            {{0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}},
            {{0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}}
    };
    //将帅的站位加分
    private static int[][][] shuai_pos = {
            {{0, 0}, {0, 0}, {0, 0}, {-74, 42}, {63, 65}, {-74, 42}, {0, 0}, {0, 0}, {0, 0}},
            {{0, 0}, {0, 0}, {0, 0}, {-221, -16}, {-71, 63}, {-221, -16}, {0, 0}, {0, 0}, {0, 0}},
            {{0, 0}, {0, 0}, {0, 0}, {1, -167}, {-36, -59}, {1, -167}, {0, 0}, {0, 0}, {0, 0}},
            {{0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}},
            {{0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}},
            {{0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}},
            {{0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}},
            {{0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}},
            {{0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}},
            {{0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}}
    };
    //ours[i][j][k]：己方i类与己方j类棋子在k(残局/中局)下的配合价值
    private static final int[][][] ours = {
            {{}},
            {{}, {71, 3}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}},
            {{}, {24, 74}, {44, -67}, {0, 0}, {0, 0}, {0, 0}, {0, 0}},
            {{}, {48, 72}, {33, 62}, {-5, -63}, {0, 0}, {0, 0}, {0, 0}},
            {{}, {75, -14}, {31, 44}, {-3, 28}, {-11, 11}, {0, 0}, {0, 0}},
            {{}, {-92, 53}, {27, -9}, {-3, 234}, {44, 88}, {-30, -29}, {0, 0}},
            {{}, {54, 104}, {175, -103}, {106, -64}, {43, -113}, {24, 6}, {2, -59}}
    };
    //ours[i][j][k]：己方i类对上*敌方*j类棋子在k(残局/中局)下的价值
    private static final int[][][] theirs = {
            {},
            {{}, {-35, -46}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}},
            {{}, {-92, 32}, {138, -7}, {0, 0}, {0, 0}, {0, 0}, {0, 0}},
            {{}, {-83, 13}, {-41, 43}, {20, 28}, {0, 0}, {0, 0}, {0, 0}},
            {{}, {-2, 13}, {-57, -118}, {-18, 121}, {70, -58}, {0, 0}, {0, 0}},
            {{}, {-37, 17}, {14, -86}, {38, -24}, {67, 43}, {-21, -42}, {0, 0}},
            {{}, {72, 38}, {6, -79}, {24, -2}, {48, 30}, {30, 14}, {-51, 35}}
    };

    //mobility[i][j]评估第i种棋子在有j个伪合法落点情况下的机动性评分 1.车\2.士\3.炮\4.兵\5.马\6.象
    private static final int[][][] mobility = {
            {},
            {{-875, -2147}, {-79, -3504}, {-1038, -5338}, {1446, -3903}, {2493, -2741}, {2941, -2931}, {3298, -2912}, {3576, -3376}, {4100, -3740}, {5367, -2957}, {6550, -2028}, {7148, -1706}, {8417, -1379}, {8188, -2042}, {8468, -1628}, {8363, -2334}, {4712, -3793}, {4849, -4219}},
            {{3930, 492}, {3841, 319}, {7139, 101}, {7553, -180}, {8138, -30}},
            {{-1024, 4793}, {-520, 4493}, {-706, 3874}, {-896, 3677}, {-28, 3869}, {-391, 3567}, {-39, 3390}, {547, 3802}, {849, 3452}, {871, 3596}, {835, 3469}, {973, 3377}, {441, 2767}, {520, 2360}, {952, 1762}, {450, 1461}, {491, 1421}, {715, 1425}},
            {},
            {{761, -3483}, {3999, -913}, {4654, 22}, {6530, 2107}, {8212, 3227}, {10889, 4221}, {9202, 4012}, {11372, 6965}, {11518, 6946}},
            {{2347, -1590}, {3288, -615}, {3400, -454}, {5904, -237}, {7994, -513}}
    };
    //step[i][j]第i种棋子的第j种走法(用于机动性计算)
    private static final int[][][] step = {
            {},
            {{}, {0, 1}, {0, -1}, {1, 0}, {-1, 0}},
            {{}, {1, -1}, {1, 1}, {-1, -1}, {-1, 1}},
            {{}, {0, 1}, {0, -1}, {1, 0}, {-1, 0}},
            {},
            {{}, {1, 2}, {1, -2}, {-1, 2}, {-1, -2}, {2, 1}, {2, -1}, {-2, -1}, {-2, 1}},
            {{}, {2, 2}, {-2, -2}, {2, -2}, {-2, 2}}
    };

    //CrossBing[i][j][k]:面对敌方有i个士时, 在有j个过河且未达底线的兵的情况下, 在中局/残局的价值
    private static final int[][][] CrossBing = {
            {{-56, -40}, {6, 24}, {11, 7}, {-29, 7}, {-9, -1}, {-4, -7}},
            {{-68, -35}, {10, 12}, {9, 3}, {-16, 9}, {-14, 0}, {-36, -13}},
            {{-79, 5}, {40, -8}, {32, 1}, {-22, 9}, {-20, -16}, {-40, -20}}
    };

    public Evaluate() {
    }

    //判断空头炮
    private boolean HollowPao(Board board, int row, int col, boolean isRed) {
        if (col != 4) return false;
        if (isRed) {
            if (board.getPieceAt(0, 4) == null || !board.getPieceAt(0, 4).getName().equals("将")) return false;
            if (board.getPieceAt(0, 3) == null || !board.getPieceAt(0, 3).getName().equals("士")) return false;
            if (board.getPieceAt(0, 5) == null || !board.getPieceAt(0, 5).getName().equals("士")) return false;
            return (board.hasPieces(row, col, 0, col) == 2);
        } else {
            if (board.getPieceAt(9, 4) == null || !board.getPieceAt(9, 4).getName().equals("帅")) return false;
            if (board.getPieceAt(9, 3) == null || !board.getPieceAt(9, 3).getName().equals("仕")) return false;
            if (board.getPieceAt(9, 5) == null || !board.getPieceAt(9, 5).getName().equals("仕")) return false;
            return (board.hasPieces(row, col, 9, col) == 2);
        }
    }

    //判断炮镇窝心马
    private boolean PaoToMa(Board board, int row, int col, boolean isRed) {
        if (col != 4) return false;
        if (isRed) {
            if (board.getPieceAt(0, 4) == null || !board.getPieceAt(0, 4).getName().equals("将")) return false;
            if (board.getPieceAt(0, 3) == null || !board.getPieceAt(0, 3).getName().equals("士")) return false;
            if (board.getPieceAt(0, 5) == null || !board.getPieceAt(0, 5).getName().equals("士")) return false;
            if (board.getPieceAt(1, 4) == null || board.getPieceAt(1, 4).isRed() || !board.getPieceAt(1, 4).getName().equals("马"))
                return false;
            return (board.hasPieces(row, col, 1, 4) == 3);
        } else {
            if (board.getPieceAt(9, 4) == null || !board.getPieceAt(9, 4).getName().equals("帅")) return false;
            if (board.getPieceAt(9, 3) == null || !board.getPieceAt(9, 3).getName().equals("仕")) return false;
            if (board.getPieceAt(9, 5) == null || !board.getPieceAt(9, 5).getName().equals("仕")) return false;
            if (board.getPieceAt(8, 4) == null || !board.getPieceAt(8, 4).isRed() || !board.getPieceAt(8, 4).getName().equals("马"))
                return false;
            return (board.hasPieces(row, col, 8, 4) == 3);
        }
    }

    //判断沉底炮
    private boolean bottomPao(Board board, int row, int col, boolean isRed) {
        if (isRed) {
            if (row != 0) return false;
            if (board.getPieceAt(0, 4) == null || !board.getPieceAt(0, 4).getName().equals("将")) return false;
            if (board.getPieceAt(1, 4) == null || board.getPieceAt(1, 4).isRed()) return false;
        } else {
            if (row != 9) return false;
            if (board.getPieceAt(9, 4) == null || !board.getPieceAt(9, 4).getName().equals("帅")) return false;
            if (board.getPieceAt(8, 4) == null || !board.getPieceAt(8, 4).isRed()) return false;
        }
        return board.hasPieces(row, col, row, 4) == 2;
    }

    //车的机动性统计
    private int che_mobility(Board board, int row, int col, boolean isRed) {
        int ans = 0;
        for (int i = 1; i <= 4; i++) {
            int dr = step[1][i][0];
            int dc = step[1][i][1];
            int torow = row + dr;
            int tocol = col + dc;
            while (board.isInside(torow, tocol)) {
                if (board.getPieceAt(torow, tocol) != null) {
                    if (board.getPieceAt(torow, tocol).isRed() != isRed) ans++;
                    break;
                }
                torow += dr;
                tocol += dc;
                ans++;
            }
        }
        return ans;
    }

    //士的机动性统计
    private int shi_mobility(Board board, int row, int col, boolean isRed) {
        int ans = 0;
        for (int i = 1; i <= 4; i++) {
            int dr = step[2][i][0];
            int dc = step[2][i][1];
            int torow = row + dr;
            int tocol = col + dc;
            if (!board.isInside(torow, tocol)) continue;
            if (!board.getPieceAt(row, col).canMove(board, row, col, torow, tocol)) continue;
            if (board.getPieceAt(torow, tocol) != null && board.getPieceAt(torow, tocol).isRed() == isRed) continue;
            ans++;
        }
        return ans;
    }

    //炮的机动性
    private int pao_mobility(Board board, int row, int col, boolean isRed) {
        int ans = 0;
        for (int i = 1; i <= 4; i++) {
            int dr = step[3][i][0];
            int dc = step[3][i][1];
            int torow = row + dr;
            int tocol = col + dc;
            while (board.isInside(torow, tocol)) {
                if (board.getPieceAt(torow, tocol) != null) {
                    torow += dr;
                    tocol += dc;
                    while (board.isInside(torow, tocol)) {
                        if (board.getPieceAt(torow, tocol) != null) {
                            if (board.getPieceAt(torow, tocol).isRed() != isRed) ans++;
                            break;
                        }
                        torow += dr;
                        tocol += dc;
                    }
                    break;
                }
                torow += dr;
                tocol += dc;
                ans++;
            }
        }
        return ans;
    }

    //马的机动性
    private int ma_mobility(Board board, int row, int col, boolean isRed) {
        int ans = 0;
        for (int i = 1; i <= 8; i++) {
            int dr = step[5][i][0];
            int dc = step[5][i][1];
            int torow = row + dr;
            int tocol = col + dc;
            if (!board.isInside(torow, tocol)) continue;
            if (!board.getPieceAt(row, col).canMove(board, row, col, torow, tocol)) continue;
            if (board.getPieceAt(torow, tocol) != null && board.getPieceAt(torow, tocol).isRed() == isRed) continue;
            ans++;
        }
        return ans;
    }

    //相的机动性
    private int xiang_mobility(Board board, int row, int col, boolean isRed) {
        int ans = 0;
        for (int i = 1; i <= 4; i++) {
            int dr = step[6][i][0];
            int dc = step[6][i][1];
            int torow = row + dr;
            int tocol = col + dc;
            if (!board.isInside(torow, tocol)) continue;
            if (!board.getPieceAt(row, col).canMove(board, row, col, torow, tocol)) continue;
            if (board.getPieceAt(torow, tocol) != null && board.getPieceAt(torow, tocol).isRed() == isRed) continue;
            ans++;
        }
        return ans;
    }

    // info 评估函数入口
    public int evaluate(Board board, int rule60) {
        //基础子力分值
        int baseRedEG = 0;
        int baseRedMG = 0;
        int baseBlackEG = 0;
        int baseBlackMG = 0;
        //占位分和其它小型附加分
        int redEG = 0;
        int redMG = 0;
        int blackEG = 0;
        int blackMG = 0;
        //机动性附加分(作为合并/100前的原始分数)
        int redMvMG = 0;
        int redMvEG = 0;
        int blackMvMG = 0;
        int blackMvEG = 0;
        //子力配合附加分(作为合并/16之前的原始分数)
        int redCoopMG = 0;
        int redCoopEG = 0;
        int blackCoopMG = 0;
        int blackCoopEG = 0;
        //棋子数量：从1-6分别为红方 1.车\2.士\3.炮\4.兵\5.马\6.象, 7-12为黑方
        int[] cnt = new int[13];
        //统计红黑方过河兵用于后面微调
        int redCross = 0;
        int blackCross = 0;

        //搜索整个棋盘
        //每个棋子进行三步:计算数量、计算基础子力分、计算位置分、计算机动性分
        int movecnt = 0;
        for (int i = 0; i <= 9; i++) {
            for (int j = 0; j <= 8; j++) {
                Piece piece = board.getPieceAt(i, j);
                if (piece == null) continue;
                switch (piece.getName()) {
                    case "车" -> {
                        if (piece.isRed()) {
                            cnt[1]++;
                            baseRedMG += 1373;
                            baseRedEG += 2122;
                            redMG += che_pos[9 - i][j][0];
                            redEG += che_pos[9 - i][j][1];

                            movecnt = che_mobility(board, i, j, true);
                            redMvMG += mobility[1][movecnt][0];
                            redMvEG += mobility[1][movecnt][1];

                        } else {
                            cnt[7]++;
                            baseBlackMG += 1373;
                            baseBlackEG += 2122;
                            blackMG += che_pos[i][j][0];
                            blackEG += che_pos[i][j][1];

                            movecnt = che_mobility(board, i, j, false);
                            blackMvMG += mobility[1][movecnt][0];
                            blackMvEG += mobility[1][movecnt][1];
                        }
                    }
                    case "马" -> {
                        if (piece.isRed()) {
                            cnt[5]++;
                            baseRedMG += 561;
                            baseRedEG += 774;
                            redMG += ma_pos[9 - i][j][0];
                            redEG += ma_pos[9 - i][j][1];

                            movecnt = ma_mobility(board, i, j, true);
                            redMvMG += mobility[5][movecnt][0];
                            redMvEG += mobility[5][movecnt][1];
                        } else {
                            cnt[11]++;
                            baseBlackMG += 561;
                            baseBlackEG += 774;
                            blackMG += ma_pos[i][j][0];
                            blackEG += ma_pos[i][j][1];

                            movecnt = ma_mobility(board, i, j, false);
                            blackMvMG += mobility[5][movecnt][0];
                            blackMvEG += mobility[5][movecnt][1];
                        }
                    }
                    case "炮" -> {
                        if (piece.isRed()) {
                            cnt[3]++;
                            baseRedMG += 768;
                            baseRedEG += 753;
                            redMG += pao_pos[9 - i][j][0];
                            redEG += pao_pos[9 - i][j][1];

                            movecnt = pao_mobility(board, i, j, true);
                            redMvMG += mobility[3][movecnt][0];
                            redMvEG += mobility[3][movecnt][1];

                            //计算红方空头炮、窝心马、沉底炮加成
                            if (HollowPao(board, i, j, true)) {
                                redMG += 85;
                                redEG += 91;
                            }
                            if (PaoToMa(board, i, j, true)) {
                                redMG += 50;
                                redEG += 53;
                            }
                            if (bottomPao(board, i, j, true)) {
                                redMG += 18;
                                redEG += 8;
                            }
                        } else {
                            cnt[9]++;
                            baseBlackMG += 768;
                            baseBlackEG += 753;
                            blackMG += pao_pos[i][j][0];
                            blackEG += pao_pos[i][j][1];

                            movecnt = pao_mobility(board, i, j, false);
                            blackMvMG += mobility[3][movecnt][0];
                            blackMvEG += mobility[3][movecnt][1];

                            //计算黑方空头炮、窝心马、沉底炮加成
                            if (HollowPao(board, i, j, false)) {
                                blackMG += 85;
                                blackEG += 91;
                            }
                            if (PaoToMa(board, i, j, false)) {
                                blackMG += 50;
                                blackEG += 53;
                            }
                            if (bottomPao(board, i, j, false)) {
                                blackMG += 18;
                                blackEG += 8;
                            }
                        }
                    }
                    case "兵" -> {
                        cnt[4]++;
                        baseRedMG += 127;
                        baseRedEG += 182;
                        redMG += bing_pos[9 - i][j][0];
                        redEG += bing_pos[9 - i][j][1];
                        if (i >= 1 && i <= 4) redCross++;
                    }
                    case "卒" -> {
                        cnt[10]++;
                        baseBlackMG += 127;
                        baseBlackEG += 182;
                        blackMG += bing_pos[i][j][0];
                        blackEG += bing_pos[i][j][1];
                        if (i >= 5 && i <= 8) blackCross++;
                    }
                    case "仕" -> {
                        cnt[2]++;
                        baseRedMG += 104;
                        baseRedEG += 121;
                        redMG += shi_pos[9 - i][j][0];
                        redEG += shi_pos[9 - i][j][1];

                        movecnt = shi_mobility(board, i, j, true);
                        redMvMG += mobility[2][movecnt][0];
                        redMvEG += mobility[2][movecnt][1];
                    }
                    case "士" -> {
                        cnt[8]++;
                        baseBlackMG += 104;
                        baseBlackEG += 121;
                        blackMG += shi_pos[i][j][0];
                        blackEG += shi_pos[i][j][1];

                        movecnt = shi_mobility(board, i, j, false);
                        blackMvMG += mobility[2][movecnt][0];
                        blackMvEG += mobility[2][movecnt][1];
                    }
                    case "相" -> {
                        cnt[6]++;
                        baseRedMG += 167;
                        baseRedEG += 72;
                        redMG += xiang_pos[9 - i][j][0];
                        redEG += xiang_pos[9 - i][j][1];

                        movecnt = xiang_mobility(board, i, j, true);
                        redMvMG += mobility[6][movecnt][0];
                        redMvEG += mobility[6][movecnt][1];
                    }
                    case "象" -> {
                        cnt[12]++;
                        baseBlackMG += 167;
                        baseBlackEG += 72;
                        blackMG += xiang_pos[i][j][0];
                        blackEG += xiang_pos[i][j][1];

                        movecnt = xiang_mobility(board, i, j, false);
                        blackMvMG += mobility[6][movecnt][0];
                        blackMvEG += mobility[6][movecnt][1];
                    }
                    case "帅" -> {
                        redMG += shuai_pos[9 - i][j][0];
                        redEG += shuai_pos[9 - i][j][1];
                    }
                    case "将" -> {
                        blackMG += shuai_pos[i][j][0];
                        blackEG += shuai_pos[i][j][1];
                    }
                }
            }
        }

        //计算子力配合分数
        //红方配合分
        for (int i = 1; i <= 6; i++) {
            if (cnt[i] == 0) continue;
            int sumMG = 0;
            int sumEG = 0;
            for (int j = 1; j < i; j++) {
                sumMG += ours[i][j][0] * cnt[j] + theirs[i][j][0] * cnt[j + 6];
                sumEG += ours[i][j][1] * cnt[j] + theirs[i][j][1] * cnt[j + 6];
            }
            redCoopMG += cnt[i] * (ours[i][i][0] * cnt[i] + sumMG);
            redCoopEG += cnt[i] * (ours[i][i][1] * cnt[i] + sumEG);
        }

        //黑方配合分
        for (int i = 1; i <= 6; i++) {
            if (cnt[i + 6] == 0) continue;
            int sumMG = 0;
            int sumEG = 0;
            for (int j = 1; j < i; j++) {
                sumMG += ours[i][j][0] * cnt[j + 6] + theirs[i][j][0] * cnt[j];
                sumEG += ours[i][j][1] * cnt[j + 6] + theirs[i][j][1] * cnt[j];
            }
            blackCoopMG += cnt[i + 6] * (ours[i][i][0] * cnt[i + 6] + sumMG);
            blackCoopEG += cnt[i + 6] * (ours[i][i][1] * cnt[i + 6] + sumEG);
        }

        //防守微调：计算士象全
        if (cnt[2] == 2 && cnt[6] == 2) {
            redMG += 24;
            redEG -= 43;
        }
        if (cnt[8] == 2 && cnt[12] == 2) {
            blackMG += 24;
            blackEG -= 43;
        }

        //进攻微调：计算过河兵的价值
        redMG += CrossBing[cnt[8]][redCross][0];
        redEG += CrossBing[cnt[8]][redCross][1];
        blackMG += CrossBing[cnt[2]][blackCross][0];
        blackEG += CrossBing[cnt[2]][blackCross][1];


        //使用中局基础子力计算阶段
        int phase = (baseRedMG + baseBlackMG - 3915) * 128 / 11343;
        //计算最后的得分差值
        int diffMG = 0;
        int diffEG = 0;
        diffMG = (baseRedMG - baseBlackMG) + (redMG - blackMG) + (redMvMG - blackMvMG) / 100 + (redCoopMG - blackCoopMG) / 16;
        diffEG = (baseRedEG - baseBlackEG) + (redEG - blackEG) + (redMvEG - blackMvEG) / 100 + (redCoopEG - blackCoopEG) / 16;
        //阶段混合、和棋衰减、统计答案
        int mix_score = (diffMG * phase + diffEG * (128 - phase)) / 128;
        int final_score = mix_score * (118 - rule60) / 118;
        return final_score;
    }

    //info 简易评估函数，用于启发式搜索
    public int simpleEvaluate(Board board, int rule60) {
        int baseRedEG = 0;
        int baseRedMG = 0;
        int baseBlackEG = 0;
        int baseBlackMG = 0;
        int redMG = 0;
        int redEG = 0;
        int blackMG = 0;
        int blackEG = 0;
        for (int i = 0; i <= 9; i++) {
            for (int j = 0; j <= 8; j++) {
                Piece piece = board.getPieceAt(i, j);
                if (piece == null) continue;
                switch (piece.getName()) {
                    case "车" -> {
                        if (piece.isRed()) {
                            baseRedMG += 1373;
                            baseRedEG += 2122;
                            redMG += che_pos[9 - i][j][0];
                            redEG += che_pos[9 - i][j][1];
                        } else {
                            baseBlackMG += 1373;
                            baseBlackEG += 2122;
                            blackMG += che_pos[i][j][0];
                            blackEG += che_pos[i][j][1];
                        }
                    }
                    case "马" -> {
                        if (piece.isRed()) {
                            baseRedMG += 561;
                            baseRedEG += 774;
                            redMG += ma_pos[9 - i][j][0];
                            redEG += ma_pos[9 - i][j][1];
                        } else {
                            baseBlackMG += 561;
                            baseBlackEG += 774;
                            blackMG += ma_pos[i][j][0];
                            blackEG += ma_pos[i][j][1];
                        }
                    }
                    case "炮" -> {
                        if (piece.isRed()) {
                            baseRedMG += 768;
                            baseRedEG += 753;
                            redMG += pao_pos[9 - i][j][0];
                            redEG += pao_pos[9 - i][j][1];
                        } else {
                            baseBlackMG += 768;
                            baseBlackEG += 753;
                            blackMG += pao_pos[i][j][0];
                            blackEG += pao_pos[i][j][1];
                        }
                    }
                    case "兵" -> {
                        baseRedMG += 127;
                        baseRedEG += 182;
                        redMG += bing_pos[9 - i][j][0];
                        redEG += bing_pos[9 - i][j][1];
                    }
                    case "卒" -> {
                        baseBlackMG += 127;
                        baseBlackEG += 182;
                        blackMG += bing_pos[i][j][0];
                        blackEG += bing_pos[i][j][1];
                    }
                    case "仕" -> {
                        baseRedMG += 104;
                        baseRedEG += 121;
                        redMG += shi_pos[9 - i][j][0];
                        redEG += shi_pos[9 - i][j][1];
                    }
                    case "士" -> {
                        baseBlackMG += 104;
                        baseBlackEG += 121;
                        blackMG += shi_pos[i][j][0];
                        blackEG += shi_pos[i][j][1];
                    }
                    case "相" -> {
                        baseRedMG += 167;
                        baseRedEG += 72;
                        redMG += xiang_pos[9 - i][j][0];
                        redEG += xiang_pos[9 - i][j][1];
                    }
                    case "象" -> {
                        baseBlackMG += 167;
                        baseBlackEG += 72;
                        blackMG += xiang_pos[i][j][0];
                        blackEG += xiang_pos[i][j][1];
                    }
                    case "帅" -> {
                        redMG += shuai_pos[9 - i][j][0];
                        redEG += shuai_pos[9 - i][j][1];
                    }
                    case "将" -> {
                        blackMG += shuai_pos[i][j][0];
                        blackEG += shuai_pos[i][j][1];
                    }
                }
            }
        }
        //使用中局基础子力计算阶段
        int phase = (baseRedMG + baseBlackMG - 3915) * 128 / 11343;
        //计算最后的得分差值
        int diffMG = 0;
        int diffEG = 0;
        diffMG = (baseRedMG - baseBlackMG) + (redMG - blackMG);
        diffEG = (baseRedEG - baseBlackEG) + (redEG - blackEG);
        //阶段混合、和棋衰减、统计答案
        int mix_score = (diffMG * phase + diffEG * (128 - phase)) / 128;
        int final_score = mix_score * (118 - rule60) / 118;
        return final_score;
    }
}
