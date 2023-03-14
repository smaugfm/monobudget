import com.github.breadmoirai.githubreleaseplugin.GithubReleaseTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlin = "1.8.10"

    kotlin("jvm") version kotlin
    kotlin("plugin.serialization") version kotlin
    id("com.star-zero.gradle.githook") version "1.2.1"
    id("com.github.johnrengelman.shadow") version "6.1.0"
    id("org.jlleitschuh.gradle.ktlint") version "10.0.0"
    id("io.gitlab.arturbosch.detekt") version "1.17.1"
    id("com.github.breadmoirai.github-release") version "2.2.12"
}

group = "com.github.smaugfm"
val version: String by project

val githubToken: String? by project

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

detekt {
    config = files(rootDir.resolve("detekt.yml"))
    baseline = rootDir.resolve("detektBaseline.xml")
    buildUponDefaultConfig = true
}

githook {
    createHooksDirIfNotExist = true
    hooks {
        create("pre-commit") {
            task = "ktlintFormat detekt"
        }
        create("pre-push") {
            task = "test"
        }
    }
}

if (githubToken != null) {
    githubRelease {
        token(githubToken)
        prerelease.set(true)
        overwrite.set(true)
        dryRun.set(false)
        releaseAssets.setFrom(
            files(
                "$buildDir/libs/${project.name}-${project.version}-fat.jar"
            )
        )
    }
}

tasks.withType<GithubReleaseTask>().configureEach {
    dependsOn(":shadowJar")
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }

    test {
        useJUnitPlatform()
    }

    shadowJar {
        archiveBaseName.set(rootProject.name)
        archiveClassifier.set("fat")
        manifest {
            attributes(mapOf("Main-Class" to "com.github.smaugfm.YnabMonoCommandKt"))
        }
    }

    build {
        dependsOn(shadowJar)
    }
}

dependencies {
    val ktor = "2.2.4"

    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation("io.insert-koin:koin-core:3.3.3")
    implementation("com.uchuhimo:kotlinx-bimap:1.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("com.github.elbekD:kt-telegram-bot:2.2.0")
    implementation("com.github.ajalt.clikt:clikt:3.5.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    implementation("de.brudaswen.kotlinx.serialization:kotlinx-serialization-csv:2.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
    implementation("io.ktor:ktor-server-core:$ktor")
    implementation("io.ktor:ktor-server-netty:$ktor")
    implementation("io.ktor:ktor-client-core:$ktor")
    implementation("io.ktor:ktor-client-cio:$ktor")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor")
    implementation("io.github.microutils:kotlin-logging:3.0.5")
    implementation("org.slf4j:slf4j-simple:2.0.6")
    implementation("com.google.code.gson:gson:2.10.1")

    val junit = "5.9.2"

    testImplementation("io.mockk:mockk:1.13.4")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.25")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junit")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junit")
}
