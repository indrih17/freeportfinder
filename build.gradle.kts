import com.novoda.gradle.release.PublishExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.konan.properties.Properties

buildscript {
    repositories {
        jcenter()
    }

    dependencies {
        classpath("com.novoda:bintray-release:0.9.1")
    }
}

apply(plugin = "com.novoda.bintray-release")

plugins {
    kotlin("jvm") version "1.3.50"
}

repositories {
    mavenCentral()
    jcenter()
    maven("https://jitpack.io")
    maven("https://plugins.gradle.org/m2/")
}

dependencies {
    compile(kotlin("stdlib-jdk8"))
    compile("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.3.1")
    compile("io.arrow-kt", "arrow-core", "0.8.2")

    testCompile("org.junit.jupiter", "junit-jupiter", "5.5.2")
    testCompile("org.hamcrest", "hamcrest-library", "2.1")
}

configure<PublishExtension> {
    userOrg = "indrih17"
    repoName = "free-port-finder"
    groupId = "indrih17.free-port-finder"
    artifactId = "free-port-finder"
    publishVersion = "1.0.0"
    desc =
        "A micro jvm library that does one thing and one thing only: finds a free local port (mainly) for testing purposes."
    website = "https://github.com/indrih17/freeportfinder"

    val properties = Properties()
    properties.load(project.rootProject.file("local.properties").inputStream())

    bintrayUser = properties.getProperty("BINTRAY_USERNAME")
    bintrayKey = properties.getProperty("BINTRAY_API_KEY")

    dryRun = false // true - не загружается, false - загружается
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}