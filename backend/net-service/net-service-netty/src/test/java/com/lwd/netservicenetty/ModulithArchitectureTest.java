package com.lwd.netservicenetty;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Spring Modulith 架构验证 — 守护模块边界
 */
class ModulithArchitectureTest {

    private final ApplicationModules modules = ApplicationModules.of(NetServiceNettyApplication.class);

    @Test
    void verifyModularity() {
        // 验证模块之间无非法依赖，内部类型不被外部访问
        assertDoesNotThrow((Executable) modules::verify);
    }

    @Test
    void writeDocumentation() {
        // 自动生成 C4 架构图 + AsciiDoc 文档到 target/spring-modulith-docs/
        assertDoesNotThrow(() -> new Documenter(modules).writeDocumentation());
    }
}
