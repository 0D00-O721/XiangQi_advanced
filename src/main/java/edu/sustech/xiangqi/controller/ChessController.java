package edu.sustech.xiangqi.controller;

import edu.sustech.xiangqi.model.Board;
import edu.sustech.xiangqi.model.Piece;
import edu.sustech.xiangqi.view.BoardPanel;
import edu.sustech.xiangqi.search.Searcher;
import edu.sustech.xiangqi.agent.CodexAppServer;
import edu.sustech.xiangqi.agent.McpServer;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

// 鼠标点击和悬停
public class ChessController extends MouseAdapter {
    private final Board b;
    private final BoardPanel bp;
    private final Searcher searcher;
    private boolean ai;
    private boolean agentMode;
    private boolean thinking;
    private int ateCnt = 0;
    private final CodexAppServer codex = new CodexAppServer(McpServer::setAgentStatus);
    //rule60变量：记录多少步没吃子，根据中国象棋规则，120步（60回合）不吃子会直接判和
    private int rule60 = 0;

    private int r0 = -1, c0 = -1;

    public ChessController(Board b, BoardPanel bp, Searcher s) {
        this.b = b;
        this.bp = bp;
        this.searcher = s;
    }

    public void setAI(boolean ai) {
        this.ai = ai;
    }

    public void setAgentMode(boolean on) {
        agentMode = on;
    }

    public String agentMove(int fr, int fc, int tr, int tc) {
        if (!agentMode) return "错误：当前对局未开启 Agent 模式。";
        if (bp.res != 0) return "错误：对局已经结束。";
        if (b.Redturn) return "错误：现在是红方回合，Agent 只能在黑方回合走棋。";
        Piece eat = b.getPieceAt(tr, tc);
        int move = b.tryMove(fr, fc, tr, tc);
        if (move > 0) return "错误：走法不合法，Board.tryMove 返回 " + move + "。";
        bp.state();
        bp.moveAnim(b.getPieceAt(tr, tc), eat, fr, fc, tr, tc);
        if (move == -2) bp.end(2, "绝杀");
        else if (move == -3) bp.end(2, "困毙");
        else if (move == -1) return "走棋成功，红方被将军。";
        return "走棋成功。";
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (thinking) return;
        if (!SwingUtilities.isLeftMouseButton(e)) return;
        Point p = bp.at(e.getX(), e.getY());
        if (p != null) click(p.y, p.x);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (thinking) return;
        Point p = bp.at(e.getX(), e.getY());
        if (p == null) bp.hv(-1, -1);
        else bp.hv(p.y, p.x);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        if (thinking) return;
        bp.hv(-1, -1);
    }

    private void click(int r, int c) {
        if (bp.res != 0 || thinking || ((ai || agentMode) && !b.Redturn)) return;//已经下完了，谁也别点

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

            //更新rule60;
            if (eat != null) {
                rule60 = 0;
                ateCnt++;
            } else rule60++;

            searcher.output(rule60);

            if (ai && bp.res == 0 && !b.Redturn) moveAI();
            else if (agentMode && bp.res == 0 && !b.Redturn) moveAgent();
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

    public void retryAgent() {
        if (agentMode && !thinking && bp.res == 0 && !b.Redturn) moveAgent();
    }

    private void moveAgent() {
        thinking = true;
        bp.tip("正在请求 Codex Agent…");
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return codex.playTurn(System.getProperty("user.dir"));
            }

            @Override
            protected void done() {
                thinking = false;
                try {
                    String answer = get();
                    if (answer.startsWith("Agent 请求失败：")) bp.tip(answer + "，可重试");
                } catch (Exception e) {
                    bp.tip("Codex Agent 请求失败，可重试");
                }
            }
        }.execute();
    }

    private void moveAI() {
        thinking = true;
        bp.tip("电脑思考中…");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    Thread.sleep(190);
                } catch (Exception e) {
                }
                bp.setThinking(true);
                try {
                    Thread.sleep(20);
                } catch (Exception e) {
                }
                if (ateCnt >= 16) {
                    Searcher.setMaxDep(Searcher.getMaxDep() + 1);
                    ateCnt = 0;
                }
                searcher.IDA_dfs(false, rule60);
                return null;
            }

            @Override
            protected void done() {
                thinking = false;
                bp.setThinking(false);
                int fr = searcher.getFromRow(), fc = searcher.getFromCol();
                int tr = searcher.getToRow(), tc = searcher.getToCol();
                Piece p = b.getPieceAt(fr, fc);
                Piece AIeat = b.getPieceAt(tr, tc);
                //更新rule60
                if (AIeat != null) {
                    rule60 = 0;
                    ateCnt++;
                } else rule60++;

                if (p == null || p.isRed()) {
                    bp.end(1, "电脑无棋可走");
                    return;
                }
                int move = b.tryMove(fr, fc, tr, tc);
                if (move > 0) {
                    bp.end(1, "电脑无棋可走");
                    return;
                }
                bp.state();
                bp.moveAnim(b.getPieceAt(tr, tc), null, fr, fc, tr, tc);
                if (move == -2) bp.end(2, "绝杀");
                else if (move == -3) bp.end(2, "困毙");
                searcher.output(rule60);
            }
        }.execute();
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
        thinking = false;
        codex.newGame();
        bp.resetState();
    }

    public void close() {
        codex.close();
    }
}
