import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("jvm")
    kotlin("plugin.serialization")
}

group = "com.github.smaugfm"
version = "1.0.0"

repositories {
    mavenCentral()
    maven(url = "https://kotlin.bintray.com/kotlinx")
}

application {
    mainClass.set("com.github.smaugfm.YnabMonoKt")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.time.ExperimentalTime"
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.github.ajalt.clikt:clikt:_")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:_")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:_")
    implementation("io.ktor:ktor-server-core:_")
    implementation("io.ktor:ktor-server-netty:_")
    implementation("io.ktor:ktor-client-core:_")
    implementation("io.ktor:ktor-client-cio:_")
    implementation("io.ktor:ktor-client-json:_")
    implementation("io.ktor:ktor-client-serialization:_")
}
