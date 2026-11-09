package edu.sustech.xiangqi.model;

// info 相/象: 只能走田字格的对角线(x +- 2, y +- 2)，田字最中心有子则不能走,不能过河

public class XiangPiece extends Piece {
    public XiangPiece(boolean isRed) {
        super(isRed ? "相" : "象", isRed);
    }

    @Override
    public boolean canMove(Board board, int fromRow, int fromCol, int toRow, int toCol) {
        //判断是否走田字
        if (Math.abs(fromRow - toRow) != 2 || Math.abs(fromCol - toCol) != 2) return false;
        //判断田中心有无子
        if (board.getPieceAt((fromRow + toRow) / 2, (fromCol + toCol) / 2) != null) return false;
        //判断过河
        if (isRed()) return toRow >= 5;
        else return toRow <= 4;
    }
}
