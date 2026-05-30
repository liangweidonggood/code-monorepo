package com.lwd.netservicenetty;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * @author Administrator
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class NetServiceNettyApplication {

    public static void main(String[] args) {
        SpringApplication.run(NetServiceNettyApplication.class, args);
    }

}
