package edu.sustech.xiangqi.model;

// info 帅/将：只能在九宫格内上下左右移动一格

public class ShuaiPiece extends Piece {
    public ShuaiPiece(boolean isRed) {
        super(isRed ? "帅" : "将", isRed);
    }

    @Override
    public boolean canMove(Board board, int fromRow, int fromCol, int toRow, int toCol) {
        //判断是否只直走了一格
        if (Math.abs(fromRow - toRow) + Math.abs(fromCol - toCol) != 1) return false;
        
        //判断红方是否位于九宫格
        if (isRed()) return toRow >= 7 && toRow <= 9 && toCol >= 3 && toCol <= 5;
            //判断黑方是否位于九宫格
        else return toRow >= 0 && toRow <= 2 && toCol >= 3 && toCol <= 5;
    }

}
