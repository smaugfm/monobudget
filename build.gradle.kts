import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import org.codehaus.plexus.util.Os
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    kotlin("jvm") version "1.8.10"
    kotlin("plugin.serialization") version "1.8.10"
    id("com.google.devtools.ksp") version "1.8.10-1.0.9"
    id("com.star-zero.gradle.githook") version "1.2.1"
    id("com.github.johnrengelman.shadow") version "6.1.0"
    id("org.jlleitschuh.gradle.ktlint") version "11.3.1"
    id("io.gitlab.arturbosch.detekt") version "1.22.0"
}

group = "io.github.smaugfm.monobudget"
val version: String by project

val jdkVersion = "11"
val ktor = "2.2.4"
val junit = "5.9.2"
val logback = "1.4.5"
val koin = "3.4.0"
val koinKsp = "1.2.0"

val githubToken: String? by project

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation("io.github.smaugfm:monobank:0.0.1")
    implementation("io.github.smaugfm:lunchmoney:0.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.6.4")
    implementation("io.insert-koin:koin-core:$koin")
    implementation("io.insert-koin:koin-annotations:$koinKsp")
    ksp("io.insert-koin:koin-ksp-compiler:$koinKsp")
    implementation("com.uchuhimo:kotlinx-bimap:1.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("com.github.elbekD:kt-telegram-bot:2.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    implementation("de.brudaswen.kotlinx.serialization:kotlinx-serialization-csv:2.0.0")
    implementation("com.charleskorn.kaml:kaml:0.53.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
    implementation("io.ktor:ktor-server-core:$ktor")
    implementation("io.ktor:ktor-server-netty:$ktor")
    implementation("io.ktor:ktor-client-core:$ktor")
    implementation("io.ktor:ktor-client-cio:$ktor")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor")
    implementation("io.github.microutils:kotlin-logging:3.0.5")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("ch.qos.logback:logback-core:$logback")
    implementation("ch.qos.logback:logback-classic:$logback")
    testImplementation("io.insert-koin:koin-test-junit5:$koin")
    testImplementation("io.mockk:mockk:1.13.4")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.25")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junit")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junit")

    if (Os.isArch("aarch64")) {
        runtimeOnly("io.netty:netty-resolver-dns-native-macos:4.1.90.Final:osx-aarch_64")
    }
}

configurations.all {
    resolutionStrategy.cacheChangingModulesFor(1, TimeUnit.SECONDS)
}

configure<KtlintExtension> {
    enableExperimentalRules.set(true)
    filter {
        exclude("**/generated/**")
    }
    reporters {
        reporter(ReporterType.HTML)
    }
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

tasks {
    test {
        useJUnitPlatform()
    }
    withType<DetektCreateBaselineTask> {
        jvmTarget = jdkVersion
    }
    withType<Detekt> {
        jvmTarget = jdkVersion
        reports {
            html.required.set(true)
            md.required.set(true)
            xml.required.set(false)
            txt.required.set(false)
            sarif.required.set(false)
        }
    }

    shadowJar {
        archiveBaseName.set(rootProject.name)
        archiveClassifier.set("fat")
        manifest {
            attributes(mapOf("Main-Class" to "io.github.smaugfm.monobudget.MainKt"))
        }
    }

    fun <T : KotlinCommonCompilerOptions> KotlinCompilationTask<T>.optIn() {
        compilerOptions.freeCompilerArgs.add(
            "-opt-in=kotlinx.serialization.ExperimentalSerializationApi"
        )
    }

    named<KotlinCompilationTask<*>>("compileKotlin") {
        optIn()
    }
    named<KotlinCompilationTask<*>>("compileTestKotlin") {
        optIn()
    }

    build {
        dependsOn(shadowJar)
    }
}

kotlin {
    jvmToolchain(jdkVersion.toInt())
}
