package com.lwd.netservicenetty;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Netty TCP 服务端应用入口。
 *
 * @author Administrator
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class NetServiceNettyApplication {

    /**
     * 应用启动入口。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(NetServiceNettyApplication.class, args);
    }

}
