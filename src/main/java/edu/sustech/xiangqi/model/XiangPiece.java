package edu.sustech.xiangqi.model;

// info 相: 只能走田字格的对角线，田字最中心有子则不能走

public class XiangPiece extends Piece {
    public XiangPiece(boolean isRed) {
        super("相", isRed);
    }

    @Override
    public boolean canMove(Board board, int fromRow, int fromCol, int toRow, int toCol) {
        // TODO：实现相的移动规则。
        return true;
    }
}
