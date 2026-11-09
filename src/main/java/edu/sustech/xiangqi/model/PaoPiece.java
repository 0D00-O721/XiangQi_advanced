package edu.sustech.xiangqi.model;

// info 炮:直线打击，只能跨过一个直方向棋子去吃下一个敌方棋子

public class PaoPiece extends Piece {
    public PaoPiece(boolean isRed) {
        super("炮", isRed);
    }

    @Override
    public boolean canMove(Board board, int fromRow, int fromCol, int toRow, int toCol) {
        // TODO：实现炮的移动规则。
        return true;
    }
}
