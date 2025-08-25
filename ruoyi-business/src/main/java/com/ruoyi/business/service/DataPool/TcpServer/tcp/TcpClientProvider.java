package com.ruoyi.business.service.DataPool.TcpServer.tcp;

import com.ruoyi.business.domain.DataPool;
import com.ruoyi.business.domain.config.ParsingRuleConfig;
import com.ruoyi.business.domain.config.TcpServerSourceConfig;
import com.ruoyi.business.domain.config.TriggerConfig;
import com.ruoyi.business.service.DataPool.DataPoolConfigFactory;
import com.ruoyi.business.service.DataPool.IDataPoolService;
import com.ruoyi.business.service.DataPool.TcpServer.tcp.handler.TcpClientHandler;
import com.ruoyi.business.service.common.DataIngestionService;
import com.ruoyi.business.service.common.ParsingRuleEngineService;
import com.ruoyi.business.enums.PoolStatus;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 针对单个数据池的 TCP 客户端提供者
 * 负责连接管理、发送请求、接收响应与回调入库
 */
public class TcpClientProvider {

    public enum ConnectionState {
        DISCONNECTED, CONNECTING, CONNECTED
    }

    private static final Logger log = LoggerFactory.getLogger(TcpClientProvider.class);

    private final Long poolId;
    private final IDataPoolService dataPoolService;
    private final DataPoolConfigFactory configFactory;
    private final DataIngestionService ingestionService;
    private final ParsingRuleEngineService parsingRuleEngineService;

    private volatile TcpServerSourceConfig sourceConfig;
    private volatile TriggerConfig triggerConfig;
    private volatile ParsingRuleConfig parsingRuleConfig;

    private final EventLoopGroup eventLoopGroup = new NioEventLoopGroup(1);
    private Bootstrap bootstrap;
    private volatile Channel channel;
    private volatile ConnectionState connectionState = ConnectionState.DISCONNECTED;
    private final AtomicBoolean requestInProgress = new AtomicBoolean(false);

    public TcpClientProvider(Long poolId,
                             IDataPoolService dataPoolService,
                             DataPoolConfigFactory configFactory,
                             DataIngestionService ingestionService,
                             ParsingRuleEngineService parsingRuleEngineService) {
        this.poolId = poolId;
        this.dataPoolService = dataPoolService;
        this.configFactory = configFactory;
        this.ingestionService = ingestionService;
        this.parsingRuleEngineService = parsingRuleEngineService;
        reloadConfigs();
        initBootstrap();
    }

    /**
     * 重新加载数据池配置（源、触发、解析）
     */
    public synchronized void reloadConfigs() {
        DataPool pool = dataPoolService.selectDataPoolById(poolId);
        if (pool == null) {
            log.warn("[TcpClientProvider] 数据池不存在: {}", poolId);
            return;
        }
        try {
            this.sourceConfig = (TcpServerSourceConfig) configFactory.parseSourceConfig("TCP_SERVER", pool.getSourceConfigJson());
        } catch (Exception e) {
            log.error("解析TCP_SERVER配置失败: {}", e.getMessage(), e);
        }
        try {
            this.triggerConfig = configFactory.parseTriggerConfig(pool.getTriggerConfigJson());
        } catch (Exception e) {
            log.error("解析触发配置失败: {}", e.getMessage(), e);
        }
        try {
            this.parsingRuleConfig = configFactory.parseParsingRuleConfig(pool.getParsingRuleJson());
        } catch (Exception e) {
            log.error("解析规则配置失败: {}", e.getMessage(), e);
        }
    }

