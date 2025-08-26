package com.ruoyi.business.service.DataPool.WebSocket;

import com.ruoyi.business.enums.ConnectionState;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * WebSocket客户端（OkHttp 实现）
 * 负责与WebSocket服务器的连接和通信
 */
public class WebSocketClient {

    private static final Logger log = LoggerFactory.getLogger(WebSocketClient.class);

    private static final OkHttpClient sharedClient = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(15))
            .readTimeout(Duration.ofSeconds(0)) // WebSocket 读超时置0表示无限
            .writeTimeout(Duration.ofSeconds(15))
            .pingInterval(30, TimeUnit.SECONDS)
            .build();

    private final URI serverUri;
    private final AtomicBoolean connected = new AtomicBoolean(false);

    private Consumer<String> messageHandler;
    private Consumer<ConnectionState> connectionStateHandler;

    private volatile WebSocket webSocket;

    public WebSocketClient(URI serverUri) {
        this.serverUri = serverUri;
    }

    /**
     * 设置消息处理器
     */
    public void setMessageHandler(Consumer<String> messageHandler) {
        this.messageHandler = messageHandler;
    }

    /**
     * 设置连接状态处理器
     */
    public void setConnectionStateHandler(Consumer<ConnectionState> connectionStateHandler) {
        this.connectionStateHandler = connectionStateHandler;
    }

    /**
     * 连接到WebSocket服务器
     */
    public CompletableFuture<Void> connect() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            Request request = new Request.Builder()
                    .url(serverUri.toString())
                    .build();

            log.info("[WebSocketClient] 连接到WebSocket服务器: {}", serverUri);

            this.webSocket = sharedClient.newWebSocket(request, new WebSocketListener() {
                @Override
                public void onOpen(WebSocket webSocket, Response response) {
                    connected.set(true);
                    notifyConnectionStateChange(ConnectionState.CONNECTED);
                    log.info("[WebSocketClient] WebSocket连接成功: {}", serverUri);
                    future.complete(null);
                }

                @Override
                public void onMessage(WebSocket webSocket, String text) {
                    log.debug("[WebSocketClient] 收到文本消息: {}", text);
                    if (messageHandler != null) {
                        try {
                            messageHandler.accept(text);
                        } catch (Exception e) {
                            log.error("[WebSocketClient] 处理文本消息异常", e);
                        }
                    }
                }

                @Override
                public void onMessage(WebSocket webSocket, ByteString bytes) {
                    String text = bytes.utf8();
                    log.debug("[WebSocketClient] 收到二进制消息(转文本): {}", text);
                    if (messageHandler != null) {
                        try {
                            messageHandler.accept(text);
                        } catch (Exception e) {
                            log.error("[WebSocketClient] 处理二进制消息异常", e);
                        }
                    }
                }

                @Override
                public void onClosing(WebSocket webSocket, int code, String reason) {
                    log.info("[WebSocketClient] 连接即将关闭: code={}, reason={}", code, reason);
                    webSocket.close(code, reason);
                }

                @Override
                public void onClosed(WebSocket webSocket, int code, String reason) {
                    connected.set(false);
                    notifyConnectionStateChange(ConnectionState.DISCONNECTED);
                    log.info("[WebSocketClient] 连接已关闭: code={}, reason={}", code, reason);
                }

                @Override
                public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                    connected.set(false);
                    notifyConnectionStateChange(ConnectionState.ERROR);
                    log.error("[WebSocketClient] 连接失败: {}", serverUri, t);
                    if (!future.isDone()) {
                        future.completeExceptionally(t);
                    }
                }
            });
        } catch (Exception e) {
            log.error("[WebSocketClient] WebSocket连接异常: {}", serverUri, e);
            notifyConnectionStateChange(ConnectionState.ERROR);
            future.completeExceptionally(e);
        }
        return future;
    }

    /**
     * 发送消息
     */
    public CompletableFuture<Void> sendMessage(String message) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        if (!isConnected() || webSocket == null) {
            future.completeExceptionally(new IllegalStateException("WebSocket未连接"));
            return future;
        }
        try {
            boolean ok = webSocket.send(message);
            if (ok) {
                future.complete(null);
            } else {
                future.completeExceptionally(new RuntimeException("发送失败"));
            }
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    /**
     * 关闭连接
     */
    public void close() {
        try {
            WebSocket ws = this.webSocket;
            this.webSocket = null;
            if (ws != null) {
                log.info("[WebSocketClient] 关闭WebSocket连接: {}", serverUri);
                ws.close(1000, "normal");
            }
        } catch (Exception e) {
            log.error("[WebSocketClient] 关闭WebSocket连接失败: {}", serverUri, e);
        }
    }

    public boolean isConnected() {
        return connected.get();
    }

    public URI getServerUri() {
        return serverUri;
    }

    private void notifyConnectionStateChange(ConnectionState newState) {
        if (connectionStateHandler != null) {
            try {
                connectionStateHandler.accept(newState);
            } catch (Exception e) {
                log.error("[WebSocketClient] 处理连接状态变化时发生异常: {}", newState, e);
            }
        }
    }
}
