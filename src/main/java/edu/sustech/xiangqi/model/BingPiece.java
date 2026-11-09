package edu.sustech.xiangqi.model;

// info 兵：一次走一格，过河前只能往前走，过河后可以往前/左/右走

public class BingPiece extends Piece {
    public BingPiece(boolean isRed) {
        super("兵", isRed);
    }

    @Override
    public boolean canMove(Board board, int fromRow, int fromCol, int toRow, int toCol) {
        // TODO：实现兵的移动规则。
        return true;
    }

}
