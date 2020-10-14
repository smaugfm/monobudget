import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("jvm")
    kotlin("plugin.serialization")
}

group = "com.github.smaugfm"
version = "1.0.0"

val myMavenRepoReadUrl: String by project
val myMavenRepoReadUsername: String by project
val myMavenRepoReadPassword: String by project

repositories {
    maven(url = myMavenRepoReadUrl) {
        credentials {
            username = myMavenRepoReadUsername
            password = myMavenRepoReadPassword
        }
    }
    maven(url = "https://jitpack.io")
    mavenCentral()
    jcenter()
    maven(url = "https://kotlin.bintray.com/kotlinx")
}

application {
    mainClass.set("com.github.smaugfm.YnabMonoKt")
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.time.ExperimentalTime"
    }

    test {
        useJUnitPlatform()
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.github.snowe2010:pretty-print:v2.0.7")
    implementation("io.michaelrocks:bimap:_")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:_")
    implementation("com.github.elbekD:kt-telegram-bot:1.3.5")
    implementation("com.github.ajalt.clikt:clikt:_")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:_")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:_")
    implementation("io.ktor:ktor-server-core:_")
    implementation("io.ktor:ktor-server-netty:_")
    implementation("io.ktor:ktor-serialization:_")
    implementation("io.ktor:ktor-client-core:_")
    implementation("io.ktor:ktor-client-cio:_")
    implementation("io.ktor:ktor-client-json:_")
    implementation("io.ktor:ktor-client-serialization:_")

    testImplementation("io.mockk:mockk:_")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:_")
    testImplementation("org.junit.jupiter:junit-jupiter-api:_")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:_")
}

