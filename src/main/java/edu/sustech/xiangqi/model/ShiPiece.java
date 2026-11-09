package edu.sustech.xiangqi.model;

// info 仕/士：每次斜走一格，不能离开九宫格

public class ShiPiece extends Piece {
    public ShiPiece(boolean isRed) {
        super(isRed ? "仕" : "士", isRed);
    }

    @Override
    public boolean canMove(Board board, int fromRow, int fromCol, int toRow, int toCol) {
        // 行和列必须都变化一格，不能直走、原地停留或跨越多格
        if (Math.abs(toRow - fromRow) != 1 || Math.abs(toCol - fromCol) != 1) return false;
        
        //判断红方是否位于九宫格
        if (isRed()) return toRow >= 7 && toRow <= 9 && toCol >= 3 && toCol <= 5;
            //判断黑方是否位于九宫格
        else return toRow >= 0 && toRow <= 2 && toCol >= 3 && toCol <= 5;
    }
}
