plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

group = "dev.zieger"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    maven("https://jitpack.io")
    maven("https://maven.zieger.dev/releases")
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))

    val kotlinSerializationVersion: String by project
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinSerializationVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinSerializationVersion")

    val kotlinCoroutinesVersion: String by project
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")

    val ktorVersion: String by project
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-host-common:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
    implementation("io.ktor:ktor-client-websockets:$ktorVersion")
    implementation("io.ktor:ktor-client-serialization:$ktorVersion")

    val exposedVersion: String by project
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.xerial:sqlite-jdbc:3.36.0.3")

    implementation("org.slf4j:slf4j-log4j12:1.7.36")

    val utilsTimeVersion: String by project
    implementation("dev.zieger.utils:time:$utilsTimeVersion")
    implementation("dev.zieger.utils:misc:$utilsTimeVersion")
    implementation("dev.zieger.utils:log:$utilsTimeVersion")

    implementation("dev.zieger:bybitapi:1.0.3")
}