    private void initBootstrap() {
        this.bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        // 基于换行符的消息边界处理，避免粘包/拆包
                        pipeline.addLast(new DelimiterBasedFrameDecoder(1024 * 1024, Delimiters.lineDelimiter()));
                        pipeline.addLast(new StringDecoder(StandardCharsets.UTF_8));
                        pipeline.addLast(new StringEncoder(StandardCharsets.UTF_8));
                        pipeline.addLast(new TcpClientHandler(TcpClientProvider.this));
                    }
                });
    }

    /**
     * 确保建立连接（自动重连）
     */
    public synchronized void ensureConnected() {

        if (connectionState == ConnectionState.CONNECTED || connectionState == ConnectionState.CONNECTING) {
            return;
        }
        // 仅在数据池状态为 RUNNING 时才尝试连接
        DataPool latest = dataPoolService.selectDataPoolById(poolId);
        if (latest == null || !PoolStatus.RUNNING.getCode().equals(latest.getStatus())) {
            log.info("[TcpClientProvider] 数据池非运行状态，跳过连接: poolId={}", poolId);
            return;
        }
        if (sourceConfig == null || sourceConfig.getIpAddress() == null || sourceConfig.getPort() == null) {
            log.error("[TcpClientProvider] 配置不完整，无法连接: poolId={}", poolId);
            return;
        }
        // 写回连接状态
        dataPoolService.updateConnectionState(poolId, ConnectionState.CONNECTING.name());
        doConnect(1);
    }

    private void doConnect(int attempt) {
        connectionState = ConnectionState.CONNECTING;
        final String host = sourceConfig.getIpAddress();
        final int port = sourceConfig.getPort();
        log.info("[TcpClientProvider] 尝试连接到 {}:{} (attempt={})", host, port, attempt);
        bootstrap.connect(host, port).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                channel = future.channel();
                connectionState = ConnectionState.CONNECTED;
                log.info("[TcpClientProvider] 连接成功: {}:{}", host, port);
                dataPoolService.updateConnectionState(poolId, ConnectionState.CONNECTED.name());
                // 监听关闭事件，仅更新状态，不再重连
                channel.closeFuture().addListener(cf -> {
                    log.warn("[TcpClientProvider] 通道关闭，准备重连 {}:{}", host, port);
                    onChannelInactive();
                });
            } else {
                connectionState = ConnectionState.DISCONNECTED;
                int nextDelay = Math.min(30, attempt <= 3 ? 5 * attempt : 10 * (attempt - 2));
                log.warn("[TcpClientProvider] 连接失败: {}", future.cause().getMessage());
                dataPoolService.updateConnectionState(poolId, ConnectionState.DISCONNECTED.name());
                // 不再自动重连
            }
        });
    }

    /**
     * 发送取数指令（带并发保护）
     */
    public void requestDataIfConnected() {
        if (connectionState != ConnectionState.CONNECTED || channel == null || !channel.isActive()) {
            return;
        }
        if (!requestInProgress.compareAndSet(false, true)) {
            // 上一次请求尚未完成
            return;
        }
        try {
            String command = triggerConfig != null && triggerConfig.getRequestCommand() != null
                    ? triggerConfig.getRequestCommand()
                    : "";
            if (Objects.equals(command, "")) {
                // 无命令则直接释放繁忙标志，避免阻塞
                requestInProgress.set(false);
                return;
            }
            // 以换行结尾，配合解码器
            String payload = command.endsWith("\n") ? command : (command + "\n");
            channel.writeAndFlush(Unpooled.copiedBuffer(payload, StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("[TcpClientProvider] 发送请求失败: {}", e.getMessage(), e);
            requestInProgress.set(false);
        }
    }

    /**
     * 收到一条完整消息
     */
    public void onMessage(String message) {
        try {
            List<String> items = parsingRuleEngineService.extractItems(message, parsingRuleConfig);
            if (items == null || items.isEmpty()) {
                requestInProgress.set(false);
                return;
            }
            ingestionService.ingestItems(poolId, items);
        } catch (Exception e) {
            log.error("[TcpClientProvider] 处理消息失败: {}", e.getMessage(), e);
        } finally {
            requestInProgress.set(false);
        }
    }

    /**
     * 断线回调
     */
    public void onChannelInactive() {
        connectionState = ConnectionState.DISCONNECTED;
        dataPoolService.updateConnectionState(poolId, ConnectionState.DISCONNECTED.name());
        // 不再自动重连
    }

    public boolean isRequestInProgress() {
        return requestInProgress.get();
    }

    public ConnectionState getConnectionState() {
        return connectionState;
    }

    public void close() {
        try {
            if (channel != null) {
                channel.close();
            }
        } finally {
            eventLoopGroup.shutdownGracefully();
            connectionState = ConnectionState.DISCONNECTED;
        }
    }
}


