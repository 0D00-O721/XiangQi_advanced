package edu.sustech.xiangqi.model;

// 保存棋盘数据，并统一检查和执行移动

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

    public boolean JiangtoShuai() {
        for (int i = 7; i <= 9; i++) {
            for (int j = 3; j <= 5; j++) {
                Piece piece = getPieceAt(i, j);
                if (piece != null && piece.getName().equals("帅")) {
                    piece = getPieceAt(0, j);
                    return hasPieces(i, j, 0, j) == 2 && piece != null && piece.getName().equals("将");
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

        //检查走完后将帅是否无遮挡面对面
        pieces[fromRow][fromCol] = null;
        pieces[toRow][toCol] = piece;
        boolean flag = JiangtoShuai();
        pieces[fromRow][fromCol] = piece;
        pieces[toRow][toCol] = topiece;
        if (flag) return false;

        // TODO：检查是否将军


        //切换棋子状态
        pieces[fromRow][fromCol] = null;
        pieces[toRow][toCol] = piece;

        //改成黑方回合
        Redturn = !Redturn;
        return true;
    }
}
