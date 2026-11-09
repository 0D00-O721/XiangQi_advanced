package edu.sustech.xiangqi.model;

// info 马：只能走日字对角而且正前方不能有任何阻挡

public class MaPiece extends Piece {
    public MaPiece(boolean isRed) {
        super("马", isRed);
    }

    @Override
    public boolean canMove(Board board, int fromRow, int fromCol, int toRow, int toCol) {
        // TODO：实现马的移动规则。
        return true;
    }
}
