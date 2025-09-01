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
import org.springframework.context.ApplicationEventPublisher;
import com.ruoyi.business.events.ConnectionStateChangedEvent;
import com.ruoyi.business.enums.PoolStatus;
import com.ruoyi.business.enums.ConnectionState;
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
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

/**
 * 针对单个数据池的 TCP 客户端提供者
 * 负责连接管理、发送请求、接收响应与回调入库
 */
public class TcpClientProvider {

    private static final Logger log = LoggerFactory.getLogger(TcpClientProvider.class);

    private final Long poolId;
    private final IDataPoolService dataPoolService;
    private final DataPoolConfigFactory configFactory;
    private final DataIngestionService ingestionService;
    private final ParsingRuleEngineService parsingRuleEngineService;
    private final ApplicationEventPublisher eventPublisher;

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
                             ParsingRuleEngineService parsingRuleEngineService,
                             ApplicationEventPublisher eventPublisher) {
        this.poolId = poolId;
        this.dataPoolService = dataPoolService;
        this.configFactory = configFactory;
        this.ingestionService = ingestionService;
        this.parsingRuleEngineService = parsingRuleEngineService;
        this.eventPublisher = eventPublisher;
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
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ch.pipeline()
                                .addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()))
                                .addLast(new StringDecoder(StandardCharsets.UTF_8))
                                .addLast(new StringEncoder(StandardCharsets.UTF_8))
                                .addLast(new TcpClientHandler(TcpClientProvider.this));
                    }
                });
    }

    /**
     * 确保连接建立
     */
    public void ensureConnected() {
        if (connectionState == ConnectionState.CONNECTED && channel != null && channel.isActive()) {
            return;
        }

        if (connectionState == ConnectionState.CONNECTING) {
            log.debug("[TcpClientProvider] 正在连接中，跳过重复连接请求");
            return;
        }

        if (sourceConfig == null) {
            log.warn("[TcpClientProvider] 源配置为空，无法建立连接");
            return;
        }

        updateConnectionState(ConnectionState.CONNECTING);
        
        try {
            ChannelFuture future = bootstrap.connect(sourceConfig.getIpAddress(), sourceConfig.getPort());
            future.addListener((ChannelFutureListener) channelFuture -> {
                if (channelFuture.isSuccess()) {
                    this.channel = channelFuture.channel();
                    updateConnectionState(ConnectionState.CONNECTED);
                    log.info("[TcpClientProvider] 连接成功: {}:{}", sourceConfig.getIpAddress(), sourceConfig.getPort());
                } else {
//                    updateConnectionState(ConnectionState.DISCONNECTED);
                    log.error("[TcpClientProvider] 连接失败: {}:{}", sourceConfig.getIpAddress(), sourceConfig.getPort(), channelFuture.cause());
                }
            });
        } catch (Exception e) {
//            updateConnectionState(ConnectionState.DISCONNECTED);
            log.error("[TcpClientProvider] 连接异常: {}:{}", sourceConfig.getIpAddress(), sourceConfig.getPort(), e);
        }
    }

    /**
     * 如果已连接则发送数据请求
     */
    public void requestDataIfConnected() {
        if (connectionState != ConnectionState.CONNECTED || channel == null || !channel.isActive()) {
            log.debug("[TcpClientProvider] 连接未建立，跳过数据请求");
            return;
        }

        if (requestInProgress.get()) {
            log.debug("[TcpClientProvider] 请求进行中，跳过重复请求");
            return;
        }

        if (triggerConfig == null || StringUtils.isEmpty(triggerConfig.getRequestCommand())) {
            log.warn("[TcpClientProvider] 触发配置或请求指令为空");
            return;
        }

        requestInProgress.set(true);
        try {
            String command = triggerConfig.getRequestCommand() + "\n";
            channel.writeAndFlush(Unpooled.copiedBuffer(command, StandardCharsets.UTF_8));
            log.debug("[TcpClientProvider] 发送请求指令: {}", triggerConfig.getRequestCommand());
        } catch (Exception e) {
            requestInProgress.set(false);
            log.error("[TcpClientProvider] 发送请求指令失败", e);
        }
    }

    /**
     * 处理接收到的数据
     */
    public void onDataReceived(String data) {
        log.debug("[TcpClientProvider] 收到数据: {}", data);
        
        try {
            // 解析数据
            List<String> items = parsingRuleEngineService.extractItems(data, parsingRuleConfig);
            
            if (items != null && !items.isEmpty()) {
                // 入库
                ingestionService.ingestItems(poolId, items);
                log.info("[TcpClientProvider] 成功处理 {} 条数据", items.size());
            } else {
                log.debug("[TcpClientProvider] 解析结果为空");
            }
        } catch (Exception e) {
            log.error("[TcpClientProvider] 处理数据失败", e);
        } finally {
            // 重置请求状态
            requestInProgress.set(false);
        }
    }

    /**
     * 关闭连接
     */
    public void close() {
        updateConnectionState(ConnectionState.DISCONNECTED);
        
        if (channel != null) {
            channel.close();
            channel = null;
        }
        
        if (eventLoopGroup != null) {
            eventLoopGroup.shutdownGracefully();
        }
        
        log.info("[TcpClientProvider] 连接已关闭");
    }

    /**
     * 更新连接状态
     */
    private void updateConnectionState(ConnectionState state) {
        this.connectionState = state;
        try {
            dataPoolService.updateConnectionState(poolId, state.getCode());
            // 发布事件，交由调度器监听
            if (eventPublisher != null) {
                eventPublisher.publishEvent(new ConnectionStateChangedEvent(poolId, state));
            }
            log.debug("[TcpClientProvider] 连接状态更新为: {}", state.getInfo());
        } catch (Exception e) {
            log.error("[TcpClientProvider] 更新连接状态失败", e);
        }
    }

    /**
     * 是否已连接
     */
    public boolean isConnected() {
        return connectionState == ConnectionState.CONNECTED && channel != null && channel.isActive();
    }
}


