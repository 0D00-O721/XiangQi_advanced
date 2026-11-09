package edu.sustech.xiangqi.model;

// info 车类：只能走直线，步数任意

public class ChePiece extends Piece {
    public ChePiece(boolean isRed) {
        super("车", isRed);
    }

    @Override
    public boolean canMove(Board board, int fromRow, int fromCol, int toRow, int toCol) {
        // TODO：实现车的移动规则。
        return true;
    }

}
