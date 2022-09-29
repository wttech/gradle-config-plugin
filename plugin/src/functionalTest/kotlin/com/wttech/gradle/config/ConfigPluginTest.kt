package com.wttech.gradle.config

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.Test
import kotlin.test.assertContains

@Suppress("MaxLineLength", "LongMethod")
class ConfigPluginTest {

    @get:Rule val tempFolder = TemporaryFolder()

    private val tempDir get() = tempFolder.root

    private val buildFile get() = tempDir.resolve("build.gradle.kts")

    private val settingsFile get() = tempDir.resolve("settings.gradle.kts")

    private fun runBuild(vararg args: String) = GradleRunner.create().run {
        forwardOutput()
        withPluginClasspath()
        withArguments(*args)
        withProjectDir(tempDir)
        build()
    }

    @Test
    fun `can run task`() {
        settingsFile.writeText("")
        buildFile.writeText("""
            plugins {
                id("com.wttech.config")
            }
            
            config {
                define {
                    label("GAT configuration")
            
                    valueSaveYml()
                    valueSaveJson()
                    valueSaveXml()
                    valueSaveProperties()
                    // valueSaveGradleProperties()
            
                    labelAbbrs("aem")
            
                    group("general") {
                        description("Infrastructure and environment type selection")
                        prop("infra") {
                            value("aws")
                            options("local", "aws", "gcp", "az", "vagrant")
                        }
                        prop("envType") {
                            options("afe_single", "aem_single", "aem_multi")
                            visible { otherValue("infra") !in listOf("local", "vagrant")}
                            validate { "Not supported on selected infra".takeIf { groups.get().none { it.name == "remote-${'$'}{otherValue("infra")}_${'$'}{value()}" } } }
                        }
                        const("domain") { "gat-${'$'}{value("infra")}.wttech.cloud" }
                    }
                    group("local") {
                        label("Local Env")
                        description("Environment set up directly on current machine")
                        visible { value("infra") == name }
            
                        prop("monitoringEnabled") {
                            checkbox()
                        }
                    }
                    group("remote-aws_afe_single") {
                        label("Remote Env")
                        description("Dedicated env for AFE app deployed on AWS infra")
                        visible { name == "remote-${'$'}{value("infra")}_${'$'}{value("envType")}" }
            
                        prop("env") {
                            value("kp")
                            alphanumeric()
                        }
                        prop("envMode") {
                            options("dev", "stg", "prod")
                            description("Controls AEM run mode")
                            enabled { otherStringValue("env") == "kp" }
                        }
                        prop("aemInstancePassword") {
                            valueDynamic { otherStringValue("env")?.takeIf { it.isNotBlank() }?.let { "${'$'}it-pass" } }
                            description("Needed to access AEM admin (author & publish)")
                            required()
                        }
                        prop("aemProxyPassword") {
                            value("admin")
                            description("Needed to access AEM dispatcher pages")
                            required()
                        }
                        listProp("aemPackages") {
                            values("a", "b", "c")
                        }
                    }
                    group("app") {
                        description("Application build settings")
                        prop("mavenArgs") {
                            value("-DskipTests")
                        }
                        prop("packageManagerDeployAvoidance") {
                            description("When package is unchanged do not upload & install it again")
                            checkbox()
                        }
                    }
                    group("test") {
                        description("Automated tests execution settings")
                        prop("percyToken")
                        prop("percyEnabled") {
                            checkbox()
                        }
                        const("testBaseUrl") {
                            when (stringValue("infra")) {
                                "local" -> "https://publish.local.gat.com"
                                else -> "https://${'$'}{value("env")}.${'$'}{value("domain")}"
                            }
                        }
                    }
                }
            }
            
            tasks {
                register("printAemInstancePassword") {
                    doLast {
                        println(config["aemInstancePassword"])
                    }
                }
            }
            """.trimIndent())

        val result = runBuild("printAemInstancePassword")
        assertContains(result.output, "kp-pass")
    }
}
