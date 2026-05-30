# net-service-netty 开发指南

> 所属: [net-service-monorepo](./README.md) ｜ 协议: [VTP](./VTP-协议规范.md)

## 快速开始

```bash
# 编译
./gradlew -p backend/net-service/net-service-netty compileJava

# 运行所有测试
./gradlew -p backend/net-service/net-service-netty test

# 启动应用（服务端 + 客户端同时启动）
./gradlew -p backend/net-service/net-service-netty bootRun
```

## TDD 开发流程

1. **RED**: 写测试 → `./gradlew test --tests "XxxTest"` → 确认失败
2. **GREEN**: 写最小代码让测试通过
3. **REFACTOR**: 清理重复、改善命名
4. 验证: `./gradlew test` 全部通过

## 新增消息类型

```text
1. 创建 POJO record（protocol/）
2. 创建 Codec（protocol/codec/），加 @Component
3. 更新 TcpPacket.permits 列表
4. 在 ProtocolFrameEncoder.msgTypeOf() 添加 instanceof 映射
5. 编译验证: ./gradlew compileJava
```

示例见 `CommonResponse` + `CommonResponseCodec`

## 模块验证

```bash
# 架构验证（自动检测循环依赖、非法跨模块访问）
./gradlew test --tests "ModulithArchitectureTest.verifyModularity"

# 生成 C4 架构图 (build/spring-modulith-docs/)
./gradlew test --tests "ModulithArchitectureTest.writeDocumentation"
```

## 已知限制

1. Windows 环境 io_uring 不可用，自动降级到 NIO
2. ProtocolFrameDecoder 中 BCC 校验需从 headerStart+1 开始计算
3. ProtocolFrameEncoder 中 BCC 必须在 write 之前计算
4. sealed interface 的 permits 类必须在同一 package

## 测试清单

| 测试类 | 覆盖 |
|--------|------|
| CommonResponseCodecTest | 编解码往返 |
| HeartbeatCodecTest | BCD 编解码 |
| LocationReportCodecTest | 15 字段往返 |
| ProtocolCodecTest | 帧编解码集成 |
| GpsTrackSimulatorTest | GPS 坐标模拟 |
| ModulithArchitectureTest | 模块边界守护 |
