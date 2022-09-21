package com.wttech.gradle.config

import kotlin.test.assertTrue
import kotlin.test.Test
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder

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
            task {
                group("general") {
                    prop("infra") {
                    
                    }
                    prop("envType") {
                    
                    }
                }
                group("aws_afe_single") {
                    visible { name == (value("infra") + value("envType")) }
                    
                    prop("env") {
                    
                    }
                    prop("envMode") {
                    
                    }
                    prop("aemInstancePassword") {
                    
                    }
                    prop("aemProxyPassword") {
                    
                    }
                }
            }
        }
        """.trimIndent())

        val result = runBuild("greeting")
        assertTrue(result.output.contains("Hello from plugin 'com.wttech.config'"))
    }
}
