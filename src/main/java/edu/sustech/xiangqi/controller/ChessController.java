package edu.sustech.xiangqi.controller;

import edu.sustech.xiangqi.model.Board;
import edu.sustech.xiangqi.model.Piece;
import edu.sustech.xiangqi.view.BoardPanel;
import edu.sustech.xiangqi.search.Searcher;

import javax.swing.SwingUtilities;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

// 鼠标点击和悬停
public class ChessController extends MouseAdapter {
    private final Board b;
    private final BoardPanel bp;
    private final Searcher searcher;

    private int r0 = -1, c0 = -1;

    public ChessController(Board b, BoardPanel bp, Searcher s) {
        this.b = b;
        this.bp = bp;
        this.searcher = s;
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
        if (bp.res != 0) return;//已经下完了，谁也别点

        if (r == r0 && c == c0) {
            clear();
            return;
        }

        Piece cp = b.getPieceAt(r, c);
        if (r0 == -1) {
            // 只能选轮到的一方，而且得有真能走的步
            if (cp != null && cp.isRed() == b.Redturn) {
                if (b.canSel(r, c)) pick(r, c);
                else bp.tip(b.isJiangjun(b.Redturn) ? "这步救不了将" : "这子无处可走");
            }
            return;
        }

        Piece sp = b.getPieceAt(r0, c0);
        if (cp != null && sp != null && cp.isRed() == sp.isRed() && cp.isRed() == b.Redturn) {
            if (b.canSel(r, c)) pick(r, c);
            else bp.tip("这子无处可走");
            return;
        }

        int fr = r0, fc = c0;
        Piece eat = b.getPieceAt(r, c);

        //对应可以走的返回值为-3, -2, -1, 0
        int movetype = b.tryMove(fr, fc, r, c);

        if (movetype <= 0) {
            clear();
            bp.state();//先算将军，再让动画 fire，免得标签用上一手的 jr
            bp.moveAnim(b.getPieceAt(r, c), eat, fr, fc, r, c);
            if (movetype == -2) bp.end(b.Redturn ? 2 : 1, "绝杀");
            else if (movetype == -3) bp.end(b.Redturn ? 2 : 1, "困毙");
            searcher.output();
        } else {
            //走不了就说清楚为什么
            if (movetype == 7) bp.tip("不能飞将");
            else if (movetype == 8) bp.tip("不能送将");
            else if (movetype == 5) bp.tip("不能吃自己的子");
            else if (movetype == 4) bp.tip("还没轮到你");
            else if (movetype == 6) bp.tip("这一步走不了");
            else if (movetype == 2) clear();
        }
    }

    private void pick(int r, int c) {
        r0 = r;
        c0 = c;
        bp.sel(r, c);
    }

    private void clear() {
        r0 = -1;
        c0 = -1;
        bp.sel(-1, -1);
    }

    public void restart() {
        b.reset();
        r0 = -1;
        c0 = -1;
        bp.resetState();
    }
}
