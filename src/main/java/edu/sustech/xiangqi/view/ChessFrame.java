package edu.sustech.xiangqi.view;

import edu.sustech.xiangqi.model.Board;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

// 窗口
public final class ChessFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    static final Color CREAM = new Color(240, 228, 205);
    static final Color RC = new Color(150, 46, 44);
    static final Color KC = new Color(64, 60, 56);
    static final Color JC = new Color(168, 74, 38);//将军
    static final Color GC = new Color(96, 78, 44);//结束

    private final BoardPanel bp;
    private final RoundLabel lab;
    private final JButton btn;
    private Runnable act;
    private Runnable closeAct;

    public ChessFrame(Board b) {
        super("中国象棋");
        bp = new BoardPanel(b);
        lab = new RoundLabel("红方走棋");
        btn = new JButton("重新开始");

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(new Color(38, 30, 24));
        root.add(top(), BorderLayout.NORTH);
        root.add(bp, BorderLayout.CENTER);

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
                if (closeAct != null) closeAct.run();
            }
        });
        setContentPane(root);
        pack();
        setResizable(false);
        setLocationRelativeTo(null);

        bp.addPropertyChangeListener("redTurn", e -> turn());
        turn();
    }

    private JPanel top() {
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.setBorder(BorderFactory.createEmptyBorder(16, 28, 12, 28));

        JLabel t = new JLabel("中国象棋");
        t.setFont(BoardPanel.kai(Font.BOLD, 24));
        t.setForeground(CREAM);
        head.add(t, BorderLayout.WEST);

        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.X_AXIS));

        lab.setFont(BoardPanel.kai(Font.PLAIN, 13));
        lab.setForeground(CREAM);
        lab.setBorder(BorderFactory.createEmptyBorder(7, 14, 7, 14));
        lab.setPreferredSize(new Dimension(126, 29));//文字长短不跳动
        lab.setAlignmentY(Component.CENTER_ALIGNMENT);

        btn.setFont(BoardPanel.kai(Font.PLAIN, 13));
        btn.setForeground(CREAM);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(112, 94, 72), 1, true),
                BorderFactory.createEmptyBorder(5, 15, 5, 15)));
        btn.setAlignmentY(Component.CENTER_ALIGNMENT);
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setForeground(Color.WHITE);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setForeground(CREAM);
            }
        });
        btn.addActionListener(e -> {
            if (act != null) act.run();
        });

        box.add(lab);
        box.add(Box.createHorizontalStrut(10));

        // 认输 / 和棋
        String[] ns = {"认输", "和棋"};
        for (int i = 0; i < ns.length; i++) {
            final JButton bt = new JButton(ns[i]);
            final int k = i;
            bt.setFont(BoardPanel.kai(Font.PLAIN, 13));
            bt.setForeground(CREAM);
            bt.setFocusPainted(false);
            bt.setContentAreaFilled(false);
            bt.setOpaque(false);
            bt.setCursor(new Cursor(Cursor.HAND_CURSOR));
            bt.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(112, 94, 72), 1, true),
                    BorderFactory.createEmptyBorder(5, 11, 5, 11)));
            bt.setAlignmentY(Component.CENTER_ALIGNMENT);
            bt.addActionListener(e -> ask(k));
            bt.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    bt.setForeground(Color.WHITE);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    bt.setForeground(CREAM);
                }
            });
            box.add(bt);
            box.add(Box.createHorizontalStrut(8));
        }
        box.add(btn);
        head.add(box, BorderLayout.EAST);
        return head;
    }

    void turn() {
        lab.setText(bp.status());
        if (bp.res != 0) lab.setBackground(GC);
        else if (bp.jr >= 0) lab.setBackground(JC);
        else lab.setBackground(bp.b.Redturn ? RC : KC);
        lab.repaint();
    }

    // 认输 / 和棋
    private void ask(int k) {
        if (bp.res != 0) return;
        String who = bp.b.Redturn ? "红方" : "黑方";
        String s = k == 0 ? who + "确认认输？" : "双方同意和棋？";
        if (!confirm(s)) return;
        if (k == 0) bp.end(bp.b.Redturn ? 2 : 1, "认输");
        else bp.end(3, null);
    }

    private boolean confirm(String s) {
        JDialog d = new JDialog(this, "确认", true);
        JPanel root = new JPanel(new BorderLayout(0, 18));
        root.setBackground(new Color(38, 30, 24));
        root.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(112, 94, 72)),
                BorderFactory.createEmptyBorder(22, 26, 20, 26)));
        JLabel msg = new JLabel(s, SwingConstants.CENTER);
        msg.setFont(BoardPanel.kai(Font.PLAIN, 17));
        msg.setForeground(CREAM);
        root.add(msg, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        buttons.setOpaque(false);
        int[] ans = {1};
        JButton yes = dialogButton("确定");
        JButton no = dialogButton("取消");
        yes.addActionListener(e -> { ans[0] = 0; d.dispose(); });
        no.addActionListener(e -> d.dispose());
        buttons.add(yes);
        buttons.add(no);
        root.add(buttons, BorderLayout.SOUTH);
        d.setContentPane(root);
        d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        d.pack();
        d.setResizable(false);
        d.setLocationRelativeTo(this);
        d.setVisible(true);
        return ans[0] == 0;
    }

    private JButton dialogButton(String s) {
        JButton b = new JButton(s);
        b.setFont(BoardPanel.kai(Font.PLAIN, 14));
        b.setForeground(CREAM);
        b.setBackground(new Color(74, 55, 38));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(196, 141, 56)),
                BorderFactory.createEmptyBorder(7, 20, 7, 20)));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    public BoardPanel getBoardPanel() {
        return bp;
    }

    public void setOnRestart(Runnable r) {
        act = r;
    }

    public void setOnClose(Runnable r) {
        closeAct = r;
    }

    static final class RoundLabel extends JLabel {
        private static final long serialVersionUID = 1L;

        RoundLabel(String s) {
            super(s);
            setOpaque(false);
            setHorizontalAlignment(SwingConstants.CENTER);
            setBackground(KC);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(getBackground());
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            g.setColor(new Color(255, 255, 255, 30));
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            g.dispose();
            super.paintComponent(g0);
        }
    }
}
