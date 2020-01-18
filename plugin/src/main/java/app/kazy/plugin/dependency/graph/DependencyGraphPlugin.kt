package app.kazy.plugin.dependency.graph

import app.kazy.plugin.dependency.graph.task.DrawGraphTask
import org.gradle.api.Plugin
import org.gradle.api.Project

class DependencyGraphPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        DrawGraphTask.register(target)
    }
}
