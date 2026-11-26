package edu.sustech.xiangqi.search;

//info:搜索函数

import edu.sustech.xiangqi.model.Board;
import edu.sustech.xiangqi.model.Piece;

public class Searcher {
    private final Board source;
    private Board board;
    private final Evaluate evaluator;
    private int fromRow = 0;
    private int fromCol = 0;
    private int toRow = 0;
    private int toCol = 0;
    private static int maxDep = 0;

    //第i种棋子的第j个移动方向 (1.车\2.士\3.炮\4.兵\5.马\6.象\7.将)
    private static final int[][][] step = {
            {},
            {{}, {0, 1}, {0, -1}, {1, 0}, {-1, 0}},
            {{}, {1, -1}, {1, 1}, {-1, -1}, {-1, 1}},
            {{}, {0, 1}, {0, -1}, {1, 0}, {-1, 0}},
            {{}, {-1, 0}, {0, 1}, {0, -1}, {1, 0}},
            {{}, {1, 2}, {1, -2}, {-1, 2}, {-1, -2}, {2, 1}, {2, -1}, {-2, -1}, {-2, 1}},
            {{}, {2, 2}, {-2, -2}, {2, -2}, {-2, 2}},
            {{}, {0, 1}, {0, -1}, {1, 0}, {-1, 0}}
    };

    public static int getMaxDep() {
        return maxDep;
    }

    public static void setMaxDep(int maxDep) {
        Searcher.maxDep = maxDep;
    }

    public int getFromRow() {
        return fromRow;
    }

    public void setFromRow(int fromRow) {
        this.fromRow = fromRow;
    }

    public int getFromCol() {
        return fromCol;
    }

    public void setFromCol(int fromCol) {
        this.fromCol = fromCol;
    }

    public int getToRow() {
        return toRow;
    }

    public void setToRow(int toRow) {
        this.toRow = toRow;
    }

    public int getToCol() {
        return toCol;
    }

    public void setToCol(int toCol) {
        this.toCol = toCol;
    }

    public Searcher(Board board, Evaluate evaluate) {
        this.source = board;
        this.board = board;
        this.evaluator = evaluate;
    }

    private void updateMove(int fromRow, int fromCol, int toRow, int toCol) {
        this.toRow = toRow;
        this.toCol = toCol;
        this.fromRow = fromRow;
        this.fromCol = fromCol;
    }

    private boolean cut(boolean isRed, int ans, int alpha) {
        //如果搜出来的结果已经超出了约束，上一层就绝不会选这一支，直接掐死
        if (!isRed) {
            return (ans <= alpha);
        } else {
            return (ans >= alpha);
        }
    }

    private int updateBeta(boolean isRed, int beta, int ans, int dep, int i, int j, int dr, int dc) {
        if (!isRed) {
            if (ans < beta) {
                if (dep == 0) updateMove(i, j, i + dr, j + dc);
                return ans;
            }
        } else {
            if (ans > beta) {
                if (dep == 0) updateMove(i, j, i + dr, j + dc);
                return ans;
            }
        }
        return beta;
    }

