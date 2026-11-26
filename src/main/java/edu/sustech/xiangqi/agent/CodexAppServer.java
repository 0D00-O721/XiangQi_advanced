package edu.sustech.xiangqi.agent;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** One Codex App Server process and one reusable thread for a single game. */
public final class CodexAppServer implements AutoCloseable {
    private final Consumer<String> status;
    private final AtomicLong ids = new AtomicLong();
    private final ConcurrentHashMap<String, CompletableFuture<String>> pending = new ConcurrentHashMap<>();
    private final Object writeLock = new Object();
    private final StringBuilder stderrText = new StringBuilder();
    private volatile Process process;
    private volatile BufferedWriter writer;
    private volatile String threadId;
    private volatile CompletableFuture<String> activeTurn;
    private volatile String activeTurnId;
    private volatile boolean initialized;
    private volatile boolean threadLoaded;

    public CodexAppServer(Consumer<String> status) {
        this.status = status == null ? s -> { } : status;
        this.status.accept("Codex Agent 待启动");
    }

    /** Starts one black-side turn, reusing this game's App Server thread. */
    public String playTurn(String cwd) {
        CompletableFuture<String> completed = null;
        try {
            ensureThread(cwd);
            completed = new CompletableFuture<>();
            activeTurn = completed;
            activeTurnId = null;
            status.accept("Codex Agent 正在思考并调用棋盘 MCP 工具…");
            String turnPrompt = "现在轮到黑方。请先调用 get_board 读取当前棋盘，然后选择一步合法黑方走法，并调用 play_move 提交。" +
                    "坐标列 a-i、行 0-9；必须通过 play_move 实际提交，不要只在文本里报告走法。" +
                    "每次收到本消息都重新读取棋盘，不要依据旧局面。若返回非法走法，请重新读取棋盘并尝试另一合法走法。";
            String params = "{\"threadId\":" + quote(threadId) + ",\"input\":[{\"type\":\"text\",\"text\":" + quote(turnPrompt) + "}]}";
            String response = rpc("turn/start", params, 30);
            if (hasError(response)) throw new IOException(errorMessage(response));
            String resultObject = object(response, "result");
            String turnObject = resultObject == null ? null : object(resultObject, "turn");
            activeTurnId = turnObject == null ? null : string(turnObject, "id");
            String result = completed.get(240, TimeUnit.SECONDS);
            if ("failed".equals(result) || "interrupted".equals(result)) {
                throw new IOException("Codex Agent 回合" + ("failed".equals(result) ? "失败" : "被中断"));
            }
            status.accept("Codex App Server 已连接；棋盘 MCP 调用完成");
            return "Agent 回合完成";
        } catch (Exception e) {
            String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            Process current = process;
            if (current != null && current.isAlive()) {
                status.accept("Codex App Server 已连接；Agent 回合异常：" + message);
            } else {
                status.accept("Codex App Server 未连接：" + message);
            }
            return "Agent 请求失败：" + message;
        } finally {
            if (completed != null && activeTurn == completed) {
                activeTurn = null;
                activeTurnId = null;
            }
        }
    }

    /** The board reset starts a fresh persistent thread on the next turn. */
    public synchronized void newGame() {
        threadId = null;
        threadLoaded = false;
        status.accept("新对局 Agent 线程待启动");
    }

