package edu.sustech.xiangqi;

import edu.sustech.xiangqi.controller.ChessController;
import edu.sustech.xiangqi.model.Board;
import edu.sustech.xiangqi.search.Evaluate;
import edu.sustech.xiangqi.view.ChessFrame;
import edu.sustech.xiangqi.search.Searcher;

import javax.swing.SwingUtilities;

// 程序入口
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Board board = new Board();
                Evaluate evaluate = new Evaluate();
                Searcher searcher = new Searcher(board, evaluate);
                ChessFrame frame = new ChessFrame(board);
                ChessController controller = new ChessController(board, frame.getBoardPanel(), searcher);

                frame.getBoardPanel().addMouseListener(controller);
                frame.getBoardPanel().addMouseMotionListener(controller);
                frame.setOnRestart(controller::restart);
                frame.setVisible(true);
            }
        });
    }
}
