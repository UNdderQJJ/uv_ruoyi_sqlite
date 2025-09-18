package com.ruoyi.business.service.DataPool.type.TcpClient.tcp;

import com.ruoyi.business.domain.DataPool.DataPool;
import com.ruoyi.business.domain.config.ParsingRuleConfig;
import com.ruoyi.business.domain.config.TcpClientSourceConfig;
import com.ruoyi.business.domain.config.TriggerConfig;
import com.ruoyi.business.service.DataPool.DataPoolConfigFactory;
import com.ruoyi.business.service.DataPool.IDataPoolService;
import com.ruoyi.business.service.DataPool.type.TcpClient.tcp.handler.TcpServerHandler;
import com.ruoyi.business.service.common.ParsingRuleEngineService;
import com.ruoyi.business.service.common.DataIngestionService;
import org.springframework.context.ApplicationEventPublisher;
import com.ruoyi.business.events.ConnectionStateChangedEvent;
import com.ruoyi.business.enums.ConnectionState;
import com.ruoyi.business.enums.PoolStatus;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * TCP 服务端提供者
 * 实现"守门员"机制：只允许一个客户端连接
 * 注意：此类不由 Spring 直接管理，而是通过 TcpServerManager 创建和管理
 */
public class TcpServerProvider {
    
    private static final Logger log = LoggerFactory.getLogger(TcpServerProvider.class);
    
    private final Long poolId;
    private final IDataPoolService dataPoolService;
    private final DataPoolConfigFactory configFactory;
    private final DataIngestionService dataIngestionService;
    private final ParsingRuleEngineService parsingRuleEngineService;
    private final ApplicationEventPublisher eventPublisher;
    
    private volatile TcpClientSourceConfig sourceConfig;
    private volatile TriggerConfig triggerConfig;
    private volatile ParsingRuleConfig parsingRuleConfig;
    
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    
    // 守门员机制：只允许一个客户端连接
    private volatile Channel activeClientChannel = null;
    
    // 防止重复请求
    private final AtomicBoolean requestInProgress = new AtomicBoolean(false);
    
    public TcpServerProvider(Long poolId,
                             IDataPoolService dataPoolService,
                             DataPoolConfigFactory configFactory,
                             DataIngestionService dataIngestionService,
                             ParsingRuleEngineService parsingRuleEngineService,
                             ApplicationEventPublisher eventPublisher) {
        this.poolId = poolId;
        this.dataPoolService = dataPoolService;
        this.configFactory = configFactory;
        this.dataIngestionService = dataIngestionService;
        this.parsingRuleEngineService = parsingRuleEngineService;
        this.eventPublisher = eventPublisher;
        reloadConfigs();
    }

    /**
     * 初始化提供者
     * 由 TcpServerManager 在创建后调用
     */
    public void initialize() {
        // 仅在数据池状态为 RUNNING 时才启动服务端
        DataPool latest = dataPoolService.selectDataPoolById(poolId);
        if (latest == null || !PoolStatus.RUNNING.getCode().equals(latest.getStatus())) {
            log.info("[TcpServerProvider] 数据池非运行状态，跳过启动: poolId={}", poolId);
            return;
        }
        
        if (!initBootstrap()) {
            log.error("Netty 服务端初始化失败，数据池ID: {}", poolId);
            return;
        }
        
        log.info("TCP 服务端提供者初始化完成，数据池ID: {}", poolId);
    }
    
