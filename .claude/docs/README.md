# net-service-monorepo

## 项目定位

车联网 VTP 协议的多语言实现 monorepo。同一套协议规范，三种语言后端 + 一个前端管理面板。

## 目录结构

```
code-monorepo/
├── backend/net-service/
│   ├── net-service-netty/   ← Java / Netty 4.2 (主力开发中)
│   ├── net-service-go/      ← Go / 标准库 net
│   └── net-service-rust/    ← Rust / Tokio
├── frontend/                ← Web 管理面板
├── .claude/                 ← AI 配置与项目文档
│   └── docs/                ← 共享文档
└── logs/                    ← 统一日志目录
```

## 协议

所有后端实现共用 [VTP 协议规范](./VTP-协议规范.md) — 帧结构、消息类型、编解码约定。

## 各子项目

| 项目 | 语言 | 状态 |
|------|------|------|
| [net-service-netty](./net-service-netty-架构设计.md) | Java 21 + Netty 4.2 + Spring Boot 4 | 基础框架完成 |
| net-service-go | Go | 待开发 |
| net-service-rust | Rust | 待开发 |
| frontend | Web | 待开发 |

## 技术文档

- [VTP 协议规范](./VTP-协议规范.md) — 所有语言的公共契约
- [net-service-netty 架构设计](./net-service-netty-架构设计.md) — Java 版设计
- [net-service-netty 开发指南](./net-service-netty-开发指南.md) — TDD 流程
