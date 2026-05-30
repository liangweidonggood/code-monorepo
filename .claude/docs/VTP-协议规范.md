# VTP 协议规范 (Vehicle Tracking Protocol)

## 1. 帧结构

```
┌──────┬──────┬──────┬──────┬──────────┬──────┐
│ FE   │ Len  │ Type │ Sub  │ Body     │ BCC  │
│ 1B   │ 1B   │ 1B   │ 1B   │ N B      │ 1B   │
│ 魔数  │ 长度  │ 类型  │ 子类型│ 消息体   │ 异或 │
└──────┴──────┴──────┴──────┴──────────┴──────┘
```

- **FE**: 帧头魔数 (0xFE)
- **Len**: 消息体长度，不含帧头帧尾，最大 255
- **Type**: 消息类型
- **Sub**: 子类型 (0x00=明文, 0x06=AES加密, 0x07=国密加密)
- **Body**: 变长消息体，由各消息类型定义
- **BCC**: 异或校验，XOR 从 Len 到 Body 末尾的所有字节

## 2. 消息类型矩阵

### 2.1 Client → Server (上报)

| Type | 名称 | Body 长度 | 说明 |
|------|------|-----------|------|
| 0x01 | RegistrationRequest | 19+plateLen | 终端注册 |
| 0x02 | AuthRequest | 20 | 鉴权 |
| 0x03 | Heartbeat | 10 | 链路保活 |
| 0x10 | LocationReport | 43 | 位置+车况 |
| 0x11 | AlarmReport | 33+descLen | 告警 |
| 0x12 | DataTransmission | 18+payloadLen | 业务透传 |
| 0x00 | CommonResponse | 4 | 对上位机指令的应答 |

### 2.2 Server → Client (下发)

| Type | 名称 | 说明 |
|------|------|------|
| 0x81 | RegistrationResponse | 注册应答（鉴权码） |
| 0x80 | CommonResponse | 通用应答 |
| 0x82 | ImmediateReplayCmd | 立即回传位置 |
| 0x83 | RemoteConfigCmd | 远程参数配置 |
| 0x84 | DataTransmission | 数据下发 |

## 3. 字段编解码约定

### 3.1 BCD 编码
变长数字串按 BCD 压缩，2 位十进制 → 1 字节。终端号 14 位 → 7 字节。

### 3.2 经纬度
double × 1,000,000 → int32 (4B)，精度 ±0.000001°。

### 3.3 GPS 时间
6 字节 BCD: `YYMMDDHHmmss`

### 3.4 车牌号
1 字节长度前缀 + N 字节 ASCII。支持变长。

## 4. 客户端生命周期

```
connect → RegistrationRequest
         ↓
       RegistrationResponse (result=0, authCode)
         ↓
       AuthRequest
         ↓
       CommonResponse (result=0)
         ↓
       ┌ 工作循环 ──────────────────────┐
       │ · 每 20s 发送 Heartbeat        │
       │ · 每 5s 发送 LocationReport    │
       │ · 5% 概率发送 AlarmReport      │
       │ · 监听服务端指令并应答          │
       └────────────────────────────────┘
         ↓
       disconnect → 指数退避重连 (1s→2s→4s→...→120s)
```
