package edu.sustech.xiangqi;

import edu.sustech.xiangqi.controller.ChessController;
import edu.sustech.xiangqi.model.Board;
import edu.sustech.xiangqi.search.Evaluate;
import edu.sustech.xiangqi.search.Searcher;
import edu.sustech.xiangqi.view.ChessFrame;

import javax.swing.*;
import java.awt.*;

// 程序入口
public class Main {
    static final Color BG = new Color(38, 30, 24);
    static final Color CREAM = new Color(240, 228, 205);
    static final Color GOLD = new Color(196, 141, 56);

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> home());
    }

    static void home() {
        JFrame f = new JFrame("中国象棋");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(BG);
        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("中国象棋");
        title.setFont(new Font("KaiTi", Font.BOLD, 42));
        title.setForeground(CREAM);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel sub = new JLabel("楚河 · 汉界");
        sub.setFont(new Font("KaiTi", Font.PLAIN, 18));
        sub.setForeground(new Color(196, 161, 112));
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(title);
        box.add(Box.createVerticalStrut(8));
        box.add(sub);
        box.add(Box.createVerticalStrut(38));
        JButton pvp = button("双人对弈");
        JButton ai = button("人机对弈");
        JComboBox<String> depth = new JComboBox<>(new String[]{"入门 · 深度 1", "标准 · 深度 2", "进阶 · 深度 3", "挑战 · 深度 4"});
        depth.setFont(new Font("KaiTi", Font.PLAIN, 15));
        depth.setForeground(CREAM);
        depth.setBackground(new Color(74, 55, 38));
        depth.setFocusable(false);
        depth.setBorder(BorderFactory.createLineBorder(GOLD));
        depth.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
                label.setFont(new Font("KaiTi", Font.PLAIN, 15));
                label.setForeground(CREAM);
                label.setBackground(selected ? new Color(112, 78, 46) : new Color(74, 55, 38));
                label.setBorder(BorderFactory.createEmptyBorder(5, 9, 5, 9));
                return label;
            }
        });
        depth.setMaximumSize(new Dimension(260, 38));
        depth.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(pvp);
        box.add(Box.createVerticalStrut(14));
        box.add(ai);
        box.add(Box.createVerticalStrut(12));
        box.add(depth);
        box.add(Box.createVerticalStrut(28));
        JLabel note = new JLabel("红方先行 · 木纹棋盘 · 搜索算法对战");
        note.setFont(new Font("KaiTi", Font.PLAIN, 13));
        note.setForeground(new Color(151, 130, 105));
        note.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(note);
        pvp.addActionListener(e -> { f.dispose(); game(false, 2); });
        ai.addActionListener(e -> { f.dispose(); game(true, depth.getSelectedIndex() + 1); });
        root.add(box);
        f.setContentPane(root);
        f.setSize(620, 500);
        f.setResizable(false);
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }

    static JButton button(String s) {
        JButton b = new JButton(s);
        b.setFont(new Font("KaiTi", Font.BOLD, 18));
        b.setForeground(CREAM);
        b.setBackground(new Color(74, 55, 38));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(GOLD), BorderFactory.createEmptyBorder(10, 50, 10, 50)));
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setMaximumSize(new Dimension(260, 50));
        return b;
    }

    static void game(boolean ai, int dep) {
        Board board = new Board();
        Evaluate evaluate = new Evaluate();
        Searcher searcher = new Searcher(board, evaluate);
        Searcher.setMaxDep(dep);
        ChessFrame frame = new ChessFrame(board);
        ChessController controller = new ChessController(board, frame.getBoardPanel(), searcher);
        controller.setAI(ai);
        frame.getBoardPanel().addMouseListener(controller);
        frame.getBoardPanel().addMouseMotionListener(controller);
        frame.setOnRestart(controller::restart);
        frame.setOnClose(Main::home);
        frame.setVisible(true);
    }
}
