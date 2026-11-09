package edu.sustech.xiangqi.model;

// 棋子的抽象父类。棋子保存身份信息，所在位置由 Board 的数组下标表示

public abstract class Piece {
    private final String name;
    private final boolean isRed;

    protected Piece(String name, boolean isRed) {
        this.name = name;
        this.isRed = isRed;
    }

    public String getName() {
        return name;
    }

    public boolean isRed() {
        return isRed;
    }

    // 判断 该种类型 的棋子是否有这样的移动方式
    public abstract boolean canMove(Board board, int fromRow, int fromCol, int toRow, int toCol);
}
