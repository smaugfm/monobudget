import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import org.codehaus.plexus.util.Os
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    kotlin("jvm") version "1.9.20"
    kotlin("plugin.serialization") version "1.9.20"
    id("com.google.devtools.ksp") version "1.9.20-1.0.14"
    id("com.star-zero.gradle.githook") version "1.2.1"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1"
    id("io.gitlab.arturbosch.detekt") version "1.23.3"
}

group = "io.github.smaugfm.monobudget"
val version: String by project

val jdkVersion = "11"
val ktor = "2.3.6"
val junit = "5.10.1"
val logback = "1.4.11"
val koin = "3.5.0"
val koinKsp = "1.3.0"
val resilience4jVersion = "1.7.0"
val kotlinxCoroutines = "1.7.3"
val sealedEnum = "0.7.0"

val githubToken: String? by project

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation("io.github.smaugfm:monobank:0.0.2")
    implementation("io.github.smaugfm:lunchmoney:1.0.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:$kotlinxCoroutines")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutines")
    implementation("com.github.livefront.sealed-enum:runtime:$sealedEnum")
    ksp("com.github.livefront.sealed-enum:ksp:$sealedEnum")
    implementation("com.uchuhimo:kotlinx-bimap:1.2")
    implementation("com.github.elbekD:kt-telegram-bot:2.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("de.brudaswen.kotlinx.serialization:kotlinx-serialization-csv:2.0.0")
    implementation("com.charleskorn.kaml:kaml:0.55.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")
    implementation("io.github.oshai:kotlin-logging:5.1.0")
    implementation("com.google.code.gson:gson:2.10.1")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.27.0")
    implementation("io.insert-koin:koin-core:$koin")
    implementation("io.insert-koin:koin-ktor:$koin")
    testImplementation("io.insert-koin:koin-test-junit5:$koin")
    implementation("io.insert-koin:koin-annotations:$koinKsp")
    ksp("io.insert-koin:koin-ksp-compiler:$koinKsp")
    implementation("io.github.resilience4j:resilience4j-retry:$resilience4jVersion")
    implementation("io.github.resilience4j:resilience4j-reactor:$resilience4jVersion")
    implementation("io.github.resilience4j:resilience4j-kotlin:$resilience4jVersion")
    implementation("io.ktor:ktor-server-core:$ktor")
    implementation("io.ktor:ktor-server-netty:$ktor")
    implementation("io.ktor:ktor-client-core:$ktor")
    implementation("io.ktor:ktor-client-cio:$ktor")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor")
    testImplementation("org.junit.jupiter:junit-jupiter:$junit")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junit")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junit")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("ch.qos.logback:logback-core:$logback")
    implementation("ch.qos.logback:logback-classic:$logback")

    if (Os.isArch("aarch64")) {
        runtimeOnly("io.netty:netty-resolver-dns-native-macos:4.1.90.Final:osx-aarch_64")
    }
}

ktlint {
    version.set("1.0.1")
    enableExperimentalRules.set(true)
    filter {
        exclude("**/generated/**")
    }
    reporters {
        reporter(ReporterType.HTML)
    }
}

detekt {
    config.setFrom(rootDir.resolve("detekt.yml"))
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
        compilerOptions.freeCompilerArgs.addAll(
            "-opt-in=kotlinx.serialization.ExperimentalSerializationApi",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
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
