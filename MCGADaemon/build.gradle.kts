// kotlin
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.qzero.mcga"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.named<ShadowJar>("shadowJar") {
    // 生成的 jar 不带 "-all" 或其他 classifier
    archiveClassifier.set("")
    // 可执行入口（Kotlin 顶层 main 在编译后为 FileNameKt）
    manifest {
        attributes["Main-Class"] = "com.qzero.mcga.daemon.DaemonMainKt"
    }
}

// 让 build 也会生成 fat jar
tasks.named("build") {
    dependsOn(tasks.named("shadowJar"))
}
