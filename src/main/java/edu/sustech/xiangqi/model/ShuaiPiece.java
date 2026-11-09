package edu.sustech.xiangqi.model;

// info 帅：只能在九宫格内上下左右移动一格

public class ShuaiPiece extends Piece {
    public ShuaiPiece(boolean isRed) {
        super("帅", isRed);
    }

    @Override
    public boolean canMove(Board board, int fromRow, int fromCol, int toRow, int toCol) {
        // TODO：实现帅的移动规则。
        return true;
    }

}
