plugins {
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish") version "0.20.0"
    id("org.jetbrains.kotlin.jvm") version "1.6.21"
}

group = "com.wttech.gradle.config"

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.yaml:snakeyaml:1.32")
    implementation("com.miglayout:miglayout:3.7.4")
    implementation("com.jgoodies:jgoodies-binding:2.13.0")

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

gradlePlugin {
    val greeting by plugins.creating {
        id = "com.wttech.config"
        implementationClass = "com.wttech.gradle.config.ConfigPlugin"
    }
}

gradlePlugin.testSourceSets(sourceSets["functionalTest"])

tasks.named<Task>("check") {
    dependsOn(testing.suites.named("functionalTest"))
}

pluginBundle {
    website = "https://github.com/wttech/gradle-aem-plugin"
    vcsUrl = "https://github.com/wttech/gradle-aem-plugin.git"
    description = "Gradle AEM Plugin"
    tags = listOf("aem", "cq", "vault", "scr")
}
