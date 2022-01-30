import com.github.breadmoirai.githubreleaseplugin.GithubReleaseTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.star-zero.gradle.githook")
    id("com.github.johnrengelman.shadow")
    id("org.jlleitschuh.gradle.ktlint")
    id("io.gitlab.arturbosch.detekt")
    id("com.github.breadmoirai.github-release")
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
    implementation(kotlin("stdlib"))
    implementation("com.uchuhimo:kotlinx-bimap:_")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:_")
    implementation("com.github.elbekD:kt-telegram-bot:_")
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
    implementation("io.github.microutils:kotlin-logging:_")
    implementation("org.slf4j:slf4j-simple:_")
    implementation("com.google.code.gson:gson:_")

    testImplementation("io.mockk:mockk:_")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:_")
    testImplementation("org.junit.jupiter:junit-jupiter-api:_")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:_")
}
