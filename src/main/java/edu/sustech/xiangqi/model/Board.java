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

    public boolean tryMove(int fromRow, int fromCol, int toRow, int toCol) {
        // TODO：实现下面注释的功能
        // info:该函数返回一个int类型的值代表当前移动操作的响应类型:
        // info -2:已经将死对方 -1:成功将军对方 0:可以移动且是一次常规移动 1:原地 2:空棋子 3:出界 4:不是你的回合
        //  5:移动目标位置是你的棋子 6:棋子没有这个走法 7:将帅不能面对面 8:不能送将

        if (!isInside(fromRow, fromCol) || !isInside(toRow, toCol)) {
            return false;
        }
        if (fromRow == toRow && fromCol == toCol) {
            return false;
        }

        Piece piece = getPieceAt(fromRow, fromCol);
        Piece topiece = getPieceAt(toRow, toCol);

        //空棋子和敌方棋子都不能动
        if (piece == null || piece.isRed() != Redturn) {
            return false;
        }

        //区分己方占用与敌方棋子，并实现吃子
        if (topiece != null && topiece.isRed() == Redturn) return false;

        if (!piece.canMove(this, fromRow, fromCol, toRow, toCol)) {
            return false;
        }

        //模拟移动并进行一系列操作
        pieces[fromRow][fromCol] = null;
        pieces[toRow][toCol] = piece;

        //检查走完后将帅是否无遮挡面对面
        if (JiangtoShuai(getJiangShuai(true).x, getJiangShuai(true).y)) {
            pieces[fromRow][fromCol] = piece;
            pieces[toRow][toCol] = topiece;
            return false;
        }

        // TODO：检查是否将军

        //1.检查己方移动是否会导致对方有棋子能将军自己
        if (isJiangjun(Redturn)) {
            pieces[fromRow][fromCol] = piece;
            pieces[toRow][toCol] = topiece;
            return false;
        }

        //2.检查己方这一步是否将军对面
        if (isJiangjun(!Redturn)) {
            Redturn = !Redturn;
            return true;
        }

        //3.检查是否将死对面


        //改成黑方回合
        Redturn = !Redturn;
        return true;
    }
}
