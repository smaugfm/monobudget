import com.github.breadmoirai.githubreleaseplugin.GithubReleaseTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlin = "1.6.10"

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
    maven(url = "https://jitpack.io")
    mavenCentral()
    maven(url = "https://kotlin.bintray.com/kotlinx")
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
        kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.time.ExperimentalTime"
        kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
        kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlinx.serialization.ExperimentalSerializationApi"
    }

    test {
        useJUnitPlatform()
    }

    shadowJar {
        archiveBaseName.set(rootProject.name)
        archiveClassifier.set("fat")
        manifest {
            attributes(mapOf("Main-Class" to "com.github.smaugfm.YnabMonoKt"))
        }
    }

    build {
        dependsOn(shadowJar)
    }
}

dependencies {
    val ktor = "1.6.7"

    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation("io.insert-koin:koin-core:3.1.5")
    implementation("com.uchuhimo:kotlinx-bimap:1.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines:0.19.2")
    implementation("com.github.elbekD:kt-telegram-bot:1.4.1")
    implementation("com.github.ajalt.clikt:clikt:3.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.3.1")
    implementation("io.ktor:ktor-server-core:$ktor")
    implementation("io.ktor:ktor-server-netty:$ktor")
    implementation("io.ktor:ktor-serialization:$ktor")
    implementation("io.ktor:ktor-client-core:$ktor")
    implementation("io.ktor:ktor-client-cio:$ktor")
    implementation("io.ktor:ktor-client-json:$ktor")
    implementation("io.ktor:ktor-client-serialization:$ktor")
    implementation("io.github.microutils:kotlin-logging:2.1.21")
    implementation("org.slf4j:slf4j-simple:1.7.35")
    implementation("com.google.code.gson:gson:2.8.9")

    val junit = "5.8.2"

    testImplementation("io.mockk:mockk:1.12.2")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.25")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junit")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junit")
}
