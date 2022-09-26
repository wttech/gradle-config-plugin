package com.wttech.gradle.config

import com.wttech.gradle.config.tasks.Config
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.*

class GradleConfigTest {

    @Test
    fun `plugin registers task`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("com.wttech.config")

        assertTrue(project.tasks.withType(Config::class.java).isEmpty(), "There should be no config tasks by default")

        val extension = project.extensions.getByType(ConfigExtension::class.java)
        extension.define {
            group("general") {
                prop("testBaseUrl") {
                    value("http://localhost:8080")
                }
            }
        }

        val task = project.tasks.named("config", Config::class.java)
        assertTrue(task.isPresent)

        val def = task.get().definition.get()
        assertEquals(InputMode.GUI, def.inputMode.get())
        assertFalse(def.debug.get())
    }
}
