# net-service-monorepo 项目上下文

## 技术栈

- Spring Boot 4 + Gradle (Kotlin DSL) + JDK 21
- Netty: io.netty:netty-all:4.2.14.Final（Netty 5 已停止维护，4.2.x 是唯一推荐版本）
- Lombok

## 构建验证

- `./gradlew compileJava` 编译验证（Windows 用 PowerShell，路径反斜杠会被 bash 吃掉）

## 编码约定

必须遵守SOLID原则
[详细规范](./docs/编码规范.md)

## Netty Pipeline 注意事项

- `SafeTrafficHandler` 背压时设 `autoRead(false)`，会导致 `IdleStateHandler` 触发假空闲
- `ServerTimeoutHandler` 关闭连接前须检查 `ctx.channel().isWritable()`，跳过背压中的假超时
- 自定义 Handler 的 `exceptionCaught` 只需在 Pipeline 末端兜底一份即可
- `ByteToMessageDecoder` 子类不要在 `decode()` 内直接 `fireChannelRead()`，改继承 `ChannelInboundHandlerAdapter`

## 静态分析

- **SonarQube**: `$env:SONAR_TOKEN='xxx'; ./gradlew sonar`（token 不放项目文件，用环境变量传）
- **Checkstyle + PMD**: 全局规则文件在 `global/config/checkstyle/checkstyle.xml` 和 `global/config/pmd/ruleset.xml`
  ，绝对不能修改
  - Checkstyle `13.4.2`, PMD `7.21.0`, `maxWarnings=0, maxErrors=0`
  - `var` 必须改为显式类型，`log` → `LOG`，`if` 必须有大括号，数字加下划线 `1_000_000`
  - record 用行尾 `//` 注释，不用 `@param`；record 必须独立文件，不嵌套在类里
  - byte 用 `& 0xFF` 避免有符号问题：`Integer.toHexString(magic & 0xFF)`
- Record 按职责分包：`config/`, `connection/`, `handler/`, `session/`, `simulator/`, `codec/`, `model/`

## 已发现的坑

- 不要凭训练数据断言 Netty API 是否存在，以 `./gradlew compileJava` 编译结果为准
- `ByteBufUtil.hexDump()` 不要在日志门面外无条件调用，生产关 INFO 后纯浪费 CPU
- `ProtocolGuardHandler` 魔数比较：`getUnsignedByte` 返回无符号 int，与 `(byte)0xFE` 比较永远不等，必须用 `getByte`
- 中文字段（车牌等）编解码必须用 `StandardCharsets.UTF_8`，不能用 `US_ASCII`
- 测试完必须清理后台进程：`Get-Process java | Stop-Process -Force`
- `net-service-netty-client` 是独立 Gradle 项目，零 Spring 依赖，协议自包含
