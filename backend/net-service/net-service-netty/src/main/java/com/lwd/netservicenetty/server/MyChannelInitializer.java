package com.lwd.netservicenetty.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import org.springframework.stereotype.Component;

/**
 * @author Administrator
 */
@Component
public class MyChannelInitializer extends ChannelInitializer<Channel> {
    @Override
    protected void initChannel(Channel ch) throws Exception {

    }
}
