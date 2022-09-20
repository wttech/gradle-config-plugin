package com.wttech.gradle.config

import java.io.File
import java.nio.file.Files
import kotlin.test.assertTrue
import kotlin.test.Test
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class ConfigPluginTest {

    @get:Rule val tempFolder = TemporaryFolder()

    private fun getProjectDir() = tempFolder.root

    private fun getBuildFile() = getProjectDir().resolve("build.gradle.kts")

    private fun getSettingsFile() = getProjectDir().resolve("settings.gradle.kts")

    @Test fun `can run task`() {
        // Setup the test build
        getSettingsFile().writeText("")
        getBuildFile().writeText("""
        plugins {
            id("com.wttech.config")
        }
        """.trimIndent())

        // Run the build
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("greeting")
        runner.withProjectDir(getProjectDir())
        val result = runner.build();

        // Verify the result
        assertTrue(result.output.contains("Hello from plugin 'com.wttech.config'"))
    }
}
