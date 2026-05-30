# Netty TCP 数据采集服务

基于 **Spring Boot 4 + Netty 4** 的 TCP 数据采集服务，遵循零信任安全原则，在 Pipeline 每一层设置独立防线，对客户端上报数据进行层层校验后入库。

## 架构概览

```
客户端 → TCP 8888 → Pipeline 流水线 → 业务处理
```

### Pipeline 流水线

```
trafficShaper ──→ SafeTrafficHandler ──→ IdleStateHandler ──→ ServerTimeoutHandler
                                                                        │
                                                                        ▼
MonitorDataHandler ←── LengthFieldBasedFrameDecoder ←── ProtocolGuardHandler
```

| 序号 | Handler | 职责 |
|------|---------|------|
| 1 | `ChannelTrafficShapingHandler` | 单连接限速，读取上限 2MB/s |
| 2 | `SafeTrafficHandler` | 背压保护，写缓冲超水位自动暂停读取 |
| 3 | `IdleStateHandler` | 60 秒无读触发空闲事件 |
| 4 | `ServerTimeoutHandler` | 接收空闲事件，关闭死连接 |
| 5 | `ProtocolGuardHandler` | 魔数 0xFE 校验，非法数据直接掐断 |
| 6 | `LengthFieldBasedFrameDecoder` | 基于长度字段的粘包/半包拆帧 |
| 7 | `MonitorDataHandler` | BCC 异或校验 + 协议解析 + 业务入库 |

## 数据协议

```
[0xFE] [长度] [消息类型] [子消息类型] [消息体(N字节)] [BCC校验]
  1B      1B      1B         1B           N B           1B
```

- **魔数**：固定 `0xFE`，用于帧起始校验
- **长度**：仅表示消息体的字节数（不含固定头尾）
- **消息类型 + 子消息类型**：业务分类标识
- **消息体**：变长负载数据
- **BCC**：从长度字段到消息体末尾的异或校验和

示例：`FE 04 73 01 00 86 EB 02 19`

## 安全防线（分层防御）

1. **流量整形层** — 2MB/s 读限速，防止单连接打满带宽
2. **背压保护层** — 写缓冲区达 1MB 高水位自动暂停 Socket 读取，低至 512KB 恢复
3. **超时断线层** — 60 秒无数据判定客户端掉线，主动关闭连接释放资源
4. **协议校验层** — 首字节非 0xFE 直接判定非法数据，清空缓冲区并掐断连接
5. **帧拆分** — 防止粘包/半包攻击，单帧上限 1KB，超限自动抛异常
6. **BCC 校验 + 长度校验** — 双重验证，防止校验碰撞导致的篡改包通过
7. **加密包适配** — 子消息类型 6/7 时自动修正校验跨度为 19 字节

## 传输层

按 `io_uring > Epoll > NIO` 优先级自动降级选择：

| 传输层 | 要求 | 特点 |
|--------|------|------|
| io_uring | Linux 5.10+ | 零系统调用，最高吞吐 |
| Epoll | Linux | 传统高性能 I/O 多路复用 |
| NIO | 全平台 | JDK 内置，通用兜底 |

## 配置参数

| 参数 | 值 | 说明 |
|------|-----|------|
| 监听端口 | 8888 | — |
| 读限速 | 2MB/s | 单连接上限 |
| 读空闲超时 | 60s | — |
| 写缓冲低水位 | 512KB | 恢复读取 |
| 写缓冲高水位 | 1MB | 暂停读取 |
| 最大帧长 | 1KB | 超限触发异常 |
| TCP backlog | 1024 | 三次握手队列 |
| Worker 线程 | CPU核数 × 2 | — |

## 项目结构

```
src/main/java/com/lwd/netservicenetty/
├── NetServiceNettyApplication.java    # Spring Boot 入口
└── server/
    ├── TcpServer.java                 # Netty 服务器，生命周期管理
    ├── Transport.java                 # 传输层自动选择（io_uring/Epoll/NIO）
    ├── MyChannelInitializer.java      # Pipeline 组装工厂
    ├── SafeTrafficHandler.java        # 背压保护（高低水位线）
    ├── ServerTimeoutHandler.java      # 空闲超时断连
    ├── ProtocolGuardHandler.java      # 魔数校验 + 非法数据拦截
    └── MonitorDataHandler.java        # BCC校验 + 协议解析 + 业务处理
```
