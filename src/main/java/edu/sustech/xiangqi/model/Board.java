package edu.sustech.xiangqi.model;

// 保存棋盘数据，并统一检查和执行移动

import java.awt.*;

public class Board {
    // 定义棋盘大小
    public static final int ROWS = 10;
    public static final int COLS = 9;
    public boolean Redturn = true;

    // 存储棋子位置
    private final Piece[][] pieces = new Piece[ROWS][COLS];

    public Board() {
        initializePieces();
    }

    //统计一个区域内有多少棋子
    public int hasPieces(int fromRow, int fromCol, int toRow, int toCol) {
        int cnt = 0;
        if (fromRow > toRow) {
            int tmp = fromRow;
            fromRow = toRow;
            toRow = tmp;
        }
        if (fromCol > toCol) {
            int tmp = fromCol;
            fromCol = toCol;
            toCol = tmp;
        }
        for (int i = fromRow; i <= toRow; i++) {
            for (int j = fromCol; j <= toCol; j++) {
                if (getPieceAt(i, j) != null) cnt++;
            }
        }
        return cnt;
    }

    //获取将/帅的坐标
    public Point getJiangShuai(boolean isRed) {
        int irow = isRed ? 7 : 0;
        int frow = isRed ? 9 : 2;
        String s = isRed ? "帅" : "将";
        for (int i = irow; i <= frow; i++)
            for (int j = 3; j <= 5; j++)
                if (getPieceAt(i, j) != null && getPieceAt(i, j).getName().equals(s))
                    return new Point(i, j);
        return null;
    }

    //判断将帅是否面对面
    public boolean JiangtoShuai(int row, int col) {
        //从红帅位置网上扫描
        for (int i = row - 1; i >= 0; i--) {
            Piece piece = getPieceAt(i, col);
            //如果有阻挡，直接否
            if (piece != null && !piece.getName().equals("将")) return false;
            //如果碰到将，返回是
            if (piece != null && piece.getName().equals("将")) return true;
        }
        //循环结束说明一路无阻挡
        return false;
    }

    //判断一方是否[被]将军
    public boolean isJiangjun(boolean isRed) {
        int Jrow = getJiangShuai(isRed).x;
        int Jcol = getJiangShuai(isRed).y;
        for (int i = 0; i <= 9; i++) {
            for (int j = 0; j <= 8; j++) {
                Piece from = getPieceAt(i, j);
                if (from != null && from.isRed() != isRed && from.canMove(this, i, j, Jrow, Jcol)) {
                    return true;
                }
            }
        }
        return false;
    }

