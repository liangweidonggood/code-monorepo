# code-monorepo

车联网 VTP 协议的全栈 monorepo — 多语言后端 + Web 管理面板。

## 架构

```
code-monorepo/
├── backend/
│   └── net-service/           ← 网络服务（VTP 协议实现）
│       ├── net-service-netty/ ← Java / Netty 4.2
│       ├── net-service-go/    ← Go
│       └── net-service-rust/  ← Rust / Tokio
├── frontend/                  ← Web 管理面板
└── .claude/                   ← AI 配置 + 项目文档
    └── docs/                  ← 详细文档
```

## 协议

所有后端实现共用 [VTP 协议规范](.claude/docs/VTP-协议规范.md)。帧结构、消息类型、编解码约定在三语言间一致。

## 快速开始

```bash
# Java (主力开发)
cd backend/net-service/net-service-netty
./gradlew bootRun

# Go
cd backend/net-service/net-service-go
go run .

# Rust
cd backend/net-service/net-service-rust
cargo run
```

## 文档

- [VTP 协议规范](.claude/docs/VTP-协议规范.md)
- [net-service-netty 架构设计](.claude/docs/net-service-netty-架构设计.md)
- [net-service-netty 开发指南](.claude/docs/net-service-netty-开发指南.md)
