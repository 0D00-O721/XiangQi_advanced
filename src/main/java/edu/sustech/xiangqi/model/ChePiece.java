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
            int dc = (toCol - fromCol) / Math.abs(toCol - fromCol);
            return board.hasPieces(fromRow, fromCol, toRow, toCol - dc) == 1;
        } else {
            int dr = (toRow - fromRow) / Math.abs(toRow - fromRow);
            return board.hasPieces(fromRow, fromCol, toRow - dr, toCol) == 1;
        }
    }

}