    private synchronized void ensureThread(String cwd) throws Exception {
        ensureProcess();
        status.accept("正在连接 Codex App Server…");
        if (!initialized) {
            String init = rpc("initialize", "{\"clientInfo\":{\"name\":\"xiangqi-desktop\",\"version\":\"1.0.0\"},\"capabilities\":{}}", 45);
            if (hasError(init)) throw new IOException(errorMessage(init));
            notifyServer("initialized", "{}");
            initialized = true;
        }
        if (threadId != null && threadLoaded) return;

        String developerInstructions = "你是中国象棋对弈 Agent，执黑方。红方由玩家控制。必须通过 MCP 工具 get_board 获取实际局面，并且只能通过 MCP 工具 play_move 提交黑方一步合法棋。不要调用 shell、改文件或使用其他工具。";
        String config = "{\"mcp_servers\":{\"xiangqi\":{\"tools\":{" +
                "\"get_board\":{\"approval_mode\":\"approve\"}," +
                "\"play_move\":{\"approval_mode\":\"approve\"}}}}}";
        String response;
        if (threadId == null) {
            String params = "{\"cwd\":" + quote(new File(cwd).getAbsolutePath()) +
                    ",\"serviceName\":\"中国象棋对弈\",\"developerInstructions\":" + quote(developerInstructions) +
                    ",\"config\":" + config + "}";
            response = rpc("thread/start", params, 90);
        } else {
            String params = "{\"threadId\":" + quote(threadId) + ",\"cwd\":" + quote(new File(cwd).getAbsolutePath()) +
                    ",\"config\":" + config + "}";
            response = rpc("thread/resume", params, 90);
        }
        if (hasError(response)) throw new IOException(errorMessage(response));
        if (threadId == null) {
            String result = object(response, "result");
            String thread = result == null ? null : string(result, "id");
            if (thread == null) throw new IOException("Codex App Server 未返回 threadId");
            threadId = thread;
            // App Server derives the initial sidebar title from the first user message.
            // Give the game a stable, recognizable title before starting the first turn.
            String rename = rpc("thread/name/set", "{\"threadId\":" + quote(threadId) +
                    ",\"name\":\"中国象棋对局\"}", 30);
            if (hasError(rename)) throw new IOException(errorMessage(rename));
        }
        threadLoaded = true;
        status.accept("Codex App Server 已连接；对局会话已建立");
    }

    private synchronized void ensureProcess() throws IOException {
        if (process != null && process.isAlive()) return;
        String executable = System.getenv("CODEX_CLI_PATH");
        if (executable == null || executable.trim().isEmpty()) executable = System.getProperty("xiangqi.codex.command", "codex");
        ProcessBuilder builder = new ProcessBuilder(executable, "app-server", "--stdio");
        builder.redirectErrorStream(false);
        Process child = builder.start();
        process = child;
        initialized = false;
        threadLoaded = false;
        synchronized (stderrText) { stderrText.setLength(0); }
        writer = new BufferedWriter(new OutputStreamWriter(child.getOutputStream(), StandardCharsets.UTF_8));
        Thread reader = new Thread(() -> readMessages(child), "codex-app-server-reader");
        reader.setDaemon(true);
        reader.start();
        Thread errors = new Thread(() -> drainErrors(child), "codex-app-server-stderr");
        errors.setDaemon(true);
        errors.start();
    }

    private String rpc(String method, String params, int timeoutSeconds) throws Exception {
        String id = Long.toString(ids.incrementAndGet());
        CompletableFuture<String> reply = new CompletableFuture<>();
        pending.put(id, reply);
        write("{\"jsonrpc\":\"2.0\",\"id\":" + id + ",\"method\":" + quote(method) + ",\"params\":" + params + "}");
        return reply.get(timeoutSeconds, TimeUnit.SECONDS);
    }

    private void notifyServer(String method, String params) throws IOException {
        write("{\"jsonrpc\":\"2.0\",\"method\":" + quote(method) + ",\"params\":" + params + "}");
    }

    private void write(String message) throws IOException {
        synchronized (writeLock) {
            if (writer == null) throw new IOException("Codex App Server 尚未启动");
            writer.write(message);
            writer.newLine();
            writer.flush();
        }
    }

