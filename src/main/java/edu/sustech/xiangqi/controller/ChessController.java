package edu.sustech.xiangqi.controller;

import edu.sustech.xiangqi.model.Board;
import edu.sustech.xiangqi.model.Piece;
import edu.sustech.xiangqi.view.BoardPanel;

import javax.swing.SwingUtilities;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

// 鼠标点击和悬停
public class ChessController extends MouseAdapter {
    private final Board b;
    private final BoardPanel bp;

    private int r0 = -1, c0 = -1;

    public ChessController(Board b, BoardPanel bp) {
        this.b = b;
        this.bp = bp;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (!SwingUtilities.isLeftMouseButton(e)) return;
        Point p = bp.at(e.getX(), e.getY());
        if (p != null) click(p.y, p.x);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        Point p = bp.at(e.getX(), e.getY());
        if (p == null) bp.hv(-1, -1);
        else bp.hv(p.y, p.x);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        bp.hv(-1, -1);
    }

    private void click(int r, int c) {
        if (r == r0 && c == c0) {
            clear();
            return;
        }

        Piece cp = b.getPieceAt(r, c);
        if (r0 == -1) {
            // 只能选轮到的一方
            if (cp != null && cp.isRed() == b.Redturn) pick(r, c);
            return;
        }

        Piece sp = b.getPieceAt(r0, c0);
        if (cp != null && sp != null && cp.isRed() == sp.isRed() && cp.isRed() == b.Redturn) {
            pick(r, c);
            return;
        }

        int fr = r0, fc = c0;
        Piece eat = b.getPieceAt(r, c);
        if (b.tryMove(fr, fc, r, c)) {
            clear();
            bp.moveAnim(b.getPieceAt(r, c), eat, fr, fc, r, c);
        }
    }

    private void pick(int r, int c) {
        r0 = r; c0 = c;
        bp.sel(r, c);
    }

    private void clear() {
        r0 = -1; c0 = -1;
        bp.sel(-1, -1);
    }

    public void restart() {
        b.reset();
        r0 = -1; c0 = -1;
        bp.resetState();
    }
}
