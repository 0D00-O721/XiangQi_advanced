package edu.sustech.xiangqi.view;

import edu.sustech.xiangqi.model.Board;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

// 窗口
public final class ChessFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    static final Color CREAM = new Color(240, 228, 205);
    static final Color RC = new Color(150, 46, 44);
    static final Color KC = new Color(64, 60, 56);

    private final BoardPanel bp;
    private final RoundLabel lab;
    private final JButton btn;
    private Runnable act;

    public ChessFrame(Board b) {
        super("中国象棋");
        bp = new BoardPanel(b);
        lab = new RoundLabel("红方走棋");
        btn = new JButton("重新开始");

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(new Color(38, 30, 24));
        root.add(top(), BorderLayout.NORTH);
        root.add(bp, BorderLayout.CENTER);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(root);
        pack();
        setResizable(false);
        setLocationRelativeTo(null);

        bp.addPropertyChangeListener("redTurn", e -> turn(b.Redturn));
        turn(b.Redturn);
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
        box.add(btn);
        head.add(box, BorderLayout.EAST);
        return head;
    }

    void turn(boolean red) {
        lab.setText(red ? "红方走棋" : "黑方走棋");
        lab.setBackground(red ? RC : KC);
        lab.repaint();
    }

    public BoardPanel getBoardPanel() {
        return bp;
    }

    public void setOnRestart(Runnable r) {
        act = r;
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
