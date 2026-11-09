package edu.sustech.xiangqi.model;

// info 兵/卒：一次走一格，过河前只能往前走，过河后可以往前/左/右走

public class BingPiece extends Piece {
    public BingPiece(boolean isRed) {
        super(isRed ? "兵" : "卒", isRed);
    }

    @Override
    public boolean canMove(Board board, int fromRow, int fromCol, int toRow, int toCol) {
        int dr = toRow - fromRow;
        int dc = toCol - fromCol;
        //不能移动超过一格或不移动
        if (Math.abs(dr) + Math.abs(dc) != 1) return false;
        //判断是那方的兵
        if (isRed()) {
            //没过河不能横着走
            if (fromRow >= 5 && Math.abs(dc) > 0) return false;
            //不能回退(行变大)
            if (dr == 1) return false;
        } else {
            //没过河不能横着走
            if (fromRow <= 4 && Math.abs(dc) > 0) return false;
            //不能回退(行变小)
            if (dr == -1) return false;
        }
        return true;
    }

}
