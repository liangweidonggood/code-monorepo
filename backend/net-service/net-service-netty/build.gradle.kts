plugins {
    java
    checkstyle
    pmd
    id("org.springframework.boot") version "4.0.6"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.sonarqube") version "7.3.0.8198"
}

checkstyle {
    toolVersion = "13.4.2"
    isIgnoreFailures = false
    isShowViolations = true
    maxWarnings = 0
    maxErrors = 0
    configFile = rootProject.file("../../../global/config/checkstyle/checkstyle.xml")
}

pmd {
    toolVersion = "7.21.0"
    isConsoleOutput = true
    isIgnoreFailures = false
    ruleSets = listOf()
    ruleSetFiles = rootProject.files("../../../global/config/pmd/ruleset.xml")
}

sonar {
    properties {
        property("sonar.host.url", "http://172.19.248.184:9000")
        property("sonar.projectKey", "code-monorepo_net-service-netty")
        property("sonar.projectName", "net-service-netty")
    }
}

group = "com.lwd"
version = "0.0.1-SNAPSHOT"
description = "net-service-netty"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

extra["springModulithVersion"] = "2.0.6"
extra["nettyVersion"] = "4.2.14.Final"

dependencies {
    // Source: https://mvnrepository.com/artifact/io.netty/netty-all
    implementation("io.netty:netty-all:${property("nettyVersion")}")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.modulith:spring-modulith-starter-core")
    compileOnly("org.projectlombok:lombok")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
   // developmentOnly("org.springframework.boot:spring-boot-docker-compose")
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.modulith:spring-modulith-starter-test")
    testCompileOnly("org.projectlombok:lombok")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testAnnotationProcessor("org.projectlombok:lombok")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.modulith:spring-modulith-bom:${property("springModulithVersion")}")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
