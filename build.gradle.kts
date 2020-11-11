import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.star-zero.gradle.githook")
    id("com.github.johnrengelman.shadow")
    id("org.jlleitschuh.gradle.ktlint")
    id("io.gitlab.arturbosch.detekt")
}

group = "com.github.smaugfm"
version = "0.2.1-alpha"

val myMavenRepoReadUrl: String by project
val myMavenRepoReadUsername: String by project
val myMavenRepoReadPassword: String by project

repositories {
    maven(url = "https://jitpack.io")
    mavenCentral()
    jcenter()
    maven(url = "https://kotlin.bintray.com/kotlinx")
    maven(url = myMavenRepoReadUrl) {
        credentials {
            username = myMavenRepoReadUsername
            password = myMavenRepoReadPassword
        }
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
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.time.ExperimentalTime"
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
    implementation("io.michaelrocks:bimap:_")
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

    testImplementation("io.mockk:mockk:_")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:_")
    testImplementation("org.junit.jupiter:junit-jupiter-api:_")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:_")
}
