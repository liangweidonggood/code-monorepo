package com.lwd.netservicenetty.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 最后一站使用SimpleChannelInboundHandler可以自动释放内存
 * ChannelHandler.Sharable  声明为所有连接安全地共享它
 * 注意：这里不能存有状态的全局变量
 * 成员变量只能有单例的 Service/Mapper
 *
 * @author Administrator
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class MonitorDataHandler extends SimpleChannelInboundHandler<ByteBuf> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        String hexStr = ByteBufUtil.hexDump(msg).toUpperCase();
        log.info("【收到网络帧】: {}", hexStr);

        int readerIndex = msg.readerIndex();
        // 1. 窥探原始长度与子消息类型
        int packetLength = msg.getUnsignedByte(readerIndex + 1);
        short subMsgTypeForCheck = msg.getUnsignedByte(readerIndex + 3);

        // 🔴 2. 核心补丁：对齐原厂规则，如果是加密包（子类型 6 或 7），强行将校验跨度修正为 19
        if (subMsgTypeForCheck == 6 || subMsgTypeForCheck == 7) {
            packetLength = 19;
        }

        // 3. 呼叫绝对下标 BCC 校验
        if (!validateChecksum(msg, packetLength)) {
            return;
        }

        // 4. 第二道防线：【读光审判】检查实际物理切片和理论长度是否一致
        int expectedTotalLength = packetLength + 5;
        if (msg.readableBytes() != expectedTotalLength) {
            log.error("【❌ 撞大运包拦截】BCC虽靠数学巧合通过，但包实际长度 {} 与理论计算长度 {} 不符！判定为指针对齐崩坏的篡改包，就地正法（丢弃）！",
                    msg.readableBytes(), expectedTotalLength);
            return;
        }

        log.info("【💚 BCC与长度双重校验通过】此包无可挑剔，开始处理业务...");

        // 5. 这一步开始用 read() 真正消费指针（此时读指针完好如初，精准停在 0xFE 面前）
        short magic = msg.readUnsignedByte();       // 读出 0xFE
        short length = msg.readUnsignedByte();      // 读出 长度 (注意：如果是6/7，这里读出的是原始长度，不是19)
        short msgType = msg.readUnsignedByte();     // 读出 消息类型
        short subMsgType = msg.readUnsignedByte();  // 读出 子消息类型

        // 如果业务后续也要考虑 6/7 加密包的剥洋葱逻辑，届时在这里用原始的 length 去 read 对应字节即可

        // 读取动态消息体
        byte[] bodyBytes = new byte[length];
        msg.readBytes(bodyBytes);
        String bodyHex = ByteBufUtil.hexDump(bodyBytes).toUpperCase();

        // 读出最后的校验位
        short bccCode = msg.readUnsignedByte();

        log.info("【解析结果】-> [魔数:0x{}] [长度:{}字节] [消息类型:0x{}] [子消息类型:0x{}] [消息体(HEX):{}] [BCC校验码:0x{}]",
                Integer.toHexString(magic).toUpperCase(),
                length,
                Integer.toHexString(msgType).toUpperCase(),
                Integer.toHexString(subMsgType).toUpperCase(),
                bodyHex,
                Integer.toHexString(bccCode).toUpperCase());

        // 6. 丢给你的 Spring Service 走数据库存储或业务分发
        // monitorService.save(msgType, subMsgType, bodyBytes);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("通道 [{}] 发生未处理异常，关闭连接", ctx.channel().id(), cause);
        ctx.close();
    }

    /**
     * 验证数据 BCC 异或校验和
     */
    private boolean validateChecksum(ByteBuf buffer, int length) {
        int startIndex = buffer.readerIndex();

        // 参与校验的物理字节总数 = 长度字段(1B) + 消息类型(1B) + 子消息类型(1B) + 动态消息体(length字节)
        int validateDataLength = 1 + 1 + 1 + length;

        // 【物理越界拦截】
        if (buffer.readableBytes() < validateDataLength + 2) {
            log.error("【💔 BCC拦截】物理越界！包内实际可读字节不足。预估所需长度: {} 字节", validateDataLength + 2);
            return false;
        }

        // 异或计算：从第 1 个字节（跳过第 0 位的 0xFE）开始
        int checksum = 0;
        for (int i = 0; i < validateDataLength; i++) {
            checksum ^= buffer.getUnsignedByte(startIndex + 1 + i);
        }

        // 提取数据包自带的校验码
        short receivedChecksum = buffer.getUnsignedByte(startIndex + 1 + validateDataLength);

        if (checksum == receivedChecksum) {
            return true;
        } else {
            log.error("【💔 BCC校验失败】帧数据: [{}], 算出: 0x{}, 包带: 0x{}",
                    ByteBufUtil.hexDump(buffer).toUpperCase(),
                    Integer.toHexString(checksum).toUpperCase(),
                    Integer.toHexString(receivedChecksum).toUpperCase());
            return false;
        }
    }
}
