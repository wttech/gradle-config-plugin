import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-gradle-plugin`
    `maven-publish`

    id("com.gradle.plugin-publish") version "1.0.0"
    id("org.jetbrains.kotlin.jvm") version "1.6.21"
    id("io.gitlab.arturbosch.detekt") version "1.21.0"
    id("net.researchgate.release") version "3.0.2"
    id("com.github.breadmoirai.github-release") version "2.4.1"
}

group = "com.wttech.gradle.config"
description = "Gradle Config Plugin"

repositories {
    mavenCentral()
}

dependencies {
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.20.0")

    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.yaml:snakeyaml:1.32")
    implementation("io.pebbletemplates:pebble:3.1.5")
    implementation("com.miglayout:miglayout:3.7.4")
    implementation("com.jgoodies:jgoodies-binding:2.13.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.4")
    implementation("com.formdev:flatlaf:2.4")
}

java {
    withJavadocJar()
    withSourcesJar()
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useKotlinTest()
        }

        val functionalTest by registering(JvmTestSuite::class) {
            useKotlinTest()
            dependencies {
                implementation(project)
            }
            targets {
                all {
                    testTask.configure { shouldRunAfter(test) }
                }
            }
        }
    }
}

detekt {
    config.from(rootProject.file("detekt.yml"))
    parallel = true
    autoCorrect = true
}

tasks {
    withType<JavaCompile>().configureEach {
        sourceCompatibility = JavaVersion.VERSION_1_8.toString()
        targetCompatibility = JavaVersion.VERSION_1_8.toString()
    }
    withType<KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = JavaVersion.VERSION_1_8.toString()
            freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
        }
    }
    withType<Test>().configureEach {
        testLogging.showStandardStreams = true
    }
    withType<Detekt>().configureEach {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
    test {
        dependsOn(detektTest)
    }
    check {
        dependsOn(testing.suites.named("functionalTest"))
    }
    publishToMavenLocal {
        dependsOn(jar)
    }
    named("functionalTest") {
        dependsOn(jar, "detektFunctionalTest")
    }
    afterReleaseBuild {
        dependsOn(publishPlugins)
    }
    named("githubRelease") {
        mustRunAfter(release)
    }
    register("fullRelease") {
        dependsOn("release", "githubRelease")
    }
}

gradlePlugin {
    plugins {
        create("config") {
            id = "com.wttech.config"
            implementationClass = "com.wttech.gradle.config.ConfigPlugin"
            displayName = "Config Plugin"
            description = "Organizes and captures configurable input values to your Gradle builds using interactive inputs (GUI/CLI)"
        }
    }
}

gradlePlugin.testSourceSets(sourceSets["functionalTest"])

tasks.named<Task>("check") {
    dependsOn(testing.suites.named("functionalTest"))
}

pluginBundle {
    website = "https://github.com/wttech/gradle-config-plugin"
    vcsUrl = "https://github.com/wttech/gradle-config-plugin.git"
    description = "Gradle Config Plugin"
    tags = listOf("config", "cli", "gui", "input", "yml", "json", "settings")
}

githubRelease {
    owner("wttech")
    repo("gradle-config-plugin")
    token((findProperty("github.token") ?: "").toString())
    tagName(project.version.toString())
    releaseName(project.version.toString())
    draft((findProperty("github.draft") ?: "false").toString().toBoolean())
    overwrite((findProperty("github.override") ?: "true").toString().toBoolean())

    gradle.projectsEvaluated {
        releaseAssets(listOf("jar").map { tasks.named(it) })
    }

    if ((findProperty("github.prerelease") ?: "true").toString().toBoolean()) {
        prerelease(true)
    } else {
        body { """
        |# What's new
        |
        |TBD
        |
        |# Upgrade notes
        |
        |Nothing to do.
        |
        |# Contributions
        |
        |None.
        """.trimMargin()
        }
    }
}