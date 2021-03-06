/*
 * Copyright (c) 2020 Bolt Technology OÜ. All rights reserved.
 */

package eu.bolt.gradle.requiresannotation.plugin

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import org.gradle.api.tasks.compile.JavaCompile

class RequiresAnnotationProcessorPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        if (project.plugins.hasPlugin(RequiresAnnotationProcessorPlugin.class)) {
            return
        }

        // Make sure the project is either an Android application or library
        def isAndroidApp = project.plugins.withType(AppPlugin)
        def isAndroidLib = project.plugins.withType(LibraryPlugin)
        if (!isAndroidApp && !isAndroidLib) {
            throw new GradleException("'com.android.application' or 'com.android.library' plugin required.")
        }

        project.extensions.create(RequiresAnnotationProcessorPluginExtension.NAME, RequiresAnnotationProcessorPluginExtension)

        project.tasks.withType(JavaCompile) {
            // this option break the build stage: :kaptGenerateStubsDebugKotlin
//            options.compilerArgs << "-proc:only"
            def requiresOptions = project.requiresAnnotationProcessor.requires
            if (requiresOptions) {
                requiresOptions.each { key, value ->
                    options.compilerArgs << "-Arequires_${key}=${value.join("_")}"
                }
            }

            def ignoreOptions = project.requiresAnnotationProcessor.ignore
            if (ignoreOptions) {
                options.compilerArgs << "-Aignore=${ignoreOptions.join("_")}"
            }
        }
    }
}
