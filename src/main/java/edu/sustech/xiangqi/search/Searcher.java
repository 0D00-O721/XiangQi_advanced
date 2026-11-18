package edu.sustech.xiangqi.search;

//info:搜索函数

import edu.sustech.xiangqi.model.Board;
import edu.sustech.xiangqi.model.Piece;

public class Searcher {
    Board board;
    Evaluate evaluator = new Evaluate();

    public Searcher(Board board) {
        this.board = board;
    }

    public void output() {
        System.out.printf("移动成功！当前局面评分(正红负黑): %d\n", evaluator.evaluate(board, 0));
    }
}
