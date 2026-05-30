# net-service-netty 架构设计

> 所属: [net-service-monorepo](./README.md) ｜ 协议: [VTP](./VTP-协议规范.md) ｜ 同级: net-service-go, net-service-rust, frontend

## 1. 模块拓扑

```
┌──────────────────────────────────────────────────────┐
│                 Application Layer                     │
│  NetServiceNettyApplication (启动类 + 配置扫描)        │
└──────┬──────┬──────┬──────┬──────┬──────┬───────────┘
       │      │      │      │      │      │
  config  transport  core  protocol  business  client
```

### 模块依赖方向（无循环）

```
business ──→ protocol ──→ core
   │                         │
   client ────────────────────┘
```

```
config ──→ (独立，零外部依赖)
transport ──→ (独立，零外部依赖)
```

### 各模块职责

| 模块 | 包 | 职责 | 导出 |
|------|-----|------|------|
| **config** | `config/` | 配置 record，Spring Bean 定义 | `NettyServerConfig`, `BusinessThreadPoolConfig` |
| **transport** | `transport/` | 传输层枚举（io_uring>Epoll>NIO） | `Transport` |
| **core** | `core/` | 基础设施：TCP 服务、编解码框架、Handler | `ProtocolFrameEncoder`, `ProtocolFrameDecoder` |
| **protocol** | `protocol/` | 协议领域：POJO + Codec（零 Spring 依赖） | `TcpPacket`, `MessageCodec`, 全部 POJO |
| **business** | `business/` | 业务处理：接收 TcpPacket 分发 | `MonitorDataHandler` |
| **client** | `client/` | 模拟客户端：注册→鉴权→工作循环 | `NettyClient`, `GpsTrackSimulator` |

## 2. 核心设计模式

### 2.1 策略模式 + 注册表 (SOLID)

```
MessageCodec<T>  ← 接口 (core)    ← DIP: 依赖倒置
     ↑
XxxCodec         ← 实现 (protocol) ← SRP: 单一职责
     ↑
MessageCodecRegistry ← 自动发现 (core) ← OCP: 开闭原则
```

新增消息类型只需：
1. 建 POJO record
2. 建 XxxCodec（加 `@Component`）
3. 更新 `TcpPacket` 的 `permits` 列表

零改动现有代码。

### 2.2 Pipeline 数据流

```
入站: [TrafficShaping] → [Logging] → [Backpressure] → [Idle] → [Timeout]
    → [Guard] → [LengthField] → [FrameDecoder] → [BusinessHandler]

出站: [BusinessHandler] → [FrameEncoder] → [Network]
```

### 2.3 编解码分层

```
Frame 层 (ProtocolFrameEncoder/Decoder):  帧头+BCC ↔ ByteBuf
Codec 层 (MessageCodec/XxxCodec):         TcpPacket ↔ Body bytes
```

## 3. 技术栈

| 组件 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 4.0.6 | 应用框架 |
| Spring Modulith | 2.0.6 | 模块化架构守护 |
| Netty | 4.2.14.Final | 网络通信 |
| JDK | 21 | 编译平台 |
| Gradle | 9.5.1 | 构建工具 |

## 4. 配置体系

### 服务端 (netty.server.*)
| 参数 | 默认 | 说明 |
|------|------|------|
| port | 8888 | 监听端口 |
| log-level | INFO | 网络日志级别 |
| business-thread-pool.core-pool-size | 4 | 业务线程核心数 |
| business-thread-pool.max-pool-size | 8 | 最大线程数 |
| business-thread-pool.queue-capacity | 200 | 有界队列 |

### 客户端 (netty.client.*)
| 参数 | 默认 | 说明 |
|------|------|------|
| host | localhost | 服务器地址 |
| port | 8888 | 服务器端口 |
| heartbeat-interval-seconds | 20 | 心跳间隔 |
| location-interval-seconds | 5 | 位置上报间隔 |
| reconnect-base-delay-seconds | 1 | 重连基础延迟 |
| reconnect-max-delay-seconds | 120 | 重连最大延迟 |
