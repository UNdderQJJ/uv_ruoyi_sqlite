# 若依系统TCP服务版本（基于Netty）

## 概述

本项目是若依管理系统的TCP服务版本，已移除所有HTTP Controller，改为使用Netty框架进行TCP Socket通信。

## 主要变更

### 已删除的文件
- 所有HTTP Controller文件（位于各模块的controller目录）
- Spring MVC相关配置
- Swagger API文档配置
- HTTP服务器配置

### 新增的配置
- Netty TCP服务器配置（`TcpServerConfig.java`）
- Netty TCP处理器（`TcpServerHandler.java`）
- TCP服务配置文件（`application.yml`中的tcp配置）

## TCP服务配置

### 配置文件
```yaml
# TCP服务配置
tcp:
  # TCP服务器端口
  port: 8030
  # 最大连接数
  max-connections: 1000
  # 连接超时时间（毫秒）
  connection-timeout: 30000
```

### 功能特性
- 基于Netty高性能网络框架
- 支持多客户端并发连接
- 自动线程池管理（EventLoopGroup）
- 连接超时控制
- 空闲连接检测
- 优雅关闭服务
- 字符串编解码支持

## 使用方法

### 启动服务
```bash
mvn spring-boot:run -pl ruoyi-admin
```

### 连接TCP服务
使用任何TCP客户端工具连接到配置的端口（默认8030）

### 示例客户端代码
```java
import java.io.*;
import java.net.*;

public class TcpClient {
    public static void main(String[] args) {
        try {
            Socket socket = new Socket("localhost", 8030);
            
            // 发送消息
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println("Hello Server!");
            
            // 接收响应
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String response = in.readLine();
            System.out.println("服务器响应: " + response);
            
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

## 开发说明

### Netty架构
- **BossGroup**: 负责接收客户端连接
- **WorkerGroup**: 负责处理客户端请求
- **Pipeline**: 包含编解码器和自定义处理器
- **Handler**: 处理具体的业务逻辑

### 添加新的TCP处理器
在`TcpServerHandler.java`的`processMessage`方法中添加具体的业务逻辑：

```java
private String processMessage(String message) {
    // 解析消息
    // 调用业务服务
    // 返回响应
    return "处理结果";
}
```

### 扩展功能
- 可以添加消息协议解析（JSON、XML等）
- 支持心跳检测
- 添加认证和授权机制
- 实现消息队列和持久化
- 支持SSL/TLS加密

## 技术栈

- Spring Boot 3.3.5
- Java 17
- Maven
- SQLite数据库
- Netty 4.1.114.Final
- 原生Java Socket API（客户端）

## 注意事项

1. 确保端口8030未被占用
2. TCP服务不支持HTTP请求
3. 需要自定义消息协议
4. 建议添加心跳机制保持连接
5. Netty提供了更好的性能和稳定性

## 性能优势

相比原生Java Socket，Netty提供：
- 更高的并发处理能力
- 更好的内存管理
- 内置的编解码器
- 更灵活的管道处理
- 更好的异常处理 