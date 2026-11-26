package edu.sustech.xiangqi.agent;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import edu.sustech.xiangqi.controller.ChessController;
import edu.sustech.xiangqi.model.Board;
import edu.sustech.xiangqi.model.Piece;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Streamable-HTTP MCP tools for the live Xiangqi board. Agent turns are started by CodexAppServer. */
public final class McpServer {
    private static HttpServer server;
    private static volatile Board board;
    private static volatile ChessController controller;
    private static volatile String agentStatus = "Codex App Server 尚未启动";

    public static synchronized boolean setEnabled(boolean on) {
        if (!on) {
            if (server != null) server.stop(0);
            server = null;
            return true;
        }
        if (server != null) return true;
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 8765), 0);
            server.createContext("/mcp", McpServer::handle);
            server.setExecutor(Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r, "xiangqi-mcp-http");
                t.setDaemon(true);
                return t;
            }));
            server.start();
            return true;
        } catch (IOException e) {
            server = null;
            return false;
        }
    }

    public static synchronized boolean isEnabled() { return server != null; }

    public static String getAgentStatus() { return agentStatus; }

    public static void setAgentStatus(String status) { agentStatus = status; }

    public static void setGame(Board b, ChessController c) {
        board = b;
        controller = c;
    }

    private static void handle(HttpExchange x) throws IOException {
        if (!"POST".equals(x.getRequestMethod())) {
            x.getResponseHeaders().set("Allow", "POST");
            send(x, 405, "");
            return;
        }
        String body = new String(x.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String id = match(body, "\\\"id\\\"\\s*:\\s*(\\d+|\\\"(?:\\\\.|[^\\\"])*\\\")");
        if (id == null) {
            send(x, 202, "");
            return;
        }
        String method = string(body, "method");
        String protocol = x.getRequestHeaders().getFirst("MCP-Protocol-Version");
        if ("initialize".equals(method)) protocol = string(body, "protocolVersion");
        if (protocol == null) protocol = "2025-03-26";
        String out;
        if ("initialize".equals(method)) {
            out = result(id, "{\"protocolVersion\":" + quote(protocol) + ",\"capabilities\":{\"tools\":{}},\"serverInfo\":{\"name\":\"xiangqi-board\",\"version\":\"2.0.0\"}}");
        } else if ("ping".equals(method)) {
            out = result(id, "{}");
        } else if ("tools/list".equals(method)) {
            out = result(id, "{\"tools\":[" +
                    "{\"name\":\"get_board\",\"description\":\"读取当前象棋棋盘及 token 序列。tokens 为 XQ1、回合标记、90 个行优先棋格；K/A/E/H/R/C/P 表示帅/仕/相/马/车/炮/兵，大写红方、小写黑方，点号为空。a0 是左上角，i9 是右下角。\",\"inputSchema\":{\"type\":\"object\",\"properties\":{},\"additionalProperties\":false}}," +
                    "{\"name\":\"play_move\",\"description\":\"黑方 Agent 提交一步走法。from/to 使用 a0 到 i9 的坐标，行 0 在棋盘顶部。\",\"inputSchema\":{\"type\":\"object\",\"properties\":{\"from\":{\"type\":\"string\",\"pattern\":\"^[a-i][0-9]$\"},\"to\":{\"type\":\"string\",\"pattern\":\"^[a-i][0-9]$\"}},\"required\":[\"from\",\"to\"],\"additionalProperties\":false}}]}");
        } else if ("tools/call".equals(method)) {
            String name = string(body, "name");
            if ("get_board".equals(name)) out = boardTool(id);
            else if ("play_move".equals(name)) {
                String args = object(body, "arguments");
                String from = args == null ? null : string(args, "from");
                String to = args == null ? null : string(args, "to");
                String answer = play(from, to);
                out = tool(id, answer, answer.startsWith("错误："));
            } else out = tool(id, "未知工具", true);
        } else {
            out = "{\"jsonrpc\":\"2.0\",\"id\":" + id + ",\"error\":{\"code\":-32601,\"message\":\"Method not found\"}}";
        }
        x.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        x.getResponseHeaders().set("MCP-Protocol-Version", protocol);
        send(x, 200, out);
    }

    private static String boardTool(String id) {
        String data = readBoard();
        if (!data.startsWith("{")) return tool(id, data, false);
        return result(id, "{\"content\":[{\"type\":\"text\",\"text\":" + quote(data) + "}],\"structuredContent\":" + data + "}");
    }

    private static String readBoard() {
        Board b = board;
        if (b == null) return "当前没有进行中的对局。";
        String[] answer = {"当前对局已关闭。"};
        try {
            SwingUtilities.invokeAndWait(() -> { if (board == b) answer[0] = encodeBoard(b); });
        } catch (Exception e) {
            return "错误：无法读取棋盘状态。";
        }
        return answer[0];
    }

    private static String encodeBoard(Board b) {
        StringBuilder tokens = new StringBuilder("[\"XQ1\",\"");
        tokens.append(b.Redturn ? "r\",\"" : "b\",\"");
        StringBuilder rows = new StringBuilder();
        for (int r = 0; r < Board.ROWS; r++) {
            if (r > 0) rows.append('/');
            for (int c = 0; c < Board.COLS; c++) {
                Piece p = b.getPieceAt(r, c);
                char token = p == null ? '.' : pieceToken(p);
                tokens.append(token);
                rows.append(token);
                if (r != Board.ROWS - 1 || c != Board.COLS - 1) tokens.append("\",\"");
            }
        }
        tokens.append("\"]");
        return "{\"encoding\":\"XQ1\",\"turn\":" + quote(b.Redturn ? "r" : "b") + ",\"rows\":" + quote(rows.toString()) + ",\"tokens\":" + tokens + "}";
    }

    private static char pieceToken(Piece p) {
        String n = p.getName();
        char c = 'P';
        if (n.equals("帅") || n.equals("将")) c = 'K';
        else if (n.equals("仕") || n.equals("士")) c = 'A';
        else if (n.equals("相") || n.equals("象")) c = 'E';
        else if (n.equals("马")) c = 'H';
        else if (n.equals("车")) c = 'R';
        else if (n.equals("炮")) c = 'C';
        return p.isRed() ? c : Character.toLowerCase(c);
    }

    private static String play(String from, String to) {
        if (from == null || to == null || !from.matches("[a-i][0-9]") || !to.matches("[a-i][0-9]")) return "错误：坐标必须是 a0 到 i9，例如 a0。";
        Board b = board;
        ChessController c = controller;
        if (b == null || c == null) return "错误：当前没有进行中的对局。";
        String[] answer = {"错误：走棋失败。"};
        try {
            SwingUtilities.invokeAndWait(() -> {
                if (board != b || controller != c) answer[0] = "错误：当前 Agent 对局已关闭。";
                else answer[0] = c.agentMove(from.charAt(1) - '0', from.charAt(0) - 'a', to.charAt(1) - '0', to.charAt(0) - 'a');
            });
        } catch (Exception e) {
            return "错误：无法切换到棋盘线程执行走棋。";
        }
        return answer[0];
    }

    private static String tool(String id, String text, boolean error) {
        return result(id, "{\"content\":[{\"type\":\"text\",\"text\":" + quote(text) + "}],\"isError\":" + error + "}");
    }

    private static String result(String id, String value) { return "{\"jsonrpc\":\"2.0\",\"id\":" + id + ",\"result\":" + value + "}"; }

    private static String string(String s, String key) {
        Matcher m = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"").matcher(s);
        if (!m.find()) return null;
        return m.group(1).replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private static String match(String s, String pattern) {
        Matcher m = Pattern.compile(pattern).matcher(s);
        return m.find() ? m.group(1) : null;
    }

    private static String object(String s, String key) {
        Matcher m = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\{").matcher(s);
        if (!m.find()) return null;
        int start = m.end() - 1, depth = 0;
        boolean quoted = false, escape = false;
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (quoted) {
                if (escape) escape = false;
                else if (c == '\\') escape = true;
                else if (c == '"') quoted = false;
            } else if (c == '"') quoted = true;
            else if (c == '{') depth++;
            else if (c == '}' && --depth == 0) return s.substring(start, i + 1);
        }
        return null;
    }

    private static String quote(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }

    private static void send(HttpExchange x, int code, String s) throws IOException {
        byte[] data = s.getBytes(StandardCharsets.UTF_8);
        x.sendResponseHeaders(code, data.length);
        x.getResponseBody().write(data);
        x.close();
    }
}
