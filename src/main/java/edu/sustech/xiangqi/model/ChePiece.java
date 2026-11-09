package edu.sustech.xiangqi.model;

// info 车类：只能走直线，步数任意，路径中间不能有遮挡

public class ChePiece extends Piece {
    public ChePiece(boolean isRed) {
        super("车", isRed);
    }

    @Override
    public boolean canMove(Board board, int fromRow, int fromCol, int toRow, int toCol) {
        //不能原地走
        if (fromCol == toCol && fromRow == toRow) return false;
        //不能斜着走
        if (Math.abs(fromCol - toCol) > 0 && Math.abs(fromRow - toRow) > 0) return false;
        //判断往哪走,路径上是否有阻挡
        if (fromRow == toRow) {
            int k = toCol - fromCol;
            int lth = Math.abs(k);
            int dc = k / lth;
            int col = fromCol;
            for (int i = 1; i <= lth - 1; i++) {
                col += dc;
                if (board.getPieceAt(fromRow, col) != null) return false;
            }
        } else {
            int k = toRow - fromRow;
            int lth = Math.abs(k);
            int dr = k / lth;
            int row = fromRow;
            for (int i = 1; i <= lth - 1; i++) {
                row += dr;
                if (board.getPieceAt(row, fromCol) != null) return false;
            }
        }
        //都不违背则返回真
        return true;
    }

}
