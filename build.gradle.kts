import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("jvm")
    kotlin("plugin.serialization")
}

group = "com.github.smaugfm"
version = "1.0.0"

repositories {
    flatDir {
        dirs("lib")
    }
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
    implementation(":ynab-sdk-0.0.3")
    implementation(":swagger-annotations-1.5.15")
    implementation(":junit-4.12")
    implementation(":mockito-all-2.0.2-beta")
    implementation(":gson-2.8.1")
    implementation(":commons-collections4-4.4")
    implementation(":okio-1.6.0")
    implementation(":okhttp-2.7.5")
    implementation(":logging-interceptor-2.7.5")
    implementation(":hamcrest-core-1.3")
    implementation(":gson-fire-1.8.0")
    implementation(":threetenbp-1.3.5")

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
}
