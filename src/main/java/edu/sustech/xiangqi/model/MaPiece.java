package edu.sustech.xiangqi.model;

// info 马：只能走日字对角而且正前方不能有任何阻挡

public class MaPiece extends Piece {
    public MaPiece(boolean isRed) {
        super("马", isRed);
    }

    @Override
    public boolean canMove(Board board, int fromRow, int fromCol, int toRow, int toCol) {
        int dr = toRow - fromRow;
        int dc = toCol - fromCol;
        //不能原地走或者直线走
        if (dr == 0 || dc == 0) return false;
        //必须走日字
        if (Math.abs(dr) + Math.abs(dc) != 3) return false;
        //前方不能有阻挡
        if (Math.abs(dr) == 2 && board.getPieceAt(fromRow + dr / 2, fromCol) != null) return false;
        if (Math.abs(dc) == 2 && board.getPieceAt(fromRow, fromCol + dc / 2) != null) return false;
        return true;
    }
}
