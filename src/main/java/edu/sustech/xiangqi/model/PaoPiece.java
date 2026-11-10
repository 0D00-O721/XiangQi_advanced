package edu.sustech.xiangqi.model;

// info 炮:直线打击，只能跨过一个直方向棋子去吃下一个敌方棋子

public class PaoPiece extends Piece {
    public PaoPiece(boolean isRed) {
        super("炮", isRed);
    }

    @Override
    public boolean canMove(Board board, int fromRow, int fromCol, int toRow, int toCol) {
        int dr = Math.abs(toRow - fromRow);
        int dc = Math.abs(toCol - fromCol);
        //不能不走，也不能斜着走
        if ((dr == 0 && dc == 0) || (dr != 0 && dc != 0)) return false;
        //如果只是位移要确保直线位置无子
        if (board.getPieceAt(toRow, toCol) == null) return board.hasPieces(fromRow, fromCol, toRow, toCol) == 1;
            //如果吃子要保证中间有子
        else return board.hasPieces(fromRow, fromCol, toRow, toCol) == 3;
    }
}