    //info: 搜索函数v1主体,暂时采用自制的α-β剪枝
    // alpha:上一个节点(敌方)已经搜到的最优情况, 用于约束这个节点(己方)
    // beta:己方节点及其历史己方节点搜索到的最优情况, 用于传给下一个节点(敌方)做下一个节点的约束
    public int dfs(int alpha, int beta, int dep, boolean isRed) {
        //如果上一次移动的发动者被将军了，它的那步就不能走
        if (board.isJiangjun(!isRed)) return (!isRed) ? -1000001 : 1000001;
        //如果上一次的移动造成了飞将，也不能走
        if (board.JiangtoShuai()) return (!isRed) ? -1000001 : 1000001;
        //如果深度抵达限制，则返回局面评分
        if (dep >= maxDep) {
            return evaluator.evaluate(board, 0);
        }
        //全盘搜索己方棋子
        boolean moveCnt = false;
        for (int i = 0; i <= 9; i++) {
            for (int j = 0; j <= 8; j++) {
                Piece piece = board.getPieceAt(i, j);
                Piece atepiece;
                if (piece == null) continue;
                //dr/dc:棋子一步的偏移
                int dr = 0;
                int dc = 0;
                //idx:棋子种类; lth_l:移动数组取值左区间; lth_r:右区间
                int idx = 0;
                int lth_l = 1;
                int lth_r = 0;
                boolean isSpecial = true;
                switch (piece.getName()) {
                    case "帅", "将" -> {
                        if (piece.isRed() == isRed) {
                            isSpecial = false;
                            idx = 7;
                            lth_r = 4;
                        }
                    }
                    case "仕", "士" -> {
                        if (piece.isRed() == isRed) {
                            isSpecial = false;
                            idx = 2;
                            lth_r = 4;
                        }
                    }
                    case "相", "象" -> {
                        if (piece.isRed() == isRed) {
                            isSpecial = false;
                            idx = 6;
                            lth_r = 4;
                        }
                    }
                    case "马" -> {
                        if (piece.isRed() == isRed) {
                            isSpecial = false;
                            idx = 5;
                            lth_r = 8;
                        }
                    }
                    case "兵", "卒" -> {
                        if (piece.isRed() == isRed) {
                            isSpecial = false;
                            idx = 4;
                            if (isRed) {
                                lth_l = 1;
                                lth_r = (i <= 4) ? 3 : 1;
                            } else {
                                lth_l = (i >= 5) ? 2 : 4;
                                lth_r = 4;
                            }

                        }
                    }
                    case "车" -> {
                        if (piece.isRed() == isRed) {
                            for (int k = 1; k <= 4; k++) {
                                boolean meetPiece = false;
                                int row = i;
                                int col = j;
                                dr = step[1][k][0];
                                dc = step[1][k][1];
                                while (board.isInside(row + dr, col + dc)) {
                                    if (board.getPieceAt(row + dr, col + dc) != null) {
                                        meetPiece = true;
                                        if (board.getPieceAt(row + dr, col + dc).isRed() == isRed) break;
                                    }
                                    atepiece = board.doMove(i, j, row + dr, col + dc, piece);
                                    if (atepiece != null && (atepiece.getName().equals("帅") || atepiece.getName().equals("将"))) {
                                        board.undoMove(i, j, row + dr, col + dc, atepiece);
                                        return isRed ? 500000 : -500000;
                                    }
                                    int ans = dfs(beta, alpha, dep + 1, !isRed);
                                    if (Math.abs(ans) != 1000001) moveCnt = true;
                                    board.undoMove(i, j, row + dr, col + dc, atepiece);
                                    if (cut(isRed, ans, alpha)) return isRed ? 1000000 : -1000000;
                                    beta = updateBeta(isRed, beta, ans, dep, i, j, row + dr - i, col + dc - j);
                                    if (meetPiece) break;
                                    row += dr;
                                    col += dc;
                                }
                            }
                        }
                    }
                    case "炮" -> {
                        if (piece.isRed() == isRed) {
                            for (int k = 1; k <= 4; k++) {
                                int row = i;
                                int col = j;
                                dr = step[3][k][0];
                                dc = step[3][k][1];
                                while (board.isInside(row + dr, col + dc)) {
                                    if (board.getPieceAt(row + dr, col + dc) != null) {
                                        row += dr;
                                        col += dc;
                                        while (board.isInside(row + dr, col + dc)) {
                                            if (board.getPieceAt(row + dr, col + dc) != null) {
                                                if (board.getPieceAt(row + dr, col + dc).isRed() == isRed) break;
                                                atepiece = board.doMove(i, j, row + dr, col + dc, piece);
                                                if (atepiece != null && (atepiece.getName().equals("帅") || atepiece.getName().equals("将"))) {
                                                    board.undoMove(i, j, row + dr, col + dc, atepiece);
                                                    return isRed ? 500000 : -500000;
                                                }
                                                int ans = dfs(beta, alpha, dep + 1, !isRed);
                                                if (Math.abs(ans) != 1000001) moveCnt = true;
                                                board.undoMove(i, j, row + dr, col + dc, atepiece);
                                                if (cut(isRed, ans, alpha)) return isRed ? 1000000 : -1000000;
                                                beta = updateBeta(isRed, beta, ans, dep, i, j, row + dr - i, col + dc - j);
                                                break;
                                            }
                                            row += dr;
                                            col += dc;
                                        }
                                        break;

                                    }
                                    atepiece = board.doMove(i, j, row + dr, col + dc, piece);
                                    if (atepiece != null && (atepiece.getName().equals("帅") || atepiece.getName().equals("将"))) {
                                        board.undoMove(i, j, row + dr, col + dc, atepiece);
                                        return isRed ? 500000 : -500000;
                                    }
                                    int ans = dfs(beta, alpha, dep + 1, !isRed);
                                    if (Math.abs(ans) != 1000001) moveCnt = true;
                                    board.undoMove(i, j, row + dr, col + dc, atepiece);
                                    if (cut(isRed, ans, alpha)) return isRed ? 1000000 : -1000000;
                                    beta = updateBeta(isRed, beta, ans, dep, i, j, row + dr - i, col + dc - j);
                                    row += dr;
                                    col += dc;
                                }
                            }
                        }
                    }
                }
                if (isSpecial) continue;
                //遍历每一种走法
                for (int k = lth_l; k <= lth_r; k++) {
                    dr = step[idx][k][0];
                    dc = step[idx][k][1];
                    if (!board.isInside(i + dr, j + dc)) continue;
                    if (!piece.canMove(board, i, j, i + dr, j + dc)) continue;
                    if (board.getPieceAt(i + dr, j + dc) != null && board.getPieceAt(i + dr, j + dc).isRed() == isRed)
                        continue;
                    //确定可以走之后，先提取出目标位置的子，然后模拟走
                    atepiece = board.doMove(i, j, i + dr, j + dc, piece);
                    //如果吃掉了敌将就返回一个绝对值小于非法走法的极值
                    if (atepiece != null && (atepiece.getName().equals("帅") || atepiece.getName().equals("将"))) {
                        board.undoMove(i, j, i + dr, j + dc, atepiece);
                        return isRed ? 500000 : -500000;
                    }
                    //带着这个新局面进入下一层进行搜索
                    //注意：这里的alpha和beta发生了互换，这是因为这一层己方节点的目前最优解beta作为的是下一层敌方节点搜索的约束alpha2
                    //而上一层敌方节点的约束alpha可以传到下一层的敌方节点让其继承为beta2以达成记忆化的效果
                    int ans = dfs(beta, alpha, dep + 1, !isRed);
                    if (Math.abs(ans) != 1000001) moveCnt = true;
                    //搜索完毕，撤销移动
                    board.undoMove(i, j, i + dr, j + dc, atepiece);
                    //剪枝与更新
                    if (cut(isRed, ans, alpha)) return isRed ? 1000000 : -1000000;
                    beta = updateBeta(isRed, beta, ans, dep, i, j, dr, dc);
                }
            }
        }
        //如果一步都没走（被将死）返回一个极值
        if (!moveCnt) return isRed ? -500000 : 500000;
        return beta;
    }

    public void AB_dfs() {
        board = source.copy();
        updateMove(0, 0, 0, 0);
        //黑方搜索
        dfs(-1000000, 1000000, 0, false);
    }

    public void output() {
        System.out.printf("移动成功！当前局面评分(正红负黑): %d\n", evaluator.evaluate(source, 0));
    }
}
