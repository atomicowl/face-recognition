plugins {
    kotlin("jvm") version "2.2.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(24)
}

dependencies {
    implementation("io.ktor:ktor-server-core:2.3.4")
    implementation("io.ktor:ktor-server-netty:2.3.4")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.4")
    implementation("io.ktor:ktor-serialization-gson:2.3.4")
    implementation("io.ktor:ktor-server-call-logging:2.3.4")

    // For image processing
    implementation("org.bytedeco:javacv-platform:1.5.9") // includes OpenCV, FFmpeg, etc.

    // Logging
    implementation("ch.qos.logback:logback-classic:1.2.11")

    testImplementation(kotlin("test"))

}
