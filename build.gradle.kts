plugins {
    id("net.researchgate.release") version "3.0.2"
    id("com.github.breadmoirai.github-release") version "2.4.1"
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
        releaseAssets(project(":plugin").tasks.named("jar"))
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

defaultTasks(":plugin:publishToMavenLocal")

tasks {
    afterReleaseBuild {
        dependsOn(":plugin:publishPlugins")
    }
    named("release") {
        dependsOn(":plugin:build", ":plugin:functionalTest")
    }
    named("githubRelease") {
        mustRunAfter(":release")
    }
    register("fullRelease") {
        dependsOn(":release", ":githubRelease")
    }
}