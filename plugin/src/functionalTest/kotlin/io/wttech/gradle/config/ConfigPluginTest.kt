package io.wttech.gradle.config

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

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
                id("io.wttech.config")
            }
            
            config {
                define {
                    label("App configuration")
            
                    valueSaveYml()
                    valueSaveJson()
                    valueSaveXml()
                    valueSaveProperties()
                    // valueSaveGradleProperties()
            
                    group("server") {
                        description("Server settings")
                        prop("serverDistUrl") {
                            description("Server distribution URL")
                            default("https://my-company.com/servers/dist/server-1.0.0.jar")
                        }
                    }
                }
            }
            
            tasks {
                register("printServerDistUrl") {
                    doLast {
                        println(config["serverDistUrl"])
                    }
                }
            }
            """.trimIndent())

        /* TODO fix it
        runBuild("printAemInstancePassword").apply {
            assertContains(output, "kp-pass")
        }
        */
        with(runBuild("printServerDistUrl")) {
            assertEquals(TaskOutcome.SUCCESS, task(":printServerDistUrl")?.outcome)
            assertContains(output, "https://my-company.com/servers/dist/server-1.0.0.jar")
        }

        with(runBuild("config", "--defaults")) {
            assertEquals(TaskOutcome.SUCCESS, task(":config")?.outcome)
        }
    }
}
