package edu.sustech.xiangqi;

import edu.sustech.xiangqi.controller.ChessController;
import edu.sustech.xiangqi.agent.McpServer;
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
        McpServer.setEnabled(true);
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
        JButton agent = button("Agent 对弈");
        JButton settings = button("设置");
        JComboBox<String> depth = new JComboBox<>(new String[]{"入门 · 深度 1", "标准 · 深度 2", "进阶 · 深度 3", "挑战 · 深度 4", "高手 · 深度 5", "大师 · 深度 6"});
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
        depth.setToolTipText("搜索深度越高，电脑思考时间越长");
        depth.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(pvp);
        box.add(Box.createVerticalStrut(14));
        box.add(ai);
        box.add(Box.createVerticalStrut(12));
        box.add(depth);
        box.add(Box.createVerticalStrut(14));
        box.add(agent);
        box.add(Box.createVerticalStrut(18));
        box.add(settings);
        box.add(Box.createVerticalStrut(28));
        JLabel note = new JLabel(McpServer.isEnabled() ? "MCP 地址：http://127.0.0.1:8765/mcp" : "红方先行 · 木纹棋盘 · 搜索算法对战");
        note.setFont(new Font("KaiTi", Font.PLAIN, 13));
        note.setForeground(new Color(151, 130, 105));
        note.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(note);
        settings.addActionListener(e -> settings(f, note));
        pvp.addActionListener(e -> { f.dispose(); game(false, 2, false); });
        ai.addActionListener(e -> { f.dispose(); game(true, depth.getSelectedIndex() + 1, false); });
        agent.addActionListener(e -> {
            if (!McpServer.isEnabled()) {
                JOptionPane.showMessageDialog(f, "请先开启本机 MCP 接入。", "Agent 对弈", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            f.dispose();
            game(false, 2, true);
        });
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

    static void settings(JFrame owner, JLabel note) {
        JDialog d = new JDialog(owner, "设置", true);
        JPanel root = new JPanel();
        root.setBackground(BG);
        root.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(112, 94, 72)), BorderFactory.createEmptyBorder(20, 24, 18, 24)));
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("对局设置");
        title.setFont(new Font("KaiTi", Font.BOLD, 20));
        title.setForeground(CREAM);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        JCheckBox mcp = new JCheckBox("开启棋盘 MCP 工具服务");
        mcp.setFont(new Font("KaiTi", Font.PLAIN, 15));
        mcp.setForeground(CREAM);
        mcp.setOpaque(false);
        mcp.setFocusPainted(false);
        mcp.setSelected(McpServer.isEnabled());
        mcp.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel endpoint = new JLabel("http://127.0.0.1:8765/mcp");
        endpoint.setFont(new Font("KaiTi", Font.PLAIN, 13));
        endpoint.setForeground(new Color(196, 161, 112));
        endpoint.setAlignmentX(Component.CENTER_ALIGNMENT);
        JButton close = button("完成");
        close.addActionListener(e -> d.dispose());
        mcp.addActionListener(e -> {
            boolean ok = McpServer.setEnabled(mcp.isSelected());
            if (!ok) {
                mcp.setSelected(false);
                JOptionPane.showMessageDialog(d, "无法启动 MCP 服务，请检查本机 8765 端口是否被占用。", "MCP 接入", JOptionPane.WARNING_MESSAGE);
            }
            note.setText(McpServer.isEnabled() ? "MCP 地址：http://127.0.0.1:8765/mcp" : "红方先行 · 木纹棋盘 · 搜索算法对战");
        });
        root.add(title);
        root.add(Box.createVerticalStrut(20));
        root.add(mcp);
        root.add(Box.createVerticalStrut(8));
        root.add(endpoint);
        root.add(Box.createVerticalStrut(20));
        root.add(close);
        d.setContentPane(root);
        d.pack();
        d.setResizable(false);
        d.setLocationRelativeTo(owner);
        d.setVisible(true);
    }

    static void game(boolean ai, int dep, boolean agent) {
        Board board = new Board();
        Evaluate evaluate = new Evaluate();
        Searcher searcher = new Searcher(board, evaluate);
        Searcher.setMaxDep(dep);
        ChessFrame frame = new ChessFrame(board);
        ChessController controller = new ChessController(board, frame.getBoardPanel(), searcher);
        controller.setAI(ai);
        controller.setAgentMode(agent);
        frame.setAgentMode(agent);
        frame.setOnAgentRequest(controller::retryAgent);
        McpServer.setGame(agent ? board : null, agent ? controller : null);
        frame.getBoardPanel().addMouseListener(controller);
        frame.getBoardPanel().addMouseMotionListener(controller);
        frame.setOnRestart(controller::restart);
        frame.setOnClose(() -> {
            McpServer.setGame(null, null);
            controller.close();
            home();
        });
        frame.setVisible(true);
    }
}
