package com.lwd.netservicenetty;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Spring Boot 应用上下文加载测试。
 */
@SpringBootTest
class NetServiceNettyApplicationTests {

    /** 验证应用上下文可正常加载。 */
    @Test
    void contextLoads() {
        assertNotNull(this, "Spring 容器应正常启动");
    }

}
