package edu.sustech.xiangqi.model;

// info 兵/卒：一次走一格，过河前只能往前走，过河后可以往前/左/右走

public class BingPiece extends Piece {
    public BingPiece(boolean isRed) {
        super(isRed ? "兵" : "卒", isRed);
    }

    @Override
    public boolean canMove(Board board, int fromRow, int fromCol, int toRow, int toCol) {
        // TODO：实现兵的移动规则。
        return true;
    }

}