    private void readMessages(Process source) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(source.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String method = string(line, "method");
                if (method != null) {
                    if ("turn/completed".equals(method)) {
                        String result = object(line, "params");
                        String turn = result == null ? null : object(result, "turn");
                        String state = turn == null ? null : string(turn, "status");
                        String completedId = turn == null ? null : string(turn, "id");
                        CompletableFuture<String> done = activeTurn;
                        if (done != null && (activeTurnId == null || completedId == null || activeTurnId.equals(completedId))) {
                            done.complete(state == null ? "completed" : state);
                        }
                    } else if ("mcpServer/startupStatus/updated".equals(method)) {
                        String detail = string(line, "serverName");
                        if (detail != null && detail.equals("xiangqi")) status.accept("Codex 正在连接棋盘 MCP…");
                    }
                    continue;
                }
                String id = scalarId(line);
                if (id != null) {
                    CompletableFuture<String> reply = pending.remove(id);
                    if (reply != null) reply.complete(line);
                }
            }
        } catch (IOException ignored) {
            // Pending requests are completed below with a clear process-exit error.
        } finally {
            if (source == process) {
                IOException ended = new IOException("Codex App Server 连接已关闭");
                pending.values().forEach(f -> f.completeExceptionally(ended));
                pending.clear();
                CompletableFuture<String> done = activeTurn;
                if (done != null) done.completeExceptionally(ended);
                initialized = false;
                threadLoaded = false;
                String detail;
                synchronized (stderrText) { detail = stderrText.toString().trim(); }
                String state = threadId == null
                        ? "Codex App Server 已断开"
                        : "Codex App Server 已断开；对局会话已保存，下回合将续接";
                status.accept(detail.isEmpty() ? state : state + "：" + detail);
            }
        }
    }

    private void drainErrors(Process source) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(source.getErrorStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                synchronized (stderrText) {
                    stderrText.append(line).append('\n');
                    if (stderrText.length() > 4000) stderrText.delete(0, stderrText.length() - 4000);
                }
            }
        } catch (IOException ignored) { }
    }

    private static boolean hasError(String json) {
        return Pattern.compile("\\\"error\\\"\\s*:").matcher(json).find();
    }

    private static String errorMessage(String json) {
        String error = object(json, "error");
        String message = error == null ? null : string(error, "message");
        return message == null ? "Codex App Server 返回错误" : message;
    }

    private static String scalarId(String json) {
        Matcher matcher = Pattern.compile("\\\"id\\\"\\s*:\\s*(\\d+|\\\"(?:\\\\.|[^\\\"])*\\\")").matcher(json);
        if (!matcher.find()) return null;
        String value = matcher.group(1);
        return value.startsWith("\"") ? value.substring(1, value.length() - 1) : value;
    }

    private static String string(String json, String key) {
        Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"").matcher(json);
        if (!matcher.find()) return null;
        return matcher.group(1).replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private static String object(String json, String key) {
        Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\{").matcher(json);
        if (!matcher.find()) return null;
        int start = matcher.end() - 1, depth = 0;
        boolean quoted = false, escaped = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (quoted) {
                if (escaped) escaped = false;
                else if (c == '\\') escaped = true;
                else if (c == '\"') quoted = false;
            } else if (c == '\"') quoted = true;
            else if (c == '{') depth++;
            else if (c == '}' && --depth == 0) return json.substring(start, i + 1);
        }
        return null;
    }

    private static String quote(String value) {
        StringBuilder out = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\': out.append("\\\\"); break;
                case '\"': out.append("\\\""); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                default:
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
            }
        }
        return out.append('\"').toString();
    }

    @Override
    public synchronized void close() {
        threadId = null;
        initialized = false;
        threadLoaded = false;
        Process current = process;
        process = null;
        writer = null;
        if (current != null) {
            try { current.getOutputStream().close(); } catch (IOException ignored) { }
            if (current.isAlive()) current.destroy();
            try {
                if (!current.waitFor(2, TimeUnit.SECONDS)) current.destroyForcibly();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                current.destroyForcibly();
            }
        }
        status.accept("Codex App Server 已关闭");
    }
}
