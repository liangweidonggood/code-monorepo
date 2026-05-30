# net-service

VTP 协议的多语言网络服务实现。

## 子项目

| 目录 | 语言 | 框架 | 端口 |
|------|------|------|------|
| [net-service-netty](./net-service-netty/) | Java 21 | Netty 4.2 + Spring Boot 4 | 8888 |
| [net-service-go](./net-service-go/) | Go | 标准库 net | — |
| [net-service-rust](./net-service-rust/) | Rust | Tokio | — |

## 协议

VTP 帧格式: `[FE] [len] [type] [sub] [body...] [BCC]`

详见: [VTP 协议规范](../../.claude/docs/VTP-协议规范.md)
