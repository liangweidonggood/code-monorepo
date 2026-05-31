package com.lwd.client;

import com.lwd.client.config.ClientConfig;
import com.lwd.client.connection.ClientConnection;
import com.lwd.client.handler.ClientHeartbeatHandler;
import com.lwd.client.handler.ClientLocationHandler;
import com.lwd.client.handler.ClientProtocolHandler;
import com.lwd.client.session.ClientSession;
import com.lwd.client.simulator.GpsTrackSimulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 客户端独立启动入口 — 零 Spring 依赖，直接 main 启动
 *
 * <pre>
 * java -cp ... com.lwd.client.ClientMain [--host localhost] [--port 8888]
 * </pre>
 *
 * @author Administrator
 */
public class ClientMain {

    private static final Logger LOG = LoggerFactory.getLogger(ClientMain.class);

    /**
     * 客户端入口 &mdash; 解析参数、创建会话和处理器、建立连接。
     *
     * @param args 命令行参数
     * @throws InterruptedException 线程被中断
     */
    public static void main(String[] args) throws InterruptedException {
        ClientConfig config = parseArgs(args);
        LOG.info("客户端配置: {}", config);

        ClientSession session = new ClientSession(config,
                new GpsTrackSimulator(39.9, 116.3, 60.0, 0.0008));
        ClientHeartbeatHandler heartbeat = new ClientHeartbeatHandler(session);
        ClientLocationHandler location = new ClientLocationHandler(session);
        ClientProtocolHandler protocolHandler = new ClientProtocolHandler(session, heartbeat, location);

        java.util.concurrent.CountDownLatch shutdownLatch = new java.util.concurrent.CountDownLatch(1);

        try (ClientConnection connection = new ClientConnection(session, protocolHandler)) {
            session.onDisconnect(connection::scheduleReconnect);
            connection.connect();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                LOG.info("收到 JVM 退出信号...");
                shutdownLatch.countDown();
            }, "client-shutdown"));

            shutdownLatch.await();
        }
    }

    private static ClientConfig parseArgs(String... args) {
        String host = "localhost";
        int port = 8888;
        String terminalId = "12345678901234";
        int manufacturerId = 1;
        String licensePlate = "京A12345";
        int heartbeatIntervalSeconds = 20;
        int locationIntervalSeconds = 5;
        int reconnectBaseDelaySeconds = 1;
        int reconnectMaxDelaySeconds = 120;

        int idx = 0;
        while (idx < args.length) {
            String flag = args[idx];
            idx++;
            switch (flag) {
              case "--host" -> {
                  host = args[idx];
                  idx++;
              }
              case "--port" -> {
                  port = Integer.parseInt(args[idx]);
                  idx++;
              }
              case "--terminal-id" -> {
                  terminalId = args[idx];
                  idx++;
              }
              case "--manufacturer-id" -> {
                  manufacturerId = Integer.parseInt(args[idx]);
                  idx++;
              }
              case "--license-plate" -> {
                  licensePlate = args[idx];
                  idx++;
              }
              case "--heartbeat-interval" -> {
                  heartbeatIntervalSeconds = Integer.parseInt(args[idx]);
                  idx++;
              }
              case "--location-interval" -> {
                  locationIntervalSeconds = Integer.parseInt(args[idx]);
                  idx++;
              }
              default -> LOG.warn("未知参数: {}", flag);
            }
        }

        return new ClientConfig(host, port, terminalId, manufacturerId, licensePlate,
                heartbeatIntervalSeconds, locationIntervalSeconds,
                reconnectBaseDelaySeconds, reconnectMaxDelaySeconds);
    }
}
