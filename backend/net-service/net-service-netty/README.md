# net-service-netty

VTP 协议的 Java/Netty 实现。基于 Spring Boot 4 + Netty 4.2，采用 Spring Modulith 模块化架构。

## 快速开始

```bash
# 编译
./gradlew compileJava

# 运行测试
./gradlew test

# 启动（服务端 + 模拟客户端）
./gradlew bootRun
```

## 模块结构

```
com.lwd.netservicenetty/
├── config/      ← 配置（端口、线程池）
├── transport/   ← 传输层（io_uring > Epoll > NIO）
├── core/        ← 核心基础设施（编解码框架、TCP Server）
├── protocol/    ← 协议领域（POJO + Codec，零Spring依赖）
├── business/    ← 业务处理（MonitorDataHandler）
└── client/      ← 模拟客户端（NettyClient + GPS模拟器）
```

## 配置

```yaml
netty:
  server:
    port: 8888
    business-thread-pool:
      core-pool-size: 4
      max-pool-size: 8
  client:
    host: localhost
    port: 8888
    heartbeat-interval-seconds: 20
    location-interval-seconds: 5
```

## 文档

- [架构设计](../../../.claude/docs/net-service-netty-架构设计.md)
- [开发指南](../../../.claude/docs/net-service-netty-开发指南.md)
- [VTP 协议规范](../../../.claude/docs/VTP-协议规范.md)
