package com.ruoyi.tcp.handler;

import com.ruoyi.business.service.TaskInfo.DeviceDataHandlerService;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 设备数据处理Handler
 * 负责接收设备数据并调用业务逻辑处理器
 */
@Component
public class DeviceDataHandler extends ChannelInboundHandlerAdapter {
    
    private static final Logger log = LoggerFactory.getLogger(DeviceDataHandler.class);
    
    @Autowired
    private DeviceDataHandlerService deviceDataHandlerService;
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            String data = (String) msg;
            String deviceId = getDeviceIdFromChannel(ctx.channel());
            
            if (deviceId != null) {
                // 调用业务逻辑处理器
                deviceDataHandlerService.handleDeviceData(deviceId, data);
            } else {
                log.warn("无法获取设备ID，通道: {}", ctx.channel().remoteAddress());
            }
            
        } catch (Exception e) {
            log.error("处理设备数据异常", e);
        }
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        try {
            String deviceId = getDeviceIdFromChannel(ctx.channel());
            if (deviceId != null) {
                deviceDataHandlerService.registerDeviceChannel(deviceId, ctx.channel());
                log.info("设备连接激活，设备ID: {}, 地址: {}", deviceId, ctx.channel().remoteAddress());
            } else {
                log.warn("设备连接激活但无法获取设备ID，地址: {}", ctx.channel().remoteAddress());
            }
        } catch (Exception e) {
            log.error("处理设备连接激活异常", e);
        }
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        try {
            String deviceId = getDeviceIdFromChannel(ctx.channel());
            if (deviceId != null) {
                deviceDataHandlerService.unregisterDeviceChannel(deviceId);
                log.info("设备连接断开，设备ID: {}, 地址: {}", deviceId, ctx.channel().remoteAddress());
            } else {
                log.warn("设备连接断开但无法获取设备ID，地址: {}", ctx.channel().remoteAddress());
            }
        } catch (Exception e) {
            log.error("处理设备连接断开异常", e);
        }
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        try {
            String deviceId = getDeviceIdFromChannel(ctx.channel());
            log.error("设备通道异常，设备ID: {}, 地址: {}", deviceId, ctx.channel().remoteAddress(), cause);
            
            if (deviceId != null) {
                deviceDataHandlerService.handleError(deviceId, "通道异常: " + cause.getMessage());
            }
            
            // 关闭通道
            ctx.close();
        } catch (Exception e) {
            log.error("处理设备通道异常时发生错误", e);
        }
    }
    
    /**
     * 从Channel中获取设备ID
     */
    private String getDeviceIdFromChannel(Channel channel) {
        try {
            // 尝试从Channel属性中获取设备ID
            AttributeKey<String> deviceIdKey = AttributeKey.valueOf("deviceId");
            String deviceId = channel.attr(deviceIdKey).get();
            
            if (deviceId != null) {
                return deviceId;
            }
            
            // 如果属性中没有，尝试从远程地址解析
            String remoteAddress = channel.remoteAddress().toString();
            log.debug("尝试从远程地址解析设备ID: {}", remoteAddress);
            
            // 这里可以根据实际业务逻辑从地址中解析设备ID
            // 例如：从IP地址映射到设备ID
            return parseDeviceIdFromAddress(remoteAddress);
            
        } catch (Exception e) {
            log.error("获取设备ID失败", e);
            return null;
        }
    }
    
    /**
     * 从地址解析设备ID
     * 这里需要根据实际业务逻辑实现
     */
    private String parseDeviceIdFromAddress(String remoteAddress) {
        // 示例实现：从IP地址解析设备ID
        // 实际项目中可能需要查询数据库或配置文件
        try {
            // 提取IP地址
            String ipAddress = remoteAddress.substring(1, remoteAddress.lastIndexOf(':'));
            log.debug("解析IP地址: {}", ipAddress);
            
            // 这里应该根据IP地址查询数据库获取对应的设备ID
            // 暂时返回null，需要在实际项目中实现
            return null;
            
        } catch (Exception e) {
            log.error("从地址解析设备ID失败: {}", remoteAddress, e);
            return null;
        }
    }
}
