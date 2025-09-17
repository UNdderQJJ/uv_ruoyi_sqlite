package com.ruoyi.tcp;

import com.ruoyi.business.domain.DeviceInfo.DeviceInfo;
import com.ruoyi.business.enums.DeviceStatus;
import com.ruoyi.business.service.DeviceInfo.IDeviceInfoService;
import com.ruoyi.business.service.TaskInfo.DeviceDataHandlerService;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.AttributeKey;
import java.util.List;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.nio.charset.StandardCharsets;
import com.ruoyi.business.utils.StxEtxProtocolUtil;

/**
 * 设备连接管理器
 * 负责在系统启动时主动连接所有启用的设备
 */
@Component
public class DeviceConnectionManager {

    private static final Logger log = LoggerFactory.getLogger(DeviceConnectionManager.class);

    @Autowired
    private IDeviceInfoService deviceInfoService;

    @Autowired
    private DeviceDataHandlerService deviceDataHandlerService;

    @Value("${device.connection.timeout:5000}")
    private int connectionTimeout;

    @Value("${device.connection.retry-interval:30000}")
    private int retryInterval;

    private final EventLoopGroup workerGroup = new NioEventLoopGroup();
    private final Bootstrap bootstrap = new Bootstrap();

    @PostConstruct
    public void init() {
        // 配置Bootstrap
        bootstrap.group(workerGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeout)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                // 空闲状态检测
                                // writer idle 20s -> trigger ping; no reader/all idle close
                                .addLast(new IdleStateHandler(0, 20, 0, TimeUnit.SECONDS))
                                // STX/ETX协议解码器
                                .addLast(new StxEtxFrameDecoder())
                                // 字符串编解码器
                                .addLast(new StringEncoder(StandardCharsets.UTF_8))
                                // 设备数据处理器
                                .addLast(new DeviceClientHandler());
                    }
                });

        // 启动设备连接
        connectAllDevices();
    }

    /**
     * 连接所有启用的设备
     */
    public void connectAllDevices() {
        try {
            // 查询所有启用的设备
            DeviceInfo query = new DeviceInfo();
            query.setIsEnabled(1); // 只查询启用的设备
            var devices = deviceInfoService.selectDeviceInfoList(query);

            if (devices == null || devices.isEmpty()) {
                log.info("没有找到启用的设备");
                return;
            }

            log.info("开始连接 {} 个设备", devices.size());

            for (DeviceInfo device : devices) {
                if (device.getConnectionType() != null &&
                    "TCP".equals(device.getConnectionType()) &&
                    device.getIpAddress() != null &&
                    device.getPort() != null) {

                    // 异步连接设备
                    connectDevice(device);
                } else {
                    log.warn("设备 {} 配置不完整，跳过连接", device.getName());
                }
            }

        } catch (Exception e) {
            log.error("连接设备时发生异常", e);
        }
    }

    /**
     * 连接单个设备
     */
    public void connectDevice(DeviceInfo device) {
        try {
            log.info("正在连接设备: {} ({}:{})", device.getName(), device.getIpAddress(), device.getPort());

            ChannelFuture future = bootstrap.connect(device.getIpAddress(), device.getPort());

            future.addListener((ChannelFutureListener) future1 -> {
                if (future1.isSuccess()) {
                    Channel channel = future1.channel();

                    // 设置设备ID到Channel属性中
                    AttributeKey<String> deviceIdKey = AttributeKey.valueOf("deviceId");
                    channel.attr(deviceIdKey).set(device.getId().toString());

                    // 注册设备通道
                    deviceDataHandlerService.registerDeviceChannel(device.getId().toString(), channel);

                    log.info("设备连接成功: {} ({}:{})", device.getName(), device.getIpAddress(), device.getPort());

                    //不等于在线打印 更新设备状态为在线
                    if (!device.getStatus().equals(DeviceStatus.ONLINE_PRINTING.getCode())) {
                        deviceInfoService.updateDeviceStatus(device.getId(), DeviceStatus.ONLINE_IDLE.getCode());
                    }

                    // 发送初始ping建立心跳
                    sendInitialPing(channel, device.getId().toString());

                } else {
                    log.error("设备连接失败: {} ({}:{}), 原因: {}",
                            device.getName(), device.getIpAddress(), device.getPort(),
                            future1.cause().getMessage());

                    // 更新设备状态为离线
                    deviceInfoService.updateDeviceStatus(device.getId(), "OFFLINE");

                    // 延迟重试连接
                    scheduleReconnect(device);
                }
            });

        } catch (Exception e) {
            log.error("连接设备异常: {} ({}:{})", device.getName(), device.getIpAddress(), device.getPort(), e);
        }
    }


    /**
     * 通过已注册的设备通道发送命令（异步发送，不等待业务响应）。
     *
     * @param deviceId 设备ID（与注册时一致的字符串）
     * @param command  纯文本命令（内部按STX/ETX协议封装）
     * @return 是否成功写入到通道
     */
    public boolean sendCommandViaRegisteredChannel(String deviceId, String command) {
        try {
            Object ch = deviceDataHandlerService.getDeviceChannel(deviceId);
            if (!(ch instanceof Channel)) {
                log.warn("未找到已注册通道或类型不匹配，设备ID: {}", deviceId);
                return false;
            }
            Channel channel = (Channel) ch;
            if (!channel.isActive()) {
                log.warn("设备通道不活跃，设备ID: {}", deviceId);
                return false;
            }

            byte[] payload = StxEtxProtocolUtil.buildCommand(command);
            // 使用底层字节发送，避免额外字符串编码器影响
            channel.writeAndFlush(Unpooled.wrappedBuffer(payload));
            log.debug("已通过注册通道发送命令，设备ID: {}，命令: {}", deviceId, command);
            return true;
        } catch (Exception e) {
            log.error("通过注册通道发送命令异常，设备ID: {}", deviceId, e);
            return false;
        }
    }

    /**
     * 延迟重连设备
     */
    private void scheduleReconnect(DeviceInfo device) {
        workerGroup.schedule(() -> {
            log.info("尝试重新连接设备: {} ({}:{})", device.getName(), device.getIpAddress(), device.getPort());
            connectDevice(device);
        }, retryInterval, TimeUnit.MILLISECONDS);
    }

    /**
     * 发送初始ping建立心跳
     */
    private void sendInitialPing(Channel channel, String deviceId) {
        try {
            if (channel != null && channel.isActive()) {
                // 延迟1秒后发送ping，确保连接稳定
                workerGroup.schedule(() -> {
                    if (channel.isActive()) {
                        String pingCommand = "ping\n";
                        channel.writeAndFlush(pingCommand);
                        log.debug("发送初始ping到设备: {}", deviceId);
                    }
                }, 1, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.error("发送初始ping失败，设备ID: {}", deviceId, e);
        }
    }

    /**
     * 设备客户端处理器
     */
    private class DeviceClientHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            String deviceId = getDeviceIdFromChannel(ctx.channel());
            log.info("设备通道激活: {}", deviceId);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            String deviceId = getDeviceIdFromChannel(ctx.channel());
//            log.warn("设备通道断开: {}", deviceId);

            if (deviceId != null) {
//                // 更新设备状态为离线
//                try {
//                    deviceInfoService.updateDeviceStatus(Long.valueOf(deviceId), "OFFLINE");
//                } catch (Exception e) {
//                    log.error("更新设备状态失败", e);
//                }

                // 延迟重连
                DeviceInfo device = deviceInfoService.selectDeviceInfoById(Long.valueOf(deviceId));
                if (device != null) {
                    scheduleReconnect(device);
                }
            }
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            String deviceId = getDeviceIdFromChannel(ctx.channel());
            if (deviceId != null) {
                // 处理设备数据
                deviceDataHandlerService.handleDeviceData(deviceId, (String) msg);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            String deviceId = getDeviceIdFromChannel(ctx.channel());
            log.error("设备通道异常: {}", deviceId, cause);

            if (deviceId != null) {
                deviceDataHandlerService.handleError(deviceId, "通道异常: " + cause.getMessage());
            }
            // 不主动关闭通道，避免短暂异常引发断开
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            // writer idle -> 发送应用层ping，保持连接活跃
            if (evt instanceof IdleStateEvent idle) {
                if (idle.state() == IdleState.WRITER_IDLE) {
                    String deviceId = getDeviceIdFromChannel(ctx.channel());
                    if (deviceId != null && ctx.channel().isActive()) {
                        ctx.channel().writeAndFlush("ping\n");
                        log.debug("writer idle，发送保活ping，设备ID: {}", deviceId);
                    }
                }
            }
            super.userEventTriggered(ctx, evt);
        }
        
        /**
         * 从Channel中获取设备ID
         */
        private String getDeviceIdFromChannel(Channel channel) {
            try {
                AttributeKey<String> deviceIdKey = AttributeKey.valueOf("deviceId");
                return channel.attr(deviceIdKey).get();
            } catch (Exception e) {
                log.error("获取设备ID失败", e);
                return null;
            }
        }
    }
    
    @PreDestroy
    public void destroy() {
        log.info("关闭设备连接管理器");
        workerGroup.shutdownGracefully();
    }
    
    /**
     * STX/ETX协议帧解码器
     * 协议格式：STX (0x02) + 0x06 + 数据 + ETX (0x03)
     */
    private static class StxEtxFrameDecoder extends ByteToMessageDecoder {
        private static final byte STX = 0x02;
        private static final byte ETX = 0x03;
        private static final byte RECV_MARKER = 0x06;
        
        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
            while (in.readableBytes() >= 4) { // 至少需要STX + 0x06 + 数据 + ETX
                // 查找STX
                int stxIndex = -1;
                for (int i = 0; i < in.readableBytes(); i++) {
                    if (in.getByte(in.readerIndex() + i) == STX) {
                        stxIndex = i;
                        break;
                    }
                }
                
                if (stxIndex == -1) {
                    // 没有找到STX，丢弃所有数据
                    in.skipBytes(in.readableBytes());
                    break;
                }
                
                // 跳过STX之前的数据
                if (stxIndex > 0) {
                    in.skipBytes(stxIndex);
                }
                
                // 检查是否有足够的数据
                if (in.readableBytes() < 4) {
                    break;
                }
                
                // 检查STX
                if (in.readByte() != STX) {
                    continue;
                }
                
                // 检查接收标记
                if (in.readByte() != RECV_MARKER) {
                    continue;
                }
                
                // 查找ETX
                int etxIndex = -1;
                for (int i = 0; i < in.readableBytes(); i++) {
                    if (in.getByte(in.readerIndex() + i) == ETX) {
                        etxIndex = i;
                        break;
                    }
                }
                
                if (etxIndex == -1) {
                    // 没有找到ETX，等待更多数据
                    break;
                }
                
                // 提取数据部分
                byte[] data = new byte[etxIndex];
                in.readBytes(data);
                
                // 跳过ETX
                in.readByte();
                
                // 将数据转换为字符串并输出
                String message = new String(data, StandardCharsets.UTF_8);
                out.add(message);
            }
        }
    }
}