    //isFinish：判断当前回合放是否将死对方
    public boolean isFinish(boolean isRed) {
        //先扫描所有敌方棋子
        for (int i = 0; i <= 9; i++) {
            for (int j = 0; j <= 8; j++) {
                Piece piece = getPieceAt(i, j);
                if (piece != null && piece.isRed() != isRed) {
                    //扫描棋盘确定该棋子能走的地方
                    for (int toRow = 0; toRow <= 9; toRow++) {
                        for (int toCol = 0; toCol <= 8; toCol++) {
                            //获取目标位置的棋子
                            Piece topiece = getPieceAt(toRow, toCol);
                            if (toRow == i && toCol == j) continue;
                            if (topiece != null && topiece.isRed() != isRed) continue;
                            if (!piece.canMove(this, i, j, toRow, toCol)) continue;
                            //移动条件满足，模拟并判断移动该步是否能解除将军
                            boolean flag = false;
                            boolean JtS = false;
                            pieces[i][j] = null;
                            pieces[toRow][toCol] = piece;
                            //不要忘了检查将帅相对
                            if (JiangtoShuai(getJiangShuai(true).x, getJiangShuai(true).y)) JtS = true;
                            //判断对方是否解将
                            if (!isJiangjun(!isRed)) flag = true;
                            pieces[i][j] = piece;
                            pieces[toRow][toCol] = topiece;
                            if (JtS) continue;
                            if (flag) return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    //重开
    public void reset() {
        for (int i = 0; i < ROWS; i++)
            for (int j = 0; j < COLS; j++) pieces[i][j] = null;
        Redturn = true;
        initializePieces();
    }

    private void initializePieces() {
        //初始化红方棋子
        pieces[9][3] = new ShiPiece(true);
        pieces[9][4] = new ShuaiPiece(true);
        pieces[9][5] = new ShiPiece(true);
        pieces[9][0] = new ChePiece(true);
        pieces[9][8] = new ChePiece(true);
        pieces[9][1] = new MaPiece(true);
        pieces[9][7] = new MaPiece(true);
        pieces[9][2] = new XiangPiece(true);
        pieces[9][6] = new XiangPiece(true);
        pieces[7][1] = new PaoPiece(true);
        pieces[7][7] = new PaoPiece(true);
        pieces[6][0] = new BingPiece(true);
        pieces[6][2] = new BingPiece(true);
        pieces[6][4] = new BingPiece(true);
        pieces[6][6] = new BingPiece(true);
        pieces[6][8] = new BingPiece(true);

        //初始化黑方棋子
        pieces[0][3] = new ShiPiece(false);
        pieces[0][4] = new ShuaiPiece(false);
        pieces[0][5] = new ShiPiece(false);
        pieces[0][0] = new ChePiece(false);
        pieces[0][8] = new ChePiece(false);
        pieces[0][1] = new MaPiece(false);
        pieces[0][7] = new MaPiece(false);
        pieces[0][2] = new XiangPiece(false);
        pieces[0][6] = new XiangPiece(false);
        pieces[2][1] = new PaoPiece(false);
        pieces[2][7] = new PaoPiece(false);
        pieces[3][0] = new BingPiece(false);
        pieces[3][2] = new BingPiece(false);
        pieces[3][4] = new BingPiece(false);
        pieces[3][6] = new BingPiece(false);
        pieces[3][8] = new BingPiece(false);
    }

    public boolean isInside(int row, int col) {
        return row >= 0 && row < ROWS && col >= 0 && col < COLS;
    }

    public Piece getPieceAt(int row, int col) {
        if (!isInside(row, col)) {
            return null;
        }
        return pieces[row][col];
    }

    public int tryMove(int fromRow, int fromCol, int toRow, int toCol) {
        // info:该函数返回一个int类型的值代表当前移动操作的响应类型:
        //      -2:已经将死对方 -1:成功将军对方 0:可以移动且是一次常规移动 1:出界 2:原地 3:空棋子 4:不是你的回合
        //      5:移动目标位置是你的棋子 6:棋子没有这个走法 7:将帅不能面对面 8:不能送将

        //类型1：出界
        if (!isInside(fromRow, fromCol) || !isInside(toRow, toCol)) return 1;

        //类型2：原地
        if (fromRow == toRow && fromCol == toCol) return 2;

        Piece piece = getPieceAt(fromRow, fromCol);
        Piece topiece = getPieceAt(toRow, toCol);

        //类型3：空棋子
        if (piece == null) return 3;

        //类型4：不是己方回合
        if (piece.isRed() != Redturn) return 4;

        //类型5：不能吃自己的子
        if (topiece != null && topiece.isRed() == Redturn) return 5;

        //类型6：棋子没有这个走法
        if (!piece.canMove(this, fromRow, fromCol, toRow, toCol)) return 6;


        //模拟移动并进行一系列操作
        pieces[fromRow][fromCol] = null;
        pieces[toRow][toCol] = piece;

        //检查走完后将帅是否无遮挡面对面
        if (JiangtoShuai(getJiangShuai(true).x, getJiangShuai(true).y)) {
            pieces[fromRow][fromCol] = piece;
            pieces[toRow][toCol] = topiece;
            return 7;
        }

        //进入将军判断
        //1.检查己方移动是否会导致对方有棋子能将军自己（不能送将）
        if (isJiangjun(Redturn)) {
            pieces[fromRow][fromCol] = piece;
            pieces[toRow][toCol] = topiece;
            return 8;
        }

        //2.检查己方这一步是否将军对面
        if (isJiangjun(!Redturn)) {
            //如果将军则判断是否将死
            if (isFinish(Redturn)) {
                Redturn = !Redturn;
                return -2;
            }
            Redturn = !Redturn;
            return -1;
        }

        //改成黑方回合
        Redturn = !Redturn;
        return 0;
    }
}
