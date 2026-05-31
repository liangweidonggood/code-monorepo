import org.gradle.kotlin.dsl.application
import org.gradle.kotlin.dsl.checkstyle
import org.gradle.kotlin.dsl.java
import org.gradle.kotlin.dsl.pmd

val nettyVersion = "4.2.14.Final"
val slf4jVersion = "2.0.17"
val logbackVersion = "1.5.18"

plugins {
    java
    application
    checkstyle
    pmd
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

application {
    mainClass = "com.lwd.client.ClientMain"
}

sonar {
    properties {
        property("sonar.host.url", "http://172.19.248.184:9000")
        property("sonar.projectKey", "code-monorepo_net-service-netty-client")
        property("sonar.projectName", "net-service-netty-client")
    }
}

group = "com.lwd"
version = "0.0.1-SNAPSHOT"
description = "net-service-netty-client"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.netty:netty-all:${nettyVersion}")
    implementation("org.slf4j:slf4j-api:${slf4jVersion}")
    runtimeOnly("ch.qos.logback:logback-classic:${logbackVersion}")
}
