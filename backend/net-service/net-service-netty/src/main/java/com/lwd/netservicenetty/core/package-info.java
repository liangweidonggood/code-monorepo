/** 核心基础设施模块 — 编解码器在根包公开供 client/business 使用 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = {"protocol", "business", "config", "transport"})
package com.lwd.netservicenetty.core;