    /**
     * 重新加载数据池配置（源、触发、解析）
     */
    public synchronized void reloadConfigs() {
        DataPool pool = dataPoolService.selectDataPoolById(poolId);
        if (pool == null) {
            log.warn("[TcpServerProvider] 数据池不存在: {}", poolId);
            return;
        }
        try {
            this.sourceConfig = (TcpClientSourceConfig) configFactory.parseSourceConfig("TCP_CLIENT", pool.getSourceConfigJson());
        } catch (Exception e) {
            log.error("解析TCP_CLIENT配置失败: {}", e.getMessage(), e);
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
    
    /**
     * 初始化 Netty 服务端
     * @return 是否成功
     */
    private boolean initBootstrap() {
        // 验证配置
        if (this.sourceConfig == null || this.sourceConfig.getListenPort() == null) {
            log.error("配置未初始化，无法启动 Netty 服务端，数据池ID: {}", poolId);
            return false;
        }
        
        try {
            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup(1);
            
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            
                            // 使用换行符作为消息分隔符
                            pipeline.addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
                            pipeline.addLast(new StringDecoder(CharsetUtil.UTF_8));
                            pipeline.addLast(new StringEncoder(CharsetUtil.UTF_8));
                            
                            // 自定义处理器
                            pipeline.addLast(new TcpServerHandler(TcpServerProvider.this));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            
            // 绑定端口
            ChannelFuture future = bootstrap.bind(sourceConfig.getListenPort()).sync();
            serverChannel = future.channel();
            
            // 更新连接状态为监听中
            updateConnectionState(ConnectionState.LISTENING);
            
            log.info("TCP 服务端启动成功，数据池ID: {}, 监听端口: {}", poolId, sourceConfig.getListenPort());
            return true;
            
        } catch (Exception e) {
            log.error("TCP 服务端启动失败，数据池ID: {}, 端口: {}", poolId, sourceConfig.getListenPort(), e);
            updateConnectionState(ConnectionState.ERROR);
            
            // 清理资源
            if (bossGroup != null) {
                bossGroup.shutdownGracefully();
            }
            if (workerGroup != null) {
                workerGroup.shutdownGracefully();
            }
            
            return false;
        }
    }
    
    /**
     * 处理客户端连接
     */
    public void onClientConnected(Channel clientChannel) {
        if (activeClientChannel != null) {
            // 已有活动连接，拒绝新连接
            log.warn("拒绝新的客户端 [{}] 连接，因为已有活动连接存在", 
                    clientChannel.remoteAddress());
            clientChannel.close();
            return;
        }
        
        // 接受第一个客户端连接
        activeClientChannel = clientChannel;
        updateConnectionState(ConnectionState.CONNECTED);
        
        log.info("客户端 [{}] 已连接，建立专线通信", clientChannel.remoteAddress());
        
        // 监听客户端断开事件
        clientChannel.closeFuture().addListener(future -> {
            onClientDisconnected();
        });
    }
    
    /**
     * 处理客户端断开
     */
    public void onClientDisconnected() {
        if (activeClientChannel != null) {
            log.info("客户端 [{}] 已断开连接", activeClientChannel.remoteAddress());
            activeClientChannel = null;
            updateConnectionState(ConnectionState.LISTENING);
        }
    }
    
    /**
     * 请求数据（由调度器触发）
     */
    public void requestData() {
        // 验证配置
        if (triggerConfig == null) {
            log.error("配置未初始化，无法发送数据请求，数据池ID: {}", poolId);
            return;
        }
        
        if (activeClientChannel == null || !activeClientChannel.isActive()) {
            log.debug("没有活动的客户端连接，跳过数据请求，数据池ID: {}", poolId);
            return;
        }
        
        if (requestInProgress.get()) {
            log.debug("请求正在进行中，跳过本次触发，数据池ID: {}", poolId);
            return;
        }
        
        try {
            requestInProgress.set(true);
            
            String requestCommand = triggerConfig.getRequestCommand();
            if (requestCommand != null && !requestCommand.trim().isEmpty()) {
                // 发送请求指令，添加换行符
                activeClientChannel.writeAndFlush(requestCommand + "\n");
                log.debug("已发送请求指令: {}, 数据池ID: {}", requestCommand, poolId);
            } else {
                log.debug("请求指令为空，跳过发送，数据池ID: {}", poolId);
            }
            
        } catch (Exception e) {
            log.error("发送请求指令失败，数据池ID: {}", poolId, e);
        } finally {
            requestInProgress.set(false);
        }
    }
    
    /**
     * 处理接收到的数据
     */
    public void onDataReceived(String responseData) {
        // 验证配置
        if (parsingRuleConfig == null) {
            log.error("配置未初始化，无法处理接收数据，数据池ID: {}", poolId);
            return;
        }
        
        try {
            log.debug("接收到数据: {}, 数据池ID: {}", responseData, poolId);

            // 处理十六进制数据
            responseData = parsingRuleEngineService.convertHexToAsciiIfNeeded(responseData);
            // 解析数据
            List<String> items = parsingRuleEngineService.extractItems(responseData, parsingRuleConfig);
            if (items == null || items.isEmpty()) {
                log.debug("解析结果为空，数据池ID: {}", poolId);
                return;
            }
            
            // 数据入库
            dataIngestionService.ingestItems(poolId, items);
            
            log.info("数据处理完成，数据池ID: {}, 解析出 {} 条记录", poolId, items.size());
            
        } catch (Exception e) {
            log.error("处理接收数据失败，数据池ID: {}", poolId, e);
        }
    }
    
    /**
     * 更新连接状态
     */
    private void updateConnectionState(ConnectionState state) {
        try {
            dataPoolService.updateConnectionState(poolId, state.getCode());
            if (eventPublisher != null) {
                eventPublisher.publishEvent(new ConnectionStateChangedEvent(poolId, state));
            }
        } catch (Exception e) {
            log.error("更新连接状态失败，数据池ID: {}, 状态: {}", poolId, state, e);
        }
    }
    
    /**
     * 关闭服务端
     */
    public void close() {
        if (activeClientChannel != null) {
            activeClientChannel.close();
            activeClientChannel = null;
        }
        
        if (serverChannel != null) {
            serverChannel.close();
        }
        
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        
        updateConnectionState(ConnectionState.DISCONNECTED);
        log.info("TCP 服务端已关闭，数据池ID: {}", poolId);
    }
    
    /**
     * 获取当前连接状态
     */
    public ConnectionState getConnectionState() {
        if (activeClientChannel != null && activeClientChannel.isActive()) {
            return ConnectionState.CONNECTED;
        } else if (serverChannel != null && serverChannel.isActive()) {
            return ConnectionState.LISTENING;
        } else {
            return ConnectionState.DISCONNECTED;
        }
    }
    
    /**
     * 获取活动客户端通道
     */
    public Channel getActiveClientChannel() {
        return activeClientChannel;
    }
    
    /**
     * 是否已建立专线（有活动客户端）
     */
    public boolean isConnected() {
        return activeClientChannel != null && activeClientChannel.isActive();
    }
    
    /**
     * 获取数据池ID
     */
    public Long getPoolId() {
        return poolId;
    }
    
    @PreDestroy
    public void destroy() {
        close();
    }
}
