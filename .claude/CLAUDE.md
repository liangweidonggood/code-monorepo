# net-service-monorepo 项目上下文

## 技术栈
- Spring Boot 4 + Gradle (Kotlin DSL) + JDK 21
- Netty: io.netty:netty-all:4.2.14.Final（Netty 5 已停止维护，4.2.x 是唯一推荐版本）
- Lombok

## 构建验证
- `./gradlew compileJava` 编译验证（Windows 用 PowerShell，路径反斜杠会被 bash 吃掉）

## 编码约定
- 配置映射用 Java `record` + `@ConfigurationProperties`，启动类加 `@ConfigurationPropertiesScan` 自动扫描
- 无状态的 Netty Handler 加 `@Component` + `@ChannelHandler.Sharable`，注入到 ChannelInitializer 复用
- 不要用 `@EnableConfigurationProperties` 逐个列举配置类

## Netty Pipeline 注意事项
- `SafeTrafficHandler` 背压时设 `autoRead(false)`，会导致 `IdleStateHandler` 触发假空闲
- `ServerTimeoutHandler` 关闭连接前须检查 `ctx.channel().isWritable()`，跳过背压中的假超时
- 自定义 Handler 的 `exceptionCaught` 只需在 Pipeline 末端兜底一份即可
- `ByteToMessageDecoder` 子类不要在 `decode()` 内直接 `fireChannelRead()`，改继承 `ChannelInboundHandlerAdapter`

## 已发现的坑
- 不要凭训练数据断言 Netty API 是否存在，以 `./gradlew compileJava` 编译结果为准
- `ByteBufUtil.hexDump()` 不要在日志门面外无条件调用，生产关 INFO 后纯浪费 CPU
