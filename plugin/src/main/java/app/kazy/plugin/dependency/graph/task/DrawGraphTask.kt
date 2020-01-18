package app.kazy.plugin.dependency.graph.task

import groovy.lang.Tuple2
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.ProjectDependency
import java.io.File

object DrawGraphTask {
    fun register(project: Project): Task {
        return project.task("drawGraph").doLast {
            val rootProject = project.rootProject
            val dot = File(rootProject.buildDir, "reports/graph/project.dot")
            dot.parentFile.mkdirs()
            dot.delete()
            dot.appendText(
                """
               digraph { 
                 graph [label="${rootProject.name}",labelloc=t,fontsize=30,ranksep=1.4];
                 node [style=filled, fillcolor="#bbbbbb"];
                 rankdir=TB;
            """.trimIndent()
            )
            val rootProjects = mutableListOf<Project>()
            var queue = mutableListOf<Project>(rootProject)
            while (queue.isNotEmpty()) {
                val project = queue.removeAt(0)
                rootProjects.add(project)
                queue.addAll(project.childProjects.values)
            }
            var projects = LinkedHashSet<Project>()
            val dependencies = LinkedHashMap<Tuple2<Project, Project>, List<String>>()
            val multiplatformProjects = mutableListOf<Project>()
            val jsProjects = mutableListOf<Project>()
            val androidProjects = mutableListOf<Project>()
            val javaProjects = mutableListOf<Project>()

            queue = mutableListOf(rootProject)
            while (queue.isNotEmpty()) {
                val project = queue.removeAt(0)
                queue.addAll(project.childProjects.values)
                if (project.plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")) {
                    multiplatformProjects.add(project)
                }
                if (project.plugins.hasPlugin("kotlin2js")) {
                    jsProjects.add(project)
                }
                if (project.plugins.hasPlugin("com.android.library") || project.plugins.hasPlugin("com.android.application")) {
                    androidProjects.add(project)
                }
                if (project.plugins.hasPlugin("java-library") || project.plugins.hasPlugin("java")) {
                    javaProjects.add(project)
                }

                project.configurations.all { config ->
                    config.dependencies
                        .withType(ProjectDependency::class.java)
                        .map { it.dependencyProject }
                        .forEach { dependency ->
                            projects.add(project)
                            projects.add(dependency)
                            rootProjects.remove(dependency)
                            val graphKey = Tuple2<Project, Project>(project, dependency)
                            val traits =
                                dependencies
                                    .computeIfAbsent(graphKey) { ArrayList() }
                                    .toMutableList()
                            if (config.name.toLowerCase().endsWith("implementation")) {
                                traits.add("style=dotted")
                            }
                        }
                }
            }
            projects = LinkedHashSet(projects.sortedBy { it.path })

            dot.appendText("\n  # Projects\n\n")
            projects.forEach { project ->
                val traits = mutableListOf<String>()
                if (rootProjects.contains(project)) {
                    traits.add("shape=box")
                }

                when {
                    multiplatformProjects.contains(project) -> {
                        traits.add("fillcolor=\"#ffd2b3\"")
                    }
                    jsProjects.contains(project) -> {
                        traits.add("fillcolor=\"#ffffba\"")
                    }
                    androidProjects.contains(project) -> {
                        traits.add("fillcolor=\"#baffc9\"")
                    }
                    javaProjects.contains(project) -> {
                        traits.add("fillcolor=\"#ffb3ba\"")
                    }
                    else -> {
                        traits.add("fillcolor=\"#eeeeee\"")
                    }
                }
                dot.appendText("  \"${project.path}\" [${traits.joinToString(", ")}];\n")
            }
            dot.appendText("\n  {rank = same;")
            projects.forEach { project ->
                if (rootProjects.contains(project)) {
                    dot.appendText(" \"${project.path}\";")
                }
            }
            dot.appendText("}\n")
            dot.appendText("\n  # Dependencies\n\n")
            dependencies.forEach { key, traits ->
                dot.appendText("  \"${key.first.path}\" -> \"${key.second.path}\"")
                if (traits.isNotEmpty()) {
                    dot.appendText(" [${traits.joinToString(", ")}]")
                }
                dot.appendText("\n")
            }
            dot.appendText("}\n")
            println("Project module dependency graph created at ${dot.absolutePath}")
//            val p = "dot -Tpng -O project.dot".execute([], dot.parentFile)

        }
    }
